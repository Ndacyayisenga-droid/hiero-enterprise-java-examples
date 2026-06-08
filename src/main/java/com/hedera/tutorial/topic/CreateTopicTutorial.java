package com.hedera.tutorial.topic;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TopicCreateTransaction;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessageQuery;
import com.hedera.hashgraph.sdk.TopicMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class CreateTopicTutorial {
    public static void main(String[] args)
            throws TimeoutException, PrecheckStatusException, ReceiptStatusException, InterruptedException {
        Dotenv dotenv = Dotenv.load();

        AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
        PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

        Client client = Client.forTestnet();
        client.setOperator(operatorId, operatorKey);

        TransactionResponse txResponse = new TopicCreateTransaction()
                .setSubmitKey(operatorKey.getPublicKey())
                .execute(client);

        TransactionReceipt receipt = txResponse.getReceipt(client);
        TopicId topicId = receipt.topicId;

        System.out.println("Your topic ID is: " + topicId);

        Thread.sleep(5000);

        new TopicMessageQuery()
                .setTopicId(topicId)
                .subscribe(client, resp -> {
                    String messageAsString = new String(resp.contents, StandardCharsets.UTF_8);
                    System.out.println(resp.consensusTimestamp + " received topic message: " + messageAsString);
                });

        TransactionResponse submitMessage = new TopicMessageSubmitTransaction()
                .setTopicId(topicId)
                .setMessage("Submitkey set!")
                .freezeWith(client)
                .sign(operatorKey)
                .execute(client);

        submitMessage.getReceipt(client);

        Thread.sleep(30000);
    }
}
