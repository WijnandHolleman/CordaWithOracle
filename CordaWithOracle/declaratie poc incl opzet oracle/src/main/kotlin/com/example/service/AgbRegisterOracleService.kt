package com.example.service

//import co.paralleluniverse.fibers.Suspendable
import com.example.api.Oracle
import com.example.flow.DeclaratieFlow
import com.example.model.Declaratie
//import net.corda.core.node.PluginServiceHub
//import net.corda.core.serialization.SingletonSerializeAsToken
import co.paralleluniverse.fibers.Suspendable
import com.example.contract.DeclaratieState
import net.corda.core.RetryableException
import net.corda.core.contracts.*
import net.corda.core.crypto.*
import net.corda.core.flows.FlowLogic
import net.corda.core.math.CubicSplineInterpolator
import net.corda.core.math.Interpolator
import net.corda.core.math.InterpolatorFactory
import net.corda.core.node.CordaPluginRegistry
import net.corda.core.node.PluginServiceHub
import net.corda.core.node.services.ServiceType
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.FilterFuns
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.node.services.api.AcceptsFileUpload
import net.corda.node.utilities.AbstractJDBCHashSet
import net.corda.node.utilities.FiberBox
import net.corda.node.utilities.JDBCHashedTable
import net.corda.node.utilities.localDate
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.io.InputStream
import java.math.BigDecimal
import java.security.KeyPair
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.*
import java.util.function.Function
import javax.annotation.concurrent.ThreadSafe

/**
 * This service registers a flow factory we wish to use when a initiating party attempts to communicate with us
 * using a particular flow. Registration is done against a marker class (in this case [ExampleFlow.Initiator]
 * which is sent in the session handshake by the other party. If this marker class has been registered then the
 * corresponding factory will be used to create the flow which will communicate with the other side. If there is no
 * mapping then the session attempt is rejected.
 *
 * In short, this bit of code is required for the seller in this Example scenario to repond to the buyer using the
 * [ExampleFlow.Acceptor] flow.
 */
object AgbRegisterOracleService {
    val type = ServiceType.corda.getSubType("agb_register_oracle_service")

    class Service(services: PluginServiceHub) : SingletonSerializeAsToken() {

        val oracle: Oracle by lazy {
            val myNodeInfo = services.myInfo
            val myIdentity = myNodeInfo.serviceIdentities(type).first()
            val mySigningKey = services.keyManagementService.toKeyPair(myIdentity.owningKey.keys)
            Oracle(myIdentity, mySigningKey, services.clock)
        }


        init {
          //  services.registerFlowInitiator(DeclaratieFlow.Zorgaanbieder::class) { DeclaratieFlow.Zorgverzekeraar(it) }
            services.registerFlowInitiator(DeclaratieFlow.Zorgaanbieder::class) { DeclaratieHandler(it, this) }
            //services.registerFlowInitiator(DeclaratieFlow.FixSignFlow::class) { FixSignHandler(it, this) }
            //services.registerFlowInitiator(DeclaratieFlow.FixQueryFlow::class) { FixQueryHandler(it, this) }

        }


        private class DeclaratieHandler(val otherParty: Party, val service: Service) : FlowLogic<Unit>() {
            @Suspendable
            override fun call() {


//                val request = receive<DeclaratieFlow.SignRequest>(zorgaanbiederParty).unwrap { it }
//                send(zorgaanbiederParty, service.oracle.sign(request.ftx, request.rootHash))



                //val request = receive<SignedTransaction>(otherParty).unwrap { it }
                val message = receive<TransactionState<DealState>>(otherParty).unwrap { it }
                val utx = message.data.generateAgreement(message.notary)

                val wtx = utx.toWireTransaction()
                //val wtx = request.tx
                val filterFuns = FilterFuns()
                val partialMerkleTx = FilteredTransaction.buildMerkleTransaction(wtx, filterFuns)
                val rootHash = wtx.id

//                send(otherParty, service.oracle.sign(request.ftx, request.rootHash))
                //var ltx = request.toLedgerTransaction(serviceHub)

                send(otherParty, service.oracle.signWiretransaction(wtx, rootHash))


//                val wtx = message ..toWireTransaction()
//                val partialMerkleTx = FilteredTransaction.buildMerkleTransaction(wtx, filterFuns)
//
//                send(zorgaanbiederParty, service.oracle.sign(message.ftx, request.rootHash))
            }
        }

//
//        private class FixSignHandler(val zorgverzekeraarParty: Party, val service: Service) : FlowLogic<Unit>() {
//            @Suspendable
//            override fun call() {
//                val request = receive<DeclaratieFlow.SignRequest>(zorgverzekeraarParty).unwrap { it }
//                send(zorgverzekeraarParty, service.oracle.sign(request.ftx, request.rootHash))
//            }
//        }

//        @Suspendable
//        override fun call(): Unit {
//            val request = receive<DeclaratieFlow.QueryRequest>(zorgverzekeraarParty).unwrap { it }
////            val answers = service.oracle.query(request.queries, request.deadline)
////            progressTracker.currentStep = SENDING
////            send(zorgverzekeraarParty, answers)
//        }

    }
}