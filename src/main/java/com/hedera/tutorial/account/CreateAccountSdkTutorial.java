package com.hedera.tutorial.account;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

public class CreateAccountSdkTutorial {
    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

        AccountCreateTransaction transaction = new AccountCreateTransaction()
                .setKey(operatorKey.getPublicKey())
                .setInitialBalance(new Hbar(1));

        TransactionResponse response = transaction.execute(client);

        TransactionReceipt receipt = response.getReceipt(client);
        AccountId newAccountId = receipt.accountId;

        System.out.println("The new account ID is: " + newAccountId);
    }
}
