package com.example.service

//import co.paralleluniverse.fibers.Suspendable
import com.example.api.Oracle
import com.example.flow.DeclaratieFlow
import com.example.model.Declaratie
//import net.corda.core.node.PluginServiceHub
//import net.corda.core.serialization.SingletonSerializeAsToken
import co.paralleluniverse.fibers.Suspendable
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
import net.corda.core.transactions.FilteredTransaction
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
object ExampleService {
  //  val type = ServiceType.corda.getSubType("interest_rates")

    class Service(services: PluginServiceHub) {

        init {
            services.registerFlowInitiator(DeclaratieFlow.Zorgaanbieder::class) { DeclaratieFlow.Zorgverzekeraar(it) }


        }


    }
}