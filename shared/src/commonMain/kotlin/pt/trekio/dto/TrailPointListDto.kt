package pt.trekio.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport.Ignore

data class TrailPointListDto(val points: List<TrailPointDto>) {
    companion object {
        @OptIn(ExperimentalJsExport::class)
        @Ignore
        fun serializer() = trailPointListSerializer
    }
}