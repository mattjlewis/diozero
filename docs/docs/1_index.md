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
[![Documentation Status](https://readthedocs.org/projects/diozero/badge/?version=latest)](http://diozero.readthedocs.io/en/latest/?badge=latest)

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
  button.whenPressed(led::on);
  button.whenReleased(led::off);
  SleepUtil.sleepSeconds(20);
}
```

As well as providing interfaces for interacing directly with physical hardware (i.e. GPIO, I2C,
SPI and Serial), diozero also provides support for common devices ranging from simple LDRs,
buttons, motors through to complex environmental sensors such as the
[Bosch Sensortec Gas Sensor BME60](https://www.bosch-sensortec.com/products/environmental-sensors/gas-sensors-bme680/).

This library makes use of modern Java features such as 
[automatic resource management](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html), 
[Lambda Expressions](https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html) and 
[Method References](https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html) 
where they simplify development and improve code readability.

Maven dependency:
```xml
<dependency>
    <groupId>com.diozero</groupId>
    <artifactId>diozero-core</artifactId>
    <version>0.14</version>
</dependency>
```

Created by [Matt Lewis](https://github.com/mattjlewis) (email [deviceiozero@gmail.com](mailto:deviceiozero@gmail.com)) ([blog](https://diozero.blogspot.co.uk/)), 
inspired by [GPIO Zero](https://gpiozero.readthedocs.org/) and [Johnny Five](http://johnny-five.io/). 
If you have any issues, comments or suggestions please use the [GitHub issues page](https://github.com/mattjlewis/diozero/issues).

## Supported Boards

diozero has out of the box support for the following Single Board Computers and micro-controllers:

* [Raspberry Pi](http://www.raspberyrpi.org/) (all versions).
* [Odroid C2](https://wiki.odroid.com/odroid-c2/odroid-c2).
* [BeagleBone Green / Black](https://beagleboard.org/black).
* [Asus TinkerBoard](https://www.asus.com/uk/Single-board-Computer/TINKER-BOARD/).
* [Allwinner H3](https://linux-sunxi.org/H3) boards, including [NanoPi Neo](https://www.friendlyarm.com/index.php?route=product/product&product_id=132) and [NanoPi Duo](https://www.friendlyarm.com/index.php?route=product/product&product_id=244).
* [The Next Thing Co CHIP](https://getchip.com/pages/chip).
* [Arduino compatible](https://www.arduino.cc) (any device that can run [Standard Firmata](https://github.com/firmata/arduino/blob/master/examples/StandardFirmata/StandardFirmata.ino)).
* [ESP8266](https://www.espressif.com/en/products/socs/esp8266) / [ESP32](https://www.espressif.com/en/products/socs/esp32) (via [Standard Firmata WiFi](https://github.com/firmata/arduino/tree/master/examples/StandardFirmataWiFi)).
* [Particle Spark](https://docs.particle.io/datasheets/discontinued/core-datasheet/) (using [Voodoo Spark](https://github.com/voodootikigod/voodoospark)).

## Development

This project is hosted on [GitHub](https://github.com/mattjlewis/diozero/), please feel free to join in:

* Make suggestions for [fixes and enhancements](https://github.com/mattjlewis/diozero/issues)
* Provide sample applications and device implementation classes
* Contribute to development

[Release History](2_Releases.md)

## License

This work is provided under the [MIT License](license.md).
