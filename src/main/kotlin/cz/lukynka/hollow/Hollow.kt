package io.github.dockyard.cz.lukynka.hollow

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.Exception
import java.util.concurrent.LinkedTransferQueue

object Hollow {

    val jobQueue = LinkedTransferQueue<() -> Unit>()
    lateinit var databaseThread: Thread

    fun initialize(databasePath: String) {
        Database.connect("jdbc:sqlite:$databasePath.db", driver = "org.sqlite.JDBC")

        databaseThread = Thread {
            while (true) {
                try {
                    val job = jobQueue.take()
                    transaction {
                        job.invoke()
                        commit()
                    }
                    println("Processed database job ${job::class.simpleName}")
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
        databaseThread.start()
    }

    fun addTask(task: () -> Unit) {
        jobQueue.add(task)
    }

    fun removeTask(task: () -> Unit) {
        jobQueue.remove(task)
    }

    fun clearTasks() {
        jobQueue.clear()
    }

    fun shutdown() {
        jobQueue.clear()
        databaseThread.interrupt()
    }
}