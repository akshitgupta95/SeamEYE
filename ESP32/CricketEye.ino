#include "BluetoothSerial.h"
#include "MPU6050_tockn.h"
#include "Wire.h"

/* Uncomment for enable debugging and calibration mode */
#define DEBUG

#define DEVICE_NAME "VP_Voor_President"
#define ESP_BAUDRATE 115200
#define BTCLIENT_PAUSE 1000 // pause: 1s
#define LOOP_PAUSE 10 // pause: 10ms
#define SAMPLES 25


BluetoothSerial BT; // @suppress("Abstract class cannot be instantiated")
MPU6050 mpu(Wire);

String jsonData = String();
uint8_t samples = 0;
bool clientConnected = false;


// callback function for BT client connect/disconnect event
void btClientCallback(esp_spp_cb_event_t event, esp_spp_cb_param_t* param) {
	if (event == ESP_SPP_SRV_OPEN_EVT) {
		clientConnected = true;
		Serial.println("Client connected!");
		delay(BTCLIENT_PAUSE);
	}
	if (event == ESP_SPP_CLOSE_EVT) {
		clientConnected = false;
		Serial.println("Client disconnected!");
		delay(BTCLIENT_PAUSE);
	}
}


void setup() {
	Serial.begin(ESP_BAUDRATE);
	// Starting MCU
	Serial.print("\n=== Starting MCU module... ");
	Wire.begin();
	mpu.begin();
	mpu.setAccelSensitivity(3);
	mpu.setGyroSensitivity(3);
	mpu.calcGyroOffsets(true);
	mpu.update();
	Serial.println("Done! ===");

	// register callback for detecting client connection/disconnection
	BT.register_callback(btClientCallback);
	// Starting Bluetooth
	Serial.print("\n=== Starting Bluetooth module... ");
	if (! BT.begin(DEVICE_NAME)) {
		Serial.println("FAILED ===");
		while (true);
	}
	Serial.println("Done! ===");
	Serial.println("\n=== Waiting for Bluetooth client connection ===\n");
	delay(BTCLIENT_PAUSE);
}


void loop() {
	if (clientConnected) {
		mpu.update();
		// Collect data every 10ms for 25 samples
		jsonData += "{time:" + String(millis()) + ",";
		// accelerometer data
		jsonData += "ax:" + String(mpu.getAccX()) + ",ay:" + String(mpu.getAccY()) + ",az:" + String(mpu.getAccZ());
		// gyroscope data
		jsonData += ",gx:" + String(mpu.getGyroX()) + ",gy:" + String(mpu.getGyroY()) + ",gz:" + String(mpu.getGyroZ()) + "}";
		if (++samples < SAMPLES) jsonData += ",";
		else {
#ifdef DEBUG
			Serial.println("[" + jsonData + "]");
#endif
			BT.println("[" + jsonData + "]"); // Send data through BT
			jsonData.clear();
			samples = 0;
		}
	}
	delay(LOOP_PAUSE);
}

