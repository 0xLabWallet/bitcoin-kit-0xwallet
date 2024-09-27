package com.wallet0x.bitcoincore.network.peer

import com.wallet0x.bitcoincore.models.InventoryItem

interface IInventoryItemsHandler {
    fun handleInventoryItems(peer: Peer, inventoryItems: List<InventoryItem>)
}

class InventoryItemsHandlerChain : IInventoryItemsHandler {

    private val concreteHandlers = mutableListOf<IInventoryItemsHandler>()

    override fun handleInventoryItems(peer: Peer, inventoryItems: List<InventoryItem>) {
        concreteHandlers.forEach {
            it.handleInventoryItems(peer, inventoryItems)
        }
    }

    fun addHandler(h: IInventoryItemsHandler) {
        concreteHandlers.add(h)
    }

}
