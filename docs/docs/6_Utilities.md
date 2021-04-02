---
Title: Utilities
nav_order: 6
permalink: /utilities.html
---

# Utilities

The diozero-sampleapps project includes some useful command-line utilities. Some of these are
reimplementations of standard Linux command-line tools to illustrate the capabilities of diozero.

## System Information

Displays information about the automatically detected local board, including the device factory in
use, local host's name, memory and O/S, as well as pin information for all detected pins for all
headers. Uses colourised console output.

```shell
> java -cp diozero-sampleapps-1.1.9.jar com.diozero.sampleapps.SystemInformation
Local System Info
Operating System: debian "10 (buster)" "10"
I2C buses: [1]
CPU Temperature: 40.41

Native Device Factory: DefaultDeviceFactory
Board: Raspberry Pi 4B (RAM: 2,048,000 bytes)

Header: J8
+-----+-----------+--------+----------+--------+-----------+-----+
+ GP# +      Name +  gpiod + Physical + gpiod  + Name      + GP# +
+-----+-----------+--------+----------+--------+-----------+-----+
|     |       3v3 |        |  1 || 2  |        | 5v        |     |
|   2 |      SDA1 |  0:2   |  3 || 4  |        | 5v        |     |
|   3 |      SCL1 |  0:3   |  5 || 6  |        | GND       |     |
|   4 | GPIO_GCLK |  0:4   |  7 || 8  |  0:14  | TXD1      | 14  |
|     |       GND |        |  9 || 10 |  0:15  | RXD1      | 15  |
|  17 |    GPIO17 |  0:17  | 11 || 12 |  0:18  | GPIO18    | 18  |
|  27 |    GPIO27 |  0:27  | 13 || 14 |        | GND       |     |
|  22 |    GPIO22 |  0:22  | 15 || 16 |  0:23  | GPIO23    | 23  |
|     |       3v3 |        | 17 || 18 |  0:24  | GPIO24    | 24  |
|  10 |  SPI_MOSI |  0:10  | 19 || 20 |        | GND       |     |
|   9 |  SPI_MISO |  0:9   | 21 || 22 |  0:25  | GPIO25    | 25  |
|  11 |  SPI_SCLK |  0:11  | 23 || 24 |  0:8   | SPI_CE0_N | 8   |
|     |       GND |        | 25 || 26 |  0:7   | SPI_CE1_N | 7   |
|   0 |    ID_SDA |  0:0   | 27 || 28 |  0:1   | ID_SCL    | 1   |
|   5 |     GPIO5 |  0:5   | 29 || 30 |        | GND       |     |
|   6 |     GPIO6 |  0:6   | 31 || 32 |  0:12  | GPIO12    | 12  |
|  13 |    GPIO13 |  0:13  | 33 || 34 |        | GND       |     |
|  19 |    GPIO19 |  0:19  | 35 || 36 |  0:16  | GPIO16    | 16  |
|  26 |    GPIO26 |  0:26  | 37 || 38 |  0:20  | GPIO20    | 20  |
|     |       GND |        | 39 || 40 |  0:21  | GPIO21    | 21  |
+-----+-----------+--------+----------+--------+-----------+-----+

Header: P5
+-----+---------------+--------+----------+--------+---------------+-----+
+ GP# +          Name +  gpiod + Physical + gpiod  + Name          + GP# +
+-----+---------------+--------+----------+--------+---------------+-----+
|  54 |         BT_ON |  1:0   |  0 || 1  |  1:1   | WL_ON         | 55  |
|  56 |   PWR_LED_OFF |  1:2   |  2 || 3  |  1:3   | GLOBAL_RESET  | 57  |
|  58 | VDD_SD_IO_SEL |  1:4   |  4 || 5  |  1:5   | CAM_GPIO      | 59  |
|  60 |     SD_PWR_ON |  1:6   |  6 || 7  |  1:7   | SD_OC_N       | 61  |
+-----+---------------+--------+----------+--------+---------------+-----+
```

## GpioDetect

Reimplementation of the `gpiodetect` application that is bundled in the gpiod tools package to list
all GPIO chips, print their labels and number of GPIO lines.

```shell
> gpiodetect
gpiochip0 [pinctrl-bcm2711] (58 lines)
gpiochip1 [raspberrypi-exp-gpio] (8 lines)

> java -cp diozero-sampleapps-1.1.9.jar com.diozero.sampleapps.GpioDetect
gpiochip0 [pinctrl-bcm2711] (58 lines)
gpiochip1 [raspberrypi-exp-gpio] (8 lines)
```

## GpioReadAll

Reimplementation of the `gpio readall` command from the now deprecated wiringpi library. Displays
information about all detected physical pins for all headers, including GPIO number, name, mode,
value, GPIO character device chip and line number as well as physical pin number.
Uses colourised console output.

