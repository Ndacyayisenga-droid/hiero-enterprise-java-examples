package com.hedera.tutorial.account;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * SDK tutorial for {@link AccountInfoQuery} — returns the current state of an account.
 *
 * <p>Anyone on the network can request account info for a given account. Queries do not change
 * account state or require network consensus.
 *
 * @see <a href="https://docs.hedera.com/native/accounts/get-info">Get account info</a>
 */
public class GetAccountInfoSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

        PrivateKey accountKey = PrivateKey.generateED25519();

        AccountId newAccountId =
                new AccountCreateTransaction()
                        .setKeyWithoutAlias(accountKey)
                        .setInitialBalance(Hbar.from(1))
                        .setAccountMemo("Get account info tutorial")
                        .execute(client)
                        .getReceipt(client)
                        .accountId;

        if (newAccountId == null) {
            throw new IllegalStateException("Account create receipt did not contain an account ID");
        }

        System.out.println("Querying account info for: " + newAccountId);

        // Create the account info query (docs sample).
        AccountInfoQuery query = new AccountInfoQuery().setAccountId(newAccountId);

        // Submit the query to a Hedera network.
        AccountInfo accountInfo = query.execute(client);

        // Print the account info to the console.
        System.out.println(accountInfo);

        printAccountInfoFields(accountInfo);

        System.out.println("\n--- Operator account info (any account can be queried) ---");
        AccountInfo operatorInfo =
                new AccountInfoQuery().setAccountId(operatorId).execute(client);
        System.out.println("Operator account ID: " + operatorInfo.accountId);
        System.out.println("Operator balance:    " + operatorInfo.balance);

        deleteAccount(client, newAccountId, accountKey, operatorId);

        client.close();
    }

    private static void printAccountInfoFields(AccountInfo accountInfo) {
        System.out.println("\n=== AccountInfo fields ===");
        System.out.println("accountId:                    " + accountInfo.accountId);
        System.out.println("contractAccountId:            " + accountInfo.contractAccountId);
        System.out.println("isDeleted:                    " + accountInfo.isDeleted);
        System.out.println("key:                          " + accountInfo.key);
        System.out.println("balance:                      " + accountInfo.balance);
        System.out.println("isReceiverSignatureRequired:  " + accountInfo.isReceiverSignatureRequired);
        System.out.println("ownedNfts:                    " + accountInfo.ownedNfts);
        System.out.println("maxAutomaticTokenAssociations:" + accountInfo.maxAutomaticTokenAssociations);
        System.out.println("accountMemo:                  " + accountInfo.accountMemo);
        System.out.println("expirationTime:               " + accountInfo.expirationTime);
        System.out.println("autoRenewPeriod:              " + accountInfo.autoRenewPeriod);
        System.out.println("ledgerId:                     " + accountInfo.ledgerId);
        System.out.println("ethereumNonce:                " + accountInfo.ethereumNonce);
        System.out.println("stakingInfo:                  " + accountInfo.stakingInfo);
        System.out.println("tokenRelationships:           " + accountInfo.tokenRelationships);
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
