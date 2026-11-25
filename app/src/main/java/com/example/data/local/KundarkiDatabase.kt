package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ShopEntity::class,
        ProductEntity::class,
        CartItemEntity::class,
        FavoriteShopEntity::class,
        PromoCodeEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        DeliveryPartnerEntity::class,
        AppSettingsEntity::class,
        UserAccountEntity::class,
        ShopSettlementEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class KundarkiDatabase : RoomDatabase() {
    abstract fun dao(): KundarkiDao

    companion object {
        @Volatile
        private var INSTANCE: KundarkiDatabase? = null

        fun getInstance(context: Context): KundarkiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KundarkiDatabase::class.java,
                    "kundarki_delivery.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

/**
 * Static Kundarki service-area geography and UI helper configuration.
 * Contains zero mock shops, products, orders, or demo user accounts —
 * all business data is fetched strictly from Supabase.
 */
object KundarkiConfig {
    val neighborhoods = listOf(
        "Main Bazar, Kundarki",
        "Station Road, Kundarki",
        "Naya Mohalla, Kundarki",
        "Purana Bazar, Kundarki",
        "Sadaat Mohalla, Kundarki",
        "Qazi Tola, Kundarki",
        "Bilari Road, Kundarki",
        "Moradabad Road, Kundarki",
        "Sheikhupur Khas",
        "Near Jama Masjid, Kundarki"
    )

    fun formatDeliveryAddress(area: String): String {
        val clean = area.trim()
        return if (clean.contains("Kundarki", ignoreCase = true)) {
            "$clean, Moradabad, UP 244413"
        } else {
            "$clean, Kundarki, Moradabad, UP 244413"
        }
    }

    val deliveryInstructionPresets = listOf(
        "Call on arrival",
        "Don't ring bell",
        "Extra spicy",
        "Leave at gate"
    )

    val defaultSettings = AppSettingsEntity(
        id = 1,
        minOrderAmount = 99,
        baseDeliveryFee = 20,
        freeDeliveryAbove = 299,
        platformFee = 5,
        supportWhatsapp = "919876543210"
    )

    fun normalizeIndianPhone(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        val last10 = when {
            digits.length == 10 -> digits
            digits.length == 12 && digits.startsWith("91") -> digits.substring(2)
            digits.length == 11 && digits.startsWith("0") -> digits.substring(1)
            else -> return null
        }
        return "+91$last10"
    }

    fun hashPin(normalizedPhone: String, pin4: String): String {
        val cleanPin = pin4.trim()
        if (cleanPin.isEmpty()) return ""
        val salted = "fatakse_v1:$normalizedPhone:$cleanPin"
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(salted.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifyPinHash(normalizedPhone: String, pin4: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return true
        val candidate = hashPin(normalizedPhone, pin4)
        if (candidate.isBlank()) return false
        return java.security.MessageDigest.isEqual(
            candidate.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }
}
