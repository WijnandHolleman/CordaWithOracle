package com.example.api

import com.example.contract.DeclaratieContract
import com.example.contract.DeclaratieState
import com.example.flow.DeclaratieFlow
import com.example.flow.DeclaratieFlowResult
import com.example.model.Declaratie
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.linearHeadsOfType
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

// This API is accessible from /api/declaratie. All paths specified below are relative to it.
@Path("declaratie")
class DeclaratieApi(val services: ServiceHub) {
    val me: String = services.myInfo.legalIdentity.name

    /**
     * Returns the party name of the node providing this end-point.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to me)

    /**
     * Returns all parties registered with the [NetworkMapService], the names can be used to look-up identities
     * by using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers() = mapOf("peers" to services.networkMapCache.partyNodes
            .map { it.legalIdentity.name }
            .filter { it != me && it != "Controller" })

    @GET
    @Path("declaraties")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPurchaseOrders() = services.vaultService.linearHeadsOfType<DeclaratieState>()

    @PUT
        @Path("{party}/indienen")
    fun createPurchaseOrder(po: Declaratie, @PathParam("party") partyName: String): Response {
        val otherParty = services.identityService.partyFromName(partyName)
        if(otherParty != null) {
            val state = DeclaratieState(po, services.myInfo.legalIdentity, otherParty, DeclaratieContract())
            // The line below blocks and waits for the future to resolve.
            val result: DeclaratieFlowResult = services.invokeFlowAsync(DeclaratieFlow.Initiator::class.java, state, otherParty).resultFuture.get()
            when (result) {
                is DeclaratieFlowResult.Success ->
                    return Response
                            .status(Response.Status.CREATED)
                            .entity(result.message)
                            .build()
                is DeclaratieFlowResult.Failure ->
                    return Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity(result.message)
                            .build()
            }
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }
    }
//
//    /**
//     * Displays all purchase order states that exist in the vault.
//     */
//    @GET
//    @Path("purchase-orders")
//    @Produces(MediaType.APPLICATION_JSON)
//    fun getPurchaseOrders() = services.vaultService.linearHeadsOfType<PurchaseOrderState>()
//
//    /**
//     * This should only be called from the 'buyer' node. It initiates a flow to agree a purchase order with a
//     * seller. Once the flow finishes it will have written the purchase order to ledger. Both the buyer and the
//     * seller will be able to see it when calling /api/example/purchase-orders on their respective nodes.
//     *
//     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
//     * in its network map cache, it will return an HTTP bad request.
//     *
//     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
//     */
//    @PUT
//    @Path("{party}/create-purchase-order")
//    fun createPurchaseOrder(po: PurchaseOrder, @PathParam("party") partyName: String): Response {
//        val otherParty = services.identityService.partyFromName(partyName)
//        if(otherParty != null) {
//            val state = PurchaseOrderState(po, services.myInfo.legalIdentity, otherParty, PurchaseOrderContract())
//            // The line below blocks and waits for the future to resolve.
//            val result: ExampleFlowResult = services.invokeFlowAsync(ExampleFlow.Initiator::class.java, state, otherParty).resultFuture.get()
//            when (result) {
//                is ExampleFlowResult.Success ->
//                    return Response
//                            .status(Response.Status.CREATED)
//                            .entity(result.message)
//                            .build()
//                is ExampleFlowResult.Failure ->
//                    return Response
//                            .status(Response.Status.BAD_REQUEST)
//                            .entity(result.message)
//                            .build()
//            }
//        } else {
//            return Response.status(Response.Status.BAD_REQUEST).build()
//        }
//    }
}