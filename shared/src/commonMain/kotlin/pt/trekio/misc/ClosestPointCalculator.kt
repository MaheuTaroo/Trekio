package pt.trekio.misc

fun List<GeoPoint>.indexOfClosestTo(
    center: GeoPoint,
    radius: Double,
) = mapIndexed { idx, point -> Triple(idx, point, HaversineDistance.between(point, center)) }
    .distinctBy { it.second }
    .filter { it.third <= radius }
    .minBy { it.third }
    .first
