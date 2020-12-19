---
parent: Internals
nav_order: 5
permalink: /internals/gpio.html
---

By default diozero uses the Linux GPIO Character Device implementation that was added in Linux kernel 4.8.
This can be disabled by running with the property `diozero.gpio.chardev=false`, in which case
diozero will revert to the legacy and deprecated [sysfs GPIO](https://www.kernel.org/doc/Documentation/gpio/sysfs.txt) interface.
This property can be set via either the command line (`java -Ddiozero.gpio.chardev=false`) or as an environment property (`export diozero.gpio.chardev=false`).

On Debian based systems the gpiod command line tools can be installed using `sudo apt install gpiod` (gpiodetect, gpioinfo, gpioget, gpioset and gpiomon). These tools can be helpful when troubleshooting.

Note that the board's Linux kernel has to be at version 4.8 or later to support GPIO Character Device. This [page](https://embeddedbits.org/new-linux-kernel-gpio-user-space-interface/) provides a good description of the Linux Kernel user-space GPIO interfaces.

Unfortunately the version of gpiod available in Linux kernels prior to 5.5 [do not provide support for pull-up / pull-down resistors](https://microhobby.com.br/blog/2020/02/02/new-linux-kernel-5-5-new-interfaces-in-gpiolib/). The latest stable kernel version officially available in Debian Buster is 4.19, at the time of diozero 1.0.0 Raspbian and BeagleBone both provide stable 5.4 based Linux Kernels.

To address this, diozero will its own internal memory mapped GPIO implementation on boards for which it has been implemented and where the current user has access to the corresponding memory file (`/dev/gpiomem` or `/dev/mem`).

diozero has memory mapped GPIO support for following boards:

* Raspberry Pi (all flavours)
* FriendlyArm H3 / Allwinner Sun8i CPU (as used in the NanoPi Duo2 / NanoPi Neo amongst others)
* Odroid C2
* ASUS Tinkerboard
* Next Think Co CHIP (Allwinner sun4i/sun5i)

I will add memory mapped support for the BeagleBone Green / Black in the near future.

Note that the pigpio provider does support configuring the pull-up / pull-down resistors on the Raspberry Pi.
