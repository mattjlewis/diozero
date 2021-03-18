---
parent: Internals
nav_order: 4
permalink: /internals/rpinotes.html
---

# Raspberry Pi Setup Notes

## User Permissions

`sudo usermod -a -G dialout ubuntu`

`sudo usermod -a -G i2c ubuntu`

## I2C Clock Speed

```shell
#!/bin/sh
# Print current maximum i2c rate
var="$(xxd /sys/class/i2c-adapter/i2c-1/of_node/clock-frequency | awk -F': ' '{print $2}')"
var=${var//[[:blank:].\}]/}
printf "I2C Clock Rate: %d Hz\n" 0x$var
```

## Development Libraries and Tools

`sudo apt install i2c-tools libi2c-dev gpiod libgpiod2 libgpiod-dev avahi-daemon gcc make unzip zip vim`

`sudo apt install pigpio-tools libpigpiod-if-dev libpigpiod-if2-1`

## Java

`sudo apt install openjdk-11-jdk`

## ZSH

`sudo apt install zsh`

`chsh -s /usr/bin/zsh`

`sh -c "$(curl -fsSL https://raw.github.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"`
