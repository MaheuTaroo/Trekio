package pt.trekio

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
