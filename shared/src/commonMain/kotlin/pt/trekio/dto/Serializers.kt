package pt.trekio.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailType
import kotlin.reflect.typeOf

internal val stringDescriptor: SerialDescriptor = String.serializer().descriptor
internal val doubleSerializer: KSerializer<Double> = Double.serializer()
internal val doubleDescriptor = doubleSerializer.descriptor

@OptIn(ExperimentalSerializationApi::class)
internal val doubleArraySerializer = ArraySerializer<Double, Double>(doubleSerializer)
internal val doubleArrayDescriptor = doubleArraySerializer.descriptor

@OptIn(ExperimentalSerializationApi::class)
internal val doubleMatrixSerializer =
    ArraySerializer<Array<Double>, Array<Double>>(doubleArraySerializer)
internal val doubleMatrixDescriptor = doubleMatrixSerializer.descriptor

@OptIn(SealedSerializationApi::class)
internal val trailPointDescriptor =
    object : SerialDescriptor {
        override val serialName = "TrailPointDto"
        override val kind = StructureKind.CLASS
        override val elementsCount = 2

        override fun getElementName(index: Int) =
            when (index) {
                0 -> "type"
                1 -> "coordinates"
                else -> throw IndexOutOfBoundsException()
            }

        override fun getElementIndex(name: String) =
            when (name) {
                "type" -> 0
                "coordinates" -> 1
                else -> CompositeDecoder.UNKNOWN_NAME
            }

        override fun getElementAnnotations(index: Int) = emptyList<Annotation>()

        override fun getElementDescriptor(index: Int) =
            when (index) {
                0 -> stringDescriptor
                1 -> doubleArrayDescriptor
                else -> throw IndexOutOfBoundsException()
            }


        override fun isElementOptional(index: Int) = false
    }

@OptIn(SealedSerializationApi::class)
internal val trailPointListDescriptor =
    object : SerialDescriptor {
        override val serialName = "TrailPointListDto"
        override val kind = StructureKind.CLASS
        override val elementsCount = 2

        override fun getElementName(index: Int) =
            when (index) {
                0 -> "type"
                1 -> "coordinates"
                else -> throw IndexOutOfBoundsException()
            }

        override fun getElementIndex(name: String) =
            when (name) {
                "type" -> 0
                "coordinates" -> 1
                else -> CompositeDecoder.UNKNOWN_NAME
            }

        override fun getElementAnnotations(index: Int) = emptyList<Annotation>()

        override fun getElementDescriptor(index: Int) =
            when (index) {
                0 -> stringDescriptor
                1 -> doubleMatrixDescriptor
                else -> throw IndexOutOfBoundsException()
            }

        override fun isElementOptional(index: Int) = false
    }

internal val trailTypeDescriptor = serialDescriptor(typeOf<TrailType>())
internal val trailDifficultyDescriptor = serialDescriptor(typeOf<TrailDifficulty>())
internal val uLongSerializer = ULong.serializer()
internal val uLongDescriptor = uLongSerializer.descriptor



@OptIn(SealedSerializationApi::class)
internal val trailPointListSerializer =
    object : KSerializer<TrailPointListDto> {
        override val descriptor = trailPointListDescriptor

        override fun serialize(
            encoder: Encoder,
            value: TrailPointListDto
        ) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, "LineString")
                encodeSerializableElement(
                    descriptor,
                    1,
                    doubleMatrixSerializer,
                    value.points.map(TrailPointDto::toGeoJsonArray).toTypedArray()
                )
            }
        }

        override fun deserialize(decoder: Decoder): TrailPointListDto {
            val matrix = decoder.decodeStructure(descriptor) {
                decodeSerializableElement(
                    descriptor,
                    1,
                    doubleMatrixSerializer
                )
            }.map(Array<Double>::toTrailPoint)

            return TrailPointListDto(matrix.toList())
        }
    }

@OptIn(SealedSerializationApi::class)
internal val trailPointSerializer =
    object : KSerializer<TrailPointDto> {
        override val descriptor = trailPointDescriptor

        override fun serialize(encoder: Encoder, value: TrailPointDto) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, "Point")
                encodeSerializableElement(descriptor, 1, doubleArraySerializer, value.toGeoJsonArray())
            }
        }

        override fun deserialize(decoder: Decoder): TrailPointDto {
            val arr = decoder.decodeStructure(descriptor) {
                decodeSerializableElement(
                    descriptor,
                    1,
                    doubleArraySerializer
                )
            }

            return try {
                arr.toTrailPoint()
            }
            catch(_: Exception) {
                throw SerializationException("Could not deserialize GeoJson point to TrailPointDto")
            }
        }
    }