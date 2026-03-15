package pt.trekio.misc

interface TokenEncoder {
    /**
     * Creates validation information for the given token.
     * The exact encoding or hashing strategy depends on the implementation.
     *
     * @param token the raw token to be transformed
     * @return a {@link TokenValidationInfo} containing the validation representation of the token
     */
    fun createValidationInformation(token: String): String
}