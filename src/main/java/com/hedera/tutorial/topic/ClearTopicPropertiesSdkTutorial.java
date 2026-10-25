package com.hedera.tutorial.topic;

import static com.hedera.tutorial.util.TestnetQueryHelper.logTopicInfo;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * SDK tutorial for clearing topic properties with {@link TopicUpdateTransaction#clearSubmitKey()},
 * {@link TopicUpdateTransaction#clearTopicMemo()} and {@link TopicUpdateTransaction#clearAdminKey()}.
 *
 * <p>Clearing the submit key makes a private topic public. Clearing the admin key makes the topic
 * immutable, which cannot be undone. Every change must be signed by the current admin key.
 *
 * @see <a href="https://docs.hedera.com/native/consensus/update-topic">Update a topic</a>
 */
public class ClearTopicPropertiesSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

        PrivateKey adminKey = operatorKey;
        PrivateKey submitKey = PrivateKey.generateED25519();

        // Step 1: create a private topic with an admin key, submit key and memo.
        TopicId topicId =
                new TopicCreateTransaction()
                        .setTopicMemo("Clear properties demo topic")
                        .setAdminKey(adminKey.getPublicKey())
                        .setSubmitKey(submitKey.getPublicKey())
                        .freezeWith(client)
                        .sign(adminKey)
                        .execute(client)
                        .getReceipt(client)
                        .topicId;

        if (topicId == null) {
            throw new IllegalStateException("Topic create receipt did not contain a topic ID");
        }
        System.out.println("Created private topic: " + topicId);
        logTopicInfo("AFTER CREATE", client, topicId);

        // Step 2: submitting without the submit key fails on a private topic.
        try {
            submitMessage(client, topicId, "Message without submit key");
            System.out.println("Unexpected: submit without submit key succeeded");
        } catch (Exception e) {
            System.out.println("\nSubmit without submit key failed as expected: " + e.getMessage());
        }

        // Step 3: clear the submit key; the topic becomes public.
        new TopicUpdateTransaction()
                .setTopicId(topicId)
                .clearSubmitKey()
                .freezeWith(client)
                .sign(adminKey)
                .execute(client)
                .getReceipt(client);
        logTopicInfo("AFTER CLEARING SUBMIT KEY", client, topicId);
        System.out.println(
                "Submit without submit key: " + submitMessage(client, topicId, "Now anyone can submit"));

        // Step 4: clear the memo.
        new TopicUpdateTransaction()
                .setTopicId(topicId)
                .clearTopicMemo()
                .freezeWith(client)
                .sign(adminKey)
                .execute(client)
                .getReceipt(client);
        logTopicInfo("AFTER CLEARING MEMO", client, topicId);

        // Step 5: clear the admin key; the topic becomes immutable. The network rejects removing the
        // admin key while an auto renew account is set (AUTORENEW_ACCOUNT_NOT_ALLOWED), and the SDK
        // sets the payer as auto renew account by default, so clear both in the same transaction.
        new TopicUpdateTransaction()
                .setTopicId(topicId)
                .clearAdminKey()
                .clearAutoRenewAccountId()
                .freezeWith(client)
                .sign(adminKey)
                .execute(client)
                .getReceipt(client);
        logTopicInfo("AFTER CLEARING ADMIN KEY", client, topicId);

        // Step 6: the former admin key can no longer update the topic.
        try {
            new TopicUpdateTransaction()
                    .setTopicId(topicId)
                    .setTopicMemo("Updated memo")
                    .freezeWith(client)
                    .sign(adminKey)
                    .execute(client)
                    .getReceipt(client);
            System.out.println("Unexpected: update succeeded after clearing the admin key");
        } catch (Exception e) {
            System.out.println("\nUpdate after clearing admin key failed as expected: " + e.getMessage());
        }

        client.close();
    }

    private static Status submitMessage(Client client, TopicId topicId, String message) throws Exception {
        return new TopicMessageSubmitTransaction()
                .setTopicId(topicId)
                .setMessage(message)
                .execute(client)
                .getReceipt(client)
                .status;
    }
}
