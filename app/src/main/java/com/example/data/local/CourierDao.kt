package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CourierDao {
    // Shipments
    @Query("SELECT * FROM shipments ORDER BY createdTimestamp DESC")
    fun getAllShipments(): Flow<List<ShipmentEntity>>

    @Query("SELECT * FROM shipments WHERE id = :id LIMIT 1")
    fun getShipmentById(id: String): Flow<ShipmentEntity?>

    @Query("SELECT * FROM shipments WHERE awbNumber = :awb LIMIT 1")
    suspend fun findShipmentByAwb(awb: String): ShipmentEntity?

    @Query("SELECT * FROM shipments WHERE awbNumber = :awb LIMIT 1")
    fun getShipmentByAwbFlow(awb: String): Flow<ShipmentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShipment(shipment: ShipmentEntity)

    @Query("UPDATE shipments SET status = :status, trackingEventsJson = :eventsJson WHERE id = :id")
    suspend fun updateShipmentStatus(id: String, status: String, eventsJson: String)

    @Query("DELETE FROM shipments WHERE id = :id")
    suspend fun deleteShipment(id: String)

    // Saved Addresses
    @Query("SELECT * FROM saved_addresses ORDER BY isDefault DESC, id DESC")
    fun getSavedAddresses(): Flow<List<SavedAddressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: SavedAddressEntity)

    @Query("DELETE FROM saved_addresses WHERE id = :id")
    suspend fun deleteAddress(id: Long)

    @Query("UPDATE saved_addresses SET isDefault = 0")
    suspend fun clearDefaultAddresses()

    @Query("UPDATE saved_addresses SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultAddress(id: Long)

    // Wallet Transactions
    @Query("SELECT * FROM wallet_transactions ORDER BY createdTimestamp DESC")
    fun getAllWalletTransactions(): Flow<List<WalletTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: WalletTransactionEntity)

    @Query("SELECT balanceAfter FROM wallet_transactions ORDER BY createdTimestamp DESC LIMIT 1")
    suspend fun getLatestWalletBalance(): Double?
}
