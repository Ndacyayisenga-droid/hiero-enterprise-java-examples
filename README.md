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

Update NFT fee schedule (enterprise — needs local SNAPSHOT with `NftClient.updateNftFeeSchedule` and create-with-fee-schedule-key):

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=update-nft-fee-schedule"
```

Update NFT fee schedule (SDK):

```bash
mvn -q exec:java -Dexec.mainClass=com.hedera.tutorial.nft.UpdateNftFeeScheduleSdkTutorial
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

Immutable topic (enterprise — needs local SNAPSHOT with `TopicClient.createImmutableTopic` / `updateExpirationTime`):

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=immutable-topic"
```

Immutable topic (SDK):

```bash
mvn -q exec:java -Dexec.mainClass=com.hedera.tutorial.topic.ImmutableTopicSdkTutorial
```

Topic auto renew and expiration (enterprise — needs local SNAPSHOT with `TopicClient.updateAutoRenewAccount` / `updateAutoRenewPeriod` / `clearAutoRenewAccount` / `updateExpirationTime`):

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=topic-auto-renew"
```

Topic auto renew and expiration (SDK):

```bash
mvn -q exec:java -Dexec.mainClass=com.hedera.tutorial.topic.TopicAutoRenewSdkTutorial
```

Clear topic properties (enterprise — needs local SNAPSHOT with `TopicClient.clearSubmitKey` / `clearTopicMemo` / `clearAdminKey`):

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=clear-topic-properties"
```

Clear topic properties (SDK):

```bash
mvn -q exec:java -Dexec.mainClass=com.hedera.tutorial.topic.ClearTopicPropertiesSdkTutorial
```
