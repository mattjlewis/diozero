---
title: Utility Classes
parent: API
nav_order: 5
permalink: /api/utilityclasses.html
---

# Utility Classes

## com.diozero.sbc

The [com.diozero.sbc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/sbc/package-summary.html)
package contains classes for interfacing with Single Board Board Computers, most notably the
[DeviceFactoryHelper](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/sbc/DeviceFactoryHelper.html)
class which provides utility methods for accessing the automatically detected device factory for the
local SBC on which your application is running.

The [Diozero](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/Diozero.html) class contains utility methods for graceful shutdown.

The other key class in this package is
[BoardPinInfo](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/sbc/BoardPinInfo.html)
which provides various methods for accessing
[PinInfo](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/api/PinInfo.html)
instances, including by physical pin, GPIO character device chip and line offset, as well as GPIO
number. The BoardPinInfo instance for your board should always be accessed via the device factory's
[getBoardPinInfo()](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/internal/spi/DeviceFactoryInterface.html#getBoardPinInfo())
method.

For example, get by GPIO character device chip and line offset:

```java
// GPIO 18 on the Raspberry Pi
int chip = 0;
int line_offset = 18;
PinInfo pin_info = DeviceFactoryHelper.getNativeDeviceFactory().getBoardPinInfo()
		.getByChipAndLineOffsetOrThrow(chip, line_offset);
try (LED led = new LED(pin_info, true, false)) {
	led.on();
	SleepUtil.sleepSeconds(1);
	led.off()
}
```

Get by physical pin:
```java
// GPIO 18 on the Raspberry Pi
String header = "J8";
int physical_pin = 12;
PinInfo pin_info = DeviceFactoryHelper.getNativeDeviceFactory().getBoardPinInfo()
		.getByPhyscialPinOrThrow(header, physical_pin);
try (LED led = new LED(pin_info, true, false)) {
	led.on();
	SleepUtil.sleepSeconds(1);
	led.off()
}
```

## com.diozero.util

The [com.diozero.util](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/package-summary.html)
package contains general utility classes, including:

[BitManipulation](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/BitManipulation.html)
: Interacting with individual bits within a byte.

[ColourUtil](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/ColourUtil.html)
: Currently contains one method for generating a colour value using the 5-6-5 colourspace from individual RGB values.

[Crc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/Crc.html)
: CRC-8 and CRC-16 calculator - supports _all_ [common configurations](https://crccalc.com).

[Diozero](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/Diozero.html)
: Utility methods for initialisation and graceful shutdown.

[DiozeroScheduler](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/DiozeroScheduler.html)
: Wrapper around the Java ExecutorService and ScheduledExecutorService; provided to enable graceful shutdown when using non-daemon threads.

[Hex](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/Hex.html)
: Utility methods for encoding and decoding hex strings.

[IOUtil](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/IOUtil.html)
: Input / output utility methods.

[MmapIntBuffer](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/MmapIntBuffer.html)
: Wrap an system file into a memory mapped java.nio.IntBuffer object for high performance direct
memory mapped access. Used for interacting with GPIO registers in `/dev/gpiomem`.

[MutableByte](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/MutableByte.html)
: A mutable byte object with operations for getting / setting individual bit values.

[PropertyUtil](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/PropertyUtil.html)
: Utility class for accessing system properties that are set either as environment variables or as `-D` command line flags.
Options set via command line flags take precedence over environment variables.

[RangeUtil](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/RangeUtil.html)
: Map and constrain values between different ranges. Useful when converting percentage values in the
range 0..1 to the 16-bit digital equivalent for ADCs and DACs.

[ServoUtil](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/ServoUtil.html)
: Utility methods for calculating servo pulse values.

[SleepUtil](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/SleepUtil.html)
: Wrap the Java Thread.sleep method with explicit options for sleeping for seconds or milliseconds.
Also contains a busySleep method for finer grained delays.

[StringUtil](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/StringUtil.html)
: Helper methods for padding a string and repeating a character.

[UsbInfo](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/util/UsbInfo.html)
: Lookup USB vendor and product names from corresponding id values using `/var/lib/usbutils/usb.ids`.
