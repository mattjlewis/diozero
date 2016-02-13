#diozero
A Device I/O library that provides an object-orientated interface for a range of GPIO/I2C/SPI devices such as LEDs, buttons and other various sensors. Makes use of modern Java features such as automatic resource management and lambda expressions to simplify development and code readability. Actually GPIO/I2C/SPI device communication is delegated via a pluggable abstraction layer to provide maximum compatibility across devices.
Created by [Matt Lewis](https://github.com/mattjlewis), inspired by [GPIO Zero](https://gpiozero.readthedocs.org/en/v1.1.0/index.html).

##Components
TODO Describe component abstraction layer.
TODO Describe device factory concept to allow same API via expansion boards.

##Install
TODO Describe getting started steps - with and without Maven.

##Development
This project is hosted on [GitHub](https://github.com/mattjlewis/diozero/), please feel free to join in:
* Make suggestions for [fixes and enhancements](https://github.com/mattjlewis/diozero/issues)
* Provide sample applications
* Contribute to development

##Contents
* Digital Input Devices
 * Button
 * PIR Motion Sensor
 * Line Sensor
* Analogue Input Devices
 * Light Dependent Resistor
 * TMP36 Temperature Sensor
 * Potentiometer
 * Sharp GP2Y0A21YK Distance Sensor
* Output Devices
 * Digital LED
 * Buzzer
 * PWM Output
 * PWM LED
 * RGB LED
* Expansion Boards
 * MCP3008 Analogue-to-Digital Converter
 * MCP23017 GPIO Expansion Board
 * PCA9685 16-channel 12-bit PWM Controller (Adafruit PWM Servo Driver)
* Motor Control
 * CamJam EduKit #3 Motor Controller Board
 * Ryanteck RPi Motor Controller Board
 * Toshiba TB6612FNG Dual Motor Driver
* Other Components
 * HC-SR04 Ultrasonic Distance Sensor
 * BMP180 Temperature and Pressure Sensor
 * TSL2561 Luminosity Sensor
 * InvenSense MPU-9150 9-axis MotionTracking Device
* API
 * Analogue Input Device
 * Digital Input Device
 * Motors (Digital and PWM)
 * Digital Output Device
 * I2C Device Support
 * SPI Device Support
 * PWM Output Device
 * Smoothed Input Device
 * Waitable Input Device

##Providers
* JDK Device I/O
* Pi4j
* wiringPi
* pigpio
TODO Describe steps for creating a new provider.

##Change-log
* Release 0.2 (TBD)

##License
This work is provided under the [MIT License](license.md).
