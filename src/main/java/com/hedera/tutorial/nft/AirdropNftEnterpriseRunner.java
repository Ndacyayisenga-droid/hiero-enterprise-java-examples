package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.TokenId;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.hiero.base.AccountClient;
import org.hiero.base.HieroContext;
import org.hiero.base.NftClient;
import org.hiero.base.data.Account;
import org.hiero.base.protocol.data.AccountInfoResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Enterprise tutorial for {@link NftClient#airdropNft} / {@link NftClient#airdropNfts} — airdrops
 * NFT serials to one or more receivers.
 *
 * <p>Unlike a standard transfer, if a receiver is not associated and has no available
 * auto-association slots, the airdrop may become pending rather than failing. This demo associates
 * receivers first so the airdrops complete immediately using only enterprise clients.
 *
 * <p>Demonstrates both the single-receiver overload and the map overload that sends different
 * serials to different accounts in one {@code TokenAirdropTransaction}.
 *
 * @see <a href="https://docs.hedera.com/native/tokens/airdrop">Airdrop a token</a>
 */
@Component
@Profile("airdrop-nft")
public class AirdropNftEnterpriseRunner implements CommandLineRunner {

    private final NftClient nftClient;
    private final AccountClient accountClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public AirdropNftEnterpriseRunner(
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
        byte[] metadata = "https://example.com/nft/airdrop".getBytes(StandardCharsets.UTF_8);

        // Enterprise: create an NFT type and mint two serials.
        TokenId tokenId = nftClient.createNftType("Airdrop Demo NFT", "ADNFT");
        System.out.println("Created NFT type: " + tokenId);

        long serial1 = nftClient.mintNft(tokenId, metadata);
        long serial2 = nftClient.mintNft(tokenId, metadata);
        System.out.println("Minted NFT serials: " + serial1 + ", " + serial2);

        // Enterprise: create receivers and associate so airdrops complete immediately.
        Account alice = accountClient.createAccount(1);
        Account bob = accountClient.createAccount(1);
        nftClient.associateNft(tokenId, alice);
        nftClient.associateNft(tokenId, bob);
        System.out.println("Created and associated receivers: " + alice.accountId() + ", " + bob.accountId());

        logOwnedNfts("BEFORE AIRDROP", treasury, alice, bob);

        // Enterprise: airdrop one serial to a single receiver.
        nftClient.airdropNft(tokenId, serial1, treasury, alice.accountId());
        System.out.println("Airdropped serial " + serial1 + " via NftClient.airdropNft() -> " + alice.accountId());
        logOwnedNfts("AFTER airdropNft", treasury, alice, bob);

        // Enterprise: airdrop different serials to different receivers in one call (map overload).
        nftClient.airdropNfts(tokenId, Map.of(serial2, bob.accountId()), treasury);
        System.out.println(
                "Airdropped serial " + serial2 + " via NftClient.airdropNfts(Map) -> " + bob.accountId());
        logOwnedNfts("AFTER airdropNfts (map)", treasury, alice, bob);
        System.out.println("Airdrop status: SUCCESS");

        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private void logOwnedNfts(String label, Account treasury, Account alice, Account bob)
            throws Exception {
        AccountInfoResponse treasuryInfo = accountClient.getAccountInfo(treasury.accountId());
        AccountInfoResponse aliceInfo = accountClient.getAccountInfo(alice.accountId());
        AccountInfoResponse bobInfo = accountClient.getAccountInfo(bob.accountId());

        System.out.println("\n=== " + label + " ===");
        System.out.println("Treasury " + treasury.accountId() + " ownedNfts: " + treasuryInfo.ownedNfts());
        System.out.println("Alice    " + alice.accountId() + " ownedNfts: " + aliceInfo.ownedNfts());
        System.out.println("Bob      " + bob.accountId() + " ownedNfts: " + bobInfo.ownedNfts());
    }
}
