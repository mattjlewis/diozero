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
library having found that existing Java libraries didn't offer such a developer-friendly experience.

TODO - devices (LEDs, buttons, sensors, motors, displays, etc) that can be connected to Single Board
Computers like the Raspberry Pi. Actual GPIO / I2C / SPI device communication is delegated 
to pluggable service providers for maximum compatibility across different boards. This library is 
known to work on the following boards: all models of the Raspberry Pi, Odroid C2, BeagleBone 
(Green and Black), C.H.I.P and ASUS Tinker Board. It should be portable to any Single Board computer that 
runs Linux and Java 8.

The aim of this library is to encapsulate real-world devices as classes with meaningful operation 
names, for example, LED (on / off), LDR (get luminosity), Button (pressed / released), Motor 
(forward / backwards / left / right).

> Pin Numbering 
>{: .admonition-title }
>
> All pin numbers are device native, i.e. Broadcom for the Raspberry Pi, ASUS for the Tinker Board. Pin layouts:
> 
> * [Raspberry pi](https://pinout.xyz/).
> * [CHIP pin numbering](http://www.chip-community.org/index.php/Hardware_Information).
> * [Odroid C2 pin layout](http://www.hardkernel.com/main/products/prdt_info.php?tab_idx=2).
> * [BeagleBone Black](http://beagleboard.org/support/bone101).
> * [ASUS Tinker Board](https://www.asus.com/uk/Single-board-Computer/TINKER-BOARD/).
{: .admonition .note }

The library makes use of [try-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) -
all devices implement `Closeable` hence will get automatically closed by the
`try (Device d = new Device()) { d.doSomething(); }` statement. This is best illustrated by some 
simple examples.

LED control:

```java
try (LED led = new LED(18)) {
	led.toggle();
	SleepUtil.sleepSeconds(.5);
	led.toggle();
	SleepUtil.sleepSeconds(.5);
}
```

Turn on an LED when you press a button:

```java
try (Button button = new Button(12); LED led = new LED(18)) {
	button.whenPressed(nanoTime -> led.on());
	button.whenReleased(nanoTime -> led.off());
	SleepUtil.sleepSeconds(10);
}
```

Or a random LED flicker effect:

```java
Random random = new Random();
try (PwmLed led = new PwmLed(18)) {
	DioZeroScheduler.getNonDaemonInstance().invokeAtFixedRate(RANDOM::nextFloat, led::setValue, 50, 50, TimeUnit.MILLISECONDS);
}
```

## Package Heirarchy

This following image illustrates the relationships between the core packages within diozero.

![Package Hierarchy](/assets/images/Packages.png "Package Hierarchy") 

## Getting Started

Snapshot builds of the library are available in the [Nexus Repository Manager](https://oss.sonatype.org/index.html#nexus-search;gav~com.diozero~~~~).
A ZIP file of diozero and all dependencies can also be downloaded via the [diozero-distribution artifact on Nexus](https://oss.sonatype.org/index.html#nexus-search;gav~com.diozero~diozero-distribution~~~~kw,versionexpand).

Javadoc for the core library is also available via [javadoc.io](http://www.javadoc.io/doc/com.diozero/diozero-core/). 

Java doesn't provide a convenient deployment-time dependency manager 
such as Python's `pip` therefore you will need to manually download all dependencies 
and setup your classpath correctly. You can do this either via setting the `CLASSPATH` 
environment variable or as a command-line option (`java -cp <jar1>:<jar2>`). 
The dependencies have been deliberately kept to as few libraries as possible - diozero is only dependent on [tinylog v2](http://www.tinylog.org).

To compile a diozero application you will need 3 JAR files - [tinylog](http://www.tinylog.org/) (both API and Impl), and diozero-core. 
To run a diozero application, you can also optionally include one of the supported device provider 
libraries and the corresponding diozero provider wrapper library. Note that the built-in default
device provider gives maximum portability but has some limitations such as not supporting hardware
PWM on the Raspberry Pi. If you need hardware PWM on a Raspberry Pi then you must use the pigpioj provider.

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
