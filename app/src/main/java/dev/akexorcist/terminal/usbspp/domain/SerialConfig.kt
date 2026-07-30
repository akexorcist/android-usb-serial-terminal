package dev.akexorcist.terminal.usbspp.domain

import java.util.Locale

enum class BaudRate(val value: Int) {
  BAUD_9600(9600),
  BAUD_19200(19200),
  BAUD_38400(38400),
  BAUD_57600(57600),
  BAUD_115200(115200),
  BAUD_230400(230400);

  val label: String get() = String.format(Locale.US, "%,d", value)
}

enum class Parity {
  NONE,
  EVEN,
  ODD,
}

enum class DataFraming(val label: String, val dataBits: Int, val parity: Parity, val stopBits: Int) {
  EIGHT_N_1("8N1", 8, Parity.NONE, 1),
  SEVEN_E_1("7E1", 7, Parity.EVEN, 1),
  EIGHT_E_1("8E1", 8, Parity.EVEN, 1),
  EIGHT_N_2("8N2", 8, Parity.NONE, 2),
}

enum class LineEnding(val label: String, val value: String) {
  NONE("None", ""),
  LF("\\n", "\n"),
  CR("\\r", "\r"),
  CRLF("\\r\\n", "\r\n"),
}

data class SerialConfig(val baudRate: BaudRate = BaudRate.BAUD_9600, val framing: DataFraming = DataFraming.EIGHT_N_1)
