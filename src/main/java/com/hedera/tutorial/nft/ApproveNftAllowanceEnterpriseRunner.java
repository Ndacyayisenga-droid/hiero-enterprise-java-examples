package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.AccountAllowanceApproveTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransferTransaction;
import com.hedera.tutorial.util.TestnetQueryHelper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hiero.base.AccountClient;
import org.hiero.base.HieroContext;
import org.hiero.base.NftClient;
import org.hiero.base.data.Account;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Enterprise tutorial for {@link NftClient#approveNftAllowance}, {@link
 * NftClient#approveNftAllowances} and {@link NftClient#approveNftAllowanceAllSerials} — an owner
 * delegates NFT spending to a spender, who can then transfer the NFTs on the owner's behalf.
 *
 * <p>All three approval shapes are shown: a single serial, a list of serials, and every serial of
 * an NFT type (including serials the owner receives later).
 *
 * <p>Each approval is proven by letting the spender move the NFT. {@code NftClient} has no
 * approved-transfer method — it always signs with the holder's key — so that one step drops to the
 * SDK {@link TransferTransaction#addApprovedNftTransfer} through the client from {@link
 * HieroContext}.
 *
 * @see <a href="https://docs.hedera.com/native/accounts/approve-allowance">Approve an allowance</a>
 */
@Component
@Profile("approve-nft-allowance")
public class ApproveNftAllowanceEnterpriseRunner implements CommandLineRunner {

    private final NftClient nftClient;
    private final AccountClient accountClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public ApproveNftAllowanceEnterpriseRunner(
            NftClient nftClient,
            AccountClient accountClient,
            HieroContext hieroContext,
            ConfigurableApplicationContext context) {
        this.nftClient = nftClient;
        this.accountClient = accountClient;
        this.hieroContext = hieroContext;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        var client = hieroContext.getClient();
        TestnetQueryHelper.tuneClientForTestnet(client);

        byte[] metadata = "https://example.com/nft/allowance".getBytes(StandardCharsets.UTF_8);

        // Enterprise: create owner, spender and receiver accounts.
        Account owner = accountClient.createAccount(2);
        Account spender = accountClient.createAccount(5);
        Account receiver = accountClient.createAccount(1);
        System.out.println("Owner:    " + owner.accountId());
        System.out.println("Spender:  " + spender.accountId());
        System.out.println("Receiver: " + receiver.accountId());

        // Enterprise: create an NFT type owned by the owner account and mint three serials.
        TokenId tokenId = nftClient.createNftType("Allowance Demo NFT", "ALNFT", owner);
        System.out.println("Created NFT type: " + tokenId);

        List<Long> serials = nftClient.mintNfts(tokenId, owner.privateKey(), metadata, metadata, metadata);
        long serial1 = serials.get(0);
        long serial2 = serials.get(1);
        long serial3 = serials.get(2);
        System.out.println("Minted NFT serials: " + serial1 + ", " + serial2 + ", " + serial3);

        // Enterprise: the receiver must be associated before it can hold the NFTs.
        nftClient.associateNft(tokenId, receiver);
        System.out.println("Associated receiver with " + tokenId);

        // Enterprise: approve the spender for a single serial.
        nftClient.approveNftAllowance(tokenId, serial1, owner, spender.accountId());
        System.out.println(
                "\nApproved serial " + serial1 + " via NftClient.approveNftAllowance() -> " + spender.accountId());

        // Enterprise: approve the spender for several serials in one transaction.
        nftClient.approveNftAllowances(tokenId, List.of(serial2, serial3), owner, spender.accountId());
        System.out.println(
                "Approved serials " + serial2 + ", " + serial3
                        + " via NftClient.approveNftAllowances() -> " + spender.accountId());

        // SDK: the spender moves serial1 and serial2 using the allowance.
        spendApprovedNft(client, tokenId, serial1, owner, receiver.accountId(), spender);
        spendApprovedNft(client, tokenId, serial2, owner, receiver.accountId(), spender);
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner(
                "AFTER APPROVED SPEND (serial " + serial1 + ")", client, tokenId, serial1, receiver.accountId());
        TestnetQueryHelper.logNftOwner(
                "AFTER APPROVED SPEND (serial " + serial2 + ")", client, tokenId, serial2, receiver.accountId());

        // Enterprise: an all-serials allowance on a second NFT type. It covers every serial the
        // owner holds now and every serial the owner receives in the future.
        TokenId allSerialsTokenId = nftClient.createNftType("Allowance All Demo NFT", "ALLNFT", owner);
        long allSerial = nftClient.mintNft(allSerialsTokenId, owner.privateKey(), metadata);
        nftClient.associateNft(allSerialsTokenId, receiver);
        System.out.println("\nCreated NFT type " + allSerialsTokenId + " with serial " + allSerial + " held by owner");

        nftClient.approveNftAllowanceAllSerials(allSerialsTokenId, owner, spender.accountId());
        System.out.println(
                "Approved ALL serials of " + allSerialsTokenId
                        + " via NftClient.approveNftAllowanceAllSerials() -> " + spender.accountId());

        spendApprovedNft(client, allSerialsTokenId, allSerial, owner, receiver.accountId(), spender);
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner(
                "AFTER ALL-SERIALS SPEND (serial " + allSerial + ")",
                client,
                allSerialsTokenId,
                allSerial,
                receiver.accountId());

        // Enterprise: the allowances on serial1 and serial2 were consumed by the approved transfers,
        // and serial3 was never spent, so its serial-level allowance is still live. Deleting it
        // needs the owner to still hold the NFT.
        accountClient.deleteNftAllowance(owner, tokenId, serial3);
        System.out.println("\nDeleted the serial-level allowance on serial " + serial3);

        // SDK: an all-serials grant outlives the transfer, so revoke it explicitly. AccountClient
        // only deletes serial-level allowances, so this uses the SDK approve transaction, whose
        // deleteTokenNftAllowanceAllSerials clears the grant for this owner/spender pair.
        new AccountAllowanceApproveTransaction()
                .deleteTokenNftAllowanceAllSerials(allSerialsTokenId, owner.accountId(), spender.accountId())
                .freezeWith(client)
                .sign(owner.privateKey())
                .execute(client)
                .getReceipt(client);
        System.out.println("Revoked the all-serials allowance on " + allSerialsTokenId);
        System.out.println("\nApprove NFT allowance status: SUCCESS");

        System.exit(SpringApplication.exit(context, () -> 0));
    }

    /**
     * Spends an approved NFT. {@code NftClient} signs transfers with the holder's key, so an
     * allowance-backed transfer is built with the SDK: the spender signs and pays, and the NFT
     * moves out of the owner's account.
     */
    private static void spendApprovedNft(
            Client client, TokenId tokenId, long serial, Account owner, AccountId receiverId, Account spender)
            throws Exception {
        AccountId spenderId = spender.accountId();
        PrivateKey spenderKey = spender.privateKey();

        TransferTransaction transfer = new TransferTransaction();
        transfer.addApprovedNftTransfer(tokenId.nft(serial), owner.accountId(), receiverId);
        // The spender pays for the transfer, so the transaction ID belongs to the spender.
        transfer.setTransactionId(TransactionId.generate(spenderId));

        Status status =
                transfer.freezeWith(client).sign(spenderKey).execute(client).getReceipt(client).status;
        if (status != Status.SUCCESS) {
            throw new IllegalStateException("Approved transfer of serial " + serial + " failed with " + status);
        }
        System.out.println("Spender moved serial " + serial + " to " + receiverId + " using the allowance (" + status + ")");
    }
}
