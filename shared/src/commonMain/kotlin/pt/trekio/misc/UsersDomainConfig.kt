package pt.trekio.misc

import kotlin.time.Duration

data class UsersDomainConfig(
    val tokenSizeInBytes: Int,
    val tokenTtl: Duration,
    val maxTokensPerUser: Int,
    val registrationTtl: Duration,
    val possibleDailyRewards: List<Pair<Int, Double>>,
) {
    init {
        require(tokenSizeInBytes > 0)
        require(tokenTtl.isPositive())
        require(maxTokensPerUser > 0)
        require(registrationTtl.isPositive())
        require(possibleDailyRewards.size > 1 && possibleDailyRewards.sumOf(Pair<Int, Double>::second) == 1.0)
    }
}