---
parent: Concepts
nav_order: 2
permalink: /concepts/remotecontrol.html
Title: Remote Control
redirect_from:
  - /en/latest/RemoteControl/index.html
  - /en/stable/RemoteControl/index.html
---

# Remote Control

It is possible to remotely control your devices from another computer, enabling you to develop and
test your application directly from your development environment.

{: .note-title }
> Command Line Examples
>
> All of the command line examples provided assume that all dependent JARs are also contained within
the current directory or on the CLASSPATH.

## Arduino

For Arduino compatible devices communication is via USB using the excellent [firmata4j](https://github.com/kurbatov/firmata4j)
and [jssc](https://github.com/scream3r/java-simple-serial-connector) libraries that are accessed when using the
[diozero-provider-firmata](https://github.com/mattjlewis/diozero/tree/master/diozero-provider-firmata) wrapper.

Make sure you set the property `FIRMATA_PORT` to the serial port (COM on Windows) to which your
Arduino is connected. This can be set either via the command line (e.g. `-DFIRMATA_PORT=COM5`)
or as an environment variable (command line takes precedence).

Example command line:

```shell
java -cp diozero-sampleapps-{{site.version}}.jar:diozero-provider-firmata4j-{{site.version}}.jar com.diozero.sampleapps.LEDTest -DFIRMATA_PORT=COM5 12
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

## gRPC

diozero provides its own remote protocol implementation that is based on [gRPC](https://grpc.io) via
the diozero-remote-common, diozero-provider-remote and diozero-remote-server modules.

Simply run the diozero-remote-server application on the target device and connect to it by adding a
dependency to diozero-provider-remote and setting `diozero.remote.hostname` either command a command
line "-D" property or as an environment variable.

The protocol is defined in [diozero-remote-common/src/main/proto/diozero.proto](https://github.com/mattjlewis/diozero/blob/main/diozero-remote-common/src/main/proto/diozero.proto).
