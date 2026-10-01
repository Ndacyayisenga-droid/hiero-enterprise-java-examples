package com.hedera.tutorial.util;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
import java.util.Map;

/**
 * Builds the SDK {@link Client} for the network the tutorials should run against, so the same
 * example works on a public network and on a local Solo network in CI.
 *
 * <p>The network is read from {@code HEDERA_NETWORK} (environment variable first, then {@code
 * .env}), falling back to {@code spring.hiero.network.name} so a single {@code .env} configures
 * both the SDK tutorials and the Spring enterprise runners. The default is {@code hedera-testnet}.
 *
 * <p>Recognised values:
 *
 * <ul>
 *   <li>{@code hedera-testnet} / {@code testnet}
 *   <li>{@code hedera-previewnet} / {@code previewnet}
 *   <li>{@code hedera-mainnet} / {@code mainnet}
 *   <li>{@code hiero-solo-action} / {@code solo} / {@code local} — the local network started by
 *       <a href="https://github.com/hiero-ledger/hiero-solo-action">hiero-solo-action</a>
 * </ul>
 *
 * <p>The returned client has no operator set; callers still call {@code setOperator(...)} with the
 * credentials they read from {@code .env}.
 */
public final class TutorialClient {

    /** Consensus node gRPC endpoint of hiero-solo-action (its {@code haproxyPort} default). */
    private static final String SOLO_CONSENSUS_NODE_DEFAULT = "127.0.0.1:35211";

    /** Account ID of the single consensus node that hiero-solo-action starts. */
    private static final String SOLO_CONSENSUS_NODE_ACCOUNT_DEFAULT = "0.0.3";

    /** Mirror node gRPC endpoint of hiero-solo-action (its {@code mirrorNodePortGrpc} default). */
    private static final String SOLO_MIRROR_GRPC_DEFAULT = "localhost:5600";

    private static final String DEFAULT_NETWORK = "hedera-testnet";

    private TutorialClient() {}

    /** Returns the configured network name, for logging or for branching inside an example. */
    public static String configuredNetwork() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String network = System.getenv("HEDERA_NETWORK");
        if (isBlank(network)) {
            network = dotenv.get("HEDERA_NETWORK");
        }
        if (isBlank(network)) {
            network = dotenv.get("spring.hiero.network.name");
        }
        return isBlank(network) ? DEFAULT_NETWORK : network.trim();
    }

    /**
     * Creates a client for the configured network. The operator is not set — the caller does that.
     *
     * @throws IllegalArgumentException if the configured network name is not recognised
     */
    public static Client forConfiguredNetwork() throws InterruptedException {
        String network = configuredNetwork();
        System.out.println("Using network: " + network);

        return switch (network.toLowerCase()) {
            case "hedera-testnet", "testnet" -> Client.forTestnet();
            case "hedera-previewnet", "previewnet" -> Client.forPreviewnet();
            case "hedera-mainnet", "mainnet" -> Client.forMainnet();
            case "hiero-solo-action", "solo", "local" -> forSolo();
            default -> throw new IllegalArgumentException(
                    "Unknown network '" + network + "'. Use hedera-testnet, hedera-previewnet, "
                            + "hedera-mainnet or hiero-solo-action.");
        };
    }

    /**
     * Creates a client for the local network started by hiero-solo-action. The endpoints can be
     * overridden with {@code HIERO_SOLO_CONSENSUS_NODE}, {@code HIERO_SOLO_CONSENSUS_NODE_ACCOUNT}
     * and {@code HIERO_SOLO_MIRROR_GRPC} when the action runs with non-default ports.
     */
    private static Client forSolo() throws InterruptedException {
        String consensusNode = envOrDefault("HIERO_SOLO_CONSENSUS_NODE", SOLO_CONSENSUS_NODE_DEFAULT);
        String nodeAccount =
                envOrDefault("HIERO_SOLO_CONSENSUS_NODE_ACCOUNT", SOLO_CONSENSUS_NODE_ACCOUNT_DEFAULT);
        String mirrorGrpc = envOrDefault("HIERO_SOLO_MIRROR_GRPC", SOLO_MIRROR_GRPC_DEFAULT);

        Client client = Client.forNetwork(Map.of(consensusNode, AccountId.fromString(nodeAccount)));
        // Needed by examples that subscribe to topic messages through the mirror node.
        client.setMirrorNetwork(List.of(mirrorGrpc));
        return client;
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        if (isBlank(value)) {
            value = Dotenv.configure().ignoreIfMissing().load().get(name);
        }
        return isBlank(value) ? fallback : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
