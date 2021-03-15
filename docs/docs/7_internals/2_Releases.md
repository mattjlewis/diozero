---
parent: Internals
nav_order: 2
permalink: /internals/releases.html
---

# Releases

## Release 0.2

+ First tagged release.

## Release 0.3

+ API change - analogue to analog.

## Release 0.4

+ Bug fixes, experimental servo support.

## Release 0.5

+ Testing improvements.

## Release 0.6

+ Stability improvements.

## Release 0.7

+ Support for non-register based I2C device read / write.

## Release 0.8

+ Added Analog Output device support (added for the PCF8591).
+ Introduced Java based sysfs and jpi providers. Bug fix to I2CLcd.
+ Added support for BME280.

## Release 0.9

+ Native support for I2C and SPI in the sysfs provider.
+ Support for CHIP, BeagleBone Black and ASUS Tinker Board.
+ Moved sysfs provider into diozero-core, use as the default provider.
+ Preliminary support for devices that support the Firmata protocol (i.e. Arduino).

## Release 0.10

+ Firmata I2C.
+ Improvements to MFRC522.
+ SDL Joystick JNI wrapper library.
+ MFRC522 fully supported (finally).
+ Added support for MCP EEPROMs.
+ I2C SMBus implementation in the internal sysfs provider.

## Release 0.11

+ New devices: BH1750 luminosity sensor, SSD1331 96x64 & SSD1351 128x128 65k colour OLEDs.
+ Moved classes in com.diozero to com.diozero.devices.
+ Changed the SPI interface from ByteBuffer to byte array.
+ Added support for Particle Photon using the VoodooSpark Firmware.
+ Flexible APIs for remotely accessing devices over a variety of protocols.
+ Optimised GPIO input interrupt handling.
+ Updated rpi_ws281x library.
+ Experimental support for 433MHz receivers.
+ I2C detect capability.

## Release 0.12

+ Bug fix release - ws281x LED strips and analog input on BBB

## Release 0.13

+ Upgrade dependencies, minor bug fixes

## Release 0.14

+ Serial devices
+ gpiod character device incorporated
+ Renamed the "sysfs" internal provider to built-in

## Release 1.0.0

+ Refactored package hierarchy to clearly distinguish between devices, API and SPI
+ Documentation update - moved from readthedocs to GitHub pages

## Release 1.0.1

+ Minor bug fix to support Raspberry Pi 1 Model B
+ Introduce readFully method for serial devices to read specified number of bytes before returning

## Release 1.1.0

+ Changed whenPressed / whenReleased to receive epoch time; removed readBlocked, minReadChars and readTimeout from SerialDevice constructors

## Release 1.1.1

+ Changed whenPressed / whenReleased to receive nano time rather than epoch time

## Release 1.1.2

+ Bugfix release - fixes to GPIO
+ I2C retries

## Release 1.1.3

+ Bugfix release - fix for I2C retry logic
+ GPIO event Epoch time is calculated from nano time

## Release 1.1.4

+ Bugfix for BME280 humidity reads - config registers need to be written to in the correct order
+ Enhancements to ADS112C04 - support for input multiplexer configuration (differential reads)
+ Renamed util.Event to util.EventLock
+ Preview / sandpit introduction of a new generic FIFO event queue

## Release 1.1.5

+ Refactored the way that GPIO devices are provisioned internally - all now use PinInfo rather than a GPIO number
+ DigitalInputDevices allow activeHigh to be overridden
+ Fix for Raspberry Pi Zero W - now loads the correct boarddefs file, rather than the generic Raspberry Pi one
+ Bugix for PwmOutputDevice cleanup
