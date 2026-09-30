package com.hedera.tutorial.util;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.MaxAttemptsExceededException;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenId;
import com.hedera.hashgraph.sdk.TokenNftInfoQuery;
import com.hedera.hashgraph.sdk.AccountId;
import java.time.Duration;

/** Retries consensus queries on Hedera testnet when nodes return {@link Status#BUSY}. */
public final class TestnetQueryHelper {

    private static final int OUTER_ATTEMPTS = 12;

    private TestnetQueryHelper() {}

    public static void tuneClientForTestnet(Client client) {
        client.setMaxAttempts(30);
        client.setMaxBackoff(Duration.ofSeconds(10));
    }

    public static AccountId queryNftOwner(Client client, TokenId tokenId, long serial) throws Exception {
        return withRetry(
                () ->
                        new TokenNftInfoQuery()
                                .setNftId(tokenId.nft(serial))
                                .execute(client)
                                .get(0)
                                .accountId);
    }

    public static void logNftOwner(String label, Client client, TokenId tokenId, long serial, AccountId expectedOwner)
            throws Exception {
        AccountId owner = queryNftOwner(client, tokenId, serial);
        System.out.println("\n=== " + label + " ===");
        System.out.println("  serial " + serial + " owner: " + owner + " (expected " + expectedOwner + ")");
        if (!owner.equals(expectedOwner)) {
            throw new IllegalStateException(
                    "Expected owner " + expectedOwner + " but NFT serial " + serial + " is owned by " + owner);
        }
    }

    public static void sleepForConsensus() throws InterruptedException {
        Thread.sleep(3_000);
    }

    public static <T> T withRetry(QueryAction<T> action) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= OUTER_ATTEMPTS; attempt++) {
            try {
                return action.run();
            } catch (Exception e) {
                last = e;
                if (!isRetryable(e) || attempt == OUTER_ATTEMPTS) {
                    throw e;
                }
                Thread.sleep(Math.min(15_000L, 2_000L * attempt));
            }
        }
        throw last;
    }

    private static boolean isRetryable(Exception e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof MaxAttemptsExceededException) {
                return true;
            }
            if (t instanceof PrecheckStatusException precheck && precheck.status == Status.BUSY) {
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    public interface QueryAction<T> {
        T run() throws Exception;
    }
}
