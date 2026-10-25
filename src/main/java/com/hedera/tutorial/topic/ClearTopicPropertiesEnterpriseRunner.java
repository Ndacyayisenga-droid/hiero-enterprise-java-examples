package com.hedera.tutorial.topic;

import static com.hedera.tutorial.util.TestnetQueryHelper.logTopicInfo;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TopicId;
import org.hiero.base.HieroContext;
import org.hiero.base.HieroException;
import org.hiero.base.TopicClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Enterprise tutorial for {@link TopicClient#clearSubmitKey}, {@link TopicClient#clearTopicMemo} and
 * {@link TopicClient#clearAdminKey}.
 *
 * <p>Clearing the submit key makes a private topic public. Clearing the admin key makes the topic
 * immutable, which cannot be undone. The operator key is used as admin key (the default for {@link
 * TopicClient#createPrivateTopic(PrivateKey, String)}). {@link TopicClient} has no topic info query
 * yet, so the state is read with the SDK via {@link HieroContext#getClient()}.
 *
 * @see <a href="https://docs.hedera.com/native/consensus/update-topic">Update a topic</a>
 */
@Component
@Profile("clear-topic-properties")
public class ClearTopicPropertiesEnterpriseRunner implements CommandLineRunner {

    private final TopicClient topicClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public ClearTopicPropertiesEnterpriseRunner(
            TopicClient topicClient,
            HieroContext hieroContext,
            ConfigurableApplicationContext context) {
        this.topicClient = topicClient;
        this.hieroContext = hieroContext;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        Client client = hieroContext.getClient();
        PrivateKey submitKey = PrivateKey.generateED25519();

        // Enterprise: create a private topic with a submit key and memo (operator is admin).
        TopicId topicId = topicClient.createPrivateTopic(submitKey, "Clear properties demo topic");
        System.out.println("Created private topic: " + topicId);
        logTopicInfo("AFTER CREATE", client, topicId);

        // Enterprise: submitting without the submit key fails on a private topic.
        try {
            topicClient.submitMessage(topicId, "Message without submit key");
            System.out.println("Unexpected: submit without submit key succeeded");
        } catch (HieroException e) {
            System.out.println("\nSubmit without submit key failed as expected: " + e.getMessage());
        }

        // Enterprise: clear the submit key; the topic becomes public.
        topicClient.clearSubmitKey(topicId);
        logTopicInfo("AFTER CLEARING SUBMIT KEY", client, topicId);
        topicClient.submitMessage(topicId, "Now anyone can submit");
        System.out.println("Submit without submit key succeeded");

        // Enterprise: clear the memo.
        topicClient.clearTopicMemo(topicId);
        logTopicInfo("AFTER CLEARING MEMO", client, topicId);

        // Enterprise: clear the admin key; the topic becomes immutable. This also removes the auto
        // renew account, since the network rejects it on a topic without an admin key.
        topicClient.clearAdminKey(topicId);
        logTopicInfo("AFTER CLEARING ADMIN KEY", client, topicId);

        // Enterprise: the former admin key can no longer update the topic.
        try {
            topicClient.updateTopic(topicId, "Updated memo");
            System.out.println("Unexpected: update succeeded after clearing the admin key");
        } catch (HieroException e) {
            System.out.println("\nUpdate after clearing admin key failed as expected: " + e.getMessage());
        }

        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
