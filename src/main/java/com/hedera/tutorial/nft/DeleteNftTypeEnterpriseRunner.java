package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.TokenId;
import java.nio.charset.StandardCharsets;
import org.hiero.base.NftClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Enterprise tutorial for {@link NftClient#deleteNftType} — deletes an NFT token class.
 *
 * <p>You cannot delete a specific NFT serial. Burn all serials first, then delete the type. The
 * operator key is used as admin (the default for {@link NftClient#createNftType}).
 *
 * @see <a href="https://docs.hedera.com/native/tokens/delete">Delete a token</a>
 */
@Component
@Profile("delete-nft-type")
public class DeleteNftTypeEnterpriseRunner implements CommandLineRunner {

    private final NftClient nftClient;
    private final ConfigurableApplicationContext context;

    public DeleteNftTypeEnterpriseRunner(
            NftClient nftClient, ConfigurableApplicationContext context) {
        this.nftClient = nftClient;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        // Enterprise: create an NFT type (token class). Admin key defaults to the operator.
        TokenId tokenId = nftClient.createNftType("Delete Demo NFT", "DDNFT");
        System.out.println("Created NFT type: " + tokenId);

        // Mint a serial so we can show the burn-before-delete requirement.
        byte[] metadata = "https://example.com/nft/1".getBytes(StandardCharsets.UTF_8);
        long serial = nftClient.mintNft(tokenId, metadata);
        System.out.println("Minted NFT serial: " + serial);

        // Burn all serials of the type before deleting the class.
        nftClient.burnNft(tokenId, serial);
        System.out.println("Burned NFT serial: " + serial);

        // Enterprise: delete the NFT type (TokenDeleteTransaction under the hood).
        nftClient.deleteNftType(tokenId);
        System.out.println("Deleted NFT type via NftClient.deleteNftType(): " + tokenId);

        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