```shell
> java -cp diozero-sampleapps-1.1.9.jar com.diozero.sampleapps.GpioReadAll
Header: J8
+-----+-----------+------+---+--------+----------+--------+---+------+-----------+-----+
+ GP# +      Name + Mode + V +  gpiod + Physical + gpiod  + V + Mode + Name      + GP# +
+-----+-----------+------+---+--------+----------+--------+---+------+-----------+-----+
|     |       3v3 |      |   |        |  1 || 2  |        |   |      | 5v        |     |
|   2 |      SDA1 | Unkn | 1 |  0:2   |  3 || 4  |        |   |      | 5v        |     |
|   3 |      SCL1 | Unkn | 1 |  0:3   |  5 || 6  |        |   |      | GND       |     |
|   4 | GPIO_GCLK |   In | 1 |  0:4   |  7 || 8  |  0:14  | 1 | In   | TXD1      | 14  |
|     |       GND |      |   |        |  9 || 10 |  0:15  | 1 | In   | RXD1      | 15  |
|  17 |    GPIO17 |   In | 0 |  0:17  | 11 || 12 |  0:18  | 0 | In   | GPIO18    | 18  |
|  27 |    GPIO27 |   In | 0 |  0:27  | 13 || 14 |        |   |      | GND       |     |
|  22 |    GPIO22 |   In | 0 |  0:22  | 15 || 16 |  0:23  | 0 | In   | GPIO23    | 23  |
|     |       3v3 |      |   |        | 17 || 18 |  0:24  | 0 | In   | GPIO24    | 24  |
|  10 |  SPI_MOSI | Unkn | 0 |  0:10  | 19 || 20 |        |   |      | GND       |     |
|   9 |  SPI_MISO | Unkn | 0 |  0:9   | 21 || 22 |  0:25  | 0 | In   | GPIO25    | 25  |
|  11 |  SPI_SCLK | Unkn | 0 |  0:11  | 23 || 24 |  0:8   | 1 | Out  | SPI_CE0_N | 8   |
|     |       GND |      |   |        | 25 || 26 |  0:7   | 1 | Out  | SPI_CE1_N | 7   |
|   0 |    ID_SDA |   In | 1 |  0:0   | 27 || 28 |  0:1   | 1 | In   | ID_SCL    | 1   |
|   5 |     GPIO5 |   In | 1 |  0:5   | 29 || 30 |        |   |      | GND       |     |
|   6 |     GPIO6 |   In | 1 |  0:6   | 31 || 32 |  0:12  | 0 | In   | GPIO12    | 12  |
|  13 |    GPIO13 |   In | 0 |  0:13  | 33 || 34 |        |   |      | GND       |     |
|  19 |    GPIO19 |   In | 0 |  0:19  | 35 || 36 |  0:16  | 0 | In   | GPIO16    | 16  |
|  26 |    GPIO26 |   In | 0 |  0:26  | 37 || 38 |  0:20  | 0 | In   | GPIO20    | 20  |
|     |       GND |      |   |        | 39 || 40 |  0:21  | 0 | In   | GPIO21    | 21  |
+-----+-----------+------+---+--------+----------+--------+---+------+-----------+-----+
Header: P5
+-----+---------------+------+---+--------+----------+--------+---+------+---------------+-----+
+ GP# +          Name + Mode + V +  gpiod + Physical + gpiod  + V + Mode + Name          + GP# +
+-----+---------------+------+---+--------+----------+--------+---+------+---------------+-----+
|  54 |         BT_ON |   In |   |  1:0   |  0 || 1  |  1:1   |   | In   | WL_ON         | 55  |
|  56 |   PWR_LED_OFF |   In |   |  1:2   |  2 || 3  |  1:3   |   | In   | GLOBAL_RESET  | 57  |
|  58 | VDD_SD_IO_SEL |   In |   |  1:4   |  4 || 5  |  1:5   |   | In   | CAM_GPIO      | 59  |
|  60 |     SD_PWR_ON |   In |   |  1:6   |  6 || 7  |  1:7   |   | In   | SD_OC_N       | 61  |
+-----+---------------+------+---+--------+----------+--------+---+------+---------------+-----+
```

## I2CDetect

Reimplementation of the `i2cdetect` application from the `i2c-tools` package.

```shell
> i2cdetect -y 1
     0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f
00:          -- -- -- -- -- -- -- -- -- -- -- -- --
10: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
20: 20 -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
30: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
40: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
50: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
60: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
70: -- -- -- -- -- -- -- --

> java -cp diozero-sampleapps-1.1.9.jar com.diozero.sampleapps.I2CDetect 1
     0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f
00:          -- -- -- -- -- -- -- -- -- -- -- -- --
10: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
20: 20 -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
30: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
40: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
50: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
60: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
70: -- -- -- -- -- -- -- --
```

