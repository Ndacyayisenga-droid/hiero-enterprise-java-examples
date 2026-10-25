package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.*;
import com.hedera.tutorial.util.TestnetQueryHelper;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;

/**
 * SDK tutorial for {@link TokenFeeScheduleUpdateTransaction} — replaces the custom fees of an NFT
 * token class.
 *
 * <p>The token must be created with a fee schedule key, and the update must be signed with it. The
 * new list replaces the whole fee schedule; an empty list removes all custom fees. NFT types support
 * fixed and royalty fees only (no fractional fees).
 *
 * @see <a href="https://docs.hedera.com/native/tokens/update-fee-schedule">Update token custom fees</a>
 */
public class UpdateNftFeeScheduleSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);
        TestnetQueryHelper.tuneClientForTestnet(client);

        PrivateKey supplyKey = PrivateKey.generateED25519();
        PrivateKey feeScheduleKey = PrivateKey.generateED25519();

        // Step 1: create an NFT type with a fee schedule key so its custom fees can be updated later.
        TokenId tokenId =
                new TokenCreateTransaction()
                        .setTokenName("Fee Schedule Demo NFT")
                        .setTokenSymbol("FSDNFT")
                        .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                        .setTreasuryAccountId(operatorId)
                        .setAdminKey(operatorKey.getPublicKey())
                        .setSupplyKey(supplyKey.getPublicKey())
                        .setFeeScheduleKey(feeScheduleKey.getPublicKey())
                        .freezeWith(client)
                        .sign(operatorKey)
                        .execute(client)
                        .getReceipt(client)
                        .tokenId;

        if (tokenId == null) {
            throw new IllegalStateException("Token create receipt did not contain a token ID");
        }
        System.out.println("Created NFT type: " + tokenId);
        printCustomFees("Initial fees", client, tokenId);

        // Step 2: set a royalty fee (with HBAR fallback) and a fixed HBAR fee, collected by the operator.
        CustomRoyaltyFee royaltyFee =
                new CustomRoyaltyFee()
                        .setNumerator(5)
                        .setDenominator(100)
                        .setFallbackFee(new CustomFixedFee().setHbarAmount(Hbar.from(1)))
                        .setFeeCollectorAccountId(operatorId);
        CustomFixedFee fixedFee =
                new CustomFixedFee().setHbarAmount(Hbar.from(1)).setFeeCollectorAccountId(operatorId);

        Status updateStatus =
                new TokenFeeScheduleUpdateTransaction()
                        .setTokenId(tokenId)
                        .setCustomFees(List.of(royaltyFee, fixedFee))
                        .freezeWith(client)
                        .sign(feeScheduleKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println("\nFee schedule update status: " + updateStatus);
        printCustomFees("Fees after update", client, tokenId);

        // Step 3: remove all custom fees by submitting an empty fee schedule.
        Status clearStatus =
                new TokenFeeScheduleUpdateTransaction()
                        .setTokenId(tokenId)
                        .setCustomFees(List.of())
                        .freezeWith(client)
                        .sign(feeScheduleKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println("\nFee schedule clear status: " + clearStatus);
        printCustomFees("Fees after clear", client, tokenId);

        client.close();
    }

    private static void printCustomFees(String label, Client client, TokenId tokenId) throws Exception {
        TokenInfo info =
                TestnetQueryHelper.withRetry(
                        () -> new TokenInfoQuery().setTokenId(tokenId).execute(client));
        System.out.println("\n=== " + label + " (" + info.customFees.size() + ") ===");
        for (CustomFee fee : info.customFees) {
            System.out.println("  " + fee);
        }
    }
}
