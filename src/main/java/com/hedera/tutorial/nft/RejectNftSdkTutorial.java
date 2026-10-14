package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.*;
import com.hedera.tutorial.util.TestnetQueryHelper;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SDK tutorial for {@link TokenRejectTransaction} — rejects airdropped NFTs and returns them to the
 * treasury.
 *
 * <p>The receiver is created with unlimited auto-associations ({@code -1}) so the airdrop completes
 * immediately. The receiver then rejects both serials in one transaction. Rejection charges no
 * custom fees and does not dissociate the receiver from the token.
 *
 * @see <a href="https://docs.hedera.com/native/tokens/reject-airdrop">Reject a token</a>
 */
public class RejectNftSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        TestnetQueryHelper.tuneClientForTestnet(client);
        client.setOperator(operatorId, operatorKey);

        PrivateKey adminKey = operatorKey;
        PrivateKey supplyKey = operatorKey;

        byte[] metadata = "https://example.com/nft/reject".getBytes(StandardCharsets.UTF_8);

        // Step 1: create an NFT type with the operator as treasury.
        TokenId tokenId =
                new TokenCreateTransaction()
                        .setTokenName("Reject Demo NFT")
                        .setTokenSymbol("RJNFT")
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

        // Step 2: mint two serials.
        List<Long> serials =
                new TokenMintTransaction()
                        .setTokenId(tokenId)
                        .setMetadata(List.of(metadata, metadata))
                        .freezeWith(client)
                        .sign(supplyKey)
                        .execute(client)
                        .getReceipt(client)
                        .serials;
        long serial1 = serials.get(0);
        long serial2 = serials.get(1);
        System.out.println("Minted NFT serials: " + serial1 + ", " + serial2);

        // Step 3: create a receiver with unlimited auto-associations so the airdrop completes.
        PrivateKey receiverKey = PrivateKey.generateED25519();
        AccountId receiverId = createReceiver(client, receiverKey);
        System.out.println("Created receiver with unlimited auto-associations: " + receiverId);

        // Step 4: airdrop both serials to the receiver.
        Status airdropStatus =
                new TokenAirdropTransaction()
                        .addNftTransfer(tokenId.nft(serial1), operatorId, receiverId)
                        .addNftTransfer(tokenId.nft(serial2), operatorId, receiverId)
                        .freezeWith(client)
                        .sign(operatorKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println(
                "Airdropped serials " + serial1 + ", " + serial2 + " -> " + receiverId + " (" + airdropStatus + ")");
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner("AFTER AIRDROP (serial " + serial1 + ")", client, tokenId, serial1, receiverId);
        TestnetQueryHelper.logNftOwner("AFTER AIRDROP (serial " + serial2 + ")", client, tokenId, serial2, receiverId);

        // Step 5: the receiver rejects both serials; they go back to the treasury.
        Status rejectStatus =
                new TokenRejectTransaction()
                        .setOwnerId(receiverId)
                        .setNftIds(List.of(tokenId.nft(serial1), tokenId.nft(serial2)))
                        .freezeWith(client)
                        .sign(receiverKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        if (rejectStatus != Status.SUCCESS) {
            throw new IllegalStateException("Reject failed with status " + rejectStatus);
        }
        System.out.println("Rejected serials via TokenRejectTransaction (" + rejectStatus + ")");
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner("AFTER REJECT (serial " + serial1 + ")", client, tokenId, serial1, operatorId);
        TestnetQueryHelper.logNftOwner("AFTER REJECT (serial " + serial2 + ")", client, tokenId, serial2, operatorId);
        System.out.println("Reject NFT status: SUCCESS");

        client.close();
    }

    private static AccountId createReceiver(Client client, PrivateKey receiverKey) throws Exception {
        AccountId receiverId =
                new AccountCreateTransaction()
                        .setKey(receiverKey)
                        .setInitialBalance(Hbar.from(1))
                        .setMaxAutomaticTokenAssociations(-1)
                        .execute(client)
                        .getReceipt(client)
                        .accountId;

        if (receiverId == null) {
            throw new IllegalStateException("Account create receipt did not contain an account ID");
        }
        return receiverId;
    }
}
