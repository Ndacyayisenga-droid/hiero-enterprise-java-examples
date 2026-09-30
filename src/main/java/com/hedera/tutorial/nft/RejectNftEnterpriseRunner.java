package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.tutorial.util.TestnetQueryHelper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hiero.base.AccountClient;
import org.hiero.base.HieroContext;
import org.hiero.base.NftClient;
import org.hiero.base.data.Account;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Enterprise tutorial for {@link NftClient#rejectNft} / {@link NftClient#rejectNfts} — rejects
 * airdropped NFTs and returns them to the treasury.
 *
 * <p>The receiver is associated first so the airdrop completes immediately. It then rejects one
 * serial with the single overload and the other with the list overload. Rejection charges no custom
 * fees and does not dissociate the receiver from the NFT type.
 *
 * @see <a href="https://docs.hedera.com/native/tokens/reject-airdrop">Reject a token</a>
 */
@Component
@Profile("reject-nft")
public class RejectNftEnterpriseRunner implements CommandLineRunner {

    private final NftClient nftClient;
    private final AccountClient accountClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public RejectNftEnterpriseRunner(
            NftClient nftClient,
            AccountClient accountClient,
            HieroContext hieroContext,
            ConfigurableApplicationContext context) {
        this.nftClient = nftClient;
        this.accountClient = accountClient;
        this.hieroContext = hieroContext;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        var client = hieroContext.getClient();
        TestnetQueryHelper.tuneClientForTestnet(client);

        Account treasury = hieroContext.getOperatorAccount();
        byte[] metadata = "https://example.com/nft/reject".getBytes(StandardCharsets.UTF_8);

        // Enterprise: create an NFT type and mint two serials.
        TokenId tokenId = nftClient.createNftType("Reject Demo NFT", "RJNFT");
        System.out.println("Created NFT type: " + tokenId);

        List<Long> serials = nftClient.mintNfts(tokenId, metadata, metadata);
        long serial1 = serials.get(0);
        long serial2 = serials.get(1);
        System.out.println("Minted NFT serials: " + serial1 + ", " + serial2);

        // Enterprise: create a receiver and associate so the airdrop completes immediately.
        Account receiver = accountClient.createAccount(1);
        nftClient.associateNft(tokenId, receiver);
        System.out.println("Created and associated receiver: " + receiver.accountId());

        // Enterprise: airdrop both serials to the receiver.
        nftClient.airdropNfts(tokenId, serials, treasury, receiver.accountId());
        System.out.println(
                "Airdropped serials " + serial1 + ", " + serial2 + " via NftClient.airdropNfts() -> "
                        + receiver.accountId());
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner(
                "AFTER AIRDROP (serial " + serial1 + ")", client, tokenId, serial1, receiver.accountId());
        TestnetQueryHelper.logNftOwner(
                "AFTER AIRDROP (serial " + serial2 + ")", client, tokenId, serial2, receiver.accountId());

        // Enterprise: reject one serial with the single overload.
        nftClient.rejectNft(tokenId, serial1, receiver);
        System.out.println("Rejected serial " + serial1 + " via NftClient.rejectNft()");
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner(
                "AFTER rejectNft (serial " + serial1 + ")", client, tokenId, serial1, treasury.accountId());

        // Enterprise: reject the remaining serial with the list overload.
        nftClient.rejectNfts(tokenId, List.of(serial2), receiver);
        System.out.println("Rejected serial " + serial2 + " via NftClient.rejectNfts()");
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner(
                "AFTER rejectNfts (serial " + serial2 + ")", client, tokenId, serial2, treasury.accountId());
        System.out.println("Reject NFT status: SUCCESS");

        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
