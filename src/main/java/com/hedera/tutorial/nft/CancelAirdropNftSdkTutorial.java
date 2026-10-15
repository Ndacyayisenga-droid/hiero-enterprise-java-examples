package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.*;
import com.hedera.tutorial.util.TestnetQueryHelper;
import com.hedera.tutorial.util.TutorialClient;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SDK tutorial for {@link TokenCancelAirdropTransaction} — cancels a pending NFT airdrop.
 *
 * @see <a href="https://docs.hedera.com/native/tokens/cancel">Cancel a token</a>
 */
public class CancelAirdropNftSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = TutorialClient.forConfiguredNetwork();
        TestnetQueryHelper.tuneClientForTestnet(client);
        client.setOperator(operatorId, operatorKey);

        PrivateKey adminKey = operatorKey;
        PrivateKey supplyKey = operatorKey;

        byte[] metadata = "https://example.com/nft/cancel-airdrop".getBytes(StandardCharsets.UTF_8);

        TokenId tokenId =
                new TokenCreateTransaction()
                        .setTokenName("Cancel Airdrop Demo NFT")
                        .setTokenSymbol("CANFT")
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

        List<Long> serials =
                new TokenMintTransaction()
                        .setTokenId(tokenId)
                        .setMetadata(List.of(metadata))
                        .freezeWith(client)
                        .sign(supplyKey)
                        .execute(client)
                        .getReceipt(client)
                        .serials;
        long serial = serials.get(0);
        System.out.println("Minted NFT serial: " + serial);

        AccountId receiverId = createReceiverWithNoAutoAssociations(client);
        System.out.println("Created receiver (0 auto-associations): " + receiverId);
        TestnetQueryHelper.sleepForConsensus();

        TestnetQueryHelper.logNftOwner("BEFORE AIRDROP", client, tokenId, serial, operatorId);

        TransactionRecord airdropRecord =
                new TokenAirdropTransaction()
                        .addNftTransfer(tokenId.nft(serial), operatorId, receiverId)
                        .freezeWith(client)
                        .sign(operatorKey)
                        .execute(client)
                        .getRecord(client);

        if (airdropRecord.pendingAirdropRecords.isEmpty()) {
            throw new IllegalStateException(
                    "Expected pending airdrop record; receiver may have auto-associated the token");
        }
        PendingAirdropId pendingId = airdropRecord.pendingAirdropRecords.get(0).getPendingAirdropId();
        System.out.println(
                "Airdropped serial "
                        + serial
                        + " (pending airdrop id: "
                        + pendingId
                        + ", status "
                        + airdropRecord.receipt.status
                        + ")");
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner("AFTER AIRDROP (pending)", client, tokenId, serial, operatorId);

        Status cancelStatus =
                new TokenCancelAirdropTransaction()
                        .addPendingAirdrop(pendingId)
                        .freezeWith(client)
                        .sign(operatorKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        if (cancelStatus != Status.SUCCESS) {
            throw new IllegalStateException("Cancel airdrop failed with status " + cancelStatus);
        }
        System.out.println("Canceled pending airdrop via TokenCancelAirdropTransaction (" + cancelStatus + ")");
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner("AFTER CANCEL", client, tokenId, serial, operatorId);
        System.out.println("Cancel airdrop status: SUCCESS");

        client.close();
    }

    private static AccountId createReceiverWithNoAutoAssociations(Client client) throws Exception {
        PrivateKey receiverKey = PrivateKey.generateED25519();
        AccountId receiverId =
                new AccountCreateTransaction()
                        .setKey(receiverKey)
                        .setInitialBalance(Hbar.from(1))
                        .setMaxAutomaticTokenAssociations(0)
                        .execute(client)
                        .getReceipt(client)
                        .accountId;

        if (receiverId == null) {
            throw new IllegalStateException("Account create receipt did not contain an account ID");
        }
        return receiverId;
    }
}
