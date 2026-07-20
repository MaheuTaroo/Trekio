package pt.trekio.misc

import co.touchlab.kermit.Logger
import io.ktor.utils.io.CancellationException
import io.ktor.websocket.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield

class WebSocketCommunicator(
    val incoming: Flow<String>,
    private val outgoing: SendChannel<Frame>,
) {
    private val logger = Logger.withTag(this.toString())
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val mutex = Mutex()
    private var closed = false

    init {
        scope.launch {
            incoming.collect(logger::i)
        }
        // outgoing.invokeOnClose { _ -> closed = true }
    }

    private suspend fun trySend(action: suspend () -> Unit): Boolean {
        if (closed) return false
        try {
            action()
            return true
        } catch (_: ClosedSendChannelException) {
            mutex.withLock { closed = true }
            return false
        } catch (_: CancellationException) {
            mutex.withLock {
                @OptIn(DelicateCoroutinesApi::class)
                if (outgoing.isClosedForSend) {
                    closed = true
                }
            }
            return false
        } catch (t: Throwable) {
            logger.e("Could not close after frame: ${t.message ?: "unknown error"}", t)
            return false
        }
    }

    private suspend fun closeAfterFrame(text: String) =
        trySend {
            mutex.withLock { outgoing.send(Frame.Text(text)) }
            @OptIn(DelicateCoroutinesApi::class)
            while (mutex.withLock { !outgoing.isClosedForSend }) yield()
            scope.cancel()
            mutex.withLock { closed = true }
        }

    /**
     * Indicates whether the WebSocket tunnel for this
     * communicator is closed.
     * @return Whether the communicator is closed.
     */
    suspend fun isClosed(): Boolean {
        mutex.withLock {
            if (closed) {
                return true
            }

            @OptIn(DelicateCoroutinesApi::class)
            if (outgoing.isClosedForSend) {
                closed = true
                return true
            }

            return false
        }
    }

    /**
     * Sends the ``cancel`` command through the WebSocket tunnel
     * and wait for the server to acknowledge and close it.
     * @return Whether the command was sent while this communicator
     * was open (that is, the underlying tunnel was open).
     */
    suspend fun cancel() = closeAfterFrame("cancel")

    /**
     * Sends the ``finish`` command through the WebSocket tunnel
     * and wait for the server to acknowledge and close it.
     * @return Whether the command was sent while this communicator
     * was open (that is, the underlying tunnel was open).
     */
    suspend fun finish() = closeAfterFrame("finish")

    /**
     * Reports the user's provided location through the WebSocket
     * tunnel.
     * @param coordinates the coordinates to send.
     * @return Whether the location was sent while this communicator
     * was open (that is, the underlying tunnel was open).
     */
    suspend fun sendLocation(coordinates: GeoPoint) =
        trySend {
            outgoing.send(Frame.Text("$coordinates"))
        }
}
