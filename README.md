# DIO-Zero - a Java Device I/O wrapper for GPIO / I2C / SPI control

[![Build Status](https://travis-ci.org/mattjlewis/diozero.svg?branch=master)](https://travis-ci.org/mattjlewis/diozero)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.diozero/diozero/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.diozero/diozero)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/com.diozero/diozero/badge.svg)](http://www.javadoc.io/doc/com.diozero/diozero)

A Device I/O library written in Java that provides an object-orientated interface for a range of 
GPIO / I2C / SPI devices (LEDs, buttons, sensors, motors, displays, etc) connected to Single 
Board Computers like the Raspberry Pi. Actual GPIO / I2C / SPI device communication is delegated 
to pluggable service providers for maximum compatibility across different boards. This library is 
known to work on the following boards: all models of the Raspberry Pi, Odroid C2, BeagleBone 
Black, C.H.I.P and Asus Tinker Board. It should be portable to any Single Board computer that 
runs Linux and Java 8.

This library makes use of modern Java features such as 
[automatic resource management](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html), 
[Lambda Expressions](https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html) and 
[Method References](https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html) 
where they simplify development and improve code readability.

Created by [Matt Lewis](https://github.com/mattjlewis) (email [deviceiozero@gmail.com](mailto:deviceiozero@gmail.com)), 
inspired by [GPIO Zero](https://gpiozero.readthedocs.org/) and [Johnny Five](http://johnny-five.io/). 
If you have any issues please use the [GitHub issues page](https://github.com/mattjlewis/diozero/issues). 
For any other comments or suggestions, please use the [diozero Google Group](https://groups.google.com/forum/#!forum/diozero).

## Concepts

The aim of this library is to encapsulate real-world devices as classes with meaningful operation 
names, for example, LED (on / off), LDR (get luminosity), Button (pressed / released), Motor 
(forward / backwards / left / right). All devices implement `Closeable` hence will get 
automatically closed by the `try (Device d = new Device()) { d.doSomething(); }` statement. This 
is best illustrated by some simple examples.

!!! note "Pin Numbering"
    All pin numbers are device native, i.e. Broadcom for the Raspberry Pi, ASUS for the Tinker Board. Pin layouts:
    
    + [Raspberry pi](https://pinout.xyz/).
    + [CHIP pin numbering](http://www.chip-community.org/index.php/Hardware_Information).
    + [Odroid C2 pin layout](http://www.hardkernel.com/main/products/prdt_info.php?tab_idx=2).
    + [BeagleBone Black](http://beagleboard.org/support/bone101).
    + [Asus Tinker Board](https://www.asus.com/uk/Single-board-Computer/TINKER-BOARD/).

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

All devices are actually provisioned by a 
[Device Factory](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/internal/spi/DeviceFactoryInterface.java) 
with a default [NativeDeviceFactory](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/internal/DeviceFactoryHelper.java) 
for provisioning via the host board itself. However, all components accept an optional 
Device Factory parameter for provisioning the same set of components via an alternative 
method. This is particularly useful for GPIO expansion boards and Analog-to-Digital converters.

!!! note "Device Factory"
    Unless you are implementing a new device you shouldn't need to use any of the Device 
    Factory interfaces or helper classes (within the `com.diozero.internal` package).

Some boards like the Raspberry Pi provide no analog input pins; attempting to create an 
AnalogInputDevice such as an LDR using the Raspberry Pi default native device factory 
would result in a runtime error (`UnsupportedOperationException`). However, support for 
Analog to Digital Converter expansion devices such as the 
[MCP3008](http://rtd.diozero.com/en/latest/ExpansionBoards/#mcp-adc) has been added 
which are implemented as analog input device factories hence can be used in the 
constructor of analog devices like LDRs:

```java
try (McpAdc adc = new McpAdc(McpAdc.Type.MCP3008, chipSelect); LDR ldr = new LDR(adc, pin, vRef, r1)) {
	System.out.println(ldr.getUnscaledValue());
}
```

Repeating the previous example of controlling an LED when you press a button but with 
all devices connected via an 
[MCP23017](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/MCP23017.java) 
GPIO expansion board:

```java
try (MCP23017 mcp23017 = new MCP23017(intAPin, intBPin);
		Button button = new Button(mcp23017, inputPin, GpioPullUpDown.PULL_UP);
		LED led = new LED(mcp23017, outputPin)) {
	button.whenPressed(led::on);
	button.whenReleased(led::off);
	SleepUtil.sleepSeconds(10);
}
```

Analog input devices also provide an event notification mechanism. To control the 
brightness of an LED based on ambient light levels:

```java
try (McpAdc adc = new McpAdc(McpAdc.Type.MCP3008, chipSelect); LDR ldr = new LDR(adc, pin, vRef, r1); PwmLed led = new PwmLed(ledPin)) {
	// Detect variations of 10%, get values every 50ms (the default)
	ldr.addListener((event) -> led.setValue(1-event.getUnscaledValue()), .1f);
	SleepUtil.sleepSeconds(20);
}
```

## Supported Devices

diozero has out of the box support for the following Single Board Computers:

+ [Raspberry Pi](http://www.raspberyrpi.org/) (all versions).
+ [Odroid C2](http://www.hardkernel.com/main/products/prdt_info.php).
+ [Beagle Bone Black](https://beagleboard.org/black).
+ [Asus Tinker Board](https://www.asus.com/uk/Single-board-Computer/TINKER-BOARD/).
+ [The Next Thing Co CHIP](https://getchip.com/pages/chip).

The builtin sysfs provider is designed to be portable across different boards. 
In addition, the [JDK Device I/O](https://wiki.openjdk.java.net/display/dio/Main) providers 
add an alternative method of compatibility, for example on the Udoo Quad.

## Getting Started

Snapshot builds of the library are available in the [Nexus Repository Manager](https://oss.sonatype.org/index.html#nexus-search;gav~com.diozero~~~~). 
For convenience a ZIP of all diozero JARs will also be available on [Google Drive](https://drive.google.com/open?id=0BxA10VX9SC74VDR6WTlLOEdpYzA).

Javadoc for the core library is also available via [javadoc.io](http://www.javadoc.io/doc/com.diozero/diozero-core/). 

Unfortunately Java doesn't provide a convenient deployment-time dependency manager 
such as Python's `pip` therefore you will need to manually download all dependencies 
and setup your classpath correctly. You can do this either via setting the `CLASSPATH` 
environment variable or as a command-line option (`java -cp <jar1>:<jar2>`). 
The dependencies have been deliberately kept to as few libraries as possible, as 
such this library is only dependent on [tinylog](http://www.tinylog.org) 
[v1.1](https://github.com/pmwmedia/tinylog/releases/download/1.1/tinylog-1.1.zip).

To compile a diozero application you will need 2 JAR files - [tinylog](http://www.tinylog.org/), and diozero-core. 
To run a diozero application, you will also need one of the supported device provider 
libraries and the corresponding diozero provider wrapper library. Note the built-in sysfs
device provider gives maximum portability but has some limitations such as not being able 
to configure internal pull up/down resistors.

SBC | Provider | Provider Jar | diozero wrapper-library
--- | -------- | ------------ | -----------------------
All | sysfs | N/A | Built-in
32bit boards | JDK Device I/O 1.0 | dio-1.0.1.jar | diozero-provider-jdkdeviceio10-&lt;version&gt;.jar
32bit boards | JDK Device I/O 1.1 | dio-1.1.jar | diozero-provider-jdkdeviceio11-&lt;version&gt;.jar
Raspberry Pi | Pi4j | pi4j-core-1.1.jar | diozero-provider-pi4j-&lt;version&gt;.jar
Raspberry Pi | wiringPi | pi4j-core-1.1.jar | diozero-provider-wiringpi-&lt;version&gt;.jar
Raspberry Pi | pigpio | pigpioj-java-1.0.1.jar | diozero-provider-pigio-&lt;version&gt;.jar
Raspberry Pi, Odroid C2 | mmap | N/A | diozero-provider-mmap-&lt;version&gt;.jar

To get started I recommend first looking at the classes in 
[com.diozero.sampleapps](https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/). 
To run the [LEDTest](https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/LEDTest.java) 
sample application using the pigpioj provider:

```sh
sudo java -cp tinylog-1.1.jar:diozero-core-0.9-SNAPSHOT.jar:diozero-sampleapps-0.9-SNAPSHOT.jar:diozero-provider-pigpio-0.9-SNAPSHOT.jar:pigpioj-java-1.0.1.jar com.diozero.sampleapps.LEDTest 12
```

For an experience similar to Python where source code is interpreted rather than compiled try 
[Groovy](http://www.groovy-lang.org/) (`sudo apt-get update && sudo apt-get install groovy2`). 
With the `CLASSPATH` environment variable set as per the instructions above, a simple test 
application can be run via the command `groovy <filename>`. There is also a Groovy shell environment `groovysh`.

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
    
    I tried setting JAVA_HOME in /etc/environment and /etc/profile.d/jdk.sh to no affect. Eventually the following fixed it for me. 
    Please let me know if there is a better way to fix this issue.
    
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
    - [Microchip Analog to Digital Converters](http://rtd.diozero.com/en/latest/ExpansionBoards/#mcp-adc), [NXP PCF8591 ADC / DAC](http://rtd.diozero.com/en/latest/ExpansionBoards/#pcf8591), [Microchip GPIO Expansion Board](http://rtd.diozero.com/en/latest/ExpansionBoards/#mcp-gpio-expansion-board), [PWM / Servo Driver](http://rtd.diozero.com/en/latest/ExpansionBoards/#pwm-servo-driver), [PCF8574](http://rtd.diozero.com/en/latest/ExpansionBoards#pcf8574)
+ [Motor Control](http://rtd.diozero.com/en/latest/MotorControl/) (support for common motor controller boards)
    - [API](http://rtd.diozero.com/en/latest/MotorControl/#api), [Servos](http://rtd.diozero.com/en/latest/MotorControl/#servo), [CamJam EduKit](http://rtd.diozero.com/en/latest/MotorControl/#camjamkitdualmotor), [Ryanteck](http://rtd.diozero.com/en/latest/MotorControl/#ryanteckdualmotor), [Toshiba TB6612FNG](http://rtd.diozero.com/en/latest/MotorControl/#tb6612fngdualmotordriver), [PiConZero](http://rtd.diozero.com/en/latest/MotorControl/#piconzero)
+ [Sensor Components](http://rtd.diozero.com/en/latest/SensorComponents/) (support for specific sensors, e.g. temperature, pressure, distance, luminosity)
    - [HC-SR04 Ultrasonic Ranging Module](http://rtd.diozero.com/en/latest/SensorComponents/#hc-sr04), [Bosch BMP180](http://rtd.diozero.com/en/latest/SensorComponents/#bosch-bmp180), [Bosch BME280](http://rtd.diozero.com/en/latest/SensorComponents/#bosch-bme280), [TSL2561 Light Sensor](http://rtd.diozero.com/en/latest/SensorComponents/#tsl2561), [STMicroelectronics HTS221 Humidity and Temperature Sensor](http://rtd.diozero.com/en/latest/SensorComponents/#hts221), [STMicroelectronics LPS25H Pressure and Temperature Sensor](http://rtd.diozero.com/en/latest/SensorComponents/#lps25h),, [1-Wire Temperature Sensors e.g. DS18B20](http://rtd.diozero.com/en/latest/SensorComponents/#1-wire-temperature-sensors), [Sharp GP2Y0A21YK distance sensor](http://rtd.diozero.com/en/latest/SensorComponents/#gp2y0a21yk), [Mifare RC522 RFID Reader](http://rtd.diozero.com/en/latest/SensorComponents/#mfrc522)
+ [LCD Displays](http://rtd.diozero.com/en/latest/LCDDisplays/)
    - [HD44780 controlled LCDs](http://rtd.diozero.com/en/latest/LCDDisplays/#i2c-lcds)
+ [IMU Devices](http://rtd.diozero.com/en/latest/IMUDevices/) Work-in-progress API for interacting with Inertial Measurement Units such as the InvenSense MPU-9150 and the Analog Devices ADXL345
    - [API](http://rtd.diozero.com/en/latest/IMUDevices/#api), [Supported Devices](http://rtd.diozero.com/en/latest/IMUDevices/#supported-devices)
+ [LED Strips](http://rtd.diozero.com/en/latest/LEDStrips/) Support for LED strips (WS2811B / WS2812B / Adafruit NeoPixel)
    - [WS2811B / WS2812B](http://rtd.diozero.com/en/latest/LEDStrips/#ws281x)

## Performance

I've done some limited performance tests (turning a GPIO on then off, see 
[GpioPerfTest](https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/GpioPerfTest.java)) 
on a Raspberry Pi 2 and 3 using the various native device factory providers. I've also run tests using JNI APIs 
directly without going via my DIO-Zero wrapper to assess the overhead of using my library (see 
[WiringPiRawPerfTest](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-wiringpi/src/main/java/com/diozero/internal/provider/wiringpi/WiringPiRawPerfTest.java) and 
[PigpioPerfTest](https://github.com/mattjlewis/pigpioj/blob/master/pigpioj-java/src/main/java/com/diozero/pigpioj/test/PigpioPerfTest.java)) - 
the overhead of DIO-Zero is approximately 25% for both pigpio and wiringPi. Here are the results:

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
| mmap | Pi3 |  7,686 |
| mmap (JNI) | Pi3 |   11,007 |

![Performance](images/Performance.png "Performance") 

For a discussion on why Pi4j 1.0 was so slow, see this [issue](https://github.com/Pi4J/pi4j/issues/158). 
These results are in-line with those documented in the book 
["Raspberry Pi with Java: Programming the Internet of Things"](http://www.amazon.co.uk/Raspberry-Pi-Java-Programming-Internet/dp/0071842012). 
For reference, the author's results were:

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

## To-Do

+ Thorough testing (various types of devices using each service provider)
+ A clean object-orientated API for IMUs
+ Native support for all devices via mmap (/dev/mem), in particular to improve performance and add support for GPIO pull up/down configuration.
+ Cleanup the logic for handling capabilities of different boards in a generic fashion (no more if / then / else)
+ Firmata SPI support (via USB cable)
+ Wireless access to Firmata devices (network and Bluetooth). E.g. [ESP32](https://learn.sparkfun.com/tutorials/esp32-thing-hookup-guide?_ga=1.116824388.33505106.1471290985#installing-the-esp32-arduino-core) [Firmata GitHub issue #315](https://github.com/firmata/arduino/issues/315)
+ Particle Photon support (via wifi using [VoodooSpark "firmware"](https://github.com/voodootikigod/voodoospark) - [JavaScript implementation](https://github.com/rwaldron/particle-io/blob/master/lib/particle.js))

## Change-log

+ Release 0.2: First tagged release.
+ Release 0.3: API change - analogue to analog.
+ Release 0.4: Bug fixes, servo support.
+ Release 0.5: Testing improvements.
+ Release 0.6: Preparing for 1.0 release.
+ Release 0.7: Support for non-register based I2C device read / write
+ Release 0.8: Added Analog Output device support (added for the PCF8591). Introduced Java based sysfs and jpi providers. Bug fix to I2CLcd. Added support for BME280.
+ Release 0.9: Native support for I2C and SPI in the sysfs provider. Support for CHIP, BeagleBone Black and Asus Tinker Board. Moved sysfs provider into diozero-core, use as the default provider. Preliminary support for devices that support the Firmata protocol (i.e. Arduino).
+ Release 0.10 (in progress): Firmata I2C. Improvements to MFRC522. SDL Joystick JNI wrapper library. MFRC522 fully supported (finally). Added support for MCP EEPROMs. I2C SMBus implementation in the internal sysfs provider.

## License

This work is provided under the [MIT License](http://rtd.diozero.com/en/latest/license/).
