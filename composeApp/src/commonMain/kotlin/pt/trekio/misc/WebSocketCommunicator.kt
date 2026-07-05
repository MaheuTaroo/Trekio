package pt.trekio.misc

import co.touchlab.kermit.Logger
import io.ktor.websocket.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class WebSocketCommunicator(
    val incoming: Flow<String>,
    private val outgoing: SendChannel<Frame>,
) {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val logger = Logger.withTag(this.toString())

    init {
        scope.launch {
            incoming.collect(logger::i)
        }
        scope.launch { outgoing.send(Frame.Text("(0.0;0.0;0.0)")) }
    }

    suspend fun cancel() {
        outgoing.send(Frame.Text("cancel"))
        scope.cancel()
    }
}
