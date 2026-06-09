package com.hedera.tutorial.account;

import org.hiero.base.AccountClient;
import org.hiero.base.data.Account;
import org.hiero.base.data.ConsensusAccountInfo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("update-account")
public class UpdateAccountEnterpriseRunner implements CommandLineRunner {
    private static final String INITIAL_MEMO = "Initial account memo";
    private static final String UPDATED_MEMO = "This is an updated account memo";

    private final AccountClient accountClient;
    private final ConfigurableApplicationContext context;

    public UpdateAccountEnterpriseRunner(
            AccountClient accountClient, ConfigurableApplicationContext context) {
        this.accountClient = accountClient;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        Account account = accountClient.createAccount(1, INITIAL_MEMO);
        System.out.println("The new account ID is: " + account.accountId());

        ConsensusAccountInfo accountInfoBefore = accountClient.getAccountInfo(account.accountId());

        System.out.println("\n=== BEFORE UPDATE ===");
        System.out.println("Account ID: " + accountInfoBefore.accountId());
        System.out.println("Memo: " + accountInfoBefore.memo());

        accountClient.updateAccountMemo(account, UPDATED_MEMO);

        ConsensusAccountInfo accountInfoAfter = accountClient.getAccountInfo(account.accountId());

        System.out.println("\n=== AFTER UPDATE ===");
        System.out.println("Account ID: " + accountInfoAfter.accountId());
        System.out.println("Memo: " + accountInfoAfter.memo());

        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
