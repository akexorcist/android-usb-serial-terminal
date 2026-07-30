const int LED_PIN = LED_BUILTIN;
const unsigned long HEARTBEAT_INTERVAL_MS = 2000;

String inputLine = "";
unsigned long lastHeartbeatMillis = 0;
unsigned long heartbeatCount = 0;

void setup() {
  pinMode(LED_PIN, OUTPUT);
  Serial.begin(9600);
  while (!Serial) {
  }
  Serial.println("Ready");
}

void loop() {
  while (Serial.available() > 0) {
    char incomingChar = (char)Serial.read();
    if (incomingChar == '\n') {
      handleLine(inputLine);
      inputLine = "";
    } else if (incomingChar != '\r') {
      inputLine += incomingChar;
    }
  }

  unsigned long now = millis();
  if (now - lastHeartbeatMillis >= HEARTBEAT_INTERVAL_MS) {
    lastHeartbeatMillis = now;
    heartbeatCount++;
    Serial.print("Heartbeat #");
    Serial.println(heartbeatCount);
  }
}

void handleLine(String line) {
  line.trim();
  if (line.length() == 0) {
    return;
  }

  if (line.equalsIgnoreCase("LED_ON")) {
    digitalWrite(LED_PIN, HIGH);
    Serial.println("LED ON");
  } else if (line.equalsIgnoreCase("LED_OFF")) {
    digitalWrite(LED_PIN, LOW);
    Serial.println("LED OFF");
  } else {
    Serial.print("Echo: ");
    Serial.println(line);
  }
}
