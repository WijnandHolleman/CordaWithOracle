package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.model.AgbEntry
import com.example.model.AgbEntryOf
import net.corda.core.contracts.CommandData
import net.corda.core.crypto.DigitalSignature
import net.corda.core.crypto.Party
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.transactions.FilterFuns
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.irs.utilities.suggestTimeWindow
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.reflect.jvm.internal.impl.javax.inject.Singleton

/**
 * Created by Wijnand on 10-2-2017.
 */

open class OracleFlow(protected val tx: TransactionBuilder,
                        /** Filtering functions over transaction, used to build partial transaction presented to oracle. */
                        private val filterFuns: FilterFuns,
                        private val oracle: Party,
                        private val agbCode: AgbEntryOf,
                        override val progressTracker: ProgressTracker = OracleFlow.tracker("Oracle")) : FlowLogic<Unit>() {

    companion object {
        class QUERYING(val name: String) : ProgressTracker.Step("Querying oracle for $name AGB Code")
        object WORKING : ProgressTracker.Step("Working with data returned by oracle")
        object SIGNING : ProgressTracker.Step("Requesting confirmation signature from AGB oracle")

        fun tracker(fixName: String) = ProgressTracker(QUERYING(fixName), WORKING, SIGNING)
    }

    data class SignRequest(val rootHash: SecureHash, val ftx: FilteredTransaction)
    data class QueryRequest(val queries: List<AgbEntryOf>, val deadline: Instant)

    @Suspendable
    override fun call() {
        progressTracker.currentStep = progressTracker.steps[1]
        val agbFlow = subFlow(AgbQueryFlow(agbCode, oracle))
        progressTracker.currentStep = WORKING
        tx.addCommand(agbFlow, oracle.owningKey)
        beforeSigning(agbFlow)
        progressTracker.currentStep = SIGNING
        val signature = subFlow(AgbSignFlow(tx, oracle, filterFuns))
        tx.addSignatureUnchecked(signature)
    }

    @Suspendable
    protected open fun beforeSigning(fix: AgbEntry) {
    }


    class AgbQueryFlow(val agbCode: AgbEntryOf, val oracle: Party) : FlowLogic<AgbEntry>() {
        @Suspendable
        override fun call(): AgbEntry {
            val deadline = suggestTimeWindow(LocalDate.now()).end
            // TODO: add deadline to receive
            val resp = sendAndReceive<List<AgbEntry>>(oracle, QueryRequest(listOf(agbCode), deadline))

            return resp.unwrap {
                val agb = it.first()
                // Check the returned code is for what we asked for.
                check(agb.of == agbCode, {"AGBcode is niet bekend"})
                agb
            }
        }
    }

    class AgbSignFlow(val tx: TransactionBuilder, val oracle: Party, val filterFuns: FilterFuns) : FlowLogic<DigitalSignature.LegallyIdentifiable>() {
        @Suspendable
        override fun call(): DigitalSignature.LegallyIdentifiable {
            val wtx = tx.toWireTransaction()
            val partialMerkleTx = FilteredTransaction.buildMerkleTransaction(wtx, filterFuns)
            val rootHash = wtx.id

            val resp = sendAndReceive<DigitalSignature.LegallyIdentifiable>(oracle, SignRequest(rootHash, partialMerkleTx))
            return resp.unwrap { sig ->
                check(sig.signer == oracle)
                tx.checkSignature(sig)
                sig
            }
        }
    }
}