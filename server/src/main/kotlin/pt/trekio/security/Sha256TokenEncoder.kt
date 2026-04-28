package pt.trekio.security

import java.security.MessageDigest
import java.util.Base64

object Sha256TokenEncoder {
    /**
     * Creates validation information for the given token.
     *
     * @param token the raw token to be transformed
     * @return a [String] containing the validation representation of the token
     */
    fun createValidationInformation(token: String) = hash(token)

    private fun hash(input: String): String {
        val messageDigest = MessageDigest.getInstance("SHA256")
        return Base64.getUrlEncoder().encodeToString(
            messageDigest.digest(
                Charsets.UTF_8.encode(input).array(),
            ),
        )
    }
}
