package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SDK tutorial for {@link TokenUpdateNftsTransaction} — updates metadata on NFT serials.
 *
 * <p>Must be signed by the metadata key set at create time. Metadata is limited to 100 bytes; at
 * most 10 serials per call.
 *
 * @see <a href="https://docs.hedera.com/native/tokens/update-nft-metadata">Update NFT metadata</a>
 */
public class UpdateNftMetadataSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

        PrivateKey adminKey = operatorKey;
        PrivateKey supplyKey = operatorKey;
        PrivateKey metadataKey = PrivateKey.generateED25519();

        byte[] initialMetadata = "https://example.com/nft/old".getBytes(StandardCharsets.UTF_8);
        byte[] updatedMetadata = "https://example.com/nft/new".getBytes(StandardCharsets.UTF_8);

        // Step 1: create an NFT type with a metadata key so serial metadata can be updated later.
        TokenId tokenId =
                new TokenCreateTransaction()
                        .setTokenName("Update Metadata Demo NFT")
                        .setTokenSymbol("UMDNFT")
                        .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                        .setTreasuryAccountId(operatorId)
                        .setAdminKey(adminKey.getPublicKey())
                        .setSupplyKey(supplyKey.getPublicKey())
                        .setMetadataKey(metadataKey.getPublicKey())
                        .freezeWith(client)
                        .sign(adminKey)
                        .execute(client)
                        .getReceipt(client)
                        .tokenId;

        if (tokenId == null) {
            throw new IllegalStateException("Token create receipt did not contain a token ID");
        }
        System.out.println("Created NFT type: " + tokenId);

        // Step 2: mint serials with initial metadata.
        List<Long> serials =
                new TokenMintTransaction()
                        .setTokenId(tokenId)
                        .setMetadata(List.of(initialMetadata, initialMetadata))
                        .freezeWith(client)
                        .sign(supplyKey)
                        .execute(client)
                        .getReceipt(client)
                        .serials;
        System.out.println("Minted NFT serials: " + serials);
        System.out.println("Initial metadata:   " + new String(initialMetadata, StandardCharsets.UTF_8));

        // Step 3: update NFT metadata (docs sample).
        Status transactionStatus =
                new TokenUpdateNftsTransaction()
                        .setTokenId(tokenId)
                        .setSerials(serials)
                        .setMetadata(updatedMetadata)
                        .freezeWith(client)
                        .sign(metadataKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;

        System.out.println("Updated metadata for serials: " + serials);
        System.out.println("Updated metadata:   " + new String(updatedMetadata, StandardCharsets.UTF_8));
        System.out.println("Update status:      " + transactionStatus);

        client.close();
    }
}
