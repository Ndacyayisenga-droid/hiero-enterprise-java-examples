package com.hedera.tutorial.topic;

import static com.hedera.tutorial.util.TestnetQueryHelper.logTopicInfo;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicInfo;
import java.time.Duration;
import java.time.Instant;
import org.hiero.base.AccountClient;
import org.hiero.base.HieroContext;
import org.hiero.base.TopicClient;
import org.hiero.base.data.Account;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Enterprise tutorial for topic auto renew settings and expiration — {@link
 * TopicClient#createTopic(PrivateKey, PrivateKey, String, Account, Duration)}, {@link
 * TopicClient#updateAutoRenewPeriod}, {@link TopicClient#updateExpirationTime}, {@link
 * TopicClient#updateAutoRenewAccount} and {@link TopicClient#clearAutoRenewAccount}.
 *
 * <p>The auto renew account is passed as an {@link Account} because it must sign any transaction
 * that sets it. The operator key is used as admin key. {@link TopicClient} has no topic info query
 * yet, so the state is read with the SDK via {@link HieroContext#getClient()}.
 *
 * @see <a href="https://docs.hedera.com/native/consensus/create-topic">Create a topic</a>
 * @see <a href="https://docs.hedera.com/native/consensus/update-topic">Update a topic</a>
 */
@Component
@Profile("topic-auto-renew")
public class TopicAutoRenewEnterpriseRunner implements CommandLineRunner {

    private final TopicClient topicClient;
    private final AccountClient accountClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public TopicAutoRenewEnterpriseRunner(
            TopicClient topicClient,
            AccountClient accountClient,
            HieroContext hieroContext,
            ConfigurableApplicationContext context) {
        this.topicClient = topicClient;
        this.accountClient = accountClient;
        this.hieroContext = hieroContext;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        Client client = hieroContext.getClient();
        PrivateKey adminKey = hieroContext.getOperatorAccount().privateKey();

        // Enterprise: create an account that will pay for auto renewal.
        Account renewAccount = accountClient.createAccount(1);
        System.out.println("Created auto renew account: " + renewAccount.accountId());

        // Enterprise: create a topic with that auto renew account and a 60 day auto renew period.
        TopicId topicId =
                topicClient.createTopic(
                        adminKey, null, "Auto renew demo topic", renewAccount, Duration.ofDays(60));
        System.out.println("Created topic: " + topicId);
        logTopicInfo("AFTER CREATE", client, topicId);

        // Enterprise: change the auto renew period (operator key used as admin key).
        topicClient.updateAutoRenewPeriod(topicId, Duration.ofDays(75));
        TopicInfo afterPeriod = logTopicInfo("AFTER UPDATING AUTO RENEW PERIOD", client, topicId);

        // Enterprise: extend the expiration time (no admin key required).
        Instant newExpiration = afterPeriod.expirationTime.plus(Duration.ofDays(10));
        topicClient.updateExpirationTime(topicId, newExpiration);
        logTopicInfo("AFTER EXTENDING EXPIRATION", client, topicId);

        // Enterprise: switch to a different auto renew account; the new account signs.
        Account newRenewAccount = accountClient.createAccount(1);
        topicClient.updateAutoRenewAccount(topicId, newRenewAccount);
        logTopicInfo("AFTER CHANGING AUTO RENEW ACCOUNT", client, topicId);

        // Enterprise: remove the auto renew account.
        topicClient.clearAutoRenewAccount(topicId);
        System.out.println("\nCleared auto renew account via TopicClient.clearAutoRenewAccount()");
        logTopicInfo("AFTER CLEARING AUTO RENEW ACCOUNT", client, topicId);

        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
