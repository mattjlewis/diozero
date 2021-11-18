---
parent: Internals
nav_order: 8
permalink: /internals/opisetup.html
---

# OrangePi Setup

Armbian setup notes.

Use `visudo` and add `NOPASSWD:` for group `sudo` so that the line looks like this:

```
%sudo	ALL=(ALL:ALL) NOPASSWD: ALL
```

Add gpio group:

```
groupadd gpio
usermod -aG gpio <<your-username>>
```

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

## Development Libraries and Tools

```
sudo apt -y install i2c-tools libi2c-dev gpiod libgpiod2 libgpiod-dev avahi-daemon gcc make unzip zip vim git
```

## Java

```shell
sudo apt -y install openjdk-11-jdk
```

## Locale / Timezone

Run: `sudo dpkg-reconfigure locales`

Select `en_GB.UTF-8`

Run: `sudo dpkg-reconfigure tzdata`

Select: `Europe / London`

## WiFi / I<sup>2</sup>C / SPI

Use `raspbi-config`.

## DTB

```
dtc -I dtb -O dts -o sun50i-h5-orangepi-zero-plus.dts /boot/dtb/allwinner/sun50i-h5-orangepi-zero-plus.dtb
dtc -I dtb -O dts -o sun50i-h5-orangepi-one-plus.dts /boot/dtb/allwinner/sun50i-h5-orangepi-one-plus.dtb
```
