package com.hedera.tutorial.account;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

public class UpdateAccountSdkTutorial {

    public static void main(String[] args) throws Exception {

        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(operatorKey.getPublicKey())
                .setAccountMemo("Initial account memo")
                .setInitialBalance(new Hbar(1));

        TransactionResponse response = transaction.execute(client);

        TransactionReceipt receipt = response.getReceipt(client);
        AccountId newAccountId = receipt.accountId;

        System.out.println("The new account ID is: " + newAccountId);

        // Query the account info before Update

        assert newAccountId != null;
        AccountInfo accountInfo = new AccountInfoQuery()
                .setAccountId(newAccountId)
                .execute(client);

        System.out.println("\n=== BEFORE UPDATE ===");
        System.out.println("Account ID: " + accountInfo.accountId);
        System.out.println("Memo: " + accountInfo.accountMemo);

        // Update the account memo

        TransactionResponse updateResponse = new AccountUpdateTransaction()
                .setAccountId(newAccountId)
                .setAccountMemo("This is an updated account memo")
                .freezeWith(client)
                .sign(operatorKey)
                .execute(client);

        updateResponse.getReceipt(client);

        // Query the account info after Update

        AccountInfo updatedAccountInfo = new AccountInfoQuery()
                .setAccountId(newAccountId)
                .execute(client);

        System.out.println("\n=== AFTER UPDATE ===");
        System.out.println("Account ID: " + updatedAccountInfo.accountId);
        System.out.println("Memo: " + updatedAccountInfo.accountMemo);

        client.close();
    }
}
