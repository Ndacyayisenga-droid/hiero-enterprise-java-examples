package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenType;
import java.nio.charset.StandardCharsets;
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
 * Enterprise tutorial for {@link NftClient#freezeNft} / {@link NftClient#unfreezeNft} — freezes
 * and unfreezes an account for an NFT type.
 *
 * <p>The token must be created with a freeze key. {@link NftClient#createNftType} does not set a
 * freeze key yet, so this demo creates the NFT type via the SDK (using {@link
 * HieroContext#getClient()}) and uses {@link NftClient} for associate, mint, transfer, freeze, and
 * unfreeze.
 *
 * @see <a href="https://docs.hedera.com/hedera/sdks-and-apis/sdks/token-service/freeze-an-account">Freeze
 *     an account</a>
 * @see <a href="https://docs.hedera.com/hedera/sdks-and-apis/sdks/token-service/unfreeze-an-account">Unfreeze
 *     an account</a>
 */
@Component
@Profile("freeze-nft")
public class FreezeNftEnterpriseRunner implements CommandLineRunner {

    private final NftClient nftClient;
    private final AccountClient accountClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public FreezeNftEnterpriseRunner(
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
        Account operator = hieroContext.getOperatorAccount();
        Client client = hieroContext.getClient();
        PrivateKey freezeKey = operator.privateKey();
        PrivateKey supplyKey = operator.privateKey();
        byte[] metadata = "https://example.com/nft/freeze".getBytes(StandardCharsets.UTF_8);

        // SDK: create an NFT type with a freeze key (NftClient.createNftType does not set one yet).
        TokenId tokenId =
                new TokenCreateTransaction()
                        .setTokenName("Freeze Demo NFT")
                        .setTokenSymbol("FDNFT")
                        .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                        .setTreasuryAccountId(operator.accountId())
                        .setAdminKey(operator.privateKey().getPublicKey())
                        .setSupplyKey(supplyKey.getPublicKey())
                        .setFreezeKey(freezeKey.getPublicKey())
                        .freezeWith(client)
                        .sign(operator.privateKey())
                        .execute(client)
                        .getReceipt(client)
                        .tokenId;

        if (tokenId == null) {
            throw new IllegalStateException("Token create receipt did not contain a token ID");
        }
        System.out.println("Created NFT type: " + tokenId);

        long serial = nftClient.mintNft(tokenId, supplyKey, metadata);
        System.out.println("Minted NFT serial: " + serial);

        Account holder = accountClient.createAccount(1);
        Account receiver = accountClient.createAccount(1);
        nftClient.associateNft(tokenId, holder);
        nftClient.associateNft(tokenId, receiver);
        nftClient.transferNft(tokenId, serial, operator, holder.accountId());
        System.out.println("Transferred serial " + serial + " to holder: " + holder.accountId());

        // Transfer succeeds while the holder is not frozen.
        nftClient.transferNft(tokenId, serial, holder, receiver.accountId());
        System.out.println("Transfer before freeze succeeded: holder -> receiver");
        nftClient.transferNft(tokenId, serial, receiver, holder.accountId());
        System.out.println("Returned serial to holder for freeze demo");

        // Enterprise: freeze the holder (operator key used as freeze key).
        nftClient.freezeNft(tokenId, holder.accountId());
        System.out.println("Froze holder via NftClient.freezeNft()");

        try {
            nftClient.transferNft(tokenId, serial, holder, receiver.accountId());
            System.out.println("Unexpected: transfer succeeded while account was frozen");
        } catch (HieroException e) {
            System.out.println("Transfer while frozen failed as expected: " + e.getMessage());
        }

        // Enterprise: unfreeze with explicit freeze key overload.
        nftClient.unfreezeNft(tokenId, holder.accountId(), freezeKey);
        System.out.println("Unfroze holder via NftClient.unfreezeNft()");

        nftClient.transferNft(tokenId, serial, holder, receiver.accountId());
        System.out.println("Transfer after unfreeze succeeded: holder -> receiver");
        System.out.println("Freeze status: SUCCESS");

        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
