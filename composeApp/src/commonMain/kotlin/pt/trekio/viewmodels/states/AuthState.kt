package pt.trekio.viewmodels.states

interface AuthState {
    data object Idle : AuthState

    data object Loading : AuthState

    data object Success : AuthState

    data class OAuthError(
        val message: String,
    ) : AuthState

    data class LoginError(
        val message: String,
    ) : AuthState

    data class SignUpError(
        val message: String,
    ) : AuthState
}
