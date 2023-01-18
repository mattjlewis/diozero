---
parent: Single Board Computers
nav_order: 3
permalink: /boards/odroidc2.html
---

# Odroid C2

## System Information

```
$ sudo java -cp diozero-sampleapps-1.3.5.jar com.diozero.sampleapps.SystemInformation

Local System Info
diozero version: 1.3.5
Operating System: debian 11 (bullseye) - aarch64
CPU Temperature: -1.00

Detected Board Info
Device Factory: DefaultDeviceFactory
Board: Odroid C2 (RAM: 2,048,000 bytes, O/S: debian 11 (bullseye))
I2C Bus Numbers: 0, 1

Header: DEFAULT
+-----+-----------------+--------+----------+--------+-----------------+-----+
+ GP# +            Name +  gpiod + Physical + gpiod  + Name            + GP# +
+-----+-----------------+--------+----------+--------+-----------------+-----+
|     |             3v3 |        |  1 || 2  |        | 5v              |     |
| 447 |       I2C A SDA |  1:69  |  3 || 4  |        | 5v              |     |
| 448 |       I2C A SCK |  1:70  |  5 || 6  |        | GND             |     |
| 491 |  J2 Header Pin7 |  1:113 |  7 || 8  |  1:104 | J2 Header Pin8  | 482 |
|     |             GND |        |  9 || 10 |  1:105 | J2 Header Pin10 | 483 |
| 489 | J2 Header Pin11 |  1:111 | 11 || 12 |  1:102 | J2 Header Pin12 | 480 |
| 481 | J2 Header Pin13 |  1:103 | 13 || 14 |        | GND             |     |
| 479 | J2 Header Pin15 |  1:101 | 15 || 16 |  1:100 | J2 Header Pin16 | 478 |
|     |             3v3 |        | 17 || 18 |  1:97  | J2 Header Pin18 | 475 |
| 477 | J2 Header Pin19 |  1:99  | 19 || 20 |        | GND             |     |
| 474 | J2 Header Pin21 |  1:96  | 21 || 22 |  1:95  | J2 Header Pin22 | 473 |
| 472 | J2 Header Pin23 |  1:94  | 23 || 24 |  1:93  | J2 Header Pin24 | 471 |
|     |             GND |        | 25 || 26 |  1:89  | J2 Header Pin26 | 467 |
| 449 |       I2C B SDA |  1:71  | 27 || 28 |  1:72  | I2C B SCK       | 450 |
| 470 | J2 Header Pin29 |  1:92  | 29 || 30 |        | GND             |     |
| 461 | J2 Header Pin31 |  1:83  | 31 || 32 |  1:88  | J2 Header Pin32 | 466 |
| 476 | J2 Header Pin33 |  1:98  | 33 || 34 |        | GND             |     |
| 456 | J2 Header Pin35 |  1:78  | 35 || 36 |  1:82  | J2 Header Pin36 | 460 |
|   1 |        ADC.AIN1 |        | 37 || 38 |        | 1v8             |     |
|     |             GND |        | 39 || 40 |        | ADC.AIN0        | 0   |
+-----+-----------------+--------+----------+--------+-----------------+-----+

Header: J7
+-----+----------------+--------+----------+--------+----------------+-----+
+ GP# +           Name +  gpiod + Physical + gpiod  + Name           + GP# +
+-----+----------------+--------+----------+--------+----------------+-----+
|     |            GND |        |  1 || 2  |  0:6   | J7 Header Pin2 | 503 |
|     |             5v |        |  3 || 4  |  0:8   | J7 Header Pin4 | 505 |
| 507 | J7 Header Pin5 |  0:10  |  5 || 6  |  0:9   | J7 Header Pin6 | 506 |
| 508 | J7 Header Pin7 |  0:11  |  7 ||    |        |                |     |
+-----+----------------+--------+----------+--------+----------------+-----+
```

## GPIO Read All

