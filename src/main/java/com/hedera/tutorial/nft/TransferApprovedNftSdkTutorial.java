package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.*;
import com.hedera.tutorial.util.TestnetQueryHelper;
import com.hedera.tutorial.util.TutorialClient;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SDK tutorial for {@link TransferTransaction#addApprovedNftTransfer} — a spender moves NFTs out of
 * the owner's account using an allowance the owner granted earlier.
 *
 * <p>The spender is the payer of an approved transfer: the transaction ID is generated for the
 * spender account and only the spender signs. The owner's key is not needed. One serial is moved on
 * its own, two more in a single transaction, and a fourth serial without an allowance shows the
 * transfer being rejected.
 *
 * @see <a href="https://docs.hedera.com/native/tokens/transfer">Transfer tokens</a>
 */
public class TransferApprovedNftSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = TutorialClient.forConfiguredNetwork();
        TestnetQueryHelper.tuneClientForTestnet(client);
        client.setOperator(operatorId, operatorKey);

        byte[] metadata = "https://example.com/nft/approved-transfer".getBytes(StandardCharsets.UTF_8);

        // Step 1: create owner, spender and receiver. The spender pays for the approved transfers.
        // Unlimited auto-associations (-1) lets the receiver hold the NFTs without an associate step.
        PrivateKey ownerKey = PrivateKey.generateED25519();
        AccountId ownerId = createAccount(client, ownerKey, Hbar.from(2), 0);
        PrivateKey spenderKey = PrivateKey.generateED25519();
        AccountId spenderId = createAccount(client, spenderKey, Hbar.from(5), 0);
        PrivateKey receiverKey = PrivateKey.generateED25519();
        AccountId receiverId = createAccount(client, receiverKey, Hbar.from(1), -1);
        System.out.println("Owner:    " + ownerId);
        System.out.println("Spender:  " + spenderId);
        System.out.println("Receiver: " + receiverId);

        // Step 2: create an NFT type with the owner as treasury, admin and supply key.
        TokenId tokenId =
                new TokenCreateTransaction()
                        .setTokenName("Approved Transfer Demo NFT")
                        .setTokenSymbol("ATNFT")
                        .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                        .setTreasuryAccountId(ownerId)
                        .setAdminKey(ownerKey.getPublicKey())
                        .setSupplyKey(ownerKey.getPublicKey())
                        .freezeWith(client)
                        .sign(ownerKey)
                        .execute(client)
                        .getReceipt(client)
                        .tokenId;
        if (tokenId == null) {
            throw new IllegalStateException("Token create receipt did not contain a token ID");
        }
        System.out.println("Created NFT type: " + tokenId);

        // Step 3: mint four serials into the owner's account.
        List<Long> serials =
                new TokenMintTransaction()
                        .setTokenId(tokenId)
                        .setMetadata(List.of(metadata, metadata, metadata, metadata))
                        .freezeWith(client)
                        .sign(ownerKey)
                        .execute(client)
                        .getReceipt(client)
                        .serials;
        long serial1 = serials.get(0);
        long serial2 = serials.get(1);
        long serial3 = serials.get(2);
        long unapprovedSerial = serials.get(3);
        System.out.println("Minted NFT serials: " + serials);

        // Step 4: the owner approves the spender for the first three serials only.
        Status approveStatus =
                new AccountAllowanceApproveTransaction()
                        .approveTokenNftAllowance(tokenId.nft(serial1), ownerId, spenderId)
                        .approveTokenNftAllowance(tokenId.nft(serial2), ownerId, spenderId)
                        .approveTokenNftAllowance(tokenId.nft(serial3), ownerId, spenderId)
                        .freezeWith(client)
                        .sign(ownerKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println(
                "Owner approved serials " + serial1 + ", " + serial2 + ", " + serial3 + " for spender ("
                        + approveStatus + ")");

        // Step 5: the spender moves one serial to the receiver.
        Status singleStatus = transferApprovedNfts(client, tokenId, List.of(serial1), ownerId, receiverId, spenderId, spenderKey);
        System.out.println("\nSpender moved serial " + serial1 + " (" + singleStatus + ")");
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner("AFTER APPROVED TRANSFER (serial " + serial1 + ")", client, tokenId, serial1, receiverId);

        // Step 6: the spender moves two serials in one transaction.
        Status multiStatus =
                transferApprovedNfts(client, tokenId, List.of(serial2, serial3), ownerId, receiverId, spenderId, spenderKey);
        System.out.println("Spender moved serials " + serial2 + ", " + serial3 + " (" + multiStatus + ")");
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner("AFTER APPROVED TRANSFER (serial " + serial2 + ")", client, tokenId, serial2, receiverId);
        TestnetQueryHelper.logNftOwner("AFTER APPROVED TRANSFER (serial " + serial3 + ")", client, tokenId, serial3, receiverId);

        // Step 7: without an allowance the network rejects the transfer and the NFT stays put.
        try {
            transferApprovedNfts(client, tokenId, List.of(unapprovedSerial), ownerId, receiverId, spenderId, spenderKey);
            throw new IllegalStateException("Transfer of unapproved serial " + unapprovedSerial + " should have failed");
        } catch (ReceiptStatusException e) {
            System.out.println("\nTransfer of unapproved serial " + unapprovedSerial + " rejected as expected: " + e.receipt.status);
        } catch (PrecheckStatusException e) {
            System.out.println("\nTransfer of unapproved serial " + unapprovedSerial + " rejected as expected: " + e.status);
        }
        TestnetQueryHelper.logNftOwner(
                "AFTER REJECTED TRANSFER (serial " + unapprovedSerial + ")", client, tokenId, unapprovedSerial, ownerId);
        System.out.println("\nTransfer approved NFT status: SUCCESS");

        client.close();
    }

    private static Status transferApprovedNfts(
            Client client,
            TokenId tokenId,
            List<Long> serials,
            AccountId ownerId,
            AccountId receiverId,
            AccountId spenderId,
            PrivateKey spenderKey)
            throws Exception {
        TransferTransaction transfer = new TransferTransaction();
        serials.forEach(serial -> transfer.addApprovedNftTransfer(tokenId.nft(serial), ownerId, receiverId));
        // The network treats the payer as the spender, so the transaction ID belongs to the spender.
        transfer.setTransactionId(TransactionId.generate(spenderId));

        return transfer.freezeWith(client).sign(spenderKey).execute(client).getReceipt(client).status;
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
