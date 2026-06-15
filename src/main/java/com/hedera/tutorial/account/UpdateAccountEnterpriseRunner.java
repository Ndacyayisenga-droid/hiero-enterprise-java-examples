package com.hedera.tutorial.account;

import com.hedera.hashgraph.sdk.AccountInfo;
import com.hedera.hashgraph.sdk.AccountInfoQuery;
import com.hedera.hashgraph.sdk.Hbar;
import org.hiero.base.AccountClient;
import org.hiero.base.HieroContext;
import org.hiero.base.data.Account;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("update-account")
public class UpdateAccountEnterpriseRunner implements CommandLineRunner {
    private static final String UPDATED_MEMO = "This is an updated account memo";

    private final AccountClient accountClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public UpdateAccountEnterpriseRunner(
            AccountClient accountClient,
            HieroContext hieroContext,
            ConfigurableApplicationContext context) {
        this.accountClient = accountClient;
        this.hieroContext = hieroContext;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        Account account = accountClient.createAccount(Hbar.from(1));
        System.out.println("The new account ID is: " + account.accountId());

        AccountInfo accountInfoBefore = queryAccountInfo(account);

        System.out.println("\n=== BEFORE UPDATE ===");
        System.out.println("Account ID: " + accountInfoBefore.accountId);
        System.out.println("Memo: " + accountInfoBefore.accountMemo);
        System.out.println("Balance: " + accountInfoBefore.balance);

        accountClient.updateAccountMemo(account, UPDATED_MEMO);

        AccountInfo accountInfoAfter = queryAccountInfo(account);

        System.out.println("\n=== AFTER UPDATE ===");
        System.out.println("Account ID: " + accountInfoAfter.accountId);
        System.out.println("Memo: " + accountInfoAfter.accountMemo);
        System.out.println("Balance: " + accountInfoAfter.balance);

        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private AccountInfo queryAccountInfo(Account account) throws Exception {
        return new AccountInfoQuery()
                .setAccountId(account.accountId())
                .execute(hieroContext.getClient());
    }
}
