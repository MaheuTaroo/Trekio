package pt.trekio.misc

sealed class Either<out F, out S> {
    data class Success<out S>(
        val value: S,
    ) : Either<Nothing, S>()

    data class Failure<out F>(
        val message: F,
    ) : Either<F, Nothing>()
}

// inline fun <E, T> Either<E, T>.getOrElse(onFailure: (E) -> Nothing): T =
//    when (this) {
//        is Success -> this.value
//        is Failure -> onFailure(this.value)
//    }

typealias Success<S> = Either.Success<S>
typealias Failure<F> = Either.Failure<F>

fun <S> success(value: S) = Success(value)

fun <F> failure(error: F) = Failure(error)
