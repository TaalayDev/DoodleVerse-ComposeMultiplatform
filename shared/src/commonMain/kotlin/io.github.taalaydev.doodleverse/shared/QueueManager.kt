package io.github.taalaydev.doodleverse.shared

object QueueManager {
    private val queue = mutableListOf<suspend () -> Unit>()
    private var isRunning = false

    suspend fun add(task: suspend () -> Unit) {
        queue.add(task)
        if (!isRunning) {
            isRunning = true
            run()
        }
    }

    private suspend fun run() {
        if (queue.isEmpty()) {
            isRunning = false
            return
        }

        val task = queue.removeAt(0)
        task()
        run()
    }
}