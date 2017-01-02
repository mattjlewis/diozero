# DIO-Zero - a Java Device I/O wrapper for GPIO / I2C / SPI devices

A Device I/O library written in Java that provides an object-orientated interface for a range of GPIO / I2C / SPI devices such as LEDs, buttons and other various sensors connected to single board computers like the Raspberry Pi, BeagleBone Black, Odroid C2, Udoo, C.H.I.P. Actual GPIO / I2C / SPI device communication is implemented via pluggable service providers for maximum compatibility across different device types. This library is known to work on all models of the Raspberry Pi, the Udoo Quad, and the Odroid C2.

This library makes use of modern Java 8 features such as [automatic resource management](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html), [Lambda Expressions](https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html) and [Method References](https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html) where they simplify development and improve code readability.

Created by [Matt Lewis](https://github.com/mattjlewis) (email [deviceiozero@gmail.com](mailto:deviceiozero@gmail.com)), inspired by [GPIO Zero](https://gpiozero.readthedocs.org/en/v1.1.0/index.html). If you have any issues, comments or suggestions please use [this thread](https://www.raspberrypi.org/forums/viewtopic.php?f=81&t=136010) on the Raspberry Pi forums. I'd very much like to hear any suggestions, feedback or other comments regarding this library.

## Concepts

The aim of this library is to encapsulate real-world devices as classes with meaningful operation names, for example LED (on / off), LDR (get luminosity), Button (pressed / released), Motor (forward / backwards / left / right). All devices implement `Closeable` hence will be automatically closed by the `try (Device d = new Device()) { d.doSomething(); }` statement. This is best illustrated by some simple examples.

!!! note "Pin Numbering"
    All pin numbers are device native, i.e. Broadcom for the Raspberry Pi.

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
Random random = new Random();
try (PwmLed led = new PwmLed(pin)) {
	DioZeroScheduler.getNonDaemonInstance().invokeAtFixedRate(RANDOM::nextFloat, led::setValue, 50, 50, TimeUnit.MILLISECONDS);
}
```

All devices are actually provisioned by a [Device Factory](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/internal/spi/DeviceFactoryInterface.java) with a default [NativeDeviceFactory](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/internal/DeviceFactoryHelper.java) for provisioning via the host board itself. However, all components accept an optional Device Factory parameter for provisioning the same set of components via an alternative method. This is particularly useful for GPIO expansion boards and Analog-to-Digital converters.

!!! note "Device Factory"
    Unless you are implementing a new device you shouldn't need to use any of the Device Factory interfaces or helper classes (under the `com.diozero.internal` package).

The Raspberry Pi provides no analog input pins; attempting to create an AnalogInputDevice such as an LDR using the Raspberry Pi default native device factory would result in a runtime error (`UnsupportedOperationException`). However, ADC classes such as the [McpAdc](http://rtd.diozero.com/en/latest/ExpansionBoards/#mcp-adc) have been implemented as analog input device factories hence can be used to construct analog devices such as LDRs:

```java
try (McpAdc adc = new McpAdc(McpAdc.Type.MCP3008, chipSelect); LDR ldr = new LDR(adc, pin, vRef, r1)) {
	System.out.println(ldr.getUnscaledValue());
}
```

Repeating the previous example of controlling an LED when you press a button but with all devices connected via an [MCP23017](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/MCP23017.java) GPIO expansion board:

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
	ldr.addListener((event) -> led.setValue(1-event.getUnscaledValue()), .1f);
	SleepUtil.sleepSeconds(20);
}
```

## Getting Started

Snapshot builds of the library are available in the [Nexus Repository Manager](https://oss.sonatype.org/index.html#nexus-search;gav~com.diozero~~~~). For convenience a ZIP of all diozero JARs will be maintained on [Google Drive](https://drive.google.com/folderview?id=0B2Kd_bs3CEYaZ3NiRkd4OXhYd3c).

Javadoc for the core library is also available via [javadoc.io](http://www.javadoc.io/doc/com.diozero/diozero-core/). 

Unfortunately Java doesn't provide a convenient deployment-time dependency manager such Python's `pip` therefore you will need to manually download all dependencies and setup your classpath correctly. You can do this either via setting the `CLASSPATH` environment variable or as a command-line option (`java -cp <jar1>:<jar2>`). The dependencies have been deliberately kept to as few libraries as possible, as such this library is only dependent on [tinylog](http://www.tinylog.org) [v1.0](https://github.com/pmwmedia/tinylog/releases/download/1.1/tinylog-1.1.zip).

To compile and run a diozero application you will need 4 JAR files - tinylog, diozero-core, one of the supported device provider libraries and the corresponding diozero provider wrapper library.

Provider | Provider Jar | diozero wrapper-library
-------- | ------------ | -----------------------
JDK Device I/O 1.0 | dio-1.0.1.jar | diozero-provider-jdkdeviceio10-&lt;version&gt;.jar
JDK Device I/O 1.1 | dio-1.1.jar | diozero-provider-jdkdeviceio11-&lt;version&gt;.jar
Pi4j | pi4j-core-1.1-SNAPSHOT.jar | diozero-provider-pi4j-&lt;version&gt;.jar
wiringPi | pi4j-core-1.1-SNAPSHOT.jar | diozero-provider-wiringpi-&lt;version&gt;.jar
pigpio | pigpioj-java-1.0.0.jar | diozero-provider-pigio-&lt;version&gt;.jar
jpi | N/A | diozero-provider-jpi-&lt;version&gt;.jar
sysfs | N/A | diozero-provider-sysfs-&lt;version&gt;.jar

To get started I recommend first looking at the classes in [com.diozero.sampleapps](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/). To run the [LEDTest](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/LEDTest.java) sample application using the pigpioj provider:

Option 1 - Setting the CLASSPATH environment variable:
```sh
CLASSPATH=tinylog-1.1.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-pigpio-0.3-SNAPSHOT.jar:pigpioj-java-1.0.0.jar; export CLASSPATH
sudo java -cp $CLASSPATH com.diozero.sampleapps.LEDTest 12
```

Option 2 - Setting the classpath via command-line:
```sh
sudo java -cp tinylog-1.1.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-pigpio-0.3-SNAPSHOT.jar:pigpioj-java-1.0.0.jar com.diozero.sampleapps.LEDTest 12
```

For an experience similar to Python where source code can be interpreted rather than compiled try [Groovy](http://www.groovy-lang.org/) (`sudo apt-get update && sudo apt-get install groovy2`). With the `CLASSPATH` environment variable set as per the instructions above, a simple test application can be run via the command `groovy <filename>`. There is also a Groovy shell environment `groovysh`.

A Groovy equivalent of the LED controlled button example:

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

!!! note "Groovy JAVA_HOME config when running via sudo"
    I was getting the error:
    
    `groovy: JAVA_HOME is not defined correctly, can not execute: /usr/lib/jvm/default-java/bin/java`
    
    I tried setting JAVA_HOME in /etc/environment and /etc/profile.d/jdk.sh to no affect. Eventually the following fixed it for me. Please let me know if there is a better way to fix this issue.
    
    ```
    ln -s /usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt /usr/lib/jvm/default-java
    ```

## Devices

This library provides support for a number of GPIO / I2C / SPI connected components and devices, I have categorised them as follows:

+ [API](http://rtd.diozero.com/en/latest/API/) for lower-level interactions
    - [Input](http://rtd.diozero.com/en/latest/API/#input-devices), [Output](http://rtd.diozero.com/en/latest/API/#output-devices), [I2C](http://rtd.diozero.com/en/latest/API/#i2c-support), [SPI](http://rtd.diozero.com/en/latest/API/#spi-support)
+ [Input Devices](http://rtd.diozero.com/en/latest/InputDevices/)
    - [Digital](http://rtd.diozero.com/en/latest/InputDevices/#digital-input-devices) and [Analog](http://rtd.diozero.com/en/latest/InputDevices/#analog-input-devices)
+ [Output Devices](http://rtd.diozero.com/en/latest/OutputDevices/)
    - [Digital](http://rtd.diozero.com/en/latest/OutputDevices/#digital-led) and [PWM](http://rtd.diozero.com/en/latest/OutputDevices/#pwm-led)
+ [Expansion Boards](http://rtd.diozero.com/en/latest/ExpansionBoards/) for adding additional GPIO / Analog / PWM pins
    - [Microchip Analog to Digital Converters](http://rtd.diozero.com/en/latest/ExpansionBoards/#mcp-adc), [Microchip GPIO Expansion Board](http://rtd.diozero.com/en/latest/ExpansionBoards/#mcp-gpio-expansion-board), [PWM / Servo Driver](http://rtd.diozero.com/en/latest/ExpansionBoards/#pwm-servo-driver)
+ [Motor Control](http://rtd.diozero.com/en/latest/MotorControl/) (support for common motor controller boards)
    - [API](http://rtd.diozero.com/en/latest/MotorControl/#api), [Servos](http://rtd.diozero.com/en/latest/MotorControl/#servo), [CamJam EduKit](http://rtd.diozero.com/en/latest/MotorControl/#camjamkitdualmotor), [Ryanteck](http://rtd.diozero.com/en/latest/MotorControl/#ryanteckdualmotor), [Toshiba TB6612FNG](http://rtd.diozero.com/en/latest/MotorControl/#tb6612fngdualmotordriver)
+ [Sensor Components](http://rtd.diozero.com/en/latest/SensorComponents/) (support for specific sensors, e.g. temperature, pressure, distance, luminosity)
    - [HC-SR04 Ultrasonic Ranging Module](http://rtd.diozero.com/en/latest/SensorComponents/#hc-sr04), [Bosch BMP180](http://rtd.diozero.com/en/latest/SensorComponents/#bosch-bmp180), [TSL2561 Light Sensor](http://rtd.diozero.com/en/latest/SensorComponents/#tsl2561), [1-Wire Temperature Sensors e.g. DS18B20](http://rtd.diozero.com/en/latest/SensorComponents/#1-wire-temperature-sensors)
+ [IMU Devices](http://rtd.diozero.com/en/latest/IMUDevices/) Work-in-progress API for interacting with Inertial Measurement Units such as the InvenSense MPU-9150 and the Analog Devices ADXL345
    - [API](http://rtd.diozero.com/en/latest/IMUDevices/#api), [Supported Devices](http://rtd.diozero.com/en/latest/IMUDevices/#supported-devices)
+ [LED Strips](http://rtd.diozero.com/en/latest/LEDStrips/) Support for LED strips (WS2811B / WS2812B / Adafruit NeoPixel)
    - [WS2811B / WS2812B](http://rtd.diozero.com/en/latest/LEDStrips/#ws281x)

## Performance

I've done some limited performance tests (turning a GPIO on then off, see [GpioPerfTest](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/GpioPerfTest.java)) on a Raspberry Pi 2 and 3 using the various native device factory providers. I've also run tests using JNI APIs directly without going via my DIO-Zero wrapper to assess the overhead of using my library (see [WiringPiRawPerfTest](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-wiringpi/src/main/java/com/diozero/internal/provider/wiringpi/WiringPiRawPerfTest.java) and [PigpioPerfTest](https://github.com/mattjlewis/pigpioj/blob/master/pigpioj-java/src/main/java/com/diozero/pigpioj/test/PigpioPerfTest.java)) - the overhead of DIO-Zero is approximately 25% for both pigpio and wiringPi. Here are the results:

| Provider | Device | Frequency (kHz) |
| -------- |:------:| ---------------:|
| Pi4j 1.0 | Pi2 | 0.91 |
| JDK DIO 1.1 | Pi2 | 8.23 |
| Pi4j 1.1 | Pi2 | 622 |
| pigpio | Pi2 | 2,019 |
| pigpio | Pi3 | 2,900 |
| pigpio (JNI) | Pi2 | 2,509 |
| pigpio (JNI) | Pi3 | 3,537 |
| wiringPi | Pi2 | 2,640 |
| wiringPi | Pi3 | 3,446 |
| wiringPi (JNI) | Pi2 | 3,298 |
| wiringPi (JNI) | Pi3 | 4,373 |
| jpi | Pi3 |  7,686 |
| jpi (JNI) | Pi3 |   11,007 |

![Performance](images/Performance.png "Performance") 

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
+ Tell me about your projects!

## To-Do

There is still a lot left to do, in particular:

+ Thorough testing (various types of devices using each service provider)
+ GPIO input debouncing
+ Other I2C & SPI devices, in particular the IMU on the SenseHAT
+ A clean object-orientated API for IMUs

## Change-log

+ Release 0.2: First tagged release
+ Release 0.3: API change - analogue to analog
+ Release 0.4: Bug fixes, servo support
+ Release 0.5: Testing improvements
+ Release 0.6: Preparing for 1.0 release
+ Release 0.7: Support for non-register based I2C device read / write
+ Release 0.8: Added Analog Output device support (added for the PCF8591). Introduced Java based sysfs and jpi providers. Bug fix to I2CLcd. Added support for BME280.

## License

This work is provided under the [MIT License](http://rtd.diozero.com/en/latest/license/).
