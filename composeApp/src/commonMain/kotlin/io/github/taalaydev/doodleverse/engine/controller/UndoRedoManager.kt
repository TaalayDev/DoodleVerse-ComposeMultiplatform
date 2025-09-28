package io.github.taalaydev.doodleverse.engine.controller

class UndoRedoManager<T>(
    private val maxHistorySize: Int = 20
) {
    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun saveState(state: T) {
        undoStack.addLast(state)
        redoStack.clear()

        // Limit stack size
        while (undoStack.size > maxHistorySize) {
            undoStack.removeFirst()
        }
    }

    fun undo(currentState: T): T? {
        if (!canUndo) return null

        redoStack.addLast(currentState)
        return undoStack.removeLast()
    }

    fun redo(currentState: T): T? {
        if (!canRedo) return null

        undoStack.addLast(currentState)
        return redoStack.removeLast()
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
