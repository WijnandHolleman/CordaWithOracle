package com.example.service

import co.paralleluniverse.fibers.Suspendable
import com.example.flow.ExampleFlow
import com.example.flow.OracleFlow
import com.example.model.Oracle
import net.corda.core.contracts.DealState
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.Party
import net.corda.core.flows.FlowLogic
import net.corda.core.node.CordaPluginRegistry
import net.corda.core.node.PluginServiceHub
import net.corda.core.node.services.ServiceType
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.FilterFuns
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.utilities.ProgressTracker
import java.util.function.Function

/**
 * Created by Wijnand on 7-2-2017.
 */

object AGBOracleService {

    class Plugin : CordaPluginRegistry() {
        override val servicePlugins = listOf(Function(::Service))
    }


    val type = ServiceType.corda.getSubType("agb_register_oracle_service")
    class Service(val services: PluginServiceHub): SingletonSerializeAsToken() {

        val oracle: Oracle by lazy {
            val myNodeInfo = services.myInfo
            val myIdentity = myNodeInfo.serviceIdentities(type).first()
            val mySigningKey = services.keyManagementService.toKeyPair(myIdentity.owningKey.keys)
            Oracle(myIdentity, mySigningKey)
        }

        init {
            // Note: access to the singleton oracle property is via the registered SingletonSerializeAsToken Service.
            // Otherwise the Kryo serialisation of the call stack in the Quasar Fiber extends to include
            // the framework Oracle and the flow will crash.
            services.registerFlowInitiator(OracleFlow.AgbSignFlow::class) { AgbSignHandler(it, this) }
            services.registerFlowInitiator(OracleFlow.AgbQueryFlow::class) { AgbQueryHandler(it, this) }
        }

        private class AgbSignHandler(val otherParty: Party, val service: Service) : FlowLogic<Unit>() {
            @Suspendable
            override fun call() {
                val request = receive<OracleFlow.SignRequest>(otherParty).unwrap { it }
                send(otherParty, service.oracle.sign(request.ftx, request.rootHash))
            }
        }

        private class AgbQueryHandler(val otherParty: Party, val service: Service) : FlowLogic<Unit>() {
            companion object {
                object RECEIVED : ProgressTracker.Step("Received request")
                object SENDING : ProgressTracker.Step("Sending response")
            }

            override val progressTracker = ProgressTracker(RECEIVED, SENDING)

            init {
                progressTracker.currentStep = RECEIVED
            }

            @Suspendable
            override fun call(): Unit {
                val request = receive<OracleFlow.QueryRequest>(otherParty).unwrap { it }
                val answers = service.oracle.query(request.queries, request.deadline)
                progressTracker.currentStep = SENDING
                send(otherParty, answers)
            }
        }
    }
}