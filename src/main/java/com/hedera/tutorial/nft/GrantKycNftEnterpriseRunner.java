package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TokenId;
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
 * Enterprise tutorial for {@link NftClient#grantKycNft} / {@link NftClient#revokeKycNft} — grants
 * and revokes KYC for an account on an NFT type.
 *
 * <p>The token is created with a KYC key via {@link NftClient#createNftType(String, String,
 * PrivateKey, PrivateKey, PrivateKey)} using the operator as treasury.
 *
 * @see <a href="https://docs.hedera.com/hedera/sdks-and-apis/sdks/token-service/enable-kyc-account-flag">Enable
 *     KYC account flag</a>
 * @see <a href="https://docs.hedera.com/hedera/sdks-and-apis/sdks/token-service/disable-kyc-account-flag">Disable
 *     KYC account flag</a>
 */
@Component
@Profile("grant-kyc-nft")
public class GrantKycNftEnterpriseRunner implements CommandLineRunner {

    private final NftClient nftClient;
    private final AccountClient accountClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public GrantKycNftEnterpriseRunner(
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
        PrivateKey kycKey = operator.privateKey();
        PrivateKey supplyKey = operator.privateKey();
        byte[] metadata = "https://example.com/nft/kyc".getBytes(StandardCharsets.UTF_8);

        TokenId tokenId =
                nftClient.createNftType("KYC Demo NFT", "KYCNFT", supplyKey, null, kycKey);
        System.out.println("Created NFT type with KYC key via NftClient.createNftType(): " + tokenId);

        long serial = nftClient.mintNft(tokenId, supplyKey, metadata);
        System.out.println("Minted NFT serial: " + serial);

        Account holder = accountClient.createAccount(1);
        nftClient.associateNft(tokenId, holder);
        System.out.println("Associated holder: " + holder.accountId());

        try {
            nftClient.transferNft(tokenId, serial, operator, holder.accountId());
            System.out.println("Unexpected: transfer succeeded without KYC");
        } catch (HieroException e) {
            System.out.println("Transfer without KYC failed as expected: " + e.getMessage());
        }

        nftClient.grantKycNft(tokenId, holder.accountId());
        System.out.println("Granted KYC via NftClient.grantKycNft()");

        nftClient.transferNft(tokenId, serial, operator, holder.accountId());
        System.out.println("Transfer after grant KYC succeeded: holder received serial " + serial);

        nftClient.revokeKycNft(tokenId, holder.accountId(), kycKey);
        System.out.println("Revoked KYC via NftClient.revokeKycNft()");

        try {
            nftClient.transferNft(tokenId, serial, holder, operator.accountId());
            System.out.println("Unexpected: transfer from holder succeeded after KYC revoke");
        } catch (HieroException e) {
            System.out.println(
                    "Transfer from holder after KYC revoke failed as expected: " + e.getMessage());
        }

        nftClient.grantKycNft(tokenId, holder.accountId(), kycKey);
        nftClient.transferNft(tokenId, serial, holder, operator.accountId());
        System.out.println("Cleanup transfer after re-grant KYC succeeded");
        System.out.println("Grant/revoke KYC status: SUCCESS");

        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
