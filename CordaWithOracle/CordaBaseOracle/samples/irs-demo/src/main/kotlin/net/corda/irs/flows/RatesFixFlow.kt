package net.corda.irs.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Fix
import net.corda.core.contracts.FixOf
import net.corda.core.contracts.VektisFix
import net.corda.core.contracts.VektisFixOf
import net.corda.core.crypto.DigitalSignature
import net.corda.core.crypto.Party
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.transactions.FilterFuns
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.irs.flows.RatesFixFlow.FixOutOfRange
import net.corda.irs.utilities.suggestInterestRateAnnouncementTimeWindow
import java.math.BigDecimal
import java.time.Instant
import java.util.*

// This code is unit tested in NodeInterestRates.kt

/**
 * This flow queries the given oracle for an interest rate fix, and if it is within the given tolerance embeds the
 * fix in the transaction and then proceeds to get the oracle to sign it. Although the [call] method combines the query
 * and signing step, you can run the steps individually by constructing this object and then using the public methods
 * for each step.
 *
 * @throws FixOutOfRange if the returned fix was further away from the expected rate by the given amount.
 */
open class RatesFixFlow(protected val tx: TransactionBuilder,
                        /** Filtering functions over transaction, used to build partial transaction presented to oracle. */
                        private val filterFuns: FilterFuns,
                        private val oracle: Party,
                        private val fixOf: FixOf,
                        private val expectedRate: BigDecimal,
                        private val rateTolerance: BigDecimal,
                        override val progressTracker: ProgressTracker = RatesFixFlow.tracker(fixOf.name)) : FlowLogic<Unit>() {

    companion object {
        class QUERYING(val name: String) : ProgressTracker.Step("Querying oracle for $name interest rate")
        object WORKING : ProgressTracker.Step("Working with data returned by oracle")
        object SIGNING : ProgressTracker.Step("Requesting confirmation signature from interest rate oracle")

        fun tracker(fixName: String) = ProgressTracker(QUERYING(fixName), WORKING, SIGNING)
    }

    class FixOutOfRange(@Suppress("unused") val byAmount: BigDecimal) : Exception("Fix out of range by $byAmount")

    data class QueryRequest(val queries: List<FixOf>, val deadline: Instant)
    data class VektisQueryRequest(val queries: List<VektisFix>, val deadline: Instant)
    data class SignRequest(val rootHash: SecureHash, val ftx: FilteredTransaction)

    // DOCSTART 2
    @Suspendable
    override fun call() {
        progressTracker.currentStep = progressTracker.steps[1]
        val fix = subFlow(FixQueryFlow(fixOf, oracle))
        progressTracker.currentStep = WORKING
        checkFixIsNearExpected(fix)
        tx.addCommand(fix, oracle.owningKey)
        beforeSigning(fix)
        progressTracker.currentStep = SIGNING
        val signature = subFlow(FixSignFlow(tx, oracle, filterFuns))
        tx.addSignatureUnchecked(signature)
    }
    // DOCEND 2

    /**
     * You can override this to perform any additional work needed after the fix is added to the transaction but
     * before it's sent back to the oracle for signing (for example, adding output states that depend on the fix).
     */
    @Suspendable
    protected open fun beforeSigning(fix: Fix) {
    }

    private fun checkFixIsNearExpected(fix: Fix) {
        val delta = (fix.value - expectedRate).abs()
        if (delta > rateTolerance) {
            // TODO: Kick to a user confirmation / ui flow if it's out of bounds instead of raising an exception.
            throw FixOutOfRange(delta)
        }
    }

    // DOCSTART 1
    class FixQueryFlow(val fixOf: FixOf, val oracle: Party) : FlowLogic<Fix>() {
        @Suspendable
        override fun call(): Fix {
            val deadline = suggestInterestRateAnnouncementTimeWindow(fixOf.name, oracle.name, fixOf.forDay).end
            // TODO: add deadline to receive
            val resp = sendAndReceive<ArrayList<Fix>>(oracle, QueryRequest(listOf(fixOf), deadline))

            return resp.unwrap {
                val fix = it.first()
                // Check the returned fix is for what we asked for.
                check(fix.of == fixOf)
                fix
            }
        }
    }

    class FixSignFlow(val tx: TransactionBuilder, val oracle: Party, val filterFuns: FilterFuns) : FlowLogic<DigitalSignature.LegallyIdentifiable>() {
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
    // DOCEND 1
}
