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
@Profile("account")
public class CreateAccountEnterpriseRunner implements CommandLineRunner {

    private static final String ACCOUNT_MEMO =
            "This is a new account created from the Java Enterprise tutorial";

    private final AccountClient accountClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public CreateAccountEnterpriseRunner(
            AccountClient accountClient,
            HieroContext hieroContext,
            ConfigurableApplicationContext context) {
        this.accountClient = accountClient;
        this.hieroContext = hieroContext;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        // Enterprise: create account (auto key, initial balance; no memo on create).
        Account account = accountClient.createAccount(Hbar.from(1));
        System.out.println("Created account ID: " + account.accountId());
        System.out.println("Account private key: " + account.privateKey());
        System.out.println("Account public key: " + account.publicKey());

        AccountInfo beforeUpdate = queryAccountInfo(account);
        System.out.println("\n=== BEFORE UPDATE ===");
        System.out.println("Account ID: " + beforeUpdate.accountId);
        System.out.println("Memo: " + beforeUpdate.accountMemo);
        System.out.println("Balance: " + beforeUpdate.balance);

        // Enterprise: update memo (signing handled internally by the lib).
        accountClient.updateAccountMemo(account, ACCOUNT_MEMO);

        AccountInfo afterUpdate = queryAccountInfo(account);
        System.out.println("\n=== AFTER UPDATE ===");
        System.out.println("Account ID: " + afterUpdate.accountId);
        System.out.println("Memo: " + afterUpdate.accountMemo);
        System.out.println("Balance: " + afterUpdate.balance);

        // Enterprise: transfer HBAR from operator to the created account.
        Hbar transferAmount = Hbar.from(1);
        System.out.println("\n=== BEFORE TRANSFER ===");
        System.out.println("Operator balance: " + accountClient.getOperatorAccountBalance());
        System.out.println("Account balance: " + accountClient.getAccountBalance(account.accountId()));

        accountClient.transferHbar(account.accountId(), transferAmount);

        System.out.println("\n=== AFTER TRANSFER ===");
        System.out.println("Transferred: " + transferAmount);
        System.out.println("Operator balance: " + accountClient.getOperatorAccountBalance());
        System.out.println("Account balance: " + accountClient.getAccountBalance(account.accountId()));

        // Enterprise: delete account; remaining HBAR transferred to operator.
        accountClient.deleteAccount(account);
        System.out.println("\nDeleted account: " + account.accountId());

        System.exit(SpringApplication.exit(context, () -> 0));
    }

    /** Enterprise has no AccountInfo API — use HieroContext + SDK for on-chain reads. */
    private AccountInfo queryAccountInfo(Account account) throws Exception {
        return new AccountInfoQuery()
                .setAccountId(account.accountId())
                .execute(hieroContext.getClient());
    }
}
