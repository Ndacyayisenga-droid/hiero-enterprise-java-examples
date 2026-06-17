package com.hedera.tutorial.account;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * SDK tutorial for deleting an HBAR allowance.
 *
 * <p>Per Hedera docs, HBAR and fungible token allowances are removed by setting the amount to zero
 * in {@link AccountAllowanceApproveTransaction}. NFT allowances use {@link
 * AccountAllowanceDeleteTransaction} instead.
 *
 * @see <a href="https://docs.hedera.com/native/accounts/adjust-allowance">Delete an allowance</a>
 */
public class DeleteAllowanceSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

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

        System.out.println("Owner:   " + ownerId);
        System.out.println("Spender: " + spenderId);

        Hbar allowanceAmount = Hbar.from(1);

        // Step 1: approve allowance
        new AccountAllowanceApproveTransaction()
                .approveHbarAllowance(ownerId, spenderId, allowanceAmount)
                .freezeWith(client)
                .sign(ownerKey)
                .execute(client)
                .getReceipt(client);

        System.out.println("\nApproved allowance: " + allowanceAmount);

        // Step 2: delete HBAR allowance — set amount to zero (docs approach)
        Status deleteStatus =
                new AccountAllowanceApproveTransaction()
                        .approveHbarAllowance(ownerId, spenderId, Hbar.ZERO)
                        .freezeWith(client)
                        .sign(ownerKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;

        System.out.println("Deleted allowance (amount set to zero). Status: " + deleteStatus);

        // Step 3: spender can no longer use the allowance
        try {
            new TransferTransaction()
                    .addApprovedHbarTransfer(ownerId, allowanceAmount.negated())
                    .addHbarTransfer(operatorId, allowanceAmount)
                    .setTransactionId(TransactionId.generate(spenderId))
                    .freezeWith(client)
                    .sign(spenderKey)
                    .execute(client)
                    .getReceipt(client);

            throw new IllegalStateException("Transfer should have failed after allowance deletion");
        } catch (Exception e) {
            System.out.println("Spend after delete failed as expected: " + e.getMessage());
        }

        deleteAccount(client, ownerId, ownerKey, operatorId);
        deleteAccount(client, spenderId, spenderKey, operatorId);

        client.close();
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
