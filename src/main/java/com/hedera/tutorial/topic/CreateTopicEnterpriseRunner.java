package com.hedera.tutorial.topic;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.SubscriptionHandle;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessageQuery;
import com.hedera.tutorial.util.TutorialClient;
import org.hiero.base.HieroContext;
import org.hiero.base.HieroException;
import org.hiero.base.TopicClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Profile("topic")
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
    public void run(String... args) throws Exception {
        PrivateKey operatorKey = hieroContext.getOperatorAccount().privateKey();

        TopicId topicId = topicClient.createPrivateTopic(operatorKey);
        System.out.println("Your topic ID is: " + topicId);

        Thread.sleep(5000);

        // TopicClient has no subscribe API, so the subscription uses the SDK directly. The client
        // from HieroContext cannot be reused here: its mirror network holds the REST URL
        // (https://...:443), while a topic subscription needs a mirror gRPC "host:port" target, so
        // it is built the same way as in the SDK tutorial.
        Client subscriptionClient = TutorialClient.forConfiguredNetwork();
        subscriptionClient.setOperator(
                hieroContext.getOperatorAccount().accountId(), operatorKey);

        SubscriptionHandle subscription =
                new TopicMessageQuery()
                        .setTopicId(topicId)
                        .subscribe(
                                subscriptionClient,
                                message -> {
                                    String messageAsString =
                                            new String(message.contents, StandardCharsets.UTF_8);
                                    System.out.println(
                                            message.consensusTimestamp
                                                    + " received topic message: "
                                                    + messageAsString);
                                });

        topicClient.submitMessage(topicId, operatorKey, "Submitkey set!");

        Thread.sleep(30000);
        subscription.unsubscribe();
        subscriptionClient.close();

        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
