package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SDK tutorial for {@link TokenWipeTransaction} — removes NFT serials from a non-treasury account.
 *
 * <p>Must be signed by the wipe key set at create time. The treasury account cannot be wiped.
 * Wiping burns the NFTs and decreases total supply.
 *
 * <p>Before/after checks use consensus {@link AccountBalanceQuery} (NFT count) and {@link
 * TokenNftInfoQuery} (owner per serial).
 *
 * @see <a href="https://docs.hedera.com/hedera/sdks-and-apis/sdks/token-service/wipe-a-token">Wipe
 *     a token</a>
 */
public class WipeNftSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

        PrivateKey adminKey = operatorKey;
        PrivateKey supplyKey = operatorKey;
        PrivateKey wipeKey = operatorKey;

        byte[] metadata = "https://example.com/nft/wipe".getBytes(StandardCharsets.UTF_8);

        // Step 1: create an NFT type with a wipe key.
        TokenId tokenId =
                new TokenCreateTransaction()
                        .setTokenName("Wipe Demo NFT")
                        .setTokenSymbol("WDNFT")
                        .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                        .setTreasuryAccountId(operatorId)
                        .setAdminKey(adminKey.getPublicKey())
                        .setSupplyKey(supplyKey.getPublicKey())
                        .setWipeKey(wipeKey.getPublicKey())
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

        // Step 3: create a holder account (wipe cannot target the treasury).
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

        // Step 4: associate the token, then transfer serials off treasury.
        new TokenAssociateTransaction()
                .setAccountId(holderId)
                .setTokenIds(List.of(tokenId))
                .freezeWith(client)
                .sign(holderKey)
                .execute(client)
                .getReceipt(client);

        new TransferTransaction()
                .addNftTransfer(tokenId.nft(serial1), operatorId, holderId)
                .addNftTransfer(tokenId.nft(serial2), operatorId, holderId)
                .freezeWith(client)
                .sign(operatorKey)
                .execute(client)
                .getReceipt(client);
        System.out.println("Transferred serials to: " + holderId);

        logHolderNfts(client, "BEFORE WIPE", tokenId, holderId, List.of(serial1, serial2));

        // Step 5: wipe a single serial (docs sample pattern).
        Status wipeOneStatus =
                new TokenWipeTransaction()
                        .setTokenId(tokenId)
                        .setAccountId(holderId)
                        .setSerials(List.of(serial1))
                        .freezeWith(client)
                        .sign(wipeKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println(
                "Wiped NFT serial via TokenWipeTransaction: " + serial1 + " (" + wipeOneStatus + ")");
        logHolderNfts(
                client, "AFTER wipe (serial " + serial1 + ")", tokenId, holderId, List.of(serial1, serial2));

        // Step 6: wipe remaining serials in one call.
        Status wipeManyStatus =
                new TokenWipeTransaction()
                        .setTokenId(tokenId)
                        .setAccountId(holderId)
                        .setSerials(List.of(serial2))
                        .freezeWith(client)
                        .sign(wipeKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println(
                "Wiped NFT serial via TokenWipeTransaction: " + serial2 + " (" + wipeManyStatus + ")");
        logHolderNfts(
                client, "AFTER wipe (serial " + serial2 + ")", tokenId, holderId, List.of(serial1, serial2));
        System.out.println("Wipe status: SUCCESS");

        client.close();
    }

    private static void logHolderNfts(
            Client client, String label, TokenId tokenId, AccountId holderId, List<Long> serials)
            throws Exception {
        long nftBalance =
                new AccountBalanceQuery()
                        .setAccountId(holderId)
                        .execute(client)
                        .tokens
                        .getOrDefault(tokenId, 0L);

        System.out.println("\n=== " + label + " ===");
        System.out.println("Holder " + holderId + " NFT balance for " + tokenId + ": " + nftBalance);
        for (long serial : serials) {
            System.out.println("  serial " + serial + ": " + describeNft(client, tokenId, serial));
        }
    }

    private static String describeNft(Client client, TokenId tokenId, long serial) {
        try {
            TokenNftInfo info =
                    new TokenNftInfoQuery().setNftId(tokenId.nft(serial)).execute(client).get(0);
            return "exists, owner=" + info.accountId;
        } catch (Exception e) {
            return "wiped/destroyed (query failed: " + e.getClass().getSimpleName() + ")";
        }
    }
}
