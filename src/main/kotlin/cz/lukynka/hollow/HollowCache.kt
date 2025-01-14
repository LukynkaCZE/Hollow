package io.github.dockyard.cz.lukynka.hollow

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

abstract class HollowCache<V>(val tableName: String) {

    private val cache: MutableMap<UUID, V> = mutableMapOf()
    protected var populated: Boolean = false
    protected var initialized: Boolean = false

    private val table = DynamicTable(tableName)

    abstract fun serialize(value: V): String
    abstract fun deserialize(string: String): V

    fun initialize() {
        if (initialized) throw IllegalStateException("Cache is already initialized!")
        transaction {
            SchemaUtils.create(table)
            populate()
            commit()
        }
        initialized = true
    }

    operator fun get(key: UUID): V {
        if(!initialized) throw IllegalStateException("Cache is not initialized yet!")
        return getOrNull(key) ?: throw IllegalArgumentException("Value with key $key was not found in the cache!")
    }

    fun getOrNull(key: UUID): V? {
        if(!initialized) throw IllegalStateException("Cache is not initialized yet!")
        return cache[key]
    }

    fun getAll(): Map<UUID, V> {
        if(!initialized) throw IllegalStateException("Cache is not initialized yet!")
        return cache.toMap()
    }

    operator fun set(key: UUID, value: V) {
        if(!initialized) throw IllegalStateException("Cache is not initialized yet!")
        val contains = cache.contains(key)
        cache[key] = value
        if (contains) {
            Hollow.addTask {
                table.update({ table.id eq key }) {
                    it[data] = serialize(value)
                }
            }
        } else {
            Hollow.addTask {
                table.insert {
                    it[id] = key
                    it[data] = serialize(value)
                }
            }
        }
    }

    fun remove(key: UUID) {
        if(!initialized) throw IllegalStateException("Cache is not initialized yet!")
        cache.remove(key)
        Hollow.addTask {
            table.deleteWhere { table.id eq key}
        }
    }

    fun clear() {
        if(!initialized) throw IllegalStateException("Cache is not initialized yet!")
        cache.clear()
        table.deleteAll()
    }

    fun clearAndPopulate() {
        if(!initialized) throw IllegalStateException("Cache is not initialized yet!")
        cache.clear()
        populated = false
        populate()
    }

    fun populate() {
        if (populated) throw IllegalStateException("${this::class.simpleName} is already populated!")
        table.selectAll().forEach { row ->
            val id = row[table.id].value
            val data = deserialize(row[table.data])
            cache[id] = data
        }
        populated = true
    }
}

class DynamicTable(name: String) : UUIDTable(name) {
    val data = text("data")
}