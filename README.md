# DIOZero - a Java Device I/O wrapper for GPIO / I2C / SPI control

[![Build Status](https://travis-ci.org/mattjlewis/diozero.svg?branch=master)](https://travis-ci.org/mattjlewis/diozero)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.diozero/diozero/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.diozero/diozero)
[![Maven Central status](https://img.shields.io/maven-central/v/com.diozero/diozero.svg)](https://search.maven.org/search?q=g:com.diozero)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Javadoc](https://www.javadoc.io/badge/com.diozero/diozero-core.svg)](https://www.javadoc.io/doc/com.diozero/diozero-core)
[![Documentation Status](https://readthedocs.org/projects/diozero/badge/?version=latest)](http://diozero.readthedocs.io/en/latest/?badge=latest)

A Device I/O library written in Java that provides an object-orientated interface for a range of 
GPIO / I2C / SPI devices (LEDs, buttons, sensors, motors, displays, etc) connected to Single 
Board Computers like the Raspberry Pi. Actual GPIO / I2C / SPI device communication is delegated 
to pluggable service providers for maximum compatibility across different boards. This library is 
known to work on the following boards: all models of the Raspberry Pi, Odroid C2, BeagleBone 
(Green and Black), C.H.I.P and ASUS Tinker Board. It should be portable to any Single Board computer that 
runs Linux and Java 8.

This library makes use of modern Java features such as 
[automatic resource management](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html), 
[Lambda Expressions](https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html) and 
[Method References](https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html) 
where they simplify development and improve code readability.

Created by [Matt Lewis](https://github.com/mattjlewis) (email [deviceiozero@gmail.com](mailto:deviceiozero@gmail.com)) ([blog](https://diozero.blogspot.co.uk/)), 
inspired by [GPIO Zero](https://gpiozero.readthedocs.org/) and [Johnny Five](http://johnny-five.io/). 
If you have any issues please use the [GitHub issues page](https://github.com/mattjlewis/diozero/issues). 
For any other comments or suggestions, please use the [diozero Google Group](https://groups.google.com/forum/#!forum/diozero).

Please refer to the [GitHub Pages site](https://github.diozero.com/) for further documentation.
