---
title: Devices
nav_order: 4
permalink: /devices.html
has_children: true
---

# Devices

This library provides support for a number of GPIO / I2C / SPI connected components and devices:

* [Expansion Boards](ExpansionBoards.md) for adding additional GPIO / Analog / PWM pins
    * [Microchip Analog to Digital Converters](ExpansionBoards.md#mcp-adc), [NXP PCF8591 ADC / DAC](ExpansionBoards.md#pcf8591), [Microchip GPIO Expansion Boards](ExpansionBoards.md#mcp-gpio-expansion-board), [PWM / Servo Driver](ExpansionBoards.md#pwm-servo-driver), [PCF8574](ExpansionBoards.md#pcf8574)
* [Motor Control](MotorControl.md) (support for common motor controller boards)
    * [API](MotorControl.md#api), [Servos](MotorControl.md#servo), [CamJam EduKit](MotorControl.md#camjamkitdualmotor), [Ryanteck](MotorControl.md#ryanteckdualmotor), [Toshiba TB6612FNG](MotorControl.md#tb6612fngdualmotordriver), [PiConZero](MotorControl.md#piconzero)
* [Sensor Components](SensorComponents.md) (support for specific sensors, e.g. temperature, pressure, distance, luminosity)
    * [HC-SR04 Ultrasonic Ranging Module](SensorComponents.md#hc-sr04), [Bosch BMP180](SensorComponents.md#bosch-bmp180), [Bosch BME280](SensorComponents.md#bosch-bme280), [TSL2561 Light Sensor](SensorComponents.md#tsl2561), [STMicroelectronics HTS221 Humidity and Temperature Sensor](SensorComponents.md#hts221), [STMicroelectronics LPS25H Pressure and Temperature Sensor](SensorComponents.md#lps25h), [1-Wire Temperature Sensors e.g. DS18B20](SensorComponents.md#1-wire-temperature-sensors), [Sharp GP2Y0A21YK distance sensor](SensorComponents.md#gp2y0a21yk), [Mifare MFRC522 RFID Reader](SensorComponents.md#mfrc522)
* [LCD Displays](LCDDisplays.md)
    * [HD44780 controlled LCDs](LCDDisplays.md#hd44780-lcds)
* [IMU Devices](IMUDevices.md) Work-in-progress API for interacting with Inertial Measurement Units such as the InvenSense MPU-9150 and the Analog Devices ADXL345
    * [API](IMUDevices.md#api), [Supported Devices](IMUDevices.md#supported-devices)
* [LED Strips](LEDStrips.md) Support for LED strips (WS2811B / WS2812B / Adafruit NeoPixel)
    * [WS2811B / WS2812B](LEDStrips.md#ws281x)
