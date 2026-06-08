# Hiero Enterprise Java Examples

## Spring Boot (enterprise)

Entry point: `com.hedera.tutorial.TutorialApplication`

Set `spring.profiles.active` in `application.properties` (or via `-Dspring.profiles.active=...`):

- `account` — `CreateAccountEnterpriseRunner`
- `topic` — `CreateTopicEnterpriseRunner`

```bash
mvn spring-boot:run
```

## Raw SDK tutorials

```bash
mvn exec:java -Dexec.mainClass=com.hedera.tutorial.account.CreateAccountTutorial
mvn exec:java -Dexec.mainClass=com.hedera.tutorial.topic.CreateTopicTutorial
```

Credentials: `.env` (`spring.hiero.*` for Spring, `OPERATOR_ID` / `OPERATOR_KEY` for raw SDK).
