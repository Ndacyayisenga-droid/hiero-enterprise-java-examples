package com.hedera.tutorial.account;

import org.hiero.base.AccountClient;
import org.hiero.base.HieroException;
import org.hiero.base.data.Account;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("account")
public class CreateAccountEnterpriseRunner implements CommandLineRunner {
    private final AccountClient accountClient;
    private final ConfigurableApplicationContext context;

    public CreateAccountEnterpriseRunner(
            AccountClient accountClient, ConfigurableApplicationContext context) {
        this.accountClient = accountClient;
        this.context = context;
    }

    @Override
    public void run(String... args) throws HieroException {
        Account newAccount = accountClient.createAccount(1);
        System.out.println("The new account ID is: " + newAccount.accountId());
        System.out.println("The new account's public key is: " + newAccount.publicKey());
        System.out.println("The new account's private key is: " + newAccount.privateKey());

        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
