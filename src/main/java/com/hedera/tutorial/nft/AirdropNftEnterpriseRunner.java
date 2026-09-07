package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenNftInfoQuery;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hiero.base.HieroContext;
import org.hiero.base.NftClient;
import org.hiero.base.data.Account;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Enterprise tutorial for {@link NftClient#airdropNft} / {@link NftClient#airdropNfts} — airdrops
 * NFT serials to a receiver.
 *
 * <p>Unlike a standard transfer, if the receiver is not associated and has no available
 * auto-association slots, the airdrop becomes pending instead of failing. This demo creates a
 * receiver with unlimited auto-associations ({@code -1}) via the SDK so the airdrop completes
 * immediately without a prior associate. {@link org.hiero.base.AccountClient#createAccount} does
 * not set max automatic associations yet.
 *
 * <p>Before/after checks use consensus {@link AccountBalanceQuery} (NFT count) and {@link
 * TokenNftInfoQuery} (owner per serial) via {@link HieroContext#getClient()}.
 *
 * @see <a href="https://docs.hedera.com/native/tokens/airdrop">Airdrop a token</a>
 */
@Component
@Profile("airdrop-nft")
public class AirdropNftEnterpriseRunner implements CommandLineRunner {

    private final NftClient nftClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public AirdropNftEnterpriseRunner(
            NftClient nftClient,
            HieroContext hieroContext,
            ConfigurableApplicationContext context) {
        this.nftClient = nftClient;
        this.hieroContext = hieroContext;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        Account treasury = hieroContext.getOperatorAccount();
        byte[] metadata = "https://example.com/nft/airdrop".getBytes(StandardCharsets.UTF_8);

        // Enterprise: create an NFT type and mint two serials.
        TokenId tokenId = nftClient.createNftType("Airdrop Demo NFT", "ADNFT");
        System.out.println("Created NFT type: " + tokenId);

        long serial1 = nftClient.mintNft(tokenId, metadata);
        long serial2 = nftClient.mintNft(tokenId, metadata);
        System.out.println("Minted NFT serials: " + serial1 + ", " + serial2);

        // Receiver with unlimited auto-associations — airdrop completes without associate.
        PrivateKey receiverKey = PrivateKey.generateED25519();
        AccountId receiverId =
                new AccountCreateTransaction()
                        .setKey(receiverKey)
                        .setInitialBalance(Hbar.from(1))
                        .setMaxAutomaticTokenAssociations(-1)
                        .execute(hieroContext.getClient())
                        .getReceipt(hieroContext.getClient())
                        .accountId;

        if (receiverId == null) {
            throw new IllegalStateException("Account create receipt did not contain an account ID");
        }
        System.out.println("Created receiver with unlimited auto-associations: " + receiverId);

        logNfts("BEFORE AIRDROP", tokenId, treasury.accountId(), receiverId, List.of(serial1, serial2));

        // Enterprise: airdrop a single serial (operator/treasury is sender).
        nftClient.airdropNft(tokenId, serial1, treasury, receiverId);
        System.out.println("Airdropped NFT serial via NftClient.airdropNft(): " + serial1);
        logNfts(
                "AFTER airdropNft (serial " + serial1 + ")",
                tokenId,
                treasury.accountId(),
                receiverId,
                List.of(serial1, serial2));

        // Enterprise: airdrop remaining serials in one call.
        nftClient.airdropNfts(tokenId, List.of(serial2), treasury, receiverId);
        System.out.println("Airdropped NFT serial via NftClient.airdropNfts(): " + serial2);
        logNfts(
                "AFTER airdropNfts (serial " + serial2 + ")",
                tokenId,
                treasury.accountId(),
                receiverId,
                List.of(serial1, serial2));
        System.out.println("Airdrop status: SUCCESS");

        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private void logNfts(
            String label,
            TokenId tokenId,
            AccountId treasuryId,
            AccountId receiverId,
            List<Long> serials)
            throws Exception {
        var client = hieroContext.getClient();
        long treasuryBalance =
                new AccountBalanceQuery()
                        .setAccountId(treasuryId)
                        .execute(client)
                        .tokens
                        .getOrDefault(tokenId, 0L);
        long receiverBalance =
                new AccountBalanceQuery()
                        .setAccountId(receiverId)
                        .execute(client)
                        .tokens
                        .getOrDefault(tokenId, 0L);

        System.out.println("\n=== " + label + " ===");
        System.out.println("Treasury " + treasuryId + " NFT balance for " + tokenId + ": " + treasuryBalance);
        System.out.println("Receiver " + receiverId + " NFT balance for " + tokenId + ": " + receiverBalance);
        for (long serial : serials) {
            System.out.println("  serial " + serial + ": " + describeNft(tokenId, serial));
        }
    }

    private String describeNft(TokenId tokenId, long serial) {
        try {
            var info =
                    new TokenNftInfoQuery()
                            .setNftId(tokenId.nft(serial))
                            .execute(hieroContext.getClient())
                            .get(0);
            return "exists, owner=" + info.accountId;
        } catch (Exception e) {
            return "query failed: " + e.getClass().getSimpleName();
        }
    }
}
