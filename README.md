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

Grant / revoke KYC (enterprise — needs local SNAPSHOT with `NftClient.grantKycNft` / `revokeKycNft`):

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=grant-kyc-nft"
```

Grant / revoke KYC (SDK):

```bash
mvn -q exec:java -Dexec.mainClass=com.hedera.tutorial.nft.GrantKycNftSdkTutorial
```

