#diozero
A Device I/O library that provides an object-orientated interface for a range of GPIO/I2C/SPI devices such as LEDs, buttons and other various sensors. Makes use of modern Java features such as automatic resource management and lambda expressions to simplify development and code readability. Actually GPIO/I2C/SPI device communication is delegated via a pluggable abstraction layer to provide maximum compatibility across devices.
Created by [Matt Lewis](https://github.com/mattjlewis), inspired by [GPIO Zero](https://gpiozero.readthedocs.org/en/v1.1.0/index.html).

##Components
TODO Describe component abstraction layer.
Example LED application:
```java
try (LED led = new LED(pin)) {
	led.on();
	SleepUtil.sleepSeconds(.5);
	led.off();
	SleepUtil.sleepSeconds(.5);
	led.toggle();
	SleepUtil.sleepSeconds(.5);
	led.toggle();
	SleepUtil.sleepSeconds(.5);
	led.blink(0.5f, 0.5f, 10, false);
}
```
Turn on an LED when you press a button:
```java
try (Button b = new Button(buttonPin, GpioPullUpDown.PULL_UP); LED led = new LED(ledPin)) {
	b.whenPressed(led::on);
	b.whenReleased(led::off);
	SleepUtil.sleepSeconds(10);
}
```
Likewise to control the brightness of an LED based on readings from an LDR:
```java
try (MCP3008 mcp3008 = new MCP3008(chipSelect); LDR ldr = new LDR(mcp3008, pin, vRef, r1); PwmLed led = new PwmLed(ledPin)) {
	// Detect variations of 5%
	ldr.addListener(.05f, (event) -> led.setValue(1-event.getValue()/mcp3008.getVoltageRange()));
	SleepUtil.sleepSeconds(20);
}
```
Or a random LED flicker effect:
```java
try (PwmLed led = new PwmLed(pin)) {
	GpioScheduler.getInstance().invokeAtFixedRate(RANDOM::nextFloat, led::setValue, 50, 50, TimeUnit.MILLISECONDS, false);
}
```

TODO Describe device factory concept to allow same API via expansion boards.

##Install
TODO Describe getting started steps - with and without Maven.

##Development
This project is hosted on [GitHub](https://github.com/mattjlewis/diozero/), please feel free to join in:
+ Make suggestions for [fixes and enhancements](https://github.com/mattjlewis/diozero/issues)
+ Provide sample applications
+ Contribute to development

##Contents
+ [Digital Input Devices](DigitalInputDevices.md)
    - Button
    - PIR Motion Sensor
    - Line Sensor
+ Analogue Input Devices
    - Light Dependent Resistor
    - TMP36 Temperature Sensor
    - Potentiometer
    - Sharp GP2Y0A21YK Distance Sensor
+ Output Devices
    - Digital LED
    - Buzzer
    - PWM Output
    - PWM LED
    - RGB LED
+ Expansion Boards
    - MCP3008 Analogue-to-Digital Converter
    - MCP23017 GPIO Expansion Board
    - PCA9685 16-channel 12-bit PWM Controller (Adafruit PWM Servo Driver)
+ Motor Control
    - CamJam EduKit #3 Motor Controller Board
    - Ryanteck RPi Motor Controller Board
    - Toshiba TB6612FNG Dual Motor Driver
+ Other Components
    - HC-SR04 Ultrasonic Distance Sensor
    - BMP180 Temperature and Pressure Sensor
    - TSL2561 Luminosity Sensor
    - InvenSense MPU-9150 9-axis MotionTracking Device
+ API
    - Analogue Input Device
    - Digital Input Device
    - Motors (Digital and PWM)
    - Digital Output Device
    - I2C Device Support
    - SPI Device Support
    - PWM Output Device
    - Smoothed Input Device
    - Waitable Input Device

##Providers
+ JDK Device I/O
+ Pi4j
+ wiringPi
+ pigpio
TODO Describe steps for creating a new provider.

##Performance
I've done some limited performance tests (turning a GPIO on then off, see [GpioPerfTest](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/GpioPerfTest.java)) on a Raspberry Pi 2 using the various native device factory providers as well as a test using Pi4j's wiringPi JNI API directly without going via my DIO-Zero wrapper (see [WiringPiRawPerfTest](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-wiringpi/src/main/java/com/diozero/internal/provider/wiringpi/WiringPiRawPerfTest.java)); here are the results:

| Provider | Iterations | Frequency (kHz) |
| -------- | ---------- | --------------- |
| Pi4j 1.0 | 10,000 | 0.91 |
| JDK DIO 1.1 | 100,000 | 8.23 |
| Pi4j 1.1 | 10,000,000 | 622 |
| wiringPi | 5,000,000 | 1,683 |
| wiringPi (direct) | 10,000,000 | 2,137 |
| pigpio | 5,000,000 | 1,266 |
| pigpio (direct) | 10,000,000 | 1,649 |

For a discussion on why Pi4j 1.0 is so slow, see this [issue](https://github.com/Pi4J/pi4j/issues/158). These results are in-line with those documented in the book ["Raspberry Pi with Java: Programming the Internet of Things"](http://www.amazon.co.uk/Raspberry-Pi-Java-Programming-Internet/dp/0071842012). For reference, the author's results were:

| Library | Frequency (kHz) |
| ------- | --------------- |
| Pi4j 1.0 | 0.751 |
| JDK DIO 1.0 | 3.048 |
| wiringPi (direct) | 1,662 |

##Change-log
+ Release 0.2 (TBD)

##License
This work is provided under the [MIT License](license.md).
