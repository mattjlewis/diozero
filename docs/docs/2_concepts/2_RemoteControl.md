---
parent: Concepts
nav_order: 2
permalink: /concepts/remotecontrol.html
Title: Remote Control & Microcontrollers
redirect_from:
  - /en/latest/RemoteControl/index.html
  - /en/stable/RemoteControl/index.html
---

# Remote Control & Microcontrollers
{: .no_toc }

It is possible to remotely control your devices, including microcontrollers, from another computer,
enabling you to develop and test your application directly from your development environment.

{: .note-title }
> Command Line Examples
>
> All of the command line examples provided assume that all dependent JARs are also contained within
the current directory or on the CLASSPATH.

## Implementations
{: .no_toc .text-delta }

1. TOC
{:toc}

## gRPC

diozero provides its own [remote protocol](https://github.com/mattjlewis/diozero/blob/main/diozero-remote-common/src/main/proto/diozero.proto)
implementation that is based on [gRPC](https://grpc.io) via the diozero-remote-common,
diozero-provider-remote and diozero-remote-server modules.

Simply run the diozero-remote-server application on the target device then connect to it from a
diozero application by adding diozero-provider-remote-{{ site.version }}.jar to the classpath and
setting `diozero.remote.hostname` either via the command line or as an environment variable.

The diozero-remote-server application will also make use of diozero providers. For example, you
could run it with `diozero-provider-pigpio-{{ site.version }}.jar` on the classpath and it would use
pigpio for the underlying device communication. You could even chain to another
diozero-remove-server instance by using `diozero-provider-remote-{{ site.version }}.jar`.

## Firmata for Microcontrollers

Firmata compatible devices are supported via the
[diozero-provider-firmata](https://github.com/mattjlewis/diozero/tree/master/diozero-provider-firmata)
device factory provider. This provider includes a [Firmata protocol](https://github.com/firmata/protocol)
implementation that can operate via either serial or sockets interfaces. The serial implementation
uses the diozero NativeSerialDevice. I recommend using [Firmata Builder](http://firmatabuilder.com)
to construct a [Configurable Firmata](https://github.com/firmata/ConfigurableFirmata) implementation
that matches your device.

The Firmata implementation should work with _any_ Arduino compatible device. It has been tested with
the following devices:

* Arduino Uno (Serial)
* Pololu A-Star 32U4 (Serial)
* Raspberry Pi Pico (\* some minor tweaks required) (Serial)
* ESP8266 (Serial, BLE & WiFi)
* [ESP32](https://www.espressif.com/en/products/socs/esp32) (Serial, BLE & WiFi) - requires latest ConfigurableFirmata

The Firmata adapter first looks for the `diozero.firmata.serialPort` property - if this set (either via
command line or environment variable) the serial communication protocol is used. Otherwise, the
adapter will look for the `diozero.firmata.tcpHostname` property and if set use sockets communication.
The sockets port defaults to 3030 but can be overridden via the `diozero.firmata.tcpPort` property.

Example command line:

```shell
java -cp diozero-sampleapps-{{site.version}}.jar:diozero-provider-firmata-{{site.version}}.jar com.diozero.sampleapps.LEDTest -Ddiozero.firmata.serialPort=COM5 12
```

\* Fixes for Raspberry Pi Pico:

`/Applications/Arduino.app/Contents/Java/libraries/Firmata/Boards.h` or `~/Documents/Arduino/libraries/ConfigurableFirmata/src/utility/Boards.h`:

```
// Raspberry Pi Pico
// https://datasheets.raspberrypi.org/pico/Pico-R3-A4-Pinout.pdf
#elif defined(TARGET_RP2040) || defined(TARGET_RASPBERRY_PI_PICO)
#define TOTAL_ANALOG_PINS       4
#define TOTAL_PINS              30
#define VERSION_BLINK_PIN       LED_BUILTIN
#define IS_PIN_DIGITAL(p)       (((p) >= 0 && (p) < 23) || (p) == LED_BUILTIN)
#define IS_PIN_ANALOG(p)        ((p) >= 26 && (p) < 26 + TOTAL_ANALOG_PINS)
#define IS_PIN_PWM(p)           digitalPinHasPWM(p)
#define IS_PIN_SERVO(p)         (IS_PIN_DIGITAL(p) && (p) != LED_BUILTIN)
// From the data sheet I2C-0 defaults to GP 4 (SDA) & 5 (SCL) (physical pins 6 & 7). Note:
// v2.3.1 of mbed_rp2040 defines WIRE_HOWMANY to 1 and uses the non-default GPs 6 & 7.
// v3.1.1 of mbed_rp2040 defines WIRE_HOWMANY to 1 and uses the default GPs 4 & 5.
// See: variants/RASPBERRY_PI_PICO/pins_arduino.h
//#define WIRE_HOWMANY	(1)
//#define PIN_WIRE_SDA            (6u)
//#define PIN_WIRE_SCL            (7u)
#define IS_PIN_I2C(p)           ((p) == PIN_WIRE_SDA || (p) == PIN_WIRE_SCL)
// SPI-0 defaults to GP 16 (RX / MISO), 17 (CSn), 18 (SCK) & 19 (TX / MOSI) (physical pins 21, 22, 24, 25)
#define IS_PIN_SPI(p)           ((p) == PIN_SPI_SCK || (p) == PIN_SPI_MOSI || (p) == PIN_SPI_MISO || (p) == PIN_SPI_SS)
// UART-0 defaults to GP 0 (TX) & 1 (RX)
#define IS_PIN_SERIAL(p)        ((p) == 0 || (p) == 1 || (p) == 4 || (p) == 5 || (p) == 8 || (p) == 9 || (p) == 12 || (p) == 13 || (p) == 16 || (p) == 17)
#define PIN_TO_DIGITAL(p)       (p)
#define PIN_TO_ANALOG(p)        ((p) - 26)
#define PIN_TO_PWM(p)           (p)
#define PIN_TO_SERVO(p)         (p)
```

`~/Documents/Arduino/libraries/ConfigurableFirmata/src/ConfigurableFirmata.h`:

```
//#define I2C                     0x06 // same as PIN_MODE_I2C
```

## pigpio

The [pigpio](http://abyz.me.uk/rpi/pigpio/) library also comes with the
[pigpiod](http://abyz.me.uk/rpi/pigpio/pigpiod.html) daemon process for remote access.
By default (for security reasons) this process only allows communication from the Raspberry Pi
device it is running on.
To enable remote access pigpiod needs to be run without the `-l` option.
Before doing this you must be aware of the security implications, only consider this on a local
network that has no inbound connectivity from the Internet.

On Raspbian Jessie and later you can enable the pigpiod daemon to run as a system service:

```shell
sudo systemctl enable pigpiod.service
```

This will start pigpiod every time the Pi boots. To remove the `-l` option which is included by
default, edit `/etc/systemd/system/pigpiod.service.d/public.conf` and change the `ExecStart` property, i.e.

```
ExecStart=/usr/bin/pigpiod
```

If the file doesn't exist, run the `raspi-config` application and choose "3 Interface Options",
"P8 Remote GPIO" and select "Yes". This will make the appropriate changes and create that file if
it didn't already exist.

Make sure you set the property `PIGPIOD_HOST` to the hostname of your Raspberry Pi.
You can also override the default pigpiod port value (8888) via the property `PIGPIOD_PORT`.
Again these properties can be set either via command line or as environment variables.

Example command line:

```shell
java -cp diozero-sampleapps-{{site.version}}.jar:diozero-provider-pigpio-{{site.version}}.jar -DPIGPIOD_HOST=raspberrypi com.diozero.sampleapps.LEDTest 12
```

## Particle Wi-Fi Connected Microcontrollers

The [Particle Photon](https://www.particle.io/products/hardware/photon-wifi-dev-kit) is a tiny Wi-Fi
connected microcontroller.
You can upload the [VoodooSpark](https://github.com/voodootikigod/voodoospark) firmware to the board
to enable remote control over Wi-Fi.

Make sure you set the properties `PARTICLE_DEVICE_ID` and `PARTICLE_TOKEN` correctly (either via
command line or as environment variables).

Example command line:

```shell
java -cp diozero-sampleapps-{{site.version}}.jar:diozero-provider-voodoospark-{{site.version}}.jar -DPARTICLE_DEVICE_ID=abc -DPARTICLE_TOKEN=xyz com.diozero.sampleapps.LEDTest 12
```
