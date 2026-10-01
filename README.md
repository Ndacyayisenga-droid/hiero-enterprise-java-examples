# Hiero Enterprise Java Examples

## Spring Boot (enterprise)

Install a local `hiero-enterprise-spring` build first when using SNAPSHOT APIs:

```bash
cd /Users/noah/hiero/hiero-enterprise-java
./mvnw -pl hiero-enterprise-spring -am install -DskipTests
```

How to run the examples:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=transfer-accounts"
```

Get account info:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=get-account-info"
```

Delete NFT type (enterprise — needs local SNAPSHOT with `NftClient.deleteNftType`):

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=delete-nft-type"
```

Delete NFT type (SDK):

```bash
mvn -q exec:java -Dexec.mainClass=com.hedera.tutorial.nft.DeleteNftTypeSdkTutorial
```

Update NFT type (enterprise — needs local SNAPSHOT with `NftClient.updateNftType`):

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=update-nft-type"
```

Update NFT type (SDK):

```bash
mvn -q exec:java -Dexec.mainClass=com.hedera.tutorial.nft.UpdateNftTypeSdkTutorial
```

Update NFT metadata (enterprise — needs local SNAPSHOT with `updateNftMetadata` and create-with-metadata-key):

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=update-nft-metadata"
```

Update NFT metadata (SDK):

```bash
mvn -q exec:java -Dexec.mainClass=com.hedera.tutorial.nft.UpdateNftMetadataSdkTutorial
```

Wipe NFT (enterprise — needs local SNAPSHOT with `NftClient.wipeNft` / `wipeNfts`):

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=wipe-nft"
```

Wipe NFT (SDK):

```bash
mvn -q exec:java -Dexec.mainClass=com.hedera.tutorial.nft.WipeNftSdkTutorial
```

Freeze NFT (enterprise — needs local SNAPSHOT with `NftClient.freezeNft` / `unfreezeNft`):

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=freeze-nft"
```

Freeze NFT (SDK):

```bash
mvn -q exec:java -Dexec.mainClass=com.hedera.tutorial.nft.FreezeNftSdkTutorial
```

Airdrop NFT (enterprise — needs local SNAPSHOT with `NftClient.airdropNft` / `airdropNfts` including map overload):

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=airdrop-nft"
```

Airdrop NFT (SDK):

```bash
mvn -q exec:java -Dexec.mainClass=com.hedera.tutorial.nft.AirdropNftSdkTutorial
```

Cancel airdrop NFT (enterprise — needs local SNAPSHOT with `NftClient.cancelAirdropNft`):

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=cancel-airdrop-nft"
```

Cancel airdrop NFT (SDK):

```bash
mvn -q exec:java -Dexec.mainClass=com.hedera.tutorial.nft.CancelAirdropNftSdkTutorial
```

Reject NFT (enterprise — needs local SNAPSHOT with `NftClient.rejectNft`):

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=reject-nft"
```

Reject NFT (SDK):

```bash
mvn -q exec:java -Dexec.mainClass=com.hedera.tutorial.nft.RejectNftSdkTutorial
```

Approve NFT allowance (enterprise — needs local SNAPSHOT with `NftClient.approveNftAllowance` / `approveNftAllowances` / `approveNftAllowanceAllSerials`):

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=approve-nft-allowance"
```

Approve NFT allowance (SDK):

```bash
mvn -q exec:java -Dexec.mainClass=com.hedera.tutorial.nft.ApproveNftAllowanceSdkTutorial
```

---

## Continuous integration

[`.github/workflows/examples.yml`](.github/workflows/examples.yml) runs **every** example on
each push and pull request — the 14 SDK tutorials and the 15 Spring enterprise profiles —
against a throwaway Hiero network started by
[hiero-solo-action](https://github.com/hiero-ledger/hiero-solo-action), so CI needs no testnet
account and no funded operator.

The job:

1. checks out this repo and `hiero-ledger/hiero-enterprise-java` (the examples depend on a
   `-SNAPSHOT` that is not published, so the library is built from source and installed),
2. starts Solo with a mirror node and waits for its REST and REST-Java endpoints,
3. writes a `.env` pointing at the Solo network, using the account the action generated,
4. runs `scripts/run-examples.sh`, and uploads each example's log as an artifact.

Run the same suite locally against whatever your `.env` points at:

```bash
./scripts/run-examples.sh                # every example
./scripts/run-examples.sh --sdk          # SDK tutorials only
./scripts/run-examples.sh --enterprise   # Spring runners only
./scripts/run-examples.sh --only nft     # only examples matching a pattern
```

The script discovers examples from the sources — any new `*SdkTutorial` class or `@Profile`
runner is picked up automatically — and prints a pass/fail summary, writing per-example logs to
`target/example-logs/`.

### Choosing the network

`TutorialClient.forConfiguredNetwork()` builds the SDK client from `HEDERA_NETWORK` (environment
variable first, then `.env`, falling back to `spring.hiero.network.name`). Recognised values are
`hedera-testnet` (the default), `hedera-previewnet`, `hedera-mainnet` and `hiero-solo-action`.
The same `.env` configures the Spring runners through `spring.hiero.*`, so one file switches both
flavours of example between testnet and a local Solo network.
