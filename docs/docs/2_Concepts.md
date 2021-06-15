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

The aim of this library is to encapsulate real-world devices (LEDs, buttons, sensors, motors,
displays, etc) as classes with meaningful operation names, for example, LED (on / off), LDR
(get luminosity), Button (pressed / released), Motor (forward / backwards / left / right).

This library is known to work on the following boards: all models of the Raspberry Pi, Odroid C2,
BeagleBone (Green and Black), Next Thing C.H.I.P and ASUS Tinker Board and is portable to any
Single Board Computer that can run Linux and Java 8.

{: .note-title }
> Pin Numbering 
>
> All pin numbers are device native, i.e. Broadcom for the Raspberry Pi, ASUS for the Tinker Board. Pin layouts:
> 
> * [Raspberry pi](https://pinout.xyz/).
> * [CHIP pin numbering](http://www.chip-community.org/index.php/Hardware_Information).
> * [Odroid C2 pin layout](https://wiki.odroid.com/odroid-c2/hardware/expansion_connectors).
> * [BeagleBone Black](http://beagleboard.org/support/bone101#headers).
> * [ASUS Tinker Board](https://www.asus.com/uk/motherboards-components/single-board-computer/all-series/tinker-board/#tinker-board-Hardware).

diozero implements a layered architecture so as to provide maximum portability:

![diozero layers](/assets/images/Layers.png "diozero layers")

[Device API](4_Devices.md)
: Refers to classes in the [com.diozero.devices](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/devices/package-summary.html)
package that are designed to represent physical devices, such as an LED, and are to be used by
diozero applications.
All of the classes in [com.diozero.devices](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/devices/package-summary.html)
rely exclusively on the [com.diozero.api](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/api/package-summary.html)
package for GPIO, I2C, SPI, and Serial communication.

[Base I/O API](3_API.md)
: Classes and interfaces in the [com.diozero.api](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/api/package-summary.html)
package for doing GPIO, I2C, SPI, and Serial communication. These classes make use of the Provider
Service Provider Interface layer in the [com.diozero.internal.spi](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/internal/spi/package-summary.html)
package for actual device communication.

[Provider](2_concepts/1_Providers.md)
: All GPIO, I2C, SPI, and Serial device communication is delegated to pluggable device providers
for maximum compatibility across different boards.
The Provider layer is split into two separate aspects (see the
[Providers](2_concepts/1_Providers.md#providers) section for additional details):
1. The Service Provider Interface ([com.diozero.internal.spi](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/internal/spi/package-summary.html)), and
1. Service provider implementations, e.g. the default built-in provider ([com.diozero.internal.provider.builtin](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/internal/provider/builtin/package-summary.html)). 

## Package Heirarchy

This following image illustrates the relationships between the core packages within diozero.

![Package Hierarchy](/assets/images/Packages.png "Package Hierarchy") 

## Clean-up

The library makes use of [try-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) -
all devices implement `AutoCloseable` hence will get automatically closed by the
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

### Shutdown

To protect against unexpected shutdown scenarios, diozero implements a [Shutdown Hook](https://docs.oracle.com/javase/7/docs/api/java/lang/Runtime.html#addShutdownHook(java.lang.Thread))
which will [close all device factories and internal devices](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sbc/Diozero.java#L78).
Custom classes that implement AutoCloseable can also be registered by calling [Diozero.registerForShutown()](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/util/Diozero.html#registerForShutdown(java.lang.AutoCloseable...))
and will be called for shutdown prior to closing the device factories and internal devices.
