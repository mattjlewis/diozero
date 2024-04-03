---
title: diozero
nav_order: 1
permalink: /
author: "Matthew Lewis"
date: 2024-03-20
redirect_from:
  - /en/latest/index.html
  - /en/stable/index.html
---
# diozero

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Maven CI Build](https://github.com/mattjlewis/diozero/actions/workflows/build.yml/badge.svg)](https://github.com/mattjlewis/diozero/actions/workflows/build.yml)
[![Maven Central Status](https://img.shields.io/maven-central/v/com.diozero/diozero.svg)](https://search.maven.org/search?q=g:com.diozero)
[![Javadoc](https://www.javadoc.io/badge/com.diozero/diozero-core.svg)](https://www.javadoc.io/doc/com.diozero/diozero-core)

A device I/O library implemented in Java that is portable across Single Board Computers and
micro-controllers to provide an intuitive and frictionless way to get started with physical
computing.

Example:

```java
try (LED led = new LED(18)) {
  led.on();
  SleepUtil.sleepSeconds(1);
  led.off();
  SleepUtil.sleepSeconds(1);
  led.toggle();
}
```

Components can easily be connected together, e.g.:

```java
try (Button button = new Button(12); LED led = new LED(18)) {
  button.whenPressed(nanoTime -> led.on();
  button.whenReleased(nanoTime -> led.off());
  SleepUtil.sleepSeconds(20);
}
```

As well as providing APIs for interfacing directly with physical hardware (i.e.
[GPIO](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/DigitalOutputDevice.java),
[I<sup>2</sup>C](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/I2CDevice.java),
[SPI](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/SpiDevice.java) and
[Serial](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/SerialDevice.java)),
diozero also provides support for simple devices including [LDRs](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/devices/LDR.java),
[Buttons](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/devices/Button.java), and
[Motors](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/devices/motor/MotorBase.java)
through to complex environmental sensors such as the
[Bosch Sensortec Gas Sensor](https://www.bosch-sensortec.com/products/environmental-sensors/gas-sensors-bme680/)
[BME60](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/devices/BME680.java).

This library makes use of modern Java features such as 
[automatic resource management](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html), 
[Lambda Expressions](https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html) and 
[Method References](https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html) 
to simplify development and improve code readability.

## Supported Boards

diozero has built-in support for the following Single Board Computers and micro-controllers. Its
adoption of common userspace APIs means that it _should_ work on all SBCs that can run Linux and all
micro-controllers that can support the [Firmata protocol](https://github.com/firmata/protocol).

* [Raspberry Pi](https://www.raspberrypi.org/) (_all_ versions + tested on Raspberry Pi OS 32-bit and 64-bit as well as Ubuntu Server 64-bit).
* [Odroid C2](https://wiki.odroid.com/odroid-c2/odroid-c2) (Armbian 64-bit) - Amlogic S905 SoC.
* [Odroid N2](https://wiki.odroid.com/odroid-n2/odroid-n2) (Armbian 64-bit) - Amlogic S922X SoC.
* [BeagleBone Green / Black](https://beagleboard.org/black).
* [ASUS TinkerBoard](https://www.asus.com/uk/Single-board-Computer/TINKER-BOARD/) (ASUS TinkerOS/Linaro as well as Armbian 64-bit) - Rockchip RK3288 SoC.
* [Radxa Rock 4C+](https://wiki.radxa.com/Rock4/4cplus) - Rockchip RK3299/RK3299T SoC.
* [Allwinner H3](https://linux-sunxi.org/H3) boards, including [NanoPi Neo](https://www.friendlyarm.com/index.php?route=product/product&product_id=132)
and [NanoPi Duo 2](https://www.friendlyelec.com/index.php?route=product/product&product_id=244) (Armbian 32-bit).
* [Allwinner H5](https://linux-sunxi.org/H5) boards, e.g. [OrangePi Zero+](http://www.orangepi.org/OrangePiZeroPlus/).
* [Allwinner H6](https://linux-sunxi.org/H6) boards, e.g. [OrangePi One+](http://www.orangepi.org/OrangePiOneplus/),
[Orange Pi 3 LTS](http://www.orangepi.org/html/hardWare/computerAndMicrocontrollers/details/orange-pi-3-LTS.html)
* [The Next Thing Co CHIP](https://getchip.com/pages/chip) - Allwinner R8 SoC.
* [Arduino compatible](https://www.arduino.cc) (any device that can run [Firmata](http://firmatabuilder.com)).
* [Raspberry Pi Pico](https://www.raspberrypi.com/products/raspberry-pi-pico/) (Firmata over serial).
* [ESP8266](https://www.espressif.com/en/products/socs/esp8266) (via Firmata over Serial, Bluetooth or WiFi).
* [ESP32](https://www.espressif.com/en/products/socs/esp32) (Firmata).
* [Particle Spark](https://docs.particle.io/datasheets/discontinued/core-datasheet/) (using [Voodoo Spark](https://github.com/voodootikigod/voodoospark)).

## Usage

diozero requires Java 11 or later - it has been tested on all JDK versions from Java 11 through to 17. Most
use cases will only require the diozero-core JAR file which has just one other dependency -
[tinylog](https://tinylog.org/v2/).

The best way to use diozero is via a Maven dependency:

```xml
<dependency>
    <groupId>com.diozero</groupId>
    <artifactId>diozero-core</artifactId>
    <version>{{ site.version }}</version>
</dependency>
```

You can initialise your own application using the `diozero-application`
[Maven archetype](https://maven.apache.org/guides/introduction/introduction-to-archetypes.html):

```
mvn archetype:generate -DinteractiveMode=false \
  -DarchetypeGroupId=com.diozero \
  -DarchetypeArtifactId=diozero-application \
  -DarchetypeVersion={{ site.version }} \
  -DgroupId=com.mycompany \
  -DartifactId=mydiozeroproject \
  -Dversion=1.0-SNAPSHOT
```

A full distribution ZIP file containing all JARs and associated dependencies is also available from
[Maven Central](https://search.maven.org/) - locate
[com.diozero:diozero-distribution](https://search.maven.org/artifact/com.diozero/diozero-distribution),
select a version and click the "[bin.zip](https://search.maven.org/remotecontent?filepath=com/diozero/diozero-distribution/{{ site.version }}/diozero-distribution-{{ site.version }}-bin.zip)" option in the Downloads link top right.
It is also available in [mvnrepository](https://mvnrepository.com/) by locating [diozero-distribution](https://mvnrepository.com/artifact/com.diozero/diozero-distribution), selecting a version and clicking the Files [View All](https://repo1.maven.org/maven2/com/diozero/diozero-distribution/{{ site.version }}) link.

## Articles

* [Building Portable Binaries for Single Board Computers with diozero and GraalVM](https://deviceiozero.medium.com/building-portable-binaries-for-single-board-computers-with-diozero-and-graalvm-b659f79d5c71)
* [Achieving Portability Across Single Board Computers and Micro-controllers](https://deviceiozero.medium.com/tbc)
* [Lego Car Controlled by Raspberry Pi and PS3 Controller](http://mattandshirl.blogspot.com/2013/06/lego-car-controlled-by-raspberry-pi-and.html) - now ported to Java

## Development

Created by [Matt Lewis](https://github.com/mattjlewis) (email [deviceiozero@gmail.com](mailto:deviceiozero@gmail.com))
([blog](https://diozero.blogspot.co.uk/)), inspired by [GPIO Zero](https://gpiozero.readthedocs.org/)
and [Johnny Five](http://johnny-five.io/). 
If you have any issues, comments or suggestions please use the [GitHub issues page](https://github.com/mattjlewis/diozero/issues).

This project is hosted on [GitHub](https://github.com/mattjlewis/diozero/), please feel free to join in:

* Make suggestions for [fixes and enhancements](https://github.com/mattjlewis/diozero/issues)
* Provide sample applications and device implementation classes
* Contribute to development

[Release History](7_internals/98_Releases.md)

This work is provided under the [MIT License](https://github.com/mattjlewis/diozero/tree/master/LICENSE.txt).

## Credits

* [pigpio](https://abyz.me.uk/rpi/pigpio/)
* [gpiozero](https://gpiozero.readthedocs.io/en/stable/)
* [Johnny Five](http://johnny-five.io)
* [rpi_ws281x](https://github.com/jgarff/rpi_ws281x)
* Many more...
