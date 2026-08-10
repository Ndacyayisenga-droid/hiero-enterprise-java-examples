package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TokenId;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hiero.base.AccountClient;
import org.hiero.base.NftClient;
import org.hiero.base.data.Account;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Enterprise tutorial for {@link NftClient#updateNftMetadata} / {@link
 * NftClient#updateNftsMetadata} — updates metadata on NFT serials.
 *
 * <p>Creates the NFT type with a dedicated metadata key, transfers a serial out of treasury, then
 * updates metadata with that key (required after transfer; see HIP-850). Metadata is limited to 100
 * bytes; at most 10 serials per call.
 *
 * @see <a href="https://docs.hedera.com/native/tokens/update-nft-metadata">Update NFT metadata</a>
 */
@Component
@Profile("update-nft-metadata")
public class UpdateNftMetadataEnterpriseRunner implements CommandLineRunner {

    private final NftClient nftClient;
    private final AccountClient accountClient;
    private final ConfigurableApplicationContext context;

    public UpdateNftMetadataEnterpriseRunner(
            NftClient nftClient,
            AccountClient accountClient,
            ConfigurableApplicationContext context) {
        this.nftClient = nftClient;
        this.accountClient = accountClient;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        byte[] initialMetadata = "https://example.com/nft/old".getBytes(StandardCharsets.UTF_8);
        byte[] updatedMetadata = "https://example.com/nft/new".getBytes(StandardCharsets.UTF_8);

        PrivateKey supplyKey = PrivateKey.generateED25519();
        PrivateKey metadataKey = PrivateKey.generateED25519();

        // Enterprise: create with a metadata key so serial metadata can be updated after transfer.
        Account treasury = accountClient.createAccount(10);
        TokenId tokenId =
                nftClient.createNftType(
                        "Update Metadata Demo NFT", "UMDNFT", treasury, supplyKey, metadataKey);
        System.out.println("Created NFT type: " + tokenId);

        long serial1 = nftClient.mintNft(tokenId, supplyKey, initialMetadata);
        long serial2 = nftClient.mintNft(tokenId, supplyKey, initialMetadata);
        System.out.println("Minted NFT serials: " + serial1 + ", " + serial2);
        System.out.println("Initial metadata:   " + new String(initialMetadata, StandardCharsets.UTF_8));

        // Transfer out of treasury — supply key can no longer update metadata; metadata key can.
        Account receiver = accountClient.createAccount(1);
        nftClient.associateNft(tokenId, receiver);
        nftClient.transferNft(tokenId, serial1, treasury, receiver.accountId());
        nftClient.transferNft(tokenId, serial2, treasury, receiver.accountId());
        System.out.println("Transferred serials to: " + receiver.accountId());

        // Enterprise: update a single serial with the metadata key.
        nftClient.updateNftMetadata(tokenId, serial1, metadataKey, updatedMetadata);
        System.out.println("Updated metadata for serial " + serial1);

        // Enterprise: update multiple serials in one call (max 10).
        nftClient.updateNftsMetadata(
                tokenId, List.of(serial1, serial2), metadataKey, updatedMetadata);
        System.out.println("Updated metadata for serials " + serial1 + ", " + serial2);
        System.out.println("Updated metadata:   " + new String(updatedMetadata, StandardCharsets.UTF_8));
        System.out.println("Update status:      SUCCESS");

        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
