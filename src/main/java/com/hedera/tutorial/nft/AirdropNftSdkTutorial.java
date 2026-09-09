package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SDK tutorial for {@link TokenAirdropTransaction} — airdrops NFT serials to one or more receivers.
 *
 * <p>Unlike a standard transfer, if a receiver is not associated and has no available
 * auto-association slots, the airdrop becomes pending instead of failing. This demo creates
 * receivers with unlimited auto-associations ({@code -1}) so the airdrop completes immediately.
 *
 * <p>Demonstrates airdropping different serials to different accounts in one transaction via
 * multiple {@code addNftTransfer} calls.
 *
 * @see <a href="https://docs.hedera.com/native/tokens/airdrop">Airdrop a token</a>
 */
public class AirdropNftSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

        PrivateKey adminKey = operatorKey;
        PrivateKey supplyKey = operatorKey;

        byte[] metadata = "https://example.com/nft/airdrop".getBytes(StandardCharsets.UTF_8);

        // Step 1: create an NFT type.
        TokenId tokenId =
                new TokenCreateTransaction()
                        .setTokenName("Airdrop Demo NFT")
                        .setTokenSymbol("ADNFT")
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

        // Step 3: create receivers with unlimited auto-associations (no prior associate needed).
        AccountId aliceId = createReceiver(client);
        AccountId bobId = createReceiver(client);
        System.out.println("Created receivers with unlimited auto-associations: " + aliceId + ", " + bobId);

        logNfts(client, "BEFORE AIRDROP", tokenId, operatorId, aliceId, bobId, List.of(serial1, serial2));

        // Step 4: airdrop different serials to different receivers in one transaction.
        Status airdropStatus =
                new TokenAirdropTransaction()
                        .addNftTransfer(tokenId.nft(serial1), operatorId, aliceId)
                        .addNftTransfer(tokenId.nft(serial2), operatorId, bobId)
                        .freezeWith(client)
                        .sign(operatorKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println(
                "Airdropped serials via TokenAirdropTransaction: "
                        + serial1
                        + " -> "
                        + aliceId
                        + ", "
                        + serial2
                        + " -> "
                        + bobId
                        + " ("
                        + airdropStatus
                        + ")");
        logNfts(client, "AFTER AIRDROP", tokenId, operatorId, aliceId, bobId, List.of(serial1, serial2));
        System.out.println("Airdrop status: SUCCESS");

        client.close();
    }

    private static AccountId createReceiver(Client client) throws Exception {
        PrivateKey receiverKey = PrivateKey.generateED25519();
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

    private static void logNfts(
            Client client,
            String label,
            TokenId tokenId,
            AccountId treasuryId,
            AccountId aliceId,
            AccountId bobId,
            List<Long> serials)
            throws Exception {
        long treasuryBalance = nftBalance(client, treasuryId, tokenId);
        long aliceBalance = nftBalance(client, aliceId, tokenId);
        long bobBalance = nftBalance(client, bobId, tokenId);

        System.out.println("\n=== " + label + " ===");
        System.out.println("Treasury " + treasuryId + " NFT balance for " + tokenId + ": " + treasuryBalance);
        System.out.println("Alice    " + aliceId + " NFT balance for " + tokenId + ": " + aliceBalance);
        System.out.println("Bob      " + bobId + " NFT balance for " + tokenId + ": " + bobBalance);
        for (long serial : serials) {
            System.out.println("  serial " + serial + ": " + describeNft(client, tokenId, serial));
        }
    }

    private static long nftBalance(Client client, AccountId accountId, TokenId tokenId) throws Exception {
        return new AccountBalanceQuery()
                .setAccountId(accountId)
                .execute(client)
                .tokens
                .getOrDefault(tokenId, 0L);
    }

    private static String describeNft(Client client, TokenId tokenId, long serial) {
        try {
            TokenNftInfo info =
                    new TokenNftInfoQuery().setNftId(tokenId.nft(serial)).execute(client).get(0);
            return "exists, owner=" + info.accountId;
        } catch (Exception e) {
            return "query failed: " + e.getClass().getSimpleName();
        }
    }
}
