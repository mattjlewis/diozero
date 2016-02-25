# diozero

A Device I/O library written in Java that provides an object-orientated interface for a range of GPIO / I2C / SPI devices such as LEDs, buttons and other various sensors connected to intelligent devices like the Raspberry Pi. Actual GPIO / I2C / SPI device communication is delegated via a pluggable abstraction layer to provide maximum compatibility across devices.

This library makes use of modern Java 8 features such as [automatic resource management](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html), [Lambda Expressions](https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html) and [Method References](https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html) where they simplify development and improve code readability.

Created by [Matt Lewis](https://github.com/mattjlewis), inspired by [GPIO Zero](https://gpiozero.readthedocs.org/en/v1.1.0/index.html).

## Concepts

The aim of this library is to encapsulated real-world devices as classes with meaningful operation names, for example LED (on / off), LDR (analog readings), Button (pressed / released), Motor (forward / backwards / left / right). All devices implement `Closeable` hence will get automatically closed by the `try (Device d = new Device()) { d.doSomething() }` statement. This is best illustrated by some simple examples.

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

All devices are provisioned by a [Device Factory](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/internal/spi/DeviceFactoryInterface.java) with a default NativeDeviceFactory for provisioning via the host board itself. However, all components accept an optional Device Factory parameter for provisioning the same set of components via an alternative method. This is particularly useful for GPIO expansion boards and Analog-to-Digital converters.

The Raspberry Pi provides no analog input pins; attempting to create an AnalogInputDevice such as an LDR using the Raspberry Pi default native device factory would result in a runtime error (`UnsupportedOperationException`). However, ADC classes such as the [McpAdc](ExpansionBoards.md#mcp-adc) have been implemented as analog input device factories hence can be used to construct analog devices such as LDRs:

```java
try (McpAdc adc = new McpAdc(McpAdc.Type.MCP3008, chipSelect); LDR ldr = new LDR(adc, pin, vRef, r1)) {
	System.out.println(ldr.getUnscaledValue());
}
```

Repeating the previous example of controlling an LED when you press a button but with components connected via an [MCP23017](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/MCP23017.java) GPIO expansion board:

```java
try (MCP23017 mcp23017 = new MCP23017(intAPin, intBPin);
		Button button = new Button(mcp23017, inputPin, GpioPullUpDown.PULL_UP);
		LED led = new LED(mcp23017, outputPin)) {
	button.whenPressed(led::on);
	button.whenReleased(led::off);
	SleepUtil.sleepSeconds(10);
}
```

Analog input devices also provide an event notification mechanism. To control the brightness of an LED based on ambient light levels:

```java
try (McpAdc adc = new McpAdc(McpAdc.Type.MCP3008, chipSelect); LDR ldr = new LDR(adc, pin, vRef, r1); PwmLed led = new PwmLed(ledPin)) {
	// Detect variations of 10%, get values every 50ms (the default)
	ldr.addListener((event) -> led.setValue(1-event.getScaledValue()), .1f);
	SleepUtil.sleepSeconds(20);
}
```

## Getting Started

Snapshot builds of the library are available in the [Nexus Repository Manager](https://oss.sonatype.org/index.html#nexus-search;gav~com.diozero~~~~). For convenience a ZIP of all diozero JARs will be maintained on [Google Drive](https://drive.google.com/folderview?id=0B2Kd_bs3CEYaZ3NiRkd4OXhYd3c).

Unfortunately Java doesn't provide a convenient deployment-time dependency manager such Python's `pip` therefore you will need to setup your classpath correctly. You can do this either via setting the `CLASSPATH` environment variable or as a command-line option (`java -cp <jar1>:<jar2>`). I've deliberately kept the dependencies to as few libraries as possible, as such this library is only dependent on [tinylog](http://www.tinylog.org) [v1.0](https://github.com/pmwmedia/tinylog/releases/download/1.0.3/tinylog-1.0.3.zip).

To compile or run diozero a application you will need 4 JAR files - tinylog, diozero-core, one of the supported device provider libraries and the corresponding diozero provider wrapper.

Provider | Provider Jar | diozero Wrapper-library
-------- | ------------ | -----------------------
JDK Device I/O 1.0 | dio-1.0.1.jar | diozero-provider-jdkdeviceio10-<version>.jar
JDK Device I/O 1.1 | dio-1.1.jar | diozero-provider-jdkdeviceio11-<version>.jar
Pi4j | pi4j-core-1.1-SNAPSHOT.jar | diozero-provider-pi4j-<version>.jar
wiringPi | pi4j-core-1.1-SNAPSHOT.jar | diozero-provider-wiringpi-<version>.jar
pigpio | pigpioj-java-1.0.0.jar | diozero-provider-pigio-<version>.jar

To get started I recommend first looking at the classes in [com.diozero.sampleapps](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/). To run the [LEDTest](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/LEDTest.java) sample application using the pigpioj provider:

Setting the CLASSPATH environment variable:
```sh
CLASSPATH=tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-pigpio-0.3-SNAPSHOT.jar:pigpioj-java-1.0.0.jar; export CLASSPATH
sudo java -cp $CLASSPATH com.diozero.sampleapps.LEDTest 12
```

Setting the classpath via the command-line:
```sh
sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-pigpio-0.3-SNAPSHOT.jar:pigpioj-java-1.0.0.jar com.diozero.sampleapps.LEDTest 12
```

For an experience similar to Python where source code is interpreted rather than compiled try [Groovy](http://www.groovy-lang.org/) (`sudo apt-get update && sudo apt-get install groovy2`). With the `CLASSPATH` environment variable set as per the instructions above, a simple test application can be run via the command `groovy test.groovy`. For example, the LED controlled button example:

```groovy
import com.diozero.Button
import com.diozero.LED
import com.diozero.util.SleepUtil

led = new LED(12)
button = new Button(25)

button.whenPressed({ led.on() })
button.whenReleased({ led.off() })

println("Waiting for button presses. Press CTRL-C to quit.")
SleepUtil.pause()
```

To run:

```sh
sudo groovy -cp $CLASSPATH test.groovy
```

!!! note "JAVA_HOME config"
    I was getting the error `groovy: JAVA_HOME is not defined correctly, can not execute: /usr/lib/jvm/default-java/bin/java`, tried setting JAVA_HOME in /etc/environment and /etc/profile.d/jdk.sh to no affect. Eventually this fixed it:
    ```sh
    ln -s /usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt /usr/lib/jvm/default-java
    ```

## Devices

This library provides support for a growing number of GPIO / I2C / SPI connected components and devices, I have categorised them as follows:

+ [Input Devices](InputDevices.md)
    - [Digital](InputDevices.md#digital-input-devices) and [Analog](InputDevices.md#analog-input-devices)
+ [Output Devices](OutputDevices.md)
    - [Digital](OutputDevices.md#digital-led) and [PWM](OutputDevices.md#pwm-led)
+ [Expansion Boards](ExpansionBoards.md) for adding additional GPIO / Analog / PWM pins
    - [Microchip Analog to Digital Converters](ExpansionBoards.md#microchip-analog-to-digital-converters), [Microchip GPIO Expansion Board](ExpansionBoards.md#microchip-gpio-expansion-board), [PWM / Server Driver](ExpansionBoards.md#pwm-servo-driver)
+ [Motor Control](MotorControl.md) (support for common motor controller boards)
    - [CamJam EduKit](MotorControl.md#camjam-edukit), [Ryanteck](MotorControl.md#ryanteck), [Toshiba TB6612FNG](MotorControl.md#toshiba-tb6612fng)
+ [Sensor Components](SensorComponents.md) (support for specific sensors, e.g. temperature, pressure, distance, luminosity)
    - [HC-SRO4 Ultrasonic Ranging Module](SensorComponents.md#hc-sr04), [Bosch BMP180](SensorComponents.md#bosch-bmp180), [TSL2561 Light Sensor](SensorComponents.md#tsl2561)
+ [API](API.md) for lower-level interactions
    - [Input](API.md#input-devices), [Output](API.md#output-devices), [I2C](API.md#i2c-support), [SPI](API.md#spi-support)
+ [IMU Devices](IMUDevices.md) Work-in-progress API for interacting with Inertial Measurement Units such as the InvenSense MPU-9150 and the Analog Devices ADXL345
    - [API](IMUDevices.md#api), [Supported Devices](IMUDevices.md#supported-devices)
+ [LED Strips](LEDStrips.md) Support for LED strips (WS2811B / WS2812B / Adafruit NeoPixel)
    - [API](LEDStrips.md), [Supported Devices](LEDStrips.md#supported-devices)

## Performance

I've done some limited performance tests (turning a GPIO on then off, see [GpioPerfTest](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/GpioPerfTest.java)) on a Raspberry Pi 2 using the various native device factory providers as well as a test using Pi4j's wiringPi JNI API directly without going via my DIO-Zero wrapper (see [WiringPiRawPerfTest](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-wiringpi/src/main/java/com/diozero/internal/provider/wiringpi/WiringPiRawPerfTest.java)); here are the results:

| Provider | Iterations | Frequency (kHz) |
| -------- | ----------:| ---------------:|
| Pi4j 1.0 | 10,000 | 0.91 |
| JDK DIO 1.1 | 100,000 | 8.23 |
| Pi4j 1.1 | 10,000,000 | 622 |
| wiringPi | 5,000,000 | 1,683 |
| wiringPi (direct) | 10,000,000 | 2,137 |
| pigpio | 5,000,000 | 1,266 |
| pigpio (direct) | 10,000,000 | 1,649 |

For a discussion on why Pi4j 1.0 was so slow, see this [issue](https://github.com/Pi4J/pi4j/issues/158). These results are in-line with those documented in the book ["Raspberry Pi with Java: Programming the Internet of Things"](http://www.amazon.co.uk/Raspberry-Pi-Java-Programming-Internet/dp/0071842012). For reference, the author's results were:

| Library | Frequency (kHz) |
|:------- | ---------------:|
|Pi4j 1.0 | 0.751 |
|JDK DIO 1.0 | 3.048 |
|wiringPi (direct) | 1,662 |

## Development

This project is hosted on [GitHub](https://github.com/mattjlewis/diozero/), please feel free to join in:

+ Make suggestions for [fixes and enhancements](https://github.com/mattjlewis/diozero/issues)
+ Provide sample applications
+ Contribute to development

TODO Add something about Maven dependencies, setting up development environments.

## To-Do

There is still a lot left to do, in particular:

+ Thorough testing (various types of devices using each service provider)
+ Testing on different devices (all flavours of Raspberry Pi, BeagleBone, ...)
+ GPIO input debouncing
+ Other I2C & SPI devices, including those on the SenseHAT
+ A clean object-orientated API for IMUs

## Change-log

+ Release 0.2: First tagged release
+ Release 0.3: API change - analogue to analog

## License

This work is provided under the [MIT License](license.md).
