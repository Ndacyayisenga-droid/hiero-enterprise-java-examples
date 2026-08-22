package com.hedera.tutorial.contract;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.ContractCallQuery;
import com.hedera.hashgraph.sdk.ContractCreateFlow;
import com.hedera.hashgraph.sdk.ContractExecuteTransaction;
import com.hedera.hashgraph.sdk.ContractFunctionParameters;
import com.hedera.hashgraph.sdk.ContractFunctionResult;
import com.hedera.hashgraph.sdk.ContractId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.MirrorNodeContractCallQuery;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionRecord;
import com.hedera.hashgraph.sdk.TransactionResponse;
import io.github.cdimascio.dotenv.Dotenv;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class CallContractFunctionSdkTutorial {
  public static void main(String[] args) throws PrecheckStatusException, TimeoutException, ReceiptStatusException, ExecutionException, InterruptedException {
    Dotenv dotenv = Dotenv.load();

    AccountId operatorId = AccountId.fromString(dotenv.get("OPERATOR_ID"));
    PrivateKey operatorKey = PrivateKey.fromString(dotenv.get("OPERATOR_KEY"));

    Client client = Client.forTestnet();
    client.setOperator(operatorId, operatorKey);

    String contractBytecode = "6080604052348015600e575f5ffd5b506040516102133803806102138339818101604052810190602e9190606b565b805f81905550506091565b5f5ffd5b5f819050919050565b604d81603d565b81146056575f5ffd5b50565b5f815190506065816046565b92915050565b5f60208284031215607d57607c6039565b5b5f6088848285016059565b91505092915050565b6101758061009e5f395ff3fe608060405234801561000f575f5ffd5b5060043610610034575f3560e01c8063a87d942c14610038578063d09de08a14610056575b5f5ffd5b610040610074565b60405161004d91906100b2565b60405180910390f35b61005e61007c565b60405161006b91906100b2565b60405180910390f35b5f5f54905090565b5f5f5f81548092919061008e906100f8565b91905055505f54905090565b5f819050919050565b6100ac8161009a565b82525050565b5f6020820190506100c55f8301846100a3565b92915050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601160045260245ffd5b5f6101028261009a565b91507f7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8203610134576101336100cb565b5b60018201905091905056fea26469706673582212205ad6576cf1255565b1f6f9cc5324f068455cd6561a95aa0258704731ebf729b964736f6c63430008220033";

    TransactionResponse response = new ContractCreateFlow()
        .setBytecode(contractBytecode)
        .setConstructorParameters(new ContractFunctionParameters().addUint256(BigInteger.ZERO))
        .setContractMemo("Counter Contract")
        .setGas(15_000_000)
        .freezeWith(client)
        .execute(client);

    TransactionReceipt receipt = response.getReceipt(client);
    ContractId contractId = receipt.contractId;

    System.out.println("Contract created with ID: " + contractId);


    /*
      function increment() public returns(int) {
          _count++;
          return _count;
      }
    */
    TransactionRecord functionCallRecord = new ContractExecuteTransaction()
        .setContractId(contractId)
        .setFunction("increment")
        .setGas(15_000_000)
        .freezeWith(client)
        .execute(client)
        .getRecord(client);

    ContractFunctionResult incrementCallResult = functionCallRecord.contractFunctionResult;
    System.out.println("Call `increment()` Return value: " + incrementCallResult.getUint256(0));


    /*
      function getCount() public view returns(int) {
          return _count;
      }
     */

    // Only for pure/view function
    ContractFunctionResult getCountCallResult = new ContractCallQuery()
        .setContractId(contractId)
        .setFunction("getCount")
        .setMaxQueryPayment(Hbar.from(50))
        .setGas(15_000_000)
        .execute(client);

    System.out.println("Call `getCount()` return value: " + getCountCallResult.getUint256(0));

    String rawReturnData = new MirrorNodeContractCallQuery()
        .setContractId(contractId)
        .setFunction("increment")
        .setGasLimit(15_000_000)
        .setGasPrice(10)
        .execute(client);

    System.out.println("Call `increment()` using mirrornode ContractQuery: " + rawReturnData);
  }
}
