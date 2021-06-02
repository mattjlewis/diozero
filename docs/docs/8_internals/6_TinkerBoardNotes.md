---
parent: Internals
nav_order: 6
permalink: /internals/tbsetup.html
has_toc: true
---

# ASUS TinkerBoard Setup

Armbian setup notes.

## I/O File Permissions

File `/etc/udev/rules.d/70-gpio.rules`:

```
# Allow group gpio to access gpiomem device
SUBSYSTEM=="rk3288-gpiomem", GROUP="gpio", MODE="0660"

# Allow group gpio to access gpiochip files
SUBSYSTEM=="gpio", GROUP="gpio", MODE="0660"

# To allow additional features like edge detection
SUBSYSTEM=="gpio*", PROGRAM="/bin/sh -c '\
  chown -R root:gpio /sys/class/gpio && chmod -R 770 /sys/class/gpio;\
  chown -R root:gpio /sys/devices/virtual/gpio && chmod -R 770 /sys/devices/virtual/gpio;\
  chown -R root:gpio /sys$devpath && chmod -R 770 /sys$devpath\
'"
```
