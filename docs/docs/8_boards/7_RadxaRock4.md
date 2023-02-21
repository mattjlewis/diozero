unam---
parent: Single Board Computers
nav_order: 7
permalink: /boards/rock4cplus.html
---
# Rock 4C+

## System Information

```shell
$ java -cp diozero-sampleapps-{{site.version}}.jar com.diozero.sampleapps.SystemInformation

Local System Info
diozero version: {{site.version}}
Operating System: debian 11 (bullseye) - aarch64
CPU Temperature: -1.00

Detected Board Info
Device Factory: DefaultDeviceFactory
Board: Radxa Radxa ROCK Pi 4C+ (RAM: 3,956,248 bytes, O/S: debian 11 (bullseye))
I2C Bus Numbers: 0, 1, 9

Header: Default
+-----+----------+--------+----------+--------+----------+-----+
+ GP# +     Name +  gpiod + Physical + gpiod  + Name     + GP# +
+-----+----------+--------+----------+--------+----------+-----+
|     |      3v3 |        |  1 || 2  |        | 5v       |     |
|  71 | I2C7_SDA |  2:7   |  3 || 4  |        | 5v       |     |
|  72 | I2C7_SCL |  2:8   |  5 || 6  |        | GND      |     |
|  75 | GPIO2_B3 |  2:11  |  7 || 8  |  4:20  | TXD2     | 148 |
|     |      GND |        |  9 || 10 |  4:19  | RXD2     | 147 |
| 146 |     PWM0 |  4:18  | 11 || 12 |  4:3   | GPIO4_A3 | 131 |
| 150 |     PWM1 |  4:22  | 13 || 14 |        | GND      |     |
| 149 | GPIO4_C5 |  4:21  | 15 || 16 |  4:26  | GPIO4_D2 | 154 |
|     |      3v3 |        | 17 || 18 |  4:28  | GPIO4_D4 | 156 |
|  40 |  SPI1_TX |  1:8   | 19 || 20 |        | GND      |     |
|  39 |  SPI1_RX |  1:7   | 21 || 22 |  4:29  | GPIO4_D5 | 157 |
|  41 | SPI1_CLK |  1:9   | 23 || 24 |  1:10  | SPI1_CS0 | 42  |
|     |      GND |        | 25 || 26 |        | ADC_IN0  | 0   |
|  64 | GPIO2_A0 |  2:0   | 27 || 28 |  2:1   | GPIO2_A1 | 65  |
|  74 | GPIO2_B2 |  2:10  | 29 || 30 |        | GND      |     |
|  73 | GPIO2_B1 |  2:9   | 31 || 32 |  3:0   | GPIO3_C0 | 112 |
|  76 | GPIO2_B4 |  2:12  | 33 || 34 |        | GND      |     |
| 133 | GPIO4_A5 |  4:5   | 35 || 36 |  4:4   | GPIO4_A4 | 132 |
| 158 | GPIO4_D6 |  4:30  | 37 || 38 |  4:6   | GPIO4_A6 | 134 |
|     |      GND |        | 39 || 40 |  4:7   | GPIO4_A7 | 135 |
+-----+----------+--------+----------+--------+----------+-----+
```

## GPIO Read All

