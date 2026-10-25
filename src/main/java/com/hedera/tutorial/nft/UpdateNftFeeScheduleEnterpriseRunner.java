package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.CustomFee;
import com.hedera.hashgraph.sdk.CustomFixedFee;
import com.hedera.hashgraph.sdk.CustomRoyaltyFee;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TokenId;
import java.util.List;
import org.hiero.base.HieroContext;
import org.hiero.base.NftClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Enterprise tutorial for {@link NftClient#updateNftFeeSchedule} — replaces the custom fees of an
 * NFT token class.
 *
 * <p>The NFT type is created with a dedicated fee schedule key, which must sign every fee schedule
 * update. The new list replaces the whole fee schedule; an empty list removes all custom fees. NFT
 * types support fixed and royalty fees only (fractional fees are rejected by the client).
 *
 * @see <a href="https://docs.hedera.com/native/tokens/update-fee-schedule">Update token custom fees</a>
 */
@Component
@Profile("update-nft-fee-schedule")
public class UpdateNftFeeScheduleEnterpriseRunner implements CommandLineRunner {

    private final NftClient nftClient;
    private final HieroContext hieroContext;
    private final ConfigurableApplicationContext context;

    public UpdateNftFeeScheduleEnterpriseRunner(
            NftClient nftClient, HieroContext hieroContext, ConfigurableApplicationContext context) {
        this.nftClient = nftClient;
        this.hieroContext = hieroContext;
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        AccountId collector = hieroContext.getOperatorAccount().accountId();
        PrivateKey supplyKey = PrivateKey.generateED25519();
        PrivateKey feeScheduleKey = PrivateKey.generateED25519();

        // Enterprise: create an NFT type with a fee schedule key. The operator is the treasury.
        TokenId tokenId =
                nftClient.createNftType(
                        "Fee Schedule Demo NFT", "FSDNFT", supplyKey, null, null, feeScheduleKey);
        System.out.println("Created NFT type: " + tokenId);

        // Enterprise: set a royalty fee (with HBAR fallback) and a fixed HBAR fee.
        List<CustomFee> customFees =
                List.of(
                        new CustomRoyaltyFee()
                                .setNumerator(5)
                                .setDenominator(100)
                                .setFallbackFee(new CustomFixedFee().setHbarAmount(Hbar.from(1)))
                                .setFeeCollectorAccountId(collector),
                        new CustomFixedFee()
                                .setHbarAmount(Hbar.from(1))
                                .setFeeCollectorAccountId(collector));
        nftClient.updateNftFeeSchedule(tokenId, customFees, feeScheduleKey);
        System.out.println("Updated fee schedule: 5% royalty (1 HBAR fallback) + 1 HBAR fixed fee");
        System.out.println("Fee collector:        " + collector);

        // Enterprise: remove all custom fees with an empty list (String overload).
        nftClient.updateNftFeeSchedule(tokenId.toString(), List.of(), feeScheduleKey.toStringDER());
        System.out.println("Cleared fee schedule");
        System.out.println("Update status:        SUCCESS");

        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
