package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * SDK tutorial for {@link TokenUpdateTransaction} — updates an NFT token class name and symbol.
 *
 * <p>Must be signed by the admin key set at create time. Fields that are not set are left unchanged.
 *
 * @see <a href="https://docs.hedera.com/native/tokens/update">Update a token</a>
 */
public class UpdateNftTypeSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

        PrivateKey adminKey = operatorKey;
        PrivateKey supplyKey = operatorKey;

        String initialName = "Update Demo NFT";
        String initialSymbol = "UDNFT";
        String updatedName = "Updated Demo NFT";
        String updatedSymbol = "UDNFT2";

        // Step 1: create an NFT type with an admin key so it can be updated later.
        TokenId tokenId =
                new TokenCreateTransaction()
                        .setTokenName(initialName)
                        .setTokenSymbol(initialSymbol)
                        .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                        .setTreasuryAccountId(operatorId)
                        .setAdminKey(adminKey.getPublicKey())
                        .setSupplyKey(supplyKey.getPublicKey())
                        .freezeWith(client)
                        .sign(adminKey)
                        .execute(client)
                        .getReceipt(client)
                        .tokenId;

        if (tokenId == null) {
            throw new IllegalStateException("Token create receipt did not contain a token ID");
        }
        System.out.println("Created NFT type: " + tokenId);
        System.out.println("Initial name:    " + initialName);
        System.out.println("Initial symbol:  " + initialSymbol);

        // Step 2: update the NFT type (docs sample).
        Status transactionStatus =
                new TokenUpdateTransaction()
                        .setTokenId(tokenId)
                        .setTokenName(updatedName)
                        .setTokenSymbol(updatedSymbol)
                        .freezeWith(client)
                        .sign(adminKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;

        System.out.println("Updated NFT type: " + tokenId);
        System.out.println("Updated name:    " + updatedName);
        System.out.println("Updated symbol:  " + updatedSymbol);
        System.out.println("Update status:   " + transactionStatus);

        client.close();
    }
}
