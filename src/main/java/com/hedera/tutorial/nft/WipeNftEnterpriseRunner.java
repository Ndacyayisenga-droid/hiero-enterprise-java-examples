package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenNftInfoQuery;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
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
 * Enterprise tutorial for {@link NftClient#wipeNft} / {@link NftClient#wipeNfts} — removes NFT
 * serials from a non-treasury account.
 *
 * <p>Newly created NFT types use the admin (treasury) key as the wipe key when none is set
 * explicitly. This demo uses the operator as treasury, so the operator key can wipe after the NFT
 * is transferred off treasury. The treasury itself cannot be wiped.
 *
 * <p>Before/after checks use consensus {@link AccountBalanceQuery} (NFT count) and {@link
 * TokenNftInfoQuery} (owner per serial) via {@link HieroContext#getClient()}.
 *
 * @see <a href="https://docs.hedera.com/hedera/sdks-and-apis/sdks/token-service/wipe-a-token">Wipe
 *     a token</a>
 */
@Component
@Profile("wipe-nft")
public class WipeNftEnterpriseRunner implements CommandLineRunner {

    private final NftClient nftClient;
    private final AccountClient accountClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public WipeNftEnterpriseRunner(
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
        Account treasury = hieroContext.getOperatorAccount();
        byte[] metadata = "https://example.com/nft/wipe".getBytes(StandardCharsets.UTF_8);

        // Enterprise: create an NFT type. Admin (and thus wipe) key defaults to the operator.
        TokenId tokenId = nftClient.createNftType("Wipe Demo NFT", "WDNFT");
        System.out.println("Created NFT type: " + tokenId);

        long serial1 = nftClient.mintNft(tokenId, metadata);
        long serial2 = nftClient.mintNft(tokenId, metadata);
        System.out.println("Minted NFT serials: " + serial1 + ", " + serial2);

        // Wipe targets a non-treasury holder — associate and transfer off treasury first.
        Account holder = accountClient.createAccount(1);
        nftClient.associateNft(tokenId, holder);
        nftClient.transferNft(tokenId, serial1, treasury, holder.accountId());
        nftClient.transferNft(tokenId, serial2, treasury, holder.accountId());
        System.out.println("Transferred serials to: " + holder.accountId());

        logHolderNfts("BEFORE WIPE", tokenId, holder.accountId(), List.of(serial1, serial2));

        // Enterprise: wipe a single serial (operator key used as wipe key).
        nftClient.wipeNft(tokenId, serial1, holder.accountId());
        System.out.println("Wiped NFT serial via NftClient.wipeNft(): " + serial1);
        logHolderNfts(
                "AFTER wipeNft (serial " + serial1 + ")",
                tokenId,
                holder.accountId(),
                List.of(serial1, serial2));

        // Enterprise: wipe remaining serials in one call.
        nftClient.wipeNfts(tokenId, Set.of(serial2), holder.accountId());
        System.out.println("Wiped NFT serial via NftClient.wipeNfts(): " + serial2);
        logHolderNfts(
                "AFTER wipeNfts (serial " + serial2 + ")",
                tokenId,
                holder.accountId(),
                List.of(serial1, serial2));
        System.out.println("Wipe status: SUCCESS");

        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private void logHolderNfts(String label, TokenId tokenId, AccountId holderId, List<Long> serials)
            throws Exception {
        var client = hieroContext.getClient();
        long nftBalance =
                new AccountBalanceQuery()
                        .setAccountId(holderId)
                        .execute(client)
                        .tokens
                        .getOrDefault(tokenId, 0L);

        System.out.println("\n=== " + label + " ===");
        System.out.println("Holder " + holderId + " NFT balance for " + tokenId + ": " + nftBalance);
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
            return "wiped/destroyed (query failed: " + e.getClass().getSimpleName() + ")";
        }
    }
}
