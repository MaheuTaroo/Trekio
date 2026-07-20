package pt.trekio.misc

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object OAuthDeepLinkBus {
    private val _events = MutableSharedFlow<OAuthDeepLinkEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun emit(event: OAuthDeepLinkEvent) = _events.tryEmit(event)
}

sealed interface OAuthDeepLinkEvent {
    data class Success(
        val username: String,
    ) : OAuthDeepLinkEvent

    data class Error(
        val message: String,
    ) : OAuthDeepLinkEvent
}
