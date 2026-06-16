package com.hedera.tutorial.account;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * SDK tutorial for {@link AccountAllowanceApproveTransaction} — owner delegates HBAR spending
 * to a spender, who can then transfer on the owner's behalf.
 *
 * @see <a href="https://docs.hedera.com/native/accounts/approve-allowance">Approve an allowance</a>
 */
public class ApproveAllowanceSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

        // Owner (Alice) and spender (Bob) — both need keys; owner grants allowance to spender.
        PrivateKey ownerKey = PrivateKey.generateED25519();
        PrivateKey spenderKey = PrivateKey.generateED25519();

        AccountId ownerId =
                new AccountCreateTransaction()
                        .setKeyWithoutAlias(ownerKey)
                        .setInitialBalance(Hbar.from(2))
                        .execute(client)
                        .getReceipt(client)
                        .accountId;

        AccountId spenderId =
                new AccountCreateTransaction()
                        .setKeyWithoutAlias(spenderKey)
                        .setInitialBalance(Hbar.from(1))
                        .execute(client)
                        .getReceipt(client)
                        .accountId;

        if (ownerId == null || spenderId == null) {
            throw new IllegalStateException("Failed to create owner or spender account");
        }

        System.out.println("Owner (Alice):   " + ownerId);
        System.out.println("Spender (Bob):   " + spenderId);
        printBalance(client, "Owner before approve", ownerId);
        printBalance(client, "Spender before approve", spenderId);

        // Step 1: Owner approves 1 HBAR allowance for spender (docs sample).
        Hbar allowanceAmount = Hbar.from(1);
        Status approveStatus =
                new AccountAllowanceApproveTransaction()
                        .approveHbarAllowance(ownerId, spenderId, allowanceAmount)
                        .freezeWith(client)
                        .sign(ownerKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;

        System.out.println("\nAllowance approved: " + allowanceAmount + " from " + ownerId + " to " + spenderId);
        System.out.println("Approve transaction status: " + approveStatus);

        // Step 2: Spender uses allowance — transfer 1 HBAR from owner to operator.
        // Spender must pay the transaction fee (setTransactionId to spender).
        Status transferStatus =
                new TransferTransaction()
                        .addApprovedHbarTransfer(ownerId, allowanceAmount.negated())
                        .addHbarTransfer(operatorId, allowanceAmount)
                        .setTransactionId(TransactionId.generate(spenderId))
                        .freezeWith(client)
                        .sign(spenderKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;

        System.out.println("\nSpender transferred " + allowanceAmount + " from owner to operator using allowance");
        System.out.println("Transfer transaction status: " + transferStatus);
        printBalance(client, "Owner after spend", ownerId);
        printBalance(client, "Spender after spend", spenderId);

        // Cleanup: revoke allowance and delete accounts.
        new AccountAllowanceApproveTransaction()
                .approveHbarAllowance(ownerId, spenderId, Hbar.ZERO)
                .freezeWith(client)
                .sign(ownerKey)
                .execute(client)
                .getReceipt(client);

        deleteAccount(client, ownerId, ownerKey, operatorId);
        deleteAccount(client, spenderId, spenderKey, operatorId);

        client.close();
    }

    private static void printBalance(Client client, String label, AccountId accountId) throws Exception {
        Hbar balance = new AccountBalanceQuery().setAccountId(accountId).execute(client).hbars;
        System.out.println(label + " balance: " + balance);
    }

    private static void deleteAccount(
            Client client, AccountId accountId, PrivateKey accountKey, AccountId transferTo)
            throws Exception {
        new AccountDeleteTransaction()
                .setAccountId(accountId)
                .setTransferAccountId(transferTo)
                .freezeWith(client)
                .sign(accountKey)
                .execute(client)
                .getReceipt(client);
    }
}
