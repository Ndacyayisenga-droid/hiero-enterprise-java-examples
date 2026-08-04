package com.hedera.tutorial.account;

import com.hedera.hashgraph.sdk.Hbar;
import org.hiero.base.AccountClient;
import org.hiero.base.data.Account;
import org.hiero.base.protocol.data.AccountInfoResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Enterprise tutorial for {@link AccountClient#getAccountInfo} — returns the current state of an
 * account from a consensus node.
 *
 * <p>Anyone on the network can request account info for a given account. Queries do not change
 * account state or require network consensus.
 *
 * @see <a href="https://docs.hedera.com/native/accounts/get-info">Get account info</a>
 */
@Component
@Profile("get-account-info")
public class GetAccountInfoEnterpriseRunner implements CommandLineRunner {

    private final AccountClient accountClient;
    private final ConfigurableApplicationContext context;

    public GetAccountInfoEnterpriseRunner(
            AccountClient accountClient, ConfigurableApplicationContext context) {
        this.accountClient = accountClient;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        // Enterprise: create account with initial balance.
        Account account = accountClient.createAccount(Hbar.from(1));
        accountClient.updateAccountMemo(account, "Get account info tutorial");

        System.out.println("Querying account info for: " + account.accountId());

        // Enterprise: submit account info query (docs sample).
        AccountInfoResponse accountInfo = accountClient.getAccountInfo(account.accountId());

        // Print the account info to the console.
        System.out.println(accountInfo);
        printAccountInfoFields(accountInfo);

        System.out.println("\n--- Operator account info (any account can be queried) ---");
        AccountInfoResponse operatorInfo = accountClient.getOperatorAccountInfo();
        System.out.println("Operator account ID: " + operatorInfo.accountId());
        System.out.println("Operator balance:    " + operatorInfo.balance());

        // Enterprise: delete account; remaining HBAR transferred to operator.
        accountClient.deleteAccount(account);
        System.out.println("\nDeleted account: " + account.accountId());

        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private static void printAccountInfoFields(AccountInfoResponse accountInfo) {
        System.out.println("\n=== AccountInfo fields ===");
        System.out.println("accountId:                    " + accountInfo.accountId());
        System.out.println("contractAccountId:            " + accountInfo.contractAccountId());
        System.out.println("deleted:                      " + accountInfo.deleted());
        System.out.println("key:                          " + accountInfo.key());
        System.out.println("balance:                      " + accountInfo.balance());
        System.out.println(
                "receiverSignatureRequired:    " + accountInfo.receiverSignatureRequired());
        System.out.println("ownedNfts:                    " + accountInfo.ownedNfts());
        System.out.println(
                "maxAutomaticTokenAssociations:" + accountInfo.maxAutomaticTokenAssociations());
        System.out.println("accountMemo:                  " + accountInfo.accountMemo());
        System.out.println("expirationTime:               " + accountInfo.expirationTime());
        System.out.println("autoRenewPeriod:              " + accountInfo.autoRenewPeriod());
        System.out.println("ledgerId:                     " + accountInfo.ledgerId());
        System.out.println("ethereumNonce:                " + accountInfo.ethereumNonce());
        System.out.println("stakingInfo:                  " + accountInfo.stakingInfo());
        System.out.println("aliasKey:                     " + accountInfo.aliasKey());
    }
}
