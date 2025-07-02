import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import kotlinx.datetime.format.parse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object CamundaLocalDateTimeSerializer : KSerializer<LocalDateTime> {

    private val compoundFormat = DateTimeComponents.Format {
        date(LocalDate.Formats.ISO)
        char('T')
        hour(); char(':'); minute(); char(':'); second(); char('.'); secondFraction(3)
        offsetHours(); offsetMinutesOfHour()
    }

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("CustomLocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        val utcOffset = "+00:00"

        val formattedDate = "${value.date} ${value.time} $utcOffset"
        encoder.encodeString(formattedDate)
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val dateStr = decoder.decodeString()
        val parsedDate = DateTimeComponents.parse(dateStr, compoundFormat)
        return parsedDate.toLocalDateTime()
    }
}
