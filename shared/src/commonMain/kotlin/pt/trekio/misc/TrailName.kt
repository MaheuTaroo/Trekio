package pt.trekio.misc

import kotlin.jvm.JvmInline

@JvmInline
value class TrailName(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Trail name must not be blank" }
        require(value.length >= 5) { "Trail name must be at least 5 characters long" }
    }
}
