package com.example.contract

import com.example.flow.ExampleFlow
import com.example.model.Address
import com.example.model.Item
import com.example.model.PurchaseOrder
import com.example.service.AGBOracleService
import com.example.service.ExampleService
import net.corda.core.bd
import net.corda.core.contracts.Command
import net.corda.core.contracts.Fix
import net.corda.core.getOrThrow
import net.corda.core.utilities.TEST_TX_TIME
import net.corda.testing.*
import net.corda.testing.node.MockNetwork
import org.junit.Test
import java.time.Duration
import java.util.*
import kotlin.test.assertEquals
import net.corda.core.node.services.ServiceInfo
import net.corda.node.utilities.databaseTransaction
import net.corda.core.contracts.*

class PurchaseOrderTests {
    @Test
    fun `transaction must be timestamped`() {
        val address = Address("London", "UK")
        val items = listOf(Item("Hammer", 1))
        val deliveryTime = TEST_TX_TIME.plus(Duration.ofDays(7))
        val purchaseOrder = PurchaseOrder(1, 1, Date(deliveryTime.toEpochMilli()), address, items)
        ledger {
            transaction {
                output { PurchaseOrderState(purchaseOrder, MINI_CORP, MEGA_CORP, PurchaseOrderContract()) }
                `fails with`("must be timestamped")
                timestamp(TEST_TX_TIME)
                command(MEGA_CORP_PUBKEY, MINI_CORP_PUBKEY) { PurchaseOrderContract.Commands.Place() }
                verifies()
            }
        }
    }

    @Test
    fun `transaction must include place command`() {
        val address = Address("London", "UK")
        val items = listOf(Item("Hammer", 1))
        val deliveryTime = TEST_TX_TIME.plus(Duration.ofDays(7))
        val purchaseOrder = PurchaseOrder(1, 1, Date(deliveryTime.toEpochMilli()), address, items)
        ledger {
            transaction {
                output { PurchaseOrderState(purchaseOrder, MINI_CORP, MEGA_CORP, PurchaseOrderContract()) }
                timestamp(TEST_TX_TIME)
                `fails with`("Required com.example.contract.PurchaseOrderContract.Commands.Place command")
                command(MEGA_CORP_PUBKEY, MINI_CORP_PUBKEY) { PurchaseOrderContract.Commands.Place() }
                verifies()
            }
        }
    }

    @Test
    fun `buyer must sign transaction`() {
        val address = Address("London", "UK")
        val items = listOf(Item("Hammer", 1))
        val deliveryTime = TEST_TX_TIME.plus(Duration.ofDays(7))
        val purchaseOrder = PurchaseOrder(1, 1, Date(deliveryTime.toEpochMilli()), address, items)
        ledger {
            transaction {
                output { PurchaseOrderState(purchaseOrder, MINI_CORP, MEGA_CORP, PurchaseOrderContract()) }
                timestamp(TEST_TX_TIME)
                command(MINI_CORP_PUBKEY) { PurchaseOrderContract.Commands.Place() }
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `seller must sign transaction`() {
        val address = Address("London", "UK")
        val items = listOf(Item("Hammer", 1))
        val deliveryTime = TEST_TX_TIME.plus(Duration.ofDays(7))
        val purchaseOrder = PurchaseOrder(1, 1, Date(deliveryTime.toEpochMilli()), address, items)
        ledger {
            transaction {
                output { PurchaseOrderState(purchaseOrder, MINI_CORP, MEGA_CORP, PurchaseOrderContract()) }
                timestamp(TEST_TX_TIME)
                command(MEGA_CORP_PUBKEY) { PurchaseOrderContract.Commands.Place() }
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `cannot place empty orders`() {
        val address = Address("London", "UK")
        val items = emptyList<Item>()
        val deliveryTime = TEST_TX_TIME.plus(Duration.ofDays(7))
        val purchaseOrder = PurchaseOrder(1, 1, Date(deliveryTime.toEpochMilli()), address, items)
        ledger {
            transaction {
                output { PurchaseOrderState(purchaseOrder, MINI_CORP, MEGA_CORP, PurchaseOrderContract()) }
                timestamp(TEST_TX_TIME)
                command(MEGA_CORP_PUBKEY, MINI_CORP_PUBKEY) { PurchaseOrderContract.Commands.Place() }
                `fails with`("must order at least one type of item")
            }
        }
    }

    @Test
    fun `cannot place historical orders`() {
        val address = Address("London", "UK")
        val items = listOf(Item("Hammer", 1))
        val deliveryTime = TEST_TX_TIME.minus(Duration.ofDays(7))
        val purchaseOrder = PurchaseOrder(1, 1, Date(deliveryTime.toEpochMilli()), address, items)
        ledger {
            transaction {
                output { PurchaseOrderState(purchaseOrder, MINI_CORP, MEGA_CORP, PurchaseOrderContract()) }
                timestamp(TEST_TX_TIME)
                command(MEGA_CORP_PUBKEY, MINI_CORP_PUBKEY) { PurchaseOrderContract.Commands.Place() }
                `fails with`("delivery date must be in the future")
            }
        }
    }

//    @Test
//    fun `network tearoff`() {
//        val net = MockNetwork()
//        val n1 = net.createNotaryNode()
//        val n2 = net.createNode(n1.info.address, advertisedServices = ServiceInfo(AGBOracleService.type))
//        databaseTransaction(n2.database) {
//            n2.findService<AGBOracleService.Service>().oracle.knownAgbCodes
//        }
//        val tx = TransactionType.General.Builder(null)
//        val fixOf = NodeInterestRates.parseFixOf("LIBOR 2016-03-16 1M")
//        val oracle = n2.info.serviceIdentities(AGBOracleService.type).first()
//        fun filterCommands(c: Command) = oracle.owningKey in c.signers && c.value is Fix
//        val filterFuns = net.corda.core.transactions.FilterFuns(filterCommands = ::filterCommands)
//        val flow = RatesFixFlow(tx, filterFuns, oracle, fixOf, "0.675".bd, "0.1".bd)
//
//        val flow = ExampleFlow.Initiator()
//        net.corda.core.utilities.LogHelper.setLevel("rates")
//        net.runNetwork()
//        val future = n1.services.startFlow(flow).resultFuture
//        net.runNetwork()
//        future.getOrThrow()
//        // We should now have a valid signature over our tx from the oracle.
//        val fix = tx.toSignedTransaction(true).tx.commands.map { it.value as Fix }.first()
//        assertEquals(fixOf, fix.of)
//        assertEquals("0.678".bd, fix.value)
//    }
}
