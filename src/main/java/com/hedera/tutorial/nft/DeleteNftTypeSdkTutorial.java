package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * SDK tutorial for {@link TokenDeleteTransaction} — deletes an NFT token class.
 *
 * <p>You cannot delete a specific NFT. Burn all serials first, then delete the class identified by
 * the token ID. Must be signed by the admin key set at create time.
 *
 * @see <a href="https://docs.hedera.com/native/tokens/delete">Delete a token</a>
 */
public class DeleteNftTypeSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

        // Admin + supply keys (operator is treasury and fee payer).
        PrivateKey adminKey = operatorKey;
        PrivateKey supplyKey = operatorKey;

        // Step 1: create an NFT type (token class) with an admin key so it can be deleted later.
        TokenId tokenId =
                new TokenCreateTransaction()
                        .setTokenName("Delete Demo NFT")
                        .setTokenSymbol("DDNFT")
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

        // Step 2: mint one NFT (serial).
        long serial =
                new TokenMintTransaction()
                        .setTokenId(tokenId)
                        .addMetadata("https://example.com/nft/1".getBytes(StandardCharsets.UTF_8))
                        .freezeWith(client)
                        .sign(supplyKey)
                        .execute(client)
                        .getReceipt(client)
                        .serials
                        .get(0);
        System.out.println("Minted NFT serial: " + serial);

        // Step 3: burn all serials before deleting the class.
        Status burnStatus =
                new TokenBurnTransaction()
                        .setTokenId(tokenId)
                        .setSerials(Collections.singletonList(serial))
                        .freezeWith(client)
                        .sign(supplyKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println("Burned NFT serial: " + serial + " (" + burnStatus + ")");

        // Step 4: delete the NFT type (docs sample).
        TokenDeleteTransaction transaction = new TokenDeleteTransaction().setTokenId(tokenId);

        TransactionResponse txResponse =
                transaction.freezeWith(client).sign(adminKey).execute(client);

        TransactionReceipt receipt = txResponse.getReceipt(client);
        Status transactionStatus = receipt.status;

        System.out.println("The transaction consensus status is " + transactionStatus);
        System.out.println("Deleted NFT type: " + tokenId);

        client.close();
    }
}
