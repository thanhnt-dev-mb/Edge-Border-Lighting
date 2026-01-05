package com.merryblue.baseapplication

import kotlinx.coroutines.*
import java.util.concurrent.Executors

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")


fun main() = runBlocking {
    val context1 = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
    val context2 = newFixedThreadPoolContext(2, "Fixed")

    repeat(4) {
        launch {
            withContext(context1) {
                log("Start sleep $it")
                Thread.sleep(200)
                log("Finished sleep $it")
            }
        }
    }

//    context1.close()
}