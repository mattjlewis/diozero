---
parent: Single Board Computers
nav_order: 5
permalink: /boards/tbsetup.html
redirect_from:
  - /internal/tbsetup.html
---

# ASUS TinkerBoard Setup

Setup notes for Armbian (32-bit).

## System Information

```
$ sudo java -cp diozero-sampleapps-{{site.version}}.jar com.diozero.sampleapps.SystemInformation

Local System Info
diozero version: {{site.version}}
Operating System: ubuntu 22.04.1 LTS (Jammy Jellyfish) - arm
CPU Temperature: 37.73

Detected Board Info
Device Factory: DefaultDeviceFactory
Board: Asus Tinker Board (RAM: 2,048,000 bytes, O/S: ubuntu 22.04.1 LTS (Jammy Jellyfish))
I2C Bus Numbers: 0, 1, 2, 4, 5

Header: DEFAULT
+-----+----------+--------+----------+--------+----------+-----+
+ GP# +     Name +  gpiod + Physical + gpiod  + Name     + GP# +
+-----+----------+--------+----------+--------+----------+-----+
|     |      3v3 |        |  1 || 2  |        | 5v       |     |
| 252 | GPIO8_A4 |  8:4   |  3 || 4  |        | 5v       |     |
| 253 | GPIO8_A5 |  8:5   |  5 || 6  |        | GND      |     |
|  17 | GPIO0_C1 |  0:17  |  7 || 8  |  5:9   | GPIO5_B1 | 161 |
|     |      GND |        |  9 || 10 |  5:8   | GPIO5_B0 | 160 |
| 164 | GPIO5_B4 |  5:12  | 11 || 12 |  6:0   | GPIO6_A0 | 184 |
| 166 | GPIO5_B6 |  5:14  | 13 || 14 |        | GND      |     |
| 167 | GPIO5_B7 |  5:15  | 15 || 16 |  5:10  | GPIO5_B2 | 162 |
|     |      3v3 |        | 17 || 18 |  5:11  | GPIO5_B3 | 163 |
| 257 | GPIO8_B1 |  8:9   | 19 || 20 |        | GND      |     |
| 256 | GPIO8_B0 |  8:8   | 21 || 22 |  5:19  | GPIO5_C3 | 171 |
| 254 | GPIO8_A6 |  8:6   | 23 || 24 |  8:7   | GPIO8_A7 | 255 |
|     |      GND |        | 25 || 26 |  8:3   | GPIO8_A3 | 251 |
| 233 | GPIO7_C1 |  7:17  | 27 || 28 |  7:18  | GPIO7_C2 | 234 |
| 165 | GPIO5_B5 |  5:13  | 29 || 30 |        | GND      |     |
| 168 | GPIO5_C0 |  5:16  | 31 || 32 |  7:23  | GPIO7_C7 | 239 |
| 238 | GPIO7_C6 |  7:22  | 33 || 34 |        | GND      |     |
| 185 | GPIO6_A1 |  6:1   | 35 || 36 |  7:7   | GPIO7_A7 | 223 |
| 224 | GPIO7_B0 |  7:8   | 37 || 38 |  6:3   | GPIO6_A3 | 187 |
|     |      GND |        | 39 || 40 |  6:4   | GPIO6_A4 | 188 |
+-----+----------+--------+----------+--------+----------+-----+
```

## GPIO Read All

```
$ sudo java -cp diozero-sampleapps-{{site.version}}.jar com.diozero.sampleapps.GpioReadAll

Header: DEFAULT
+-----+----------+------+---+--------+----------+--------+---+------+----------+-----+
+ GP# +     Name + Mode + V +  gpiod + Physical + gpiod  + V + Mode + Name     + GP# +
+-----+----------+------+---+--------+----------+--------+---+------+----------+-----+
|     |      3v3 |      |   |        |  1 || 2  |        |   |      | 5v       |     |
| 252 | GPIO8_A4 | Unkn | 1 |  8:4   |  3 || 4  |        |   |      | 5v       |     |
| 253 | GPIO8_A5 | Unkn | 1 |  8:5   |  5 || 6  |        |   |      | GND      |     |
|  17 | GPIO0_C1 |   In | 0 |  0:17  |  7 || 8  |  5:9   | 1 | Unkn | GPIO5_B1 | 161 |
|     |      GND |      |   |        |  9 || 10 |  5:8   | 1 | Unkn | GPIO5_B0 | 160 |
| 164 | GPIO5_B4 |   In | 1 |  5:12  | 11 || 12 |  6:0   | 0 | Unkn | GPIO6_A0 | 184 |
| 166 | GPIO5_B6 | Unkn | 1 |  5:14  | 13 || 14 |        |   |      | GND      |     |
| 167 | GPIO5_B7 | Unkn | 1 |  5:15  | 15 || 16 |  5:10  | 1 | In   | GPIO5_B2 | 162 |
|     |      3v3 |      |   |        | 17 || 18 |  5:11  | 1 | In   | GPIO5_B3 | 163 |
| 257 | GPIO8_B1 | Unkn | 0 |  8:9   | 19 || 20 |        |   |      | GND      |     |
| 256 | GPIO8_B0 | Unkn | 1 |  8:8   | 21 || 22 |  5:19  | 0 | In   | GPIO5_C3 | 171 |
| 254 | GPIO8_A6 | Unkn | 0 |  8:6   | 23 || 24 |  8:7   | 1 | Unkn | GPIO8_A7 | 255 |
|     |      GND |      |   |        | 25 || 26 |  8:3   | 1 | Unkn | GPIO8_A3 | 251 |
| 233 | GPIO7_C1 |   In | 1 |  7:17  | 27 || 28 |  7:18  | 1 | Unkn | GPIO7_C2 | 234 |
| 165 | GPIO5_B5 |   In | 1 |  5:13  | 29 || 30 |        |   |      | GND      |     |
| 168 | GPIO5_C0 |   In | 1 |  5:16  | 31 || 32 |  7:23  | 1 | Unkn | GPIO7_C7 | 239 |
| 238 | GPIO7_C6 | Unkn | 1 |  7:22  | 33 || 34 |        |   |      | GND      |     |
| 185 | GPIO6_A1 | Unkn | 0 |  6:1   | 35 || 36 |  7:7   | 1 | Unkn | GPIO7_A7 | 223 |
| 224 | GPIO7_B0 | Unkn | 1 |  7:8   | 37 || 38 |  6:3   | 1 | Unkn | GPIO6_A3 | 187 |
|     |      GND |      |   |        | 39 || 40 |  6:4   | 0 | Unkn | GPIO6_A4 | 188 |
+-----+----------+------+---+--------+----------+--------+---+------+----------+-----+
```

## GPIO File Permissions

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
