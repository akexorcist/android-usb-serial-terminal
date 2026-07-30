package dev.akexorcist.terminal.usbspp.domain

enum class LineDirection {
  SENT,
  RECEIVED,
}

data class SerialLine(
  val id: Long,
  val text: String,
  val bytes: List<Byte>,
  val direction: LineDirection,
  val timestampMillis: Long,
)
