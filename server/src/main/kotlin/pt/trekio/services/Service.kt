package pt.trekio.services

import pt.trekio.errors.DomainError
import pt.trekio.misc.Either
import pt.trekio.misc.failure
import pt.trekio.misc.success

abstract class Service {
    protected fun <S> paginated(
        skip: Int,
        limit: Int,
        operation: (Int, Int) -> S,
    ): Either<DomainError, S> {
        if (skip < 0) return failure(DomainError.NegativeSkip)
        if (limit < 1) return failure(DomainError.NonPositiveLimit)

        return success(operation(skip, limit))
    }
}
