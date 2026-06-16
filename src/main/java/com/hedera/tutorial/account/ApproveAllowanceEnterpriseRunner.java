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
 * Enterprise tutorial for HBAR allowance approve — {@link AccountClient} for approve/revoke,
 * {@link HieroContext} + SDK for spending the allowance.
 *
 * @see <a href="https://docs.hedera.com/native/accounts/approve-allowance">Approve an allowance</a>
 */
@Component
@Profile("approve-allowance")
public class ApproveAllowanceEnterpriseRunner implements CommandLineRunner {

    private final AccountClient accountClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public ApproveAllowanceEnterpriseRunner(
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

        // Advanced: spender uses allowance via SDK (spender pays transaction fee).
        var client = hieroContext.getClient();
        new TransferTransaction()
                .addApprovedHbarTransfer(owner.accountId(), allowanceAmount.negated())
                .addHbarTransfer(hieroContext.getOperatorAccount().accountId(), allowanceAmount)
                .setTransactionId(TransactionId.generate(spender.accountId()))
                .freezeWith(client)
                .sign(spender.privateKey())
                .execute(client)
                .getReceipt(client);

        System.out.println(
                "Spender transferred " + allowanceAmount + " from owner to operator using allowance");
        printBalances("AFTER SPEND", owner, spender);

        // Enterprise: revoke allowance and clean up.
        accountClient.revokeHbarAllowance(owner, spender.accountId());
        accountClient.deleteAccount(owner);
        accountClient.deleteAccount(spender);
        System.out.println("\nRevoked allowance and deleted owner/spender accounts.");

        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private void printBalances(String label, Account owner, Account spender) throws Exception {
        System.out.println("\n=== " + label + " ===");
        System.out.println("Owner balance:   " + accountClient.getAccountBalance(owner.accountId()));
        System.out.println("Spender balance: " + accountClient.getAccountBalance(spender.accountId()));
    }
}
