---
parent: Single Board Computers
nav_order: 6
permalink: /boards/opisetup.html
redirect_from:
  - /internal/opisetup.html
---

# OrangePi Setup

Armbian setup notes.

TBD udev rules for /dev/gpiochip* file permissions.

Along the lines of:
File `/etc/udev/rules.d/70-gpio.rules`:

```
# Allow group gpio to access gpiochip files
SUBSYSTEM=="gpio", GROUP="gpio", MODE="0660"

# To allow additional features like edge detection
SUBSYSTEM=="gpio*", PROGRAM="/bin/sh -c '\
  chown -R root:gpio /sys/class/gpio && chmod -R 770 /sys/class/gpio;\
  chown -R root:gpio /sys/devices/virtual/gpio && chmod -R 770 /sys/devices/virtual/gpio;\
  chown -R root:gpio /sys$devpath && chmod -R 770 /sys$devpath\
'"
```

## WiFi / I<sup>2</sup>C / SPI

Use `armbian-config`.

## DTB

```
dtc -I dtb -O dts -o sun50i-h5-orangepi-zero-plus.dts /boot/dtb/allwinner/sun50i-h5-orangepi-zero-plus.dtb
dtc -I dtb -O dts -o sun50i-h5-orangepi-one-plus.dts /boot/dtb/allwinner/sun50i-h5-orangepi-one-plus.dtb
```
