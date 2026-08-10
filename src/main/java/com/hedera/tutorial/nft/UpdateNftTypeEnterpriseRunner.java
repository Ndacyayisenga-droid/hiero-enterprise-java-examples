package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.TokenId;
import org.hiero.base.NftClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Enterprise tutorial for {@link NftClient#updateNftType} — updates an NFT token class name and
 * symbol.
 *
 * <p>The operator key is used as admin (the default for {@link NftClient#createNftType}).
 *
 * @see <a href="https://docs.hedera.com/native/tokens/update">Update a token</a>
 */
@Component
@Profile("update-nft-type")
public class UpdateNftTypeEnterpriseRunner implements CommandLineRunner {

    private final NftClient nftClient;
    private final ConfigurableApplicationContext context;

    public UpdateNftTypeEnterpriseRunner(
            NftClient nftClient, ConfigurableApplicationContext context) {
        this.nftClient = nftClient;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        String initialName = "Update Demo NFT";
        String initialSymbol = "UDNFT";
        String updatedName = "Updated Demo NFT";
        String updatedSymbol = "UDNFT2";

        // Enterprise: create an NFT type (token class). Admin key defaults to the operator.
        TokenId tokenId = nftClient.createNftType(initialName, initialSymbol);
        System.out.println("Created NFT type: " + tokenId);
        System.out.println("Initial name:    " + initialName);
        System.out.println("Initial symbol:  " + initialSymbol);

        // Enterprise: update name and symbol (TokenUpdateTransaction under the hood).
        nftClient.updateNftType(tokenId, updatedName, updatedSymbol);
        System.out.println("Updated NFT type: " + tokenId);
        System.out.println("Updated name:    " + updatedName);
        System.out.println("Updated symbol:  " + updatedSymbol);
        System.out.println("Update status:   SUCCESS");

        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
