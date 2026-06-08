package com.hedera.tutorial;

import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.SubscriptionHandle;
import com.hedera.hashgraph.sdk.TopicId;
import org.hiero.base.HieroContext;
import org.hiero.base.HieroException;
import org.hiero.base.TopicClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class CreateTopicEnterpriseRunner implements CommandLineRunner {
    private final TopicClient topicClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public CreateTopicEnterpriseRunner(
            TopicClient topicClient,
            HieroContext hieroContext,
            ConfigurableApplicationContext context) {
        this.topicClient = topicClient;
        this.hieroContext = hieroContext;
        this.context = context;
    }

    @Override
    public void run(String... args) throws HieroException, InterruptedException {
        PrivateKey operatorKey = hieroContext.getOperatorAccount().privateKey();

        TopicId topicId = topicClient.createPrivateTopic(operatorKey);
        System.out.println("Your topic ID is: " + topicId);

        Thread.sleep(5000);

        SubscriptionHandle subscription = topicClient.subscribeTopic(
                topicId,
                message -> {
                    String messageAsString = new String(message.contents, StandardCharsets.UTF_8);
                    System.out.println(
                            message.consensusTimestamp + " received topic message: " + messageAsString);
                });

        topicClient.submitMessage(topicId, operatorKey, "Submitkey set!");

        Thread.sleep(30000);
        subscription.unsubscribe();

        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
