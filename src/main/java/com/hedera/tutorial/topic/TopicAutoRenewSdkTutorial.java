package com.hedera.tutorial.topic;

import static com.hedera.tutorial.util.TestnetQueryHelper.logTopicInfo;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.time.Duration;
import java.time.Instant;

/**
 * SDK tutorial for topic auto-renew settings and expiration — {@link TopicCreateTransaction} and
 * {@link TopicUpdateTransaction}.
 *
 * <p>The auto-renewal account pays to extend the topic when it expires, so it must sign any
 * transaction that sets it. The auto-renewal period must be between roughly 30 and 92 days. Changing
 * auto-renew settings requires the admin key; extending the expiration time does not.
 *
 * @see <a href="https://docs.hedera.com/native/consensus/create-topic">Create a topic</a>
 * @see <a href="https://docs.hedera.com/native/consensus/update-topic">Update a topic</a>
 */
public class TopicAutoRenewSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

        PrivateKey adminKey = operatorKey;

        // Step 1: create an account that will pay for auto renewal.
        PrivateKey renewKey = PrivateKey.generateED25519();
        AccountId renewAccountId = createAccount(client, renewKey);
        System.out.println("Created auto renew account: " + renewAccountId);

        // Step 2: create a topic with that auto renew account and a 60 day auto renew period.
        TopicId topicId =
                new TopicCreateTransaction()
                        .setTopicMemo("Auto renew demo topic")
                        .setAdminKey(adminKey.getPublicKey())
                        .setAutoRenewAccountId(renewAccountId)
                        .setAutoRenewPeriod(Duration.ofDays(60))
                        .freezeWith(client)
                        .sign(adminKey)
                        .sign(renewKey)
                        .execute(client)
                        .getReceipt(client)
                        .topicId;

        if (topicId == null) {
            throw new IllegalStateException("Topic create receipt did not contain a topic ID");
        }
        System.out.println("Created topic: " + topicId);
        logTopicInfo("AFTER CREATE", client, topicId);

        // Step 3: change the auto renew period.
        new TopicUpdateTransaction()
                .setTopicId(topicId)
                .setAutoRenewPeriod(Duration.ofDays(75))
                .freezeWith(client)
                .sign(adminKey)
                .execute(client)
                .getReceipt(client);
        TopicInfo afterPeriod = logTopicInfo("AFTER UPDATING AUTO RENEW PERIOD", client, topicId);

        // Step 4: extend the expiration time (no admin key required).
        Instant newExpiration = afterPeriod.expirationTime.plus(Duration.ofDays(10));
        new TopicUpdateTransaction()
                .setTopicId(topicId)
                .setExpirationTime(newExpiration)
                .execute(client)
                .getReceipt(client);
        logTopicInfo("AFTER EXTENDING EXPIRATION", client, topicId);

        // Step 5: switch to a different auto renew account; the new account must sign.
        PrivateKey newRenewKey = PrivateKey.generateED25519();
        AccountId newRenewAccountId = createAccount(client, newRenewKey);
        new TopicUpdateTransaction()
                .setTopicId(topicId)
                .setAutoRenewAccountId(newRenewAccountId)
                .freezeWith(client)
                .sign(adminKey)
                .sign(newRenewKey)
                .execute(client)
                .getReceipt(client);
        logTopicInfo("AFTER CHANGING AUTO RENEW ACCOUNT", client, topicId);

        // Step 6: remove the auto renew account.
        Status clearStatus =
                new TopicUpdateTransaction()
                        .setTopicId(topicId)
                        .clearAutoRenewAccountId()
                        .freezeWith(client)
                        .sign(adminKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println("\nClear auto renew account: " + clearStatus);
        logTopicInfo("AFTER CLEARING AUTO RENEW ACCOUNT", client, topicId);

        client.close();
    }

    private static AccountId createAccount(Client client, PrivateKey key) throws Exception {
        AccountId accountId =
                new AccountCreateTransaction()
                        .setKey(key)
                        .setInitialBalance(Hbar.from(1))
                        .execute(client)
                        .getReceipt(client)
                        .accountId;
        if (accountId == null) {
            throw new IllegalStateException("Account create receipt did not contain an account ID");
        }
        return accountId;
    }
}
