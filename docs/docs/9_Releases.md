---
nav_order: 9
permalink: /releases.html
redirect_from:
  - /en/latest/RELEASE/index.html
  - /en/stable/RELEASE/index.html
  - /internals/releases.html
---

# Release History

## Future

+ Complete MAX30102
+ Tidy up ADS112C04
+ Tidy up IMU devices
+ ST7735 / ST7789 displays
+ Stepper motors
+ Tidy up sampleapps with sub-packages as per diozero core (api, devices, motor, util, sbc, ...)
* SPI support for Arduino devices over Firmata

## 1.3.1 (in progress)

+ Fully removed dependency on Firmata4j + plus fixes to the built-in implementation
+ Fix for SerialDevice shutdown
+ Waveshare 7.5" e-ink display
+ Pi PWM - support use of PWM kernel module
+ Switch from Apache Commons Math to [Hipparchus](https://hipparchus.org/index.html) (ref [Hipparchus vs Apache Commons Math](https://www.orekit.org/mailing-list-archives/orekit-users/msg00031.html))
+ Servo - add as a first-class device type (as supported by PiconZero and Arduino)
+ Added Diozero.getVersion

## 1.3.0

+ I2C read-write support with the pigpio provider (JNI and sockets) - pigpioj v2.5.9
+ I2C read-write utility method
+ Introduced diozero.util.Diozero as a more logical place for initialisation and shutdown utilities
+ Replace all of the diozero-remote protocol variants with just gRPC
+ Switch from Firmata4j to the Diozero Firmata implementation (to use the builtin serial library rather than java-native/JSSC)
+ Add getGpioMode(gpio) and getGpioValue(gpio) to NativeDeviceFactoryInterface so that GpioReadAll works without mmap
+ Add remote CPU temperature to NativeDeviceFactoryInterface and gRPC protocol

## 1.2.3

+ Use of BlockingQueue within GpioChip
+ Support for Orange Pi Zero Plus and Orange Pi One Plus
* MFRC522 bug fix (#71) - allow antenna gain to be set prior to turning on the antenna
+ Support for BME688
+ Improvements to SGP30
+ Moved DebouncedDigitalInputDevice out of sandpit

## 1.2.2

+ Docker based build system for system-utils-native
+ Separate libraries for armv6 and armv7
+ DigitalOutputDevice:
    + Renamed setValueUnsafe to just setValue - the unsafe was a legacy from when the class had synchronisation
    + setOn/off no longer calls stopOnOffLoop - it is now the caller's responsibility to do this
+ MmapIntBuffer.intBuffer is no longer volatile, need to validate with an oscilloscope
+ Use of ThreadLocalRandom rather than Random where possible
+ Added a call to start() when reopening a device factory (#70)
+ Bug-fix to NativeGpioInputDevice when setting pud value - line offset was being used rather than gpio (#70)
+ Added diozero-bom

## 1.2.1

+ Tweaked mmap for 64-bit systems - map pointer was an int, changed to long
+ mmap int buffer is volatile to ensure main memory is written
+ DigitalOutputDevice on-off loop enhancements courtesy of Greg Flurry
+ gpio chardev cleanup improvements
* Fixing DiozeroScheduler - pool size of 0 to allow safe shutdown

## 1.2.0

+ Moved all IMU devices out of diozero-core to diozero-imu-devices to remove dependency on commons-math3 (Quaternion) from diozero-core
+ Removed deprecated methods in DigitalInputDevice and MmapIntBuffer and the deprecated class WaitableDigitalInputDevice
+ Added BoardPinInfo getByPhysicalPin()
+ Added AnalogOutputDevice
+ Switched to non-daemon threads
* Changed DiozeroScheduler to only create a single (scheduled) thread pool - the cached thread pool was blocking the JVM from terminating for 60 seconds
+ Improvements to DigitalOutputDevice on-off loops

## 1.1.9

+ Tweaks to native library cross compilation
+ #68 bug-fix for I2C on 64-bit systems
+ Fix to SystemInformation coloured output with Jansi on armv6

## 1.1.8

+ Shutdown logic moved to DeviceFactoryHelper.shutdown
+ Deprecated WaitableDigitalInputDevice and moved waitable logic up to AbstractDigitalInputDevice
+ New Debounced DigitalInputDevice added to api.sandpit
+ Added Builder to Button
+ Use mmap (if available) when getting / setting GPIO values using built-in sysfs
+ Ensure all nano timestamp values use CLOCK_MONOTONIC rather than CLOCK_REALTIME

## 1.1.7

+ Fixed system-utils-native library for aarch64
+ Added PWM setFrequency operation
+ Detect RPi400 and CM4

## 1.1.6

+ Added operation to allow AutoCloseable interfaces to be automatically invoked on shutdown

## 1.1.5

+ Refactored the way that GPIO devices are provisioned internally - all now use PinInfo rather than a GPIO number
+ DigitalInputDevices allow activeHigh to be overridden
+ Fix for Raspberry Pi Zero W - now loads the correct boarddefs file, rather than the generic Raspberry Pi one
+ Bugix for PwmOutputDevice cleanup

## 1.1.4

+ Bugfix for BME280 humidity reads - config registers need to be written to in the correct order
+ Enhancements to ADS112C04 - support for input multiplexer configuration (differential reads)
+ Renamed util.Event to util.EventLock
+ Preview / sandpit introduction of a new generic FIFO event queue

## 1.1.3

+ Bugfix release - fix for I2C retry logic
+ GPIO event Epoch time is calculated from nano time

## 1.1.2

+ Bugfix release - fixes to GPIO
+ I2C retries

## 1.1.1

+ Changed whenPressed / whenReleased to receive nano time rather than epoch time

## 1.1.0

+ Changed whenPressed / whenReleased to receive epoch time; removed readBlocked, minReadChars and readTimeout from SerialDevice constructors

## 1.0.1

+ Minor bug fix to support Raspberry Pi 1 Model B
+ Introduce readFully method for serial devices to read specified number of bytes before returning

## 1.0.0

+ Refactored package hierarchy to clearly distinguish between devices, API and SPI
+ Documentation update - moved from readthedocs to GitHub pages

## 0.14

+ Serial devices
+ gpiod character device incorporated
+ Renamed the "sysfs" internal provider to built-in

## 0.13

+ Upgrade dependencies, minor bug fixes

## 0.12

+ Bug fix release - ws281x LED strips and analog input on BBB

## 0.11

+ New devices: BH1750 luminosity sensor, SSD1331 96x64 & SSD1351 128x128 65k colour OLEDs.
+ Moved classes in com.diozero to com.diozero.devices.
+ Changed the SPI interface from ByteBuffer to byte array.
+ Added support for Particle Photon using the VoodooSpark Firmware.
+ Flexible APIs for remotely accessing devices over a variety of protocols.
+ Optimised GPIO input interrupt handling.
+ Updated rpi_ws281x library.
+ Experimental support for 433MHz receivers.
+ I2C detect capability.

## 0.10

+ Firmata I2C.
+ Improvements to MFRC522.
+ SDL Joystick JNI wrapper library.
+ MFRC522 fully supported (finally).
+ Added support for MCP EEPROMs.
+ I2C SMBus implementation in the internal sysfs provider.

## 0.9

+ Native support for I2C and SPI in the sysfs provider.
+ Support for CHIP, BeagleBone Black and ASUS Tinker Board.
+ Moved sysfs provider into diozero-core, use as the default provider.
+ Preliminary support for devices that support the Firmata protocol (i.e. Arduino).

## 0.8

+ Added Analog Output device support (added for the PCF8591).
+ Introduced Java based sysfs and jpi providers. Bug fix to I2CLcd.
+ Added support for BME280.

## 0.7

+ Support for non-register based I2C device read / write.

## 0.6

+ Stability improvements.

## 0.5

+ Testing improvements.

## 0.4

+ Bug fixes, experimental servo support.

## 0.3

+ API change - analogue to analog.

## 0.2

+ First tagged release.
