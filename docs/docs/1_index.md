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

## Development

This project is hosted on [GitHub](https://github.com/mattjlewis/diozero/), please feel free to join in:

* Make suggestions for [fixes and enhancements](https://github.com/mattjlewis/diozero/issues)
* Provide sample applications
* Contribute to development

## To-Do

* Thorough testing (various types of devices using each service provider)
* A clean object-orientated API for IMUs
* Remote API for board capabilities
* SPI support for Arduino devices
* Introduce Servo as a device type
* Try out ConfigurableFirmata - is there actually any difference to the StandardFirmata protocol?
* Complete ADSL1x15
* BME680
* DONE Native support for all devices via mmap (/dev/mem), in particular to improve performance and add support for GPIO pull up/down configuration.
* DONE Wireless access to Firmata devices (network and Bluetooth). E.g. [ESP32](https://learn.sparkfun.com/tutorials/esp32-thing-hookup-guide?_ga=1.116824388.33505106.1471290985#installing-the-esp32-arduino-core) [Firmata GitHub issue #315](https://github.com/firmata/arduino/issues/315)

[Release History](RELEASE.md)

## License

This work is provided under the [MIT License](license.md).
