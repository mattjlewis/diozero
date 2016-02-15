# diozero
A Device I/O library that provides an object-orientated interface for a range of GPIO / I2C / SPI devices such as LEDs, buttons and other various sensors. Actual GPIO / I2C / SPI device communication is delegated via a pluggable abstraction layer to provide maximum compatibility across devices.

This library makes use of modern Java 8 features such as automatic resource management, [Lambda Expressions](https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html) and [Method References](https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html) where they simplify development and improve code readability.

Created by [Matt Lewis](https://github.com/mattjlewis), inspired by [GPIO Zero](https://gpiozero.readthedocs.org/en/v1.1.0/index.html).

## Concepts
The aim of this library is to encapsulated real-world devices as classes with meaningful operation names, for example LED (on / off), LDR (analogue readings), Button (pressed / released), Motor (forward / backwards / left / right). All devices implement Closeable hence will get automatically closed by the `try (Device d = new Device()) { d.doSomething() }` block. This is best illustrated by some simple examples.

LED control:
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
try (Button button = new Button(buttonPin, GpioPullUpDown.PULL_UP); LED led = new LED(ledPin)) {
	button.whenPressed(led::on);
	button.whenReleased(led::off);
	SleepUtil.sleepSeconds(10);
}
```

Or a random LED flicker effect:
```java
try (PwmLed led = new PwmLed(pin)) {
	GpioScheduler.getInstance().invokeAtFixedRate(RANDOM::nextFloat, led::setValue, 50, 50, TimeUnit.MILLISECONDS, false);
}
```

All devices are provisioned by a [Device Factory](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/internal/spi/DeviceFactoryInterface.java). There is a default NativeDeviceFactory for provisioning via the host board itself. However, all components accept an optional Device Factory parameter for provisioning the same set of components via an alternative method. This is particularly useful for GPIO expansion boards and Analogue-to-Digital converters.

The Raspberry Pi provides no analogue input pins; attempting to create an AnalogueInputDevice such as an LDR using the Raspberry Pi default native device factory would result in a runtime error (UnsupportedOperationException). However, ADC classes such as the [MCP3008](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/MCP3008.java) are also analogue input device factories and can be used to construct devices such as LDRs:
```java
try (MCP3008 mcp3008 = new MCP3008(chipSelect); LDR ldr = new LDR(mcp3008, pin, vRef, r1)) {
	System.out.println(ldr.getUnscaledValue());
}
```

Repeating the previous example of controlling an LED when you press a button but with components connected via an [MCP23017](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/MCP23017.java) GPIO expansion board:
```java
try (MCP23017 mcp23017 = new MCP23017(intAPin, intBPin);
		Button button = new Button(mcp23017, inputPin, GpioPullUpDown.PULL_UP);
		LED led = new LED(mcp23017, outputPin, false, true)) {
	button.whenPressed(led::on);
	button.whenReleased(led::off);
	SleepUtil.sleepSeconds(10);
}
```

Analogue input devices also provide an event notification mechanism; to control the brightness of an LED based on readings from an LDR:
```java
try (MCP3008 mcp3008 = new MCP3008(chipSelect); LDR ldr = new LDR(mcp3008, pin, vRef, r1); PwmLed led = new PwmLed(ledPin)) {
	// Detect variations of 5%
	ldr.addListener(.05f, (event) -> led.setValue(1-event.getScaledValue()));
	SleepUtil.sleepSeconds(20);
}
```

## Install
TODO Describe getting started steps.
Snapshot builds of the library are available in the [Nexus Repository Manager](https://oss.sonatype.org/index.html#nexus-search;gav~com.diozero~~~~).
This library uses [tinylog](www.tinylog.org) [v1.0](https://github.com/pmwmedia/tinylog/releases/download/1.0.3/tinylog-1.0.3.zip).

## Devices
+ [Digital Input Devices](DigitalInputDevices.md)
    - Button
    - PIR Motion Sensor
    - Line Sensor
+ [Analogue Input Devices](AnalogueInputDevices.md)
    - Light Dependent Resistor
    - TMP36 Temperature Sensor
    - Potentiometer
    - Sharp GP2Y0A21YK Distance Sensor
+ [Output Devices](OutputDevices.md)
    - Digital LED
    - Buzzer
    - PWM Output
    - PWM LED
    - RGB LED
+ [Expansion Boards](ExpansionBoards.md)
    - MCP3008 Analogue-to-Digital Converter
    - MCP23017 GPIO Expansion Board
    - PCA9685 16-channel 12-bit PWM Controller (Adafruit PWM Servo Driver)
+ [Motor Control](MotorControl.md)
    - CamJam EduKit #3 Motor Controller Board
    - Ryanteck RPi Motor Controller Board
    - Toshiba TB6612FNG Dual Motor Driver
+ [Other Components](OtherComponents.md)
    - HC-SR04 Ultrasonic Distance Sensor
    - BMP180 Temperature and Pressure Sensor
    - TSL2561 Luminosity Sensor
    - InvenSense MPU-9150 9-axis MotionTracking Device
+ [API](API.md)
    - Analogue Input Device
    - Digital Input Device
    - Motors (Digital and PWM)
    - Digital Output Device
    - I2C Device Support
    - SPI Device Support
    - PWM Output Device
    - Smoothed Input Device
    - Waitable Input Device

## Performance
I've done some limited performance tests (turning a GPIO on then off, see [GpioPerfTest](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/GpioPerfTest.java)) on a Raspberry Pi 2 using the various native device factory providers as well as a test using Pi4j's wiringPi JNI API directly without going via my DIO-Zero wrapper (see [WiringPiRawPerfTest](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-wiringpi/src/main/java/com/diozero/internal/provider/wiringpi/WiringPiRawPerfTest.java)); here are the results:

| Provider | Iterations | Frequency (kHz) |
| -------- | ---------- | --------------- |
| Pi4j 1.0 | 10,000: | 0.91: |
| JDK DIO 1.1 | 100,000: | 8.23: |
| Pi4j 1.1 | 10,000,000: | 622: |
| wiringPi | 5,000,000: | 1,683: |
| wiringPi (direct) | 10,000,000: | 2,137: |
| pigpio | 5,000,000 | 1,266: |
| pigpio (direct) | 10,000,000: | 1,649: |

For a discussion on why Pi4j 1.0 was so slow, see this [issue](https://github.com/Pi4J/pi4j/issues/158). These results are in-line with those documented in the book ["Raspberry Pi with Java: Programming the Internet of Things"](http://www.amazon.co.uk/Raspberry-Pi-Java-Programming-Internet/dp/0071842012). For reference, the author's results were:

| Library | Frequency (kHz) |
| ------- | --------------- |
| Pi4j 1.0 | 0.751: |
| JDK DIO 1.0 | 3.048: |
| wiringPi (direct) | 1,662: |

## Development
This project is hosted on [GitHub](https://github.com/mattjlewis/diozero/), please feel free to join in:
+ Make suggestions for [fixes and enhancements](https://github.com/mattjlewis/diozero/issues)
+ Provide sample applications
+ Contribute to development
TODO Add something about Maven dependencies, setting up development environments.

## To-Do
There is still a lot left to do, in particular:
* Thorough testing (various types of devices using each service provider)
* Testing on different devices (all flavours of Raspberry Pi, BeagleBone, ...)
* GPIO input debouncing
* Other I2C & SPI devices, including those on the SenseHAT
* A clean object-orientated API for IMUs

## Change-log
+ Release 0.2 (TBD)

## License
This work is provided under the [MIT License](license.md).
