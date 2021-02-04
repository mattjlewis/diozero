---
title: diozero
nav_order: 1
permalink: /
has_toc: true
---

# diozero
{: .no_toc }

[![Build Status](https://travis-ci.org/mattjlewis/diozero.svg?branch=master)](https://travis-ci.org/mattjlewis/diozero)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.diozero/diozero/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.diozero/diozero)
[![Maven Central status](https://img.shields.io/maven-central/v/com.diozero/diozero.svg)](https://search.maven.org/search?q=g:com.diozero)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Javadoc](https://www.javadoc.io/badge/com.diozero/diozero-core.svg)](https://www.javadoc.io/doc/com.diozero/diozero-core)

A Device I/O library implemented in Java that is portable across Single Board Computers to
provide an intuitive and frictionless way to get started with physical computing.

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
  button.whenPressed(epochTime -> led.on();
  button.whenReleased(epochTime -> led.off());
  SleepUtil.sleepSeconds(20);
}
```

As well as providing interfaces for interacing directly with physical hardware (i.e.
[GPIO](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/DigitalOutputDevice.java),
[I2C](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/I2CDevice.java),
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
to development and improve code readability.

## Supported Boards

diozero has out of the box support for the following Single Board Computers and micro-controllers:

* [Raspberry Pi](http://www.raspberyrpi.org/) (all versions).
* [Odroid C2](https://wiki.odroid.com/odroid-c2/odroid-c2).
* [BeagleBone Green / Black](https://beagleboard.org/black).
* [ASUS TinkerBoard](https://www.asus.com/uk/Single-board-Computer/TINKER-BOARD/).
* [Allwinner H3](https://linux-sunxi.org/H3) boards, including [NanoPi Neo](https://www.friendlyarm.com/index.php?route=product/product&product_id=132) and [NanoPi Duo](https://www.friendlyarm.com/index.php?route=product/product&product_id=244).
* [The Next Thing Co CHIP](https://getchip.com/pages/chip).
* [Arduino compatible](https://www.arduino.cc) (any device that can run [Standard Firmata](https://github.com/firmata/arduino/blob/master/examples/StandardFirmata/StandardFirmata.ino)).
* [ESP8266](https://www.espressif.com/en/products/socs/esp8266) / [ESP32](https://www.espressif.com/en/products/socs/esp32) (via [Standard Firmata WiFi](https://github.com/firmata/arduino/tree/master/examples/StandardFirmataWiFi)).
* [Particle Spark](https://docs.particle.io/datasheets/discontinued/core-datasheet/) (using [Voodoo Spark](https://github.com/voodootikigod/voodoospark)).

## Maven Dependency / Download Link

Maven dependency:
```xml
<dependency>
    <groupId>com.diozero</groupId>
    <artifactId>diozero-core</artifactId>
    <version>{{ site.version }}</version>
</dependency>
```

Create your own application using the diozero-application Maven archetype:
```
mvn archetype:generate -DarchetypeGroupId=com.diozero -DarchetypeArtifactId=diozero-application -DgroupId=com.mycompany -DartifactId=mydiozeroproject -Dversion=1.0-SNAPSHOT
```

A distribution ZIP file containing all JARs and their dependencies is also available via [Maven Central](https://search.maven.org/) -
locate [com.diozero:diozero-distribution](https://search.maven.org/artifact/com.diozero/diozero-distribution),
select a version and click the "[bin.zip](https://search.maven.org/remotecontent?filepath=com/diozero/diozero-distribution/{{ site.version }}/diozero-distribution-{{ site.version }}-bin.zip)" option in the Downloads link top right.
It is also available in [mvnrepository](https://mvnrepository.com/) by locating [diozero-distribution](https://mvnrepository.com/artifact/com.diozero/diozero-distribution), selecting a version and clicking the Files [View All](https://repo1.maven.org/maven2/com/diozero/diozero-distribution/{{ site.version }}) link.

## Development

Created by [Matt Lewis](https://github.com/mattjlewis) (email [deviceiozero@gmail.com](mailto:deviceiozero@gmail.com))
([blog](https://diozero.blogspot.co.uk/)), 
inspired by [GPIO Zero](https://gpiozero.readthedocs.org/) and [Johnny Five](http://johnny-five.io/). 
If you have any issues, comments or suggestions please use the [GitHub issues page](https://github.com/mattjlewis/diozero/issues).

This project is hosted on [GitHub](https://github.com/mattjlewis/diozero/), please feel free to join in:

* Make suggestions for [fixes and enhancements](https://github.com/mattjlewis/diozero/issues)
* Provide sample applications and device implementation classes
* Contribute to development

[Release History](7_internals/2_Releases.md)

This work is provided under the [MIT License](https://github.com/mattjlewis/diozero/tree/master/LICENSE.txt).
