package com.hedera.tutorial.contract;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.ContractCreateTransaction;
import com.hedera.hashgraph.sdk.ContractFunctionParameters;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.ContractInfo;
import com.hedera.hashgraph.sdk.ContractInfoQuery;
import com.hedera.hashgraph.sdk.FileCreateTransaction;
import com.hedera.hashgraph.sdk.FileDeleteTransaction;
import com.hedera.hashgraph.sdk.FileId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.github.cdimascio.dotenv.Dotenv;

import java.math.BigInteger;
import java.util.concurrent.TimeoutException;

public class CreateContractSdkTutorial {
  public static void main(String[] args) throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
    Dotenv dotenv = Dotenv.load();

    AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
    PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

    Client client = Client.forTestnet();
    client.setOperator(operatorId, operatorKey);

    String contractBytecode = "6080604052348015600e575f5ffd5b506040516102133803806102138339818101604052810190602e9190606b565b805f81905550506091565b5f5ffd5b5f819050919050565b604d81603d565b81146056575f5ffd5b50565b5f815190506065816046565b92915050565b5f60208284031215607d57607c6039565b5b5f6088848285016059565b91505092915050565b6101758061009e5f395ff3fe608060405234801561000f575f5ffd5b5060043610610034575f3560e01c8063a87d942c14610038578063d09de08a14610056575b5f5ffd5b610040610074565b60405161004d91906100b2565b60405180910390f35b61005e61007c565b60405161006b91906100b2565b60405180910390f35b5f5f54905090565b5f5f5f81548092919061008e906100f8565b91905055505f54905090565b5f819050919050565b6100ac8161009a565b82525050565b5f6020820190506100c55f8301846100a3565b92915050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601160045260245ffd5b5f6101028261009a565b91507f7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8203610134576101336100cb565b5b60018201905091905056fea26469706673582212205ad6576cf1255565b1f6f9cc5324f068455cd6561a95aa0258704731ebf729b964736f6c63430008220033";

    // Admin Key (Need to update/delete contract)
    PrivateKey adminKey = PrivateKey.generateECDSA();

    TransactionResponse fileResponse = new FileCreateTransaction()
        .setContents(contractBytecode)
        .setFileMemo("Contract Bytecode")
        .setKeys(adminKey) // require to delete file
        .freezeWith(client)
        .sign(adminKey)
        .execute(client);

    TransactionReceipt fileReceipt = fileResponse.getReceipt(client);
    FileId fileId = fileReceipt.fileId;


    /*
      constructor(int count) {
          _count = count;
      }
     */
    TransactionResponse contractResponse = new ContractCreateTransaction()
        .setContractMemo("Counter Contract")
        .setConstructorParameters(new ContractFunctionParameters().addInt256(BigInteger.ZERO))
        .setBytecodeFileId(fileId)
        .setMaxTransactionFee(Hbar.from(50))
        .setGas(15_000_000)
        .setAdminKey(adminKey)
        .freezeWith(client)
        .sign(adminKey)
        .execute(client);

    TransactionReceipt contractReceipt = contractResponse.getReceipt(client);
    ContractId contractId = contractReceipt.contractId;

    System.out.println("Contract Created with ID: " + contractId);

    new FileDeleteTransaction()
        .setFileId(fileId)
        .freezeWith(client)
        .sign(adminKey)
        .execute(client);

    ContractInfo contractInfo = new ContractInfoQuery().setContractId(contractId).execute(client);
    System.out.println("Contract Info:\n" + contractInfo);

  }
}
