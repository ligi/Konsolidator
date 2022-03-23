package org.komputing.konsolidator

import com.github.michaelbull.retry.policy.*
import com.github.michaelbull.retry.retry
import com.natpryce.konfig.*
import org.kethereum.eip155.signViaEIP155
import org.kethereum.erc55.hasValidERC55Checksum
import org.kethereum.erc55.isValid
import org.kethereum.extensions.transactions.encodeLegacyTxRLP
import org.kethereum.keystore.api.FileKeyStore
import org.kethereum.model.Address
import org.kethereum.model.ChainId
import org.kethereum.model.SignatureData
import org.kethereum.model.createTransactionWithDefaults
import org.kethereum.rpc.*
import org.komputing.khex.extensions.toHexString
import java.io.Console
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.math.RoundingMode
import kotlin.system.exitProcess

var skipConfirm = false

private val retryPolicy: RetryPolicy<Throwable> = binaryExponentialBackoff(50, 5000) + limitAttempts(7)

suspend fun main() {
    println("please enter config file location")
    val configFileName = readLine()!!
    val config = ConfigurationProperties.fromFile(File(configFileName))

    val receiveAddress = Address(config[Key("address", stringType)])
    val rpcURL = config[Key("rpc", stringType)]
    val keyPath = config[Key("keypath", stringType)]
    val pwd = config[Key("pwd", stringType)]

    println("config:")
    println("address=$receiveAddress")

    if (!receiveAddress.isValid()) {
        println("Address not valid")
        exitProcess(1)
    }

    if (!receiveAddress.hasValidERC55Checksum()) {
        println("Address checksum error")
        exitProcess(1)
    }

    println("rpc=$rpcURL")

    val keyStore = FileKeyStore(File(keyPath))

    //val rpc = BaseEthereumRPC(ConsoleLoggingTransportWrapper(HttpTransport(rpcURL)))
    val rpc = HttpEthereumRPC(rpcURL)
    val gasPrice = retry(retryPolicy) { rpc.gasPrice() } ?: throw IllegalStateException("Could not get gas price")
    val chainID = retry(retryPolicy) { rpc.chainId() } ?: throw java.lang.IllegalArgumentException("Could not get chainID")
    println("derived:")
    println("chainID=${chainID.value}")
    println("gasPrice=${gasPrice.toBigDecimal().divide(BigDecimal("1000000000"),2,RoundingMode.UP)} GWEI")
    println("keyCount=${keyStore.getAddresses().size}")

    println("correct? Type 'y' to continue")

    val response = readLine()

    if (response != "y") {
        println("exit on your wish")
        exitProcess(1)
    }

    keyStore.getAddresses().forEach { address ->
        val balance = retry(retryPolicy) { rpc.getBalance(address) } ?: ZERO
        println("$address@$chainID=$balance")
        if (balance != ZERO) {

            val nonce = retry(retryPolicy) { rpc.getTransactionCount(address) }

            val protoTX = createTransactionWithDefaults(
                chain = chainID,
                from = address,
                to = receiveAddress,
                value = ZERO,
                gasPrice = gasPrice,
                gasLimit = ZERO,
                nonce = nonce
            )
            val gas = retry(retryPolicy) { rpc.estimateGas(protoTX) } ?: throw IllegalStateException("Could not estimate gas limit")

            println("estimated gasLimit to: $gas")

            val tx = protoTX.copy(gasLimit = gas, value = balance - gas * gasPrice)
            println(tx)

            var input: String? = null
            if (!skipConfirm) {

                while (!listOf("y", "n", "a").contains(input)) {
                    println("send? (y/n/a)")
                    input = readLine()
                }

                if (input == "n") {
                    println("exit on your wish")
                    exitProcess(1)
                }

                if (input == "a") {
                    skipConfirm = true
                }
            }

            val key = keyStore.getKeyForAddress(address, pwd) ?: throw java.lang.IllegalArgumentException("Could not get key for address $address")
            val signedTx: SignatureData = tx.signViaEIP155(key, chainID)

            val rlp: ByteArray = retry(retryPolicy) { tx.encodeLegacyTxRLP(signedTx) }
            val rlpHex = rlp.toHexString()
            println("tx hash: " + rpc.sendRawTransaction(rlpHex))
        }
    }
}
