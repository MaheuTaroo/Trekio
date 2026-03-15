package pt.trekio.misc

import kotlin.io.encoding.Base64

/*class Sha256TokenEncoder : TokenEncoder {
    override fun createValidationInformation(token: String) = hash(token)

    private fun hash(input: String): String {
        val messageDigest = MessageDigest.getInstance("SHA256")
        return Base64.getUrlEncoder().encodeToString(
            messageDigest.digest(
                Charsets.UTF_8.encode(input).array(),
            ),
        )
    }
}*/