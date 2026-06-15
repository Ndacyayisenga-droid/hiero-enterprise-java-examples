package com.hedera.tutorial.account;

import com.hedera.hashgraph.sdk.Hbar;
import org.hiero.base.AccountClient;
import org.hiero.base.data.Account;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("transfer-accounts")
public class TransferBetweenAccountsEnterpriseRunner implements CommandLineRunner {

    private final AccountClient accountClient;
    private final ConfigurableApplicationContext context;

    public TransferBetweenAccountsEnterpriseRunner(
            AccountClient accountClient, ConfigurableApplicationContext context) {
        this.accountClient = accountClient;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        Account alice = accountClient.createAccount(Hbar.from(2));
        Account bob = accountClient.createAccount(Hbar.from(2));

        System.out.println("Created Alice: " + alice.accountId());
        System.out.println("Created Bob:   " + bob.accountId());

        Hbar aliceToBob = Hbar.from(1);
        printBalances("BEFORE ALICE → BOB", alice, bob);
        accountClient.transferHbar(alice, bob.accountId(), aliceToBob);
        printBalances("AFTER ALICE → BOB", alice, bob);

        Hbar bobToAlice = Hbar.fromTinybars(50_000_000L); // 0.5 HBAR (1 ℏ = 100_000_000 tinybars)
        printBalances("BEFORE BOB → ALICE", alice, bob);
        accountClient.transferHbar(bob, alice.accountId(), bobToAlice);
        printBalances("AFTER BOB → ALICE", alice, bob);

        accountClient.deleteAccount(alice);
        accountClient.deleteAccount(bob);
        System.out.println("\nDeleted Alice and Bob — remaining HBAR returned to operator.");

        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private void printBalances(String label, Account alice, Account bob) throws Exception {
        System.out.println("\n=== " + label + " ===");
        System.out.println("Alice balance: " + accountClient.getAccountBalance(alice.accountId()));
        System.out.println("Bob balance:   " + accountClient.getAccountBalance(bob.accountId()));
    }
}
