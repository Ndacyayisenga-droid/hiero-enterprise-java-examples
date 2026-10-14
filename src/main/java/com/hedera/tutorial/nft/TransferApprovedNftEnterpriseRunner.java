package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.AccountAllowanceApproveTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.tutorial.util.TestnetQueryHelper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hiero.base.AccountClient;
import org.hiero.base.HieroContext;
import org.hiero.base.HieroException;
import org.hiero.base.NftClient;
import org.hiero.base.data.Account;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Enterprise tutorial for {@link NftClient#transferApprovedNft} / {@link
 * NftClient#transferApprovedNfts} — a spender moves NFTs out of the owner's account using an
 * allowance the owner granted earlier.
 *
 * <p>The spender pays for the transfer and is the only signer; the owner's key is not needed. One
 * serial is moved with the single overload, two more with the list overload, and a fourth serial
 * without an allowance shows the transfer being rejected.
 *
 * <p>The allowances themselves are granted with the SDK {@link AccountAllowanceApproveTransaction}
 * through the client from {@link HieroContext}, so this runner only depends on the
 * approved-transfer methods of {@code NftClient}.
 *
 * @see <a href="https://docs.hedera.com/native/tokens/transfer">Transfer tokens</a>
 */
@Component
@Profile("transfer-approved-nft")
public class TransferApprovedNftEnterpriseRunner implements CommandLineRunner {

    private final NftClient nftClient;
    private final AccountClient accountClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public TransferApprovedNftEnterpriseRunner(
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

        byte[] metadata = "https://example.com/nft/approved-transfer".getBytes(StandardCharsets.UTF_8);

        // Enterprise: create owner, spender and receiver accounts. The spender pays the fees.
        Account owner = accountClient.createAccount(2);
        Account spender = accountClient.createAccount(5);
        Account receiver = accountClient.createAccount(1);
        System.out.println("Owner:    " + owner.accountId());
        System.out.println("Spender:  " + spender.accountId());
        System.out.println("Receiver: " + receiver.accountId());

        // Enterprise: create an NFT type held by the owner and mint four serials.
        TokenId tokenId = nftClient.createNftType("Approved Transfer Demo NFT", "ATNFT", owner);
        System.out.println("Created NFT type: " + tokenId);

        List<Long> serials =
                nftClient.mintNfts(tokenId, owner.privateKey(), metadata, metadata, metadata, metadata);
        long serial1 = serials.get(0);
        long serial2 = serials.get(1);
        long serial3 = serials.get(2);
        long unapprovedSerial = serials.get(3);
        System.out.println("Minted NFT serials: " + serials);

        // Enterprise: the receiver must be associated before it can hold the NFTs.
        nftClient.associateNft(tokenId, receiver);
        System.out.println("Associated receiver with " + tokenId);

        // SDK: the owner approves the spender for the first three serials only.
        approveNftAllowance(client, tokenId, List.of(serial1, serial2, serial3), owner, spender.accountId());
        System.out.println(
                "Owner approved serials " + serial1 + ", " + serial2 + ", " + serial3 + " for " + spender.accountId());

        // Enterprise: the spender moves one serial with the single overload.
        nftClient.transferApprovedNft(
                tokenId,
                serial1,
                owner.accountId(),
                spender.accountId(),
                spender.privateKey(),
                receiver.accountId());
        System.out.println("\nSpender moved serial " + serial1 + " via NftClient.transferApprovedNft()");
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner(
                "AFTER transferApprovedNft (serial " + serial1 + ")", client, tokenId, serial1, receiver.accountId());

        // Enterprise: the spender moves two serials in one transaction with the list overload.
        nftClient.transferApprovedNfts(
                tokenId, List.of(serial2, serial3), owner.accountId(), spender, receiver.accountId());
        System.out.println(
                "Spender moved serials " + serial2 + ", " + serial3 + " via NftClient.transferApprovedNfts()");
        TestnetQueryHelper.sleepForConsensus();
        TestnetQueryHelper.logNftOwner(
                "AFTER transferApprovedNfts (serial " + serial2 + ")", client, tokenId, serial2, receiver.accountId());
        TestnetQueryHelper.logNftOwner(
                "AFTER transferApprovedNfts (serial " + serial3 + ")", client, tokenId, serial3, receiver.accountId());

        // Enterprise: without an allowance the network rejects the transfer
        // (SPENDER_DOES_NOT_HAVE_ALLOWANCE) and the NFT stays with the owner.
        try {
            nftClient.transferApprovedNft(
                    tokenId, unapprovedSerial, owner.accountId(), spender, receiver.accountId());
            throw new IllegalStateException("Transfer of unapproved serial " + unapprovedSerial + " should have failed");
        } catch (HieroException e) {
            System.out.println("\nTransfer of unapproved serial " + unapprovedSerial + " rejected as expected: "
                    + rootCauseMessage(e));
        }
        TestnetQueryHelper.logNftOwner(
                "AFTER REJECTED TRANSFER (serial " + unapprovedSerial + ")",
                client,
                tokenId,
                unapprovedSerial,
                owner.accountId());
        System.out.println("\nTransfer approved NFT status: SUCCESS");

        System.exit(SpringApplication.exit(context, () -> 0));
    }

    /** Grants serial-level NFT allowances. The owner signs; the operator pays. */
    private static void approveNftAllowance(
            Client client, TokenId tokenId, List<Long> serials, Account owner, AccountId spenderId)
            throws Exception {
        AccountAllowanceApproveTransaction approve = new AccountAllowanceApproveTransaction();
        serials.forEach(serial -> approve.approveTokenNftAllowance(tokenId.nft(serial), owner.accountId(), spenderId));
        approve.freezeWith(client).sign(owner.privateKey()).execute(client).getReceipt(client);
    }

    private static String rootCauseMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}
