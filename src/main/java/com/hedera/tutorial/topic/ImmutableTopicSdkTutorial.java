package com.hedera.tutorial.topic;

import static com.hedera.tutorial.util.TestnetQueryHelper.logTopicInfo;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.time.Duration;
import java.time.Instant;

/**
 * SDK tutorial for an immutable topic — a {@link TopicCreateTransaction} without an admin key.
 *
 * <p>A topic without an admin key can never be updated or deleted. Anyone can still submit
 * messages (if there is no submit key) and anyone can extend its expiration time with a {@link
 * TopicUpdateTransaction} that only sets the expiration time.
 *
 * @see <a href="https://docs.hedera.com/native/consensus/create-topic">Create a topic</a>
 * @see <a href="https://docs.hedera.com/native/consensus/update-topic">Update a topic</a>
 */
public class ImmutableTopicSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

        // Step 1: create a topic without an admin key.
        TopicId topicId =
                new TopicCreateTransaction()
                        .setTopicMemo("Immutable demo topic")
                        .execute(client)
                        .getReceipt(client)
                        .topicId;

        if (topicId == null) {
            throw new IllegalStateException("Topic create receipt did not contain a topic ID");
        }
        System.out.println("Created immutable topic: " + topicId);
        TopicInfo created = logTopicInfo("AFTER CREATE", client, topicId);

        // Step 2: messages can still be submitted (no submit key, so the topic is public).
        Status submitStatus =
                new TopicMessageSubmitTransaction()
                        .setTopicId(topicId)
                        .setMessage("Hello immutable topic")
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println("\nSubmit message: " + submitStatus);

        // Step 3: updating the memo fails because there is no admin key to sign.
        try {
            new TopicUpdateTransaction()
                    .setTopicId(topicId)
                    .setTopicMemo("Updated memo")
                    .freezeWith(client)
                    .sign(operatorKey)
                    .execute(client)
                    .getReceipt(client);
            System.out.println("Unexpected: update succeeded on an immutable topic");
        } catch (Exception e) {
            System.out.println("Update failed as expected: " + e.getMessage());
        }

        // Step 4: deleting fails for the same reason.
        try {
            new TopicDeleteTransaction()
                    .setTopicId(topicId)
                    .freezeWith(client)
                    .sign(operatorKey)
                    .execute(client)
                    .getReceipt(client);
            System.out.println("Unexpected: delete succeeded on an immutable topic");
        } catch (Exception e) {
            System.out.println("Delete failed as expected: " + e.getMessage());
        }

        // Step 5: extending the expiration time needs no admin key.
        Instant newExpiration = created.expirationTime.plus(Duration.ofDays(1));
        Status extendStatus =
                new TopicUpdateTransaction()
                        .setTopicId(topicId)
                        .setExpirationTime(newExpiration)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println("\nExtend expiration: " + extendStatus);
        logTopicInfo("AFTER EXTENDING EXPIRATION", client, topicId);

        client.close();
    }
}
