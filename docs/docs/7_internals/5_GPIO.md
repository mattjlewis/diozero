By default diozero uses the Linux GPIO Character Device implementation that was added in kernel 4.8.
This can be disabled by running with the property `diozero.gpio.chardev=false`, in which case
diozero will revert to the legacy and deprecated [sysfs GPIO](https://www.kernel.org/doc/Documentation/gpio/sysfs.txt) interface.
This property can be via either the command line or an environment property.

This command ```sudo apt install gpiod``` installs the gpiod command line tools (gpiodetect, gpioinfo, gpioget, gpioset).

Finally the Linux kernel has to be 4.8+ to support the gpiod system ioctl commands - this means that I can statically link the gpiod library and it works even if you don’t have the gpiod shared library installed.

The mmap provider has no impact on the PinInfo API / implementation. This is part of the magic - the mmap implementation is entirely based on GPIO numbers. When building in the gpiod support I saw where the GPIO numbers actually come from! Basically they relate to the cumulative gpio chip and line offset, so for the Pi, GPIO 18 is chip 0, line 18. The Pi is easy as it has one main gpiochip (0). The TinkerBoard, however, has 9 gpio chips - it was only when adding gpiod support that I worked out the GPIO numbering scheme.

All boards are slightly different, however, there is normally a fairly simply formula to map from GPIO number to the specific 32-bit register number and bits within that register for controlling GPIO direction, pull-up / down config and reading / writing values. E.g. on the Pi, the mode register / bit combo magic is:
	int reg = gpio / 10;
	int shift = (gpio % 10) * 3;
	int mode = (mmapIntBuffer.get(reg) >> shift) & 0xf;