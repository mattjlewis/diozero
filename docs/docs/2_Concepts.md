---
title: Concepts
nav_order: 2
permalink: /concepts.html
has_children: true
---

# Concepts

diozero is a multi-faceted “library” for interacting with low-level devices such as environmental
sensors and GPIOs. It achieves this via object-oriented APIs that abstract developers from the
complexities of low-level device interface code. An initial motivation to developing diozero
was to provide a Java equivalent to the excellent Python [gpiozero](https://gpiozero.readthedocs.io/)
library having found that existing Java libraries didn't offer such a developer-friendly interface.

## Sections
{: .no_toc .text-delta }

1. TOC
{:toc}

TODO - devices (LEDs, buttons, sensors, motors, displays, etc) that can be connected to Single Board
Computers like the Raspberry Pi. Actual GPIO / I2C / SPI device communication is delegated 
to pluggable service providers for maximum compatibility across different boards. This library is 
known to work on the following boards: all models of the Raspberry Pi, Odroid C2, BeagleBone 
Black, C.H.I.P and Asus Tinker Board. It should be portable to any Single Board computer that 
runs Linux and Java 8.

The aim of this library is to encapsulate real-world devices as classes with meaningful operation 
names, for example, LED (on / off), LDR (get luminosity), Button (pressed / released), Motor 
(forward / backwards / left / right). All devices implement `Closeable` hence will get 
automatically closed by the `try (Device d = new Device()) { d.doSomething(); }` statement. This 
is best illustrated by some simple examples.

> Pin Numbering 
>{: .admonition-title }
>
> All pin numbers are device native, i.e. Broadcom for the Raspberry Pi, ASUS for the Tinker Board. Pin layouts:
> 
> * [Raspberry pi](https://pinout.xyz/).
> * [CHIP pin numbering](http://www.chip-community.org/index.php/Hardware_Information).
> * [Odroid C2 pin layout](http://www.hardkernel.com/main/products/prdt_info.php?tab_idx=2).
> * [BeagleBone Black](http://beagleboard.org/support/bone101).
> * [Asus Tinker Board](https://www.asus.com/uk/Single-board-Computer/TINKER-BOARD/).
{: .admonition .note }

LED control:

```java
int led_gpio = 18;
try (LED led = new LED(led_gpio)) {
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
int button_gpio = 12;
int led_gpio = 18;
try (Button button = new Button(button_gpio); LED led = new LED(led_gpio)) {
	button.whenPressed(led::on);
	button.whenReleased(led::off);
	SleepUtil.sleepSeconds(10);
}
```

Or a random LED flicker effect:

```java
Random random = new Random();
int led_gpio = 18;
try (PwmLed led = new PwmLed(led_gpio)) {
	DioZeroScheduler.getNonDaemonInstance().invokeAtFixedRate(RANDOM::nextFloat, led::setValue, 50, 50, TimeUnit.MILLISECONDS);
}
```

Iternally, all devices are provisioned via a
[Device Factory](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/internal/spi/DeviceFactoryInterface.java) -
all of the classes in com.diozero.api and com.diozero.devices include an additional constructor
parameter that allows a device factory to be specified. If a device factory isn't specified, the
host board itself is used to provision the device using
[DeviceFactoryHelper.getNativeDeviceFactory()](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sbc/DeviceFactoryHelper.java).
This is particularly useful for GPIO expansion boards and Analog-to-Digital converters.
The [Providers](2_concepts/1_Providers.md) section provides further details on board device providers.

> Device Factories
>{: .admonition-title }
>
> Unless you are implementing a new device you shouldn't need to use any of the Device 
> Factory interfaces or helper classes (within the `com.diozero.internal` package).
{: .admonition .note }

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

## Getting Started

Snapshot builds of the library are available in the [Nexus Repository Manager](https://oss.sonatype.org/index.html#nexus-search;gav~com.diozero~~~~). 
For convenience a ZIP of all diozero JARs (currently v0.13) will also be available on [Google Drive](https://drive.google.com/file/d/1WZH6zTwo_xlFDn7CVkx_ABkXbkYK6E2n/view?usp=sharing).

Javadoc for the core library is also available via [javadoc.io](http://www.javadoc.io/doc/com.diozero/diozero-core/). 

Unfortunately Java doesn't provide a convenient deployment-time dependency manager 
such as Python's `pip` therefore you will need to manually download all dependencies 
and setup your classpath correctly. You can do this either via setting the `CLASSPATH` 
environment variable or as a command-line option (`java -cp <jar1>:<jar2>`). 
The dependencies have been deliberately kept to as few libraries as possible, as 
such this library is only dependent on [tinylog](http://www.tinylog.org) 
[v2.1.2](https://github.com/pmwmedia/tinylog/releases/download/2.1.2/tinylog-2.1.2.zip).

To compile a diozero application you will need 3 JAR files - [tinylog](http://www.tinylog.org/) (API and Impl), and diozero-core. 
To run a diozero application, you can also include one of the supported device provider 
libraries and the corresponding diozero provider wrapper library. Note that the built-in sysfs
device provider gives maximum portability but has some limitations such as not being able 
to configure internal pull up/down resistors.

Provider | Dependency | diozero Provider Library | SBC
--- | -------- | ------------ | -----------------------
built-in | None | Built-in | All
mmap     | diozero system-utils-native | diozero-provider-mmap-&lt;version&gt;.jar | \*
pigpio | pigpioj-java | diozero-provider-pigio | Raspberry Pi (all flavours)
bbbiolib | bbbiolib | diozero-provider-bbiolib | BeagleBone Green / Black
firmata | firmata4j | diozero-provider-firmata | Arduino compatible boards
voodoospark | None | diozero-provider-voodoospark | Arduino compatible boards

\* MMAP is currently supported on the following SBCs:

* Raspberry Pi
* FriendlyArm H3 / Allwinner Sun8i CPU (as used in the NanoPi Duo2 / NanoPi Neo amongst others)
* ASUS TinkerBoard
* Odroid C2
* Next Think Co CHIP (Allwinner sun4i/sun5i)

To get started I recommend first looking at the classes in 
[com.diozero.sampleapps](https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/). 
To run the [LEDTest](https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/LEDTest.java) 
sample application using the pigpioj provider:

```sh
sudo java -cp tinylog-api-2.1.2.jar:tinylog-impl-2.1.2.jar:diozero-core-0.13.jar:diozero-sampleapps-0.13.jar:diozero-provider-pigpio-0.13.jar:pigpioj-java-2.4.jar com.diozero.sampleapps.LEDTest 12
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
