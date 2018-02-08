package com.example.model

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
import net.corda.core.transactions.WireTransaction
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

data class AgbEntryOf(val agbCode: Int, val geldigVan: LocalDate, val geldigTot: LocalDate)
data class AgbEntry(val of: AgbEntryOf, val exists: Boolean) : CommandData

    /**
        Oracle class
     */
    @ThreadSafe
    class Oracle (val identity: Party, private val signingKey: KeyPair) {

        val knownAgbCodes: List<AgbEntry> = listOf(
                AgbEntry(AgbEntryOf(agbCode = 5, geldigVan = LocalDate.of(2016, 5, 1), geldigTot = LocalDate.of(2018, 1, 1)), true),
                AgbEntry(AgbEntryOf(agbCode = 15, geldigVan = LocalDate.of(2016, 5, 1), geldigTot = LocalDate.of(2018, 1, 1)), true)
        )

        fun query(queries: List<AgbEntryOf>, deadline: Instant): List<AgbEntry>
        {
            require(queries.isNotEmpty())
            return knownAgbCodes;
        }

        fun sign(ftx: FilteredTransaction, merkleRoot: SecureHash): DigitalSignature.LegallyIdentifiable
        {
            //TODO: Niet alles signen!
            return signingKey.signWithECDSA(merkleRoot.bytes, identity)
        }
    }

