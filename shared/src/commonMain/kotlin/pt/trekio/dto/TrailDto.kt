package pt.trekio.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonEncoder
import pt.trekio.misc.TrailDifficulty
import pt.trekio.misc.TrailType
import kotlin.collections.emptyList
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport.Ignore

data class TrailDto(
    val name: String,
    val start: TrailPointDto,
    val end: TrailPointDto,
    val path: TrailPointListDto, //List<TrailPointDto>,
    val distance: Double,
    val type: TrailType,
    val difficulty: TrailDifficulty,
    val parent: ULong?,
) {
    companion object {
        @OptIn(SealedSerializationApi::class, ExperimentalSerializationApi::class)
        private val serializer =
            object: KSerializer<TrailDto> {
                override val descriptor =
                    object : SerialDescriptor {
                        override val serialName = "TrailDto"
                        override val kind = StructureKind.CLASS
                        override val elementsCount = 8

                        override fun getElementName(index: Int): String =
                            when (index) {
                                0 -> "name"
                                1 -> "start"
                                2 -> "end"
                                3 -> "path"
                                4 -> "distance"
                                5 -> "type"
                                6 -> "difficulty"
                                7 -> "parent"
                                else -> throw IndexOutOfBoundsException()
                            }

                        override fun getElementIndex(name: String) =
                            when (name) {
                                "name" -> 0
                                "start" -> 1
                                "end" -> 2
                                "path" -> 3
                                "distance" -> 4
                                "type" -> 5
                                "difficulty" -> 6
                                "parent" -> 7
                                else -> CompositeDecoder.UNKNOWN_NAME
                            }

                        override fun getElementAnnotations(index: Int) = emptyList<Annotation>()

                        override fun getElementDescriptor(index: Int) =
                            when (index) {
                                0 -> stringDescriptor
                                1, 2 -> trailPointDescriptor
                                3 -> trailPointListDescriptor
                                4 -> doubleDescriptor
                                5 -> trailTypeDescriptor
                                6 -> trailDifficultyDescriptor
                                7 -> uLongDescriptor
                                else -> throw IndexOutOfBoundsException()
                            }

                        override fun isElementOptional(index: Int) = index == 7
                    }

                override fun serialize(encoder: Encoder, value: TrailDto) {
                    encoder.encodeStructure(descriptor) {
                        encodeStringElement(descriptor, 0, value.name)
                        encodeSerializableElement(descriptor, 1, trailPointSerializer, value.start)
                        encodeSerializableElement(descriptor, 2, trailPointSerializer, value.end)
                        encodeSerializableElement(descriptor, 3, trailPointListSerializer, value.path)
                        encodeDoubleElement(descriptor, 4, value.distance)
                        encodeStringElement(descriptor, 5, value.type.name)
                        encodeStringElement(descriptor, 6, value.difficulty.name)
                        encodeNullableSerializableElement(descriptor, 7, uLongSerializer, value.parent)
                    }
                }

                override fun deserialize(decoder: Decoder): TrailDto =
                    try {
                        decoder.decodeStructure(descriptor) {
                            val name = decodeStringElement(descriptor, 0)
                            val start = decodeSerializableElement(
                                trailPointDescriptor,
                                1,
                                trailPointSerializer
                            )
                            val end = decodeSerializableElement(
                                descriptor,
                                2,
                                trailPointSerializer
                            )
                            val path = decodeSerializableElement(
                                descriptor,
                                3,
                                trailPointListSerializer
                            )
                            val distance = decodeDoubleElement(descriptor, 4)
                            val type = TrailType.valueOf(decodeStringElement(descriptor, 5))
                            val difficulty = TrailDifficulty.valueOf(decodeStringElement(descriptor, 6))
                            val parent =
                                try {
                                    decodeNullableSerializableElement(
                                        descriptor,
                                        7,
                                        uLongSerializer
                                    )
                                }
                                catch (_: Exception) {
                                    null
                                }

                            TrailDto(name, start, end, path, distance, type, difficulty, parent)
                        }
                    }
                    catch (_: Exception) {
                        throw SerializationException(
                            "Could not deserialize trail due to type or difficulty malformation"
                        )
                    }
            }

        @OptIn(ExperimentalJsExport::class)
        @Ignore
        fun serializer() = serializer
    }
}
