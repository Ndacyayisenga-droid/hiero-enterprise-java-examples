package com.hedera.tutorial.account;

import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransferTransaction;
import org.hiero.base.AccountClient;
import org.hiero.base.HieroContext;
import org.hiero.base.data.Account;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Enterprise tutorial for deleting an HBAR allowance — {@link AccountClient} for approve and
 * delete, {@link HieroContext} + SDK to verify the allowance can no longer be spent.
 *
 * @see <a href="https://docs.hedera.com/native/accounts/adjust-allowance">Delete an allowance</a>
 */
@Component
@Profile("delete-allowance")
public class DeleteAllowanceEnterpriseRunner implements CommandLineRunner {

    private final AccountClient accountClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public DeleteAllowanceEnterpriseRunner(
            AccountClient accountClient,
            HieroContext hieroContext,
            ConfigurableApplicationContext context) {
        this.accountClient = accountClient;
        this.hieroContext = hieroContext;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        Account owner = accountClient.createAccount(Hbar.from(2));
        Account spender = accountClient.createAccount(Hbar.from(1));

        System.out.println("Owner:   " + owner.accountId());
        System.out.println("Spender: " + spender.accountId());

        Hbar allowanceAmount = Hbar.from(1);
        printBalances("BEFORE APPROVE", owner, spender);

        // Enterprise: owner approves HBAR allowance for spender.
        accountClient.approveHbarAllowance(owner, spender.accountId(), allowanceAmount);
        System.out.println("\nApproved allowance: " + allowanceAmount);

        // Enterprise: owner deletes the allowance (sets approved amount to zero).
        accountClient.deleteHbarAllowance(owner, spender.accountId());
        System.out.println("Deleted allowance via AccountClient.deleteHbarAllowance()");

        // Advanced: spender can no longer spend — verify via SDK.
        try {
            var client = hieroContext.getClient();
            new TransferTransaction()
                    .addApprovedHbarTransfer(owner.accountId(), allowanceAmount.negated())
                    .addHbarTransfer(hieroContext.getOperatorAccount().accountId(), allowanceAmount)
                    .setTransactionId(TransactionId.generate(spender.accountId()))
                    .freezeWith(client)
                    .sign(spender.privateKey())
                    .execute(client)
                    .getReceipt(client);

            throw new IllegalStateException("Transfer should have failed after allowance deletion");
        } catch (Exception e) {
            System.out.println("Spend after delete failed as expected: " + e.getMessage());
        }

        printBalances("AFTER DELETE", owner, spender);

        accountClient.deleteAccount(owner);
        accountClient.deleteAccount(spender);
        System.out.println("\nDeleted owner/spender accounts.");

        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private void printBalances(String label, Account owner, Account spender) throws Exception {
        System.out.println("\n=== " + label + " ===");
        System.out.println("Owner balance:   " + accountClient.getAccountBalance(owner.accountId()));
        System.out.println("Spender balance: " + accountClient.getAccountBalance(spender.accountId()));
    }
}
