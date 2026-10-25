package com.hedera.tutorial.topic;

import static com.hedera.tutorial.util.TestnetQueryHelper.logTopicInfo;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicInfo;
import java.time.Duration;
import java.time.Instant;
import org.hiero.base.HieroContext;
import org.hiero.base.HieroException;
import org.hiero.base.TopicClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Enterprise tutorial for {@link TopicClient#createImmutableTopic} — creates a topic without an
 * admin key.
 *
 * <p>A topic without an admin key can never be updated or deleted. Anyone can still submit
 * messages (if there is no submit key) and extend its expiration time with {@link
 * TopicClient#updateExpirationTime}, which needs no admin key. {@link TopicClient} has no topic
 * info query yet, so the state is read with the SDK via {@link HieroContext#getClient()}.
 *
 * @see <a href="https://docs.hedera.com/native/consensus/create-topic">Create a topic</a>
 * @see <a href="https://docs.hedera.com/native/consensus/update-topic">Update a topic</a>
 */
@Component
@Profile("immutable-topic")
public class ImmutableTopicEnterpriseRunner implements CommandLineRunner {

    private final TopicClient topicClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public ImmutableTopicEnterpriseRunner(
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

        // Enterprise: create a topic without an admin key.
        TopicId topicId = topicClient.createImmutableTopic("Immutable demo topic");
        System.out.println("Created immutable topic: " + topicId);
        TopicInfo created = logTopicInfo("AFTER CREATE", client, topicId);

        // Enterprise: messages can still be submitted (no submit key, so the topic is public).
        topicClient.submitMessage(topicId, "Hello immutable topic");
        System.out.println("\nSubmitted message via TopicClient.submitMessage()");

        // Enterprise: updating the memo fails because there is no admin key to sign.
        try {
            topicClient.updateTopic(topicId, "Updated memo");
            System.out.println("Unexpected: update succeeded on an immutable topic");
        } catch (HieroException e) {
            System.out.println("Update failed as expected: " + e.getMessage());
        }

        // Enterprise: deleting fails for the same reason.
        try {
            topicClient.deleteTopic(topicId);
            System.out.println("Unexpected: delete succeeded on an immutable topic");
        } catch (HieroException e) {
            System.out.println("Delete failed as expected: " + e.getMessage());
        }

        // Enterprise: extending the expiration time needs no admin key.
        Instant newExpiration = created.expirationTime.plus(Duration.ofDays(1));
        topicClient.updateExpirationTime(topicId, newExpiration);
        System.out.println("\nExtended expiration via TopicClient.updateExpirationTime()");
        logTopicInfo("AFTER EXTENDING EXPIRATION", client, topicId);

        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
