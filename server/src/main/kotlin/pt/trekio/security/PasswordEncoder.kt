package pt.trekio.security
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

object PasswordEncoder {
    private val passwordEncoder = BCryptPasswordEncoder()

    fun encode(password: String): String = passwordEncoder.encode(password)!!

    fun matches(
        rawPassword: String,
        encodedPassword: String,
    ) = passwordEncoder.matches(rawPassword, encodedPassword)
}