```
$ sudo java -cp diozero-sampleapps-1.3.5.jar com.diozero.sampleapps.GpioReadAll

Header: DEFAULT
+-----+-----------------+------+---+--------+----------+--------+---+------+-----------------+-----+
+ GP# +            Name + Mode + V +  gpiod + Physical + gpiod  + V + Mode + Name            + GP# +
+-----+-----------------+------+---+--------+----------+--------+---+------+-----------------+-----+
|     |             3v3 |      |   |        |  1 || 2  |        |   |      | 5v              |     |
| 447 |       I2C A SDA |   In | 1 |  1:69  |  3 || 4  |        |   |      | 5v              |     |
| 448 |       I2C A SCK |   In | 1 |  1:70  |  5 || 6  |        |   |      | GND             |     |
| 491 |  J2 Header Pin7 |   In | 1 |  1:113 |  7 || 8  |  1:104 | 1 | In   | J2 Header Pin8  | 482 |
|     |             GND |      |   |        |  9 || 10 |  1:105 | 1 | In   | J2 Header Pin10 | 483 |
| 489 | J2 Header Pin11 |   In | 1 |  1:111 | 11 || 12 |  1:102 | 1 | In   | J2 Header Pin12 | 480 |
| 481 | J2 Header Pin13 |   In | 1 |  1:103 | 13 || 14 |        |   |      | GND             |     |
| 479 | J2 Header Pin15 |   In | 1 |  1:101 | 15 || 16 |  1:100 | 1 | In   | J2 Header Pin16 | 478 |
|     |             3v3 |      |   |        | 17 || 18 |  1:97  | 1 | In   | J2 Header Pin18 | 475 |
| 477 | J2 Header Pin19 |   In | 1 |  1:99  | 19 || 20 |        |   |      | GND             |     |
| 474 | J2 Header Pin21 |   In | 1 |  1:96  | 21 || 22 |  1:95  | 1 | In   | J2 Header Pin22 | 473 |
| 472 | J2 Header Pin23 |   In | 1 |  1:94  | 23 || 24 |  1:93  | 1 | In   | J2 Header Pin24 | 471 |
|     |             GND |      |   |        | 25 || 26 |  1:89  | 0 | In   | J2 Header Pin26 | 467 |
| 449 |       I2C B SDA |   In | 1 |  1:71  | 27 || 28 |  1:72  | 1 | In   | I2C B SCK       | 450 |
| 470 | J2 Header Pin29 |   In | 1 |  1:92  | 29 || 30 |        |   |      | GND             |     |
| 461 | J2 Header Pin31 |   In | 1 |  1:83  | 31 || 32 |  1:88  | 1 | In   | J2 Header Pin32 | 466 |
| 476 | J2 Header Pin33 |   In | 0 |  1:98  | 33 || 34 |        |   |      | GND             |     |
| 456 | J2 Header Pin35 |   In | 1 |  1:78  | 35 || 36 |  1:82  | 1 | In   | J2 Header Pin36 | 460 |
|   1 |        ADC.AIN1 | Unkn |   |        | 37 || 38 |        |   |      | 1v8             |     |
|     |             GND |      |   |        | 39 || 40 |        |   | Unkn | ADC.AIN0        | 0   |
+-----+-----------------+------+---+--------+----------+--------+---+------+-----------------+-----+
Header: J7
+-----+----------------+------+---+--------+----------+--------+---+------+----------------+-----+
+ GP# +           Name + Mode + V +  gpiod + Physical + gpiod  + V + Mode + Name           + GP# +
+-----+----------------+------+---+--------+----------+--------+---+------+----------------+-----+
|     |            GND |      |   |        |  1 || 2  |  0:6   | 1 | In   | J7 Header Pin2 | 503 |
|     |             5v |      |   |        |  3 || 4  |  0:8   | 1 | In   | J7 Header Pin4 | 505 |
| 507 | J7 Header Pin5 |   In | 1 |  0:10  |  5 || 6  |  0:9   | 1 | In   | J7 Header Pin6 | 506 |
| 508 | J7 Header Pin7 |   In | 1 |  0:11  |  7 ||    |        |   |      |                |     |
+-----+----------------+------+---+--------+----------+--------+---+------+----------------+-----+
```
