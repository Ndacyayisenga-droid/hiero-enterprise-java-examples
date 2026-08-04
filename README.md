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
