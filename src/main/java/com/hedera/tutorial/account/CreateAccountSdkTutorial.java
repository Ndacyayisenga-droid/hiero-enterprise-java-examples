package com.hedera.tutorial.account;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

public class CreateAccountSdkTutorial {

    private static final PrivateKey ECDSA_PRIVATE_KEY = PrivateKey.generateECDSA();
    private static final PublicKey ECDSA_PUBLIC_KEY = ECDSA_PRIVATE_KEY.getPublicKey();

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

        AccountCreateTransaction transaction =
                new AccountCreateTransaction()
                        .setKeyWithAlias(ECDSA_PUBLIC_KEY)
                        .setInitialBalance(Hbar.from(1))
                        .setAccountMemo("Before Update");

        TransactionResponse response = transaction.execute(client);
        TransactionReceipt receipt = response.getReceipt(client);
        AccountId newAccountId = receipt.accountId;

        if (newAccountId == null) {
            throw new IllegalStateException("Account create receipt did not contain an account ID");
        }

        AccountInfo accountInfoBefore =
                new AccountInfoQuery().setAccountId(newAccountId).execute(client);

        System.out.println(accountInfoBefore);
        System.out.println("Account Memo: " + accountInfoBefore.accountMemo);
        System.out.println("The new account ID is: " + newAccountId);

        // Account key must sign memo updates.
        new AccountUpdateTransaction()
                .setAccountId(newAccountId)
                .setAccountMemo("After Update")
                .freezeWith(client)
                .sign(ECDSA_PRIVATE_KEY)
                .execute(client)
                .getReceipt(client);

        AccountInfo accountInfoAfter =
                new AccountInfoQuery().setAccountId(newAccountId).execute(client);

        System.out.println("\n" + accountInfoAfter);
        System.out.println("Account ID: " + newAccountId);
        System.out.println("Account Memo: " + accountInfoAfter.accountMemo);

        client.close();
    }
}
