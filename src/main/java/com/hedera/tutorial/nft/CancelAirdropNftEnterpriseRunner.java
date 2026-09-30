package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.tutorial.util.TestnetQueryHelper;
import java.nio.charset.StandardCharsets;
import org.hiero.base.HieroContext;
import org.hiero.base.NftClient;
import org.hiero.base.data.Account;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Enterprise tutorial for {@link NftClient#cancelAirdropNft} — cancels a pending NFT airdrop.
 *
 * <p>Receiver is created with {@code maxAutomaticTokenAssociations = 0} so the airdrop stays
 * pending until canceled.
 */
@Component
@Profile("cancel-airdrop-nft")
public class CancelAirdropNftEnterpriseRunner implements CommandLineRunner {

    private final NftClient nftClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public CancelAirdropNftEnterpriseRunner(
            NftClient nftClient, HieroContext hieroContext, ConfigurableApplicationContext context) {
        this.nftClient = nftClient;
        this.hieroContext = hieroContext;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        var client = hieroContext.getClient();
        TestnetQueryHelper.tuneClientForTestnet(client);

        Account treasury = hieroContext.getOperatorAccount();
        byte[] metadata = "https://example.com/nft/cancel-airdrop".getBytes(StandardCharsets.UTF_8);

        TokenId tokenId = nftClient.createNftType("Cancel Airdrop Demo NFT", "CANFT");
        System.out.println("Created NFT type: " + tokenId);

        long serial = nftClient.mintNft(tokenId, metadata);
        System.out.println("Minted NFT serial: " + serial);

        AccountId receiverId = createReceiverWithNoAutoAssociations(client);
        System.out.println("Created receiver (0 auto-associations): " + receiverId);
        TestnetQueryHelper.sleepForConsensus();

        TestnetQueryHelper.logNftOwner("BEFORE AIRDROP", client, tokenId, serial, treasury.accountId());

        nftClient.airdropNft(tokenId, serial, treasury, receiverId);
        System.out.println(
                "Airdropped serial "
                        + serial
                        + " via NftClient.airdropNft() -> "
                        + receiverId
                        + " (expected pending)");
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner(
                "AFTER AIRDROP (pending)", client, tokenId, serial, treasury.accountId());

        nftClient.cancelAirdropNft(tokenId, serial, treasury, receiverId);
        System.out.println(
                "Canceled pending airdrop via NftClient.cancelAirdropNft() for serial "
                        + serial
                        + " -> "
                        + receiverId);
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner("AFTER CANCEL", client, tokenId, serial, treasury.accountId());
        System.out.println("Cancel airdrop status: SUCCESS");

        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private static AccountId createReceiverWithNoAutoAssociations(com.hedera.hashgraph.sdk.Client client)
            throws Exception {
        PrivateKey receiverKey = PrivateKey.generateED25519();
        AccountId receiverId =
                new AccountCreateTransaction()
                        .setKey(receiverKey)
                        .setInitialBalance(Hbar.from(1))
                        .setMaxAutomaticTokenAssociations(0)
                        .execute(client)
                        .getReceipt(client)
                        .accountId;

        if (receiverId == null) {
            throw new IllegalStateException("Account create receipt did not contain an account ID");
        }
        return receiverId;
    }
}
