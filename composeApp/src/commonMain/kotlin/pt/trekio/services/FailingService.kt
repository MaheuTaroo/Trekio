package pt.trekio.services

import pt.trekio.dto.TokenExternalInfoDto
import pt.trekio.misc.Either
import pt.trekio.misc.GeoPoint
import pt.trekio.misc.Language
import pt.trekio.misc.Metric
import pt.trekio.misc.UserAndToken
import pt.trekio.misc.UserDetailsAndToken
import pt.trekio.misc.failure
import pt.trekio.repos.SettingsRepo
import pt.trekio.repos.UserRepository
import pt.trekio.services.hikes.HikeService
import pt.trekio.services.trails.TrailService
import pt.trekio.services.user.UserService
import pt.trekio.ui.theme.ThemeMode

object FailingService : UserService, TrailService, HikeService, SettingsRepo, UserRepository {
    private const val ERROR = "You are not logged in"

    override suspend fun signUp(
        username: String,
        email: String,
        password: String,
    ) = failure(ERROR)

    override suspend fun login(
        email: String,
        password: String,
    ) = failure(ERROR)

    override suspend fun logout() = failure(ERROR)

    override suspend fun getSelfDetails() = failure(ERROR)

    override suspend fun getStatsOf(id: ULong) = failure(ERROR)

    override suspend fun updateDetails(
        username: String?,
        password: String?,
    ): Either<String, TokenExternalInfoDto> = failure(ERROR)

    override suspend fun deleteUser() = failure(ERROR)

    override suspend fun googlePopup() = failure(ERROR)

    override suspend fun googleCallback() { }

    override suspend fun createTrail(
        name: String,
        start: GeoPoint,
        end: GeoPoint,
        path: List<GeoPoint>,
        parentId: ULong?,
    ) = failure(ERROR)

    override suspend fun importTrail() = failure(ERROR)

    override suspend fun getTrailDetails(id: ULong) = failure(ERROR)

    override suspend fun startHike(trailId: ULong) = failure(ERROR)

    override suspend fun getHikeDetails(id: ULong) = failure(ERROR)

    override suspend fun finishHike(id: ULong) = failure(ERROR)

    override suspend fun cancelHike(id: ULong) = failure(ERROR)

    override suspend fun getTrailsOf(
        userId: ULong,
        page: ULong,
    ) = failure(ERROR)

    override suspend fun getAllTrails(page: ULong) = failure(ERROR)

    override suspend fun updateTrail(
        id: ULong,
        name: String,
        parentId: ULong?,
    ) = failure(ERROR)

    override suspend fun deleteTrail(id: ULong) = failure(ERROR)

    override fun getTheme(): ThemeMode = ThemeMode.LIGHT

    override fun setTheme(theme: ThemeMode) {}

    override fun getLanguage(): Language = Language.English

    override fun setLanguage(language: Language) {}

    override fun getMetric(): Metric = Metric.Kilometers

    override fun setMetric(metric: Metric) {}

    override suspend fun saveToken(
        accessToken: String,
        refreshToken: String,
        expiration: Long,
        email: String?,
    ) {}

    override suspend fun getTokens(): UserAndToken? = null

    override suspend fun saveOwnDetails(
        id: ULong?,
        username: String,
        rank: String?,
    ) {}

    override suspend fun getOwnDetails(): UserDetailsAndToken? = null

    override suspend fun clear() {}
}
