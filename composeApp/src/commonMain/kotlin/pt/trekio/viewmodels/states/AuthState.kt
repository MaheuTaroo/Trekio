package pt.trekio.viewmodels.states

interface AuthState {
    data object Idle : AuthState

    data object Loading : AuthState

    data object Success : AuthState

    data class OAuthError(
        val message: String,
    ) : AuthState

    data class Error(
        val message: String,
    ) : AuthState
}
