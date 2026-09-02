package com.hedera.tutorial.nft;

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SDK tutorial for {@link TokenGrantKycTransaction} and {@link TokenRevokeKycTransaction} — grants
 * and revokes KYC for an account on an NFT type.
 *
 * <p>Must be signed by the KYC key set at create time. When a token has a KYC key, accounts must
 * be KYC-granted before they can receive that token.
 *
 * @see <a href="https://docs.hedera.com/hedera/sdks-and-apis/sdks/token-service/enable-kyc-account-flag">Enable
 *     KYC account flag</a>
 * @see <a href="https://docs.hedera.com/hedera/sdks-and-apis/sdks/token-service/disable-kyc-account-flag">Disable
 *     KYC account flag</a>
 */
public class GrantKycNftSdkTutorial {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

        PrivateKey adminKey = operatorKey;
        PrivateKey supplyKey = operatorKey;
        PrivateKey kycKey = operatorKey;

        byte[] metadata = "https://example.com/nft/kyc".getBytes(StandardCharsets.UTF_8);

        // Step 1: create an NFT type with a KYC key.
        TokenId tokenId =
                new TokenCreateTransaction()
                        .setTokenName("KYC Demo NFT")
                        .setTokenSymbol("KYCNFT")
                        .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                        .setTreasuryAccountId(operatorId)
                        .setAdminKey(adminKey.getPublicKey())
                        .setSupplyKey(supplyKey.getPublicKey())
                        .setKycKey(kycKey.getPublicKey())
                        .freezeWith(client)
                        .sign(adminKey)
                        .execute(client)
                        .getReceipt(client)
                        .tokenId;

        if (tokenId == null) {
            throw new IllegalStateException("Token create receipt did not contain a token ID");
        }
        System.out.println("Created NFT type: " + tokenId);

        // Step 2: mint an NFT serial.
        long serial =
                new TokenMintTransaction()
                        .setTokenId(tokenId)
                        .setMetadata(List.of(metadata))
                        .freezeWith(client)
                        .sign(supplyKey)
                        .execute(client)
                        .getReceipt(client)
                        .serials
                        .get(0);
        System.out.println("Minted NFT serial: " + serial);

        // Step 3: create a holder account and associate the token.
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

        new TokenAssociateTransaction()
                .setAccountId(holderId)
                .setTokenIds(List.of(tokenId))
                .freezeWith(client)
                .sign(holderKey)
                .execute(client)
                .getReceipt(client);
        System.out.println("Associated holder: " + holderId);

        // Step 4: transfer without KYC should fail.
        try {
            new TransferTransaction()
                    .addNftTransfer(tokenId.nft(serial), operatorId, holderId)
                    .freezeWith(client)
                    .sign(operatorKey)
                    .execute(client)
                    .getReceipt(client);
            System.out.println("Unexpected: transfer succeeded without KYC");
        } catch (Exception e) {
            System.out.println("Transfer without KYC failed as expected: " + e.getMessage());
        }

        // Step 5: grant KYC to the holder.
        Status grantStatus =
                new TokenGrantKycTransaction()
                        .setTokenId(tokenId)
                        .setAccountId(holderId)
                        .freezeWith(client)
                        .sign(kycKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println("Granted KYC via TokenGrantKycTransaction: " + grantStatus);

        // Step 6: transfer succeeds after KYC is granted.
        Status transferStatus =
                new TransferTransaction()
                        .addNftTransfer(tokenId.nft(serial), operatorId, holderId)
                        .freezeWith(client)
                        .sign(operatorKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println("Transfer after grant KYC succeeded: " + transferStatus);

        // Step 7: revoke KYC from the holder.
        Status revokeStatus =
                new TokenRevokeKycTransaction()
                        .setTokenId(tokenId)
                        .setAccountId(holderId)
                        .freezeWith(client)
                        .sign(kycKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println("Revoked KYC via TokenRevokeKycTransaction: " + revokeStatus);

        // Step 8: transfer back to operator should fail after KYC is revoked.
        try {
            new TransferTransaction()
                    .addNftTransfer(tokenId.nft(serial), holderId, operatorId)
                    .freezeWith(client)
                    .sign(holderKey)
                    .execute(client)
                    .getReceipt(client);
            System.out.println("Unexpected: transfer from holder succeeded after KYC revoke");
        } catch (Exception e) {
            System.out.println(
                    "Transfer from holder after KYC revoke failed as expected: " + e.getMessage());
        }

        // Move NFT back to treasury with KYC re-granted for cleanup.
        new TokenGrantKycTransaction()
                .setTokenId(tokenId)
                .setAccountId(holderId)
                .freezeWith(client)
                .sign(kycKey)
                .execute(client)
                .getReceipt(client);

        Status cleanupStatus =
                new TransferTransaction()
                        .addNftTransfer(tokenId.nft(serial), holderId, operatorId)
                        .freezeWith(client)
                        .sign(holderKey)
                        .execute(client)
                        .getReceipt(client)
                        .status;
        System.out.println("Cleanup transfer after re-grant KYC succeeded: " + cleanupStatus);

        client.close();
    }
}