```shell
$ sudo java -cp diozero-sampleapps-{{site.version}}.jar com.diozero.sampleapps.GpioReadAll

Header: Default
+-----+----------+------+---+--------+----------+--------+---+------+----------+-----+
+ GP# +     Name + Mode + V +  gpiod + Physical + gpiod  + V + Mode + Name     + GP# +
+-----+----------+------+---+--------+----------+--------+---+------+----------+-----+
|     |      3v3 |      |   |        |  1 || 2  |        |   |      | 5v       |     |
|  71 | I2C7_SDA |   In | 1 |  2:7   |  3 || 4  |        |   |      | 5v       |     |
|  72 | I2C7_SCL |   In | 1 |  2:8   |  5 || 6  |        |   |      | GND      |     |
|  75 | GPIO2_B3 |   In | 1 |  2:11  |  7 || 8  |  4:20  | 1 | UART | TXD2     | 148 |
|     |      GND |      |   |        |  9 || 10 |  4:19  | 1 | UART | RXD2     | 147 |
| 146 |     PWM0 |   In | 0 |  4:18  | 11 || 12 |  4:3   | 1 | In   | GPIO4_A3 | 131 |
| 150 |     PWM1 |   In | 0 |  4:22  | 13 || 14 |        |   |      | GND      |     |
| 149 | GPIO4_C5 |   In | 0 |  4:21  | 15 || 16 |  4:26  | 0 | In   | GPIO4_D2 | 154 |
|     |      3v3 |      |   |        | 17 || 18 |  4:28  | 0 | In   | GPIO4_D4 | 156 |
|  40 |  SPI1_TX |  SPI | 0 |  1:8   | 19 || 20 |        |   |      | GND      |     |
|  39 |  SPI1_RX |  SPI | 1 |  1:7   | 21 || 22 |  4:29  | 0 | In   | GPIO4_D5 | 157 |
|  41 | SPI1_CLK |  SPI | 0 |  1:9   | 23 || 24 |  1:10  | 1 | SPI  | SPI1_CS0 | 42  |
|     |      GND |      |   |        | 25 || 26 |        |   | In   | ADC_IN0  | 0   |
|  64 | GPIO2_A0 |   In | 1 |  2:0   | 27 || 28 |  2:1   | 1 | In   | GPIO2_A1 | 65  |
|  74 | GPIO2_B2 |   In | 1 |  2:10  | 29 || 30 |        |   |      | GND      |     |
|  73 | GPIO2_B1 |   In | 1 |  2:9   | 31 || 32 |  3:0   | 1 | In   | GPIO3_C0 | 112 |
|  76 | GPIO2_B4 |   In | 1 |  2:12  | 33 || 34 |        |   |      | GND      |     |
| 133 | GPIO4_A5 |   In | 1 |  4:5   | 35 || 36 |  4:4   | 1 | In   | GPIO4_A4 | 132 |
| 158 | GPIO4_D6 |  Out | 0 |  4:30  | 37 || 38 |  4:6   | 1 | In   | GPIO4_A6 | 134 |
|     |      GND |      |   |        | 39 || 40 |  4:7   | 1 | In   | GPIO4_A7 | 135 |
+-----+----------+------+---+--------+----------+--------+---+------+----------+-----+
```

## Setup

export DISTRO=bullseye-stable
curl http://apt.radxa.com/$DISTRO/public.key | sudo apt-key add -

## Onboard Thermal Zones

[Reference](https://github.com/armbian/build/pull/4129).

Create file `rockchip-rk3399-thermals.dtsi`:

```
/dts-v1/;
/plugin/;

/ {
        compatible = "radxa,rockpi4c-plus", "radxa,rockpi4", "rockchip,rk3399";

        fragment@0 {
                target=<&tsadc>;
                __overlay__ {
                        status = "okay";
                        /* tshut mode 0:CRU 1:GPIO */
                        rockchip,hw-tshut-mode = <1>;
                        /* tshut polarity 0:LOW 1:HIGH */
                        rockchip,hw-tshut-polarity = <1>;
                };
        };

};
```

Compile and copy over:

```shell
dtc -@ -O dtb -o rockchip-rk3399-thermals.dtbo rockchip-rk3399-thermals.dtsi
chmod +x rockchip-rk3399-thermals.dtbo
sudo cp rockchip-rk3399-thermals.dtbo /boot/dtb/rockchip/overlay/.
```

Install either via `armbian-config` (System / Hardware) or by directly adding `rk3399-thermals` to the `overlays` list in `/boot/armbianEnv.txt`; reboot.


https://github.com/TheRemote/PiBenchmarks

## GPIO File Permissions

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
