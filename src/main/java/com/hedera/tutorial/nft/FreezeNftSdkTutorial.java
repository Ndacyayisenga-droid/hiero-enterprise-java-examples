package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SDK tutorial for {@link TokenFreezeTransaction} and {@link TokenUnfreezeTransaction} — freezes
 * and unfreezes an account for an NFT type.
 *
 * <p>Must be signed by the freeze key set at create time. While frozen, the account cannot send or
 * receive NFTs of that type.
 *
 * @see <a href="https://docs.hedera.com/hedera/sdks-and-apis/sdks/token-service/freeze-an-account">Freeze
 *     an account</a>
 * @see <a href="https://docs.hedera.com/hedera/sdks-and-apis/sdks/token-service/unfreeze-an-account">Unfreeze
 *     an account</a>
 */
public class FreezeNftSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

        PrivateKey adminKey = operatorKey;
        PrivateKey supplyKey = operatorKey;
        PrivateKey freezeKey = operatorKey;

        byte[] metadata = "https://example.com/nft/freeze".getBytes(StandardCharsets.UTF_8);

        // Step 1: create an NFT type with a freeze key.
        TokenId tokenId =
                new TokenCreateTransaction()
                        .setTokenName("Freeze Demo NFT")
                        .setTokenSymbol("FDNFT")
                        .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                        .setTreasuryAccountId(operatorId)
                        .setAdminKey(adminKey.getPublicKey())
                        .setSupplyKey(supplyKey.getPublicKey())
                        .setFreezeKey(freezeKey.getPublicKey())
                        .freezeWith(client)
                        .sign(adminKey)
                        .execute(client)
                        .getReceipt(client)
                        .tokenId;

        if (tokenId == null) {
            throw new IllegalStateException("Token create receipt did not contain a token ID");
        }
        System.out.println("Created NFT type: " + tokenId);

        // Step 2: mint an NFT serial.
        long serial =
                new TokenMintTransaction()
                        .setTokenId(tokenId)
                        .setMetadata(List.of(metadata))
                        .freezeWith(client)
                        .sign(supplyKey)
                        .execute(client)
                        .getReceipt(client)
                        .serials
                        .get(0);
        System.out.println("Minted NFT serial: " + serial);

        // Step 3: create a holder account and associate the token.
        PrivateKey holderKey = PrivateKey.generateED25519();
        AccountId holderId =
                new AccountCreateTransaction()
                        .setKey(holderKey)
                        .setInitialBalance(Hbar.from(1))
                        .execute(client)
                        .getReceipt(client)
                        .accountId;

        if (holderId == null) {
            throw new IllegalStateException("Account create receipt did not contain an account ID");
        }

        new TokenAssociateTransaction()
                .setAccountId(holderId)
                .setTokenIds(List.of(tokenId))
                .freezeWith(client)
                .sign(holderKey)
                .execute(client)
                .getReceipt(client);

        new TransferTransaction()
                .addNftTransfer(tokenId.nft(serial), operatorId, holderId)
                .freezeWith(client)
                .sign(operatorKey)
                .execute(client)
                .getReceipt(client);
        System.out.println("Transferred serial " + serial + " to holder: " + holderId);

        // Step 4: create a receiver for transfer attempts.
        PrivateKey receiverKey = PrivateKey.generateED25519();
        AccountId receiverId =
                new AccountCreateTransaction()
                        .setKey(receiverKey)
                        .setInitialBalance(Hbar.from(1))
                        .execute(client)
                        .getReceipt(client)
                        .accountId;

        if (receiverId == null) {
            throw new IllegalStateException("Receiver account create receipt did not contain an account ID");
        }

        new TokenAssociateTransaction()
                .setAccountId(receiverId)
                .setTokenIds(List.of(tokenId))
                .freezeWith(client)
                .sign(receiverKey)
                .execute(client)
                .getReceipt(client);

        // Step 5: transfer from holder succeeds while not frozen.
        new TransferTransaction()
                .addNftTransfer(tokenId.nft(serial), holderId, receiverId)
                .freezeWith(client)
                .sign(holderKey)
                .execute(client)
                .getReceipt(client);
        System.out.println("Transfer before freeze succeeded: holder -> receiver");

        // Move NFT back to holder for freeze demo.
        new TransferTransaction()
                .addNftTransfer(tokenId.nft(serial), receiverId, holderId)
                .freezeWith(client)
                .sign(receiverKey)
                .execute(client)
                .getReceipt(client);
        System.out.println("Returned serial to holder for freeze demo");

        // Step 6: freeze the holder account for this token.
        Status freezeStatus =
                new TokenFreezeTransaction()
                        .setTokenId(tokenId)
                        .setAccountId(holderId)
                        .freezeWith(client)
                        .sign(freezeKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println("Froze holder via TokenFreezeTransaction: " + freezeStatus);

        // Step 7: transfer from frozen holder should fail.
        try {
            new TransferTransaction()
                    .addNftTransfer(tokenId.nft(serial), holderId, receiverId)
                    .freezeWith(client)
                    .sign(holderKey)
                    .execute(client)
                    .getReceipt(client);
            System.out.println("Unexpected: transfer succeeded while account was frozen");
        } catch (Exception e) {
            System.out.println("Transfer while frozen failed as expected: " + e.getMessage());
        }

        // Step 8: unfreeze the holder account.
        Status unfreezeStatus =
                new TokenUnfreezeTransaction()
                        .setTokenId(tokenId)
                        .setAccountId(holderId)
                        .freezeWith(client)
                        .sign(freezeKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println("Unfroze holder via TokenUnfreezeTransaction: " + unfreezeStatus);

        // Step 9: transfer succeeds after unfreeze.
        Status transferStatus =
                new TransferTransaction()
                        .addNftTransfer(tokenId.nft(serial), holderId, receiverId)
                        .freezeWith(client)
                        .sign(holderKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println("Transfer after unfreeze succeeded: " + transferStatus);

        client.close();
    }
}
