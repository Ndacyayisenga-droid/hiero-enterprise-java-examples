package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.*;
import com.hedera.tutorial.util.TestnetQueryHelper;
import com.hedera.tutorial.util.TutorialClient;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SDK tutorial for {@link AccountAllowanceApproveTransaction} NFT allowances — an owner delegates
 * NFT spending to a spender, who can then transfer the NFTs on the owner's behalf.
 *
 * <p>Three approval shapes are shown:
 *
 * <ul>
 *   <li>{@code approveTokenNftAllowance} for a single serial
 *   <li>{@code approveTokenNftAllowance} called repeatedly for several serials
 *   <li>{@code approveTokenNftAllowanceAllSerials} for every serial of an NFT type, including
 *       serials the owner receives later
 * </ul>
 *
 * <p>Each approval is proven by letting the spender move the NFT with {@code
 * addApprovedNftTransfer}. The spender pays for that transfer, so the transaction ID is generated
 * for the spender account.
 *
 * @see <a href="https://docs.hedera.com/native/accounts/approve-allowance">Approve an allowance</a>
 */
public class ApproveNftAllowanceSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = TutorialClient.forConfiguredNetwork();
        TestnetQueryHelper.tuneClientForTestnet(client);
        client.setOperator(operatorId, operatorKey);

        byte[] metadata = "https://example.com/nft/allowance".getBytes(StandardCharsets.UTF_8);

        // Step 1: create an NFT type with the operator as treasury, admin and supply key.
        TokenId tokenId = createNftType(client, operatorId, operatorKey, "Allowance Demo NFT", "ALNFT");
        System.out.println("Created NFT type: " + tokenId);

        // Step 2: mint three serials.
        List<Long> serials =
                new TokenMintTransaction()
                        .setTokenId(tokenId)
                        .setMetadata(List.of(metadata, metadata, metadata))
                        .freezeWith(client)
                        .sign(operatorKey)
                        .execute(client)
                        .getReceipt(client)
                        .serials;
        long serial1 = serials.get(0);
        long serial2 = serials.get(1);
        long serial3 = serials.get(2);
        System.out.println("Minted NFT serials: " + serial1 + ", " + serial2 + ", " + serial3);

        // Step 3: create owner, spender and receiver. Unlimited auto-associations (-1) keeps the
        // owner and receiver able to hold the NFTs without an explicit associate transaction.
        PrivateKey ownerKey = PrivateKey.generateED25519();
        AccountId ownerId = createAccount(client, ownerKey, Hbar.from(2), -1);
        PrivateKey spenderKey = PrivateKey.generateED25519();
        AccountId spenderId = createAccount(client, spenderKey, Hbar.from(5), 0);
        PrivateKey receiverKey = PrivateKey.generateED25519();
        AccountId receiverId = createAccount(client, receiverKey, Hbar.from(1), -1);
        System.out.println("Owner:    " + ownerId);
        System.out.println("Spender:  " + spenderId);
        System.out.println("Receiver: " + receiverId);

        // Step 4: move all three serials from the treasury to the owner.
        new TransferTransaction()
                .addNftTransfer(tokenId.nft(serial1), operatorId, ownerId)
                .addNftTransfer(tokenId.nft(serial2), operatorId, ownerId)
                .addNftTransfer(tokenId.nft(serial3), operatorId, ownerId)
                .freezeWith(client)
                .sign(operatorKey)
                .execute(client)
                .getReceipt(client);
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner("AFTER TRANSFER TO OWNER (serial " + serial1 + ")", client, tokenId, serial1, ownerId);

        // Step 5: owner approves the spender for a single serial.
        Status singleStatus =
                new AccountAllowanceApproveTransaction()
                        .approveTokenNftAllowance(tokenId.nft(serial1), ownerId, spenderId)
                        .freezeWith(client)
                        .sign(ownerKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println("\nApproved serial " + serial1 + " for spender (" + singleStatus + ")");

        // Step 6: owner approves the spender for several serials in one transaction.
        Status multiStatus =
                new AccountAllowanceApproveTransaction()
                        .approveTokenNftAllowance(tokenId.nft(serial2), ownerId, spenderId)
                        .approveTokenNftAllowance(tokenId.nft(serial3), ownerId, spenderId)
                        .freezeWith(client)
                        .sign(ownerKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println("Approved serials " + serial2 + ", " + serial3 + " for spender (" + multiStatus + ")");

        // Step 7: the spender moves serial1 and serial2 to the receiver using the allowance.
        spendApprovedNft(client, tokenId, serial1, ownerId, receiverId, spenderId, spenderKey);
        spendApprovedNft(client, tokenId, serial2, ownerId, receiverId, spenderId, spenderKey);
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner("AFTER APPROVED SPEND (serial " + serial1 + ")", client, tokenId, serial1, receiverId);
        TestnetQueryHelper.logNftOwner("AFTER APPROVED SPEND (serial " + serial2 + ")", client, tokenId, serial2, receiverId);

        // Step 8: an all-serials allowance on a second NFT type. It covers every serial the owner
        // holds now and every serial the owner receives in the future.
        TokenId allSerialsTokenId =
                createNftType(client, operatorId, operatorKey, "Allowance All Demo NFT", "ALLNFT");
        long allSerial =
                new TokenMintTransaction()
                        .setTokenId(allSerialsTokenId)
                        .setMetadata(List.of(metadata))
                        .freezeWith(client)
                        .sign(operatorKey)
                        .execute(client)
                        .getReceipt(client)
                        .serials
                        .get(0);
        new TransferTransaction()
                .addNftTransfer(allSerialsTokenId.nft(allSerial), operatorId, ownerId)
                .freezeWith(client)
                .sign(operatorKey)
                .execute(client)
                .getReceipt(client);
        System.out.println("\nCreated NFT type " + allSerialsTokenId + " with serial " + allSerial + " held by owner");

        Status allSerialsStatus =
                new AccountAllowanceApproveTransaction()
                        .approveTokenNftAllowanceAllSerials(allSerialsTokenId, ownerId, spenderId)
                        .freezeWith(client)
                        .sign(ownerKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println("Approved ALL serials of " + allSerialsTokenId + " for spender (" + allSerialsStatus + ")");

        spendApprovedNft(client, allSerialsTokenId, allSerial, ownerId, receiverId, spenderId, spenderKey);
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner(
                "AFTER ALL-SERIALS SPEND (serial " + allSerial + ")", client, allSerialsTokenId, allSerial, receiverId);

        // Cleanup: the allowances on serial1 and serial2 were consumed by the approved transfers,
        // and serial3 was never spent, so its serial-level allowance is still live. Deleting it
        // needs the owner to still hold the NFT.
        new AccountAllowanceDeleteTransaction()
                .deleteAllTokenNftAllowances(tokenId.nft(serial3), ownerId)
                .freezeWith(client)
                .sign(ownerKey)
                .execute(client)
                .getReceipt(client);
        System.out.println("\nDeleted the serial-level allowance on serial " + serial3);

        // An all-serials grant outlives the transfer, so revoke it explicitly. This is the approve
        // transaction, not the delete one: deleteTokenNftAllowanceAllSerials clears the grant for
        // this owner/spender pair without needing the owner to hold any particular serial.
        new AccountAllowanceApproveTransaction()
                .deleteTokenNftAllowanceAllSerials(allSerialsTokenId, ownerId, spenderId)
                .freezeWith(client)
                .sign(ownerKey)
                .execute(client)
                .getReceipt(client);
        System.out.println("Revoked the all-serials allowance on " + allSerialsTokenId);
        System.out.println("\nApprove NFT allowance status: SUCCESS");

        client.close();
    }

    private static void spendApprovedNft(
            Client client,
            TokenId tokenId,
            long serial,
            AccountId ownerId,
            AccountId receiverId,
            AccountId spenderId,
            PrivateKey spenderKey)
            throws Exception {
        TransferTransaction transfer = new TransferTransaction();
        transfer.addApprovedNftTransfer(tokenId.nft(serial), ownerId, receiverId);
        // The spender pays for the transfer, so the transaction ID belongs to the spender.
        transfer.setTransactionId(TransactionId.generate(spenderId));

        Status status =
                transfer.freezeWith(client).sign(spenderKey).execute(client).getReceipt(client).status;
        if (status != Status.SUCCESS) {
            throw new IllegalStateException("Approved transfer of serial " + serial + " failed with " + status);
        }
        System.out.println("Spender moved serial " + serial + " to " + receiverId + " using the allowance (" + status + ")");
    }

    private static TokenId createNftType(
            Client client, AccountId treasuryId, PrivateKey treasuryKey, String name, String symbol)
            throws Exception {
        TokenId tokenId =
                new TokenCreateTransaction()
                        .setTokenName(name)
                        .setTokenSymbol(symbol)
                        .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                        .setTreasuryAccountId(treasuryId)
                        .setAdminKey(treasuryKey.getPublicKey())
                        .setSupplyKey(treasuryKey.getPublicKey())
                        .freezeWith(client)
                        .sign(treasuryKey)
                        .execute(client)
                        .getReceipt(client)
                        .tokenId;

        if (tokenId == null) {
            throw new IllegalStateException("Token create receipt did not contain a token ID");
        }
        return tokenId;
    }

    private static AccountId createAccount(
            Client client, PrivateKey accountKey, Hbar initialBalance, int maxAutoAssociations)
            throws Exception {
        AccountId accountId =
                new AccountCreateTransaction()
                        .setKeyWithoutAlias(accountKey)
                        .setInitialBalance(initialBalance)
                        .setMaxAutomaticTokenAssociations(maxAutoAssociations)
                        .execute(client)
                        .getReceipt(client)
                        .accountId;

        if (accountId == null) {
            throw new IllegalStateException("Account create receipt did not contain an account ID");
        }
        return accountId;
    }
}
