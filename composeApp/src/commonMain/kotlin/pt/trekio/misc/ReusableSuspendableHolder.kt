package pt.trekio.misc

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield

class ReusableSuspendableHolder<T> {
    val mutex = Mutex()
    var value: T? = null

    suspend fun set(value: T) {
        mutex.withLock {
            if (value != null)
                return

            this.value = value
        }
    }

    suspend fun get(): T = mutex.withLock {
        if (value == null) yield()
        val tmp = value!!
        value = null
        return tmp
    }
}