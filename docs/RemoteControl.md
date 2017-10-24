# Remote Control

It is possible to remotely control your devices from another computer, enabling you to develop and test your application directly from your development environment.

## Arduino

For Arduino compatible devices communication is via USB using the excellent [firmata4j](https://github.com/kurbatov/firmata4j) and [jssc](https://github.com/scream3r/java-simple-serial-connector) libraries and the [diozero-provider-firmata](https://github.com/mattjlewis/diozero/tree/master/diozero-provider-firmata) wrapper.
Make sure you set the property ```FIRMATA_PORT``` to the serial port (COM on Windows) to which your Arduino is connected. This can be set either via the command line (e.g. ```-DFIRMATA_PORT=COM5```) or as an environment variable (command line takes precedence).
Example command line:
```shell
java -cp tinylog-1.2.jar:diozero-core-0.11-SNAPSHOT.jar:diozero-provider-firmata4j-0.11-SNAPSHOT.jar:firmata4j-2.3.5.jar:jssc-2.8.0.jar:diozero-sampleapps-0.11-SNAPSHOT.jar com.diozero.sampleapps.LEDTest -DFIRMATA_PORT=COM5 12
```

## Particle Wi-Fi Connected Microcontrollers

The [Particle Photon](https://www.particle.io/products/hardware/photon-wifi-dev-kit) is a tiny Wi-Fi connected microcontroller.
You can upload the [VoodooSpark](https://github.com/voodootikigod/voodoospark) firmware to the board to enable remote control over Wi-Fi.
Make sure you set the properties ```PARTICLE_DEVICE_ID``` and ```PARTICLE_TOKEN``` correctly (either via command line or as environment variables).
Example command line:
```shell
java -cp tinylog-1.2.jar:diozero-core-0.11-SNAPSHOT.jar:diozero-provider-voodoospark-0.11-SNAPSHOT.jar:diozero-sampleapps-0.11-SNAPSHOT.jar -DPARTICLE_DEVICE_ID=abc -DPARTICLE_TOKEN=xyz com.diozero.sampleapps.LEDTest 12
```

## pigpio

The [pigpio](http://abyz.co.uk/rpi/pigpio/) library also comes with the [pigpiod](http://abyz.co.uk/rpi/pigpio/pigpiod.html) daemon process for remote access.
By default (for security reasons) this process only allows communication from the Raspberry Pi device it is running on.
To enable remote access pigpiod needs to be run without the ```-l``` option.
Before doing this you must be aware of the security implications, only consider this on a local network that has no inbound connectivity from the Internet.
On Raspbian Jessie and later you can enable the pigpiod daemon to run as a system service:
```shell
sudo systemctl enable pigpiod.service
```
This will start pigpiod every time the Pi boots. To remove the ```-l``` option which is included by default, edit ```/lib/systemd/system/pigpiod.service``` and change the ```ExecStart``` property, i.e.
```
ExecStart=/usr/bin/pigpiod
```
Make sure you set the property ```PIGPIOD_HOST``` to the hostname of your Raspberry Pi.
You can also override the default pigpiod port value (8888) via the property ```PIGPIOD_PORT```.
Again these can be set either via command line or as environment variables.
Example command line:
```shell
java -cp tinylog-1.2.jar:diozero-core-0.11-SNAPSHOT.jar:diozero-provider-pigpio-0.11-SNAPSHOT.jar:pigpioj-java-2.2.jar:diozero-sampleapps-0.11-SNAPSHOT.jar -DPIGPIOD_HOST=raspberrypi com.diozero.sampleapps.LEDTest 12
```

## Generic Remote Access Library (experimental)

I am in the process of adding generic remote device access protocols to diozero, please see the [diozero-remote-server](https://github.com/mattjlewis/diozero/tree/master/diozero-remote-server) and [diozero-provider-remote](https://github.com/mattjlewis/diozero/tree/master/diozero-provider-remote) projects.
The idea is that support for these protocols can be developed for any device, including those that don't run Java in particular the extremely cheap Wi-Fi connected [ESP8266](https://en.wikipedia.org/wiki/ESP8266) and  [ESP32](https://en.wikipedia.org/wiki/ESP32).

The code has been designed to be generic so as to support any communication channel and protocol. To switch between the different client implementations you currently need to modify the [RemoteDeviceFactory](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-remote/src/main/java/com/diozero/internal/provider/remote/devicefactory/RemoteDeviceFactory.java) class.

So far I have developed support for the following remote protocols:

| Channel | Protocol(s) | Implementation | Notes |
| ------- | ----------- | -------------- | ----- |
| HTTP | JSON | [Server](https://github.com/mattjlewis/diozero/blob/master/diozero-remote-server/src/main/java/com/diozero/remote/http/DiozeroController.java), [Client](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-remote/src/main/java/com/diozero/internal/provider/remote/http/JsonHttpProtocolHandler.java) | GPIO (read and write), SPI. I2C support not yet implemented. Something like [Server-sent Events](https://en.wikipedia.org/wiki/Server-sent_events) would be required to support GPIO events. |
| [WebSockets](https://en.wikipedia.org/wiki/WebSocket) | JSON, [Google Protobuf](https://developers.google.com/protocol-buffers/) | [JSON Server](https://github.com/mattjlewis/diozero/blob/master/diozero-remote-server/src/main/java/com/diozero/remote/websocket/JsonWebSocket.java), [JSON Client](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-remote/src/main/java/com/diozero/internal/provider/remote/websocket/JsonWebSocketProtocolHandler.java), [Protobuf Client](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-remote/src/main/java/com/diozero/internal/provider/remote/websocket/ProtobufWebSocketProtocolHandler.java) | GPIO (read, write and events), SPI. I2C support not yet implemented. Java Protobuf server not yet implemented. |
| [MQTT](https://en.wikipedia.org/wiki/MQTT) | [Google Protobuf](https://developers.google.com/protocol-buffers/) | [Server](https://github.com/mattjlewis/diozero/blob/master/diozero-remote-server/src/main/java/com/diozero/remote/mqtt/MqttServer.java), [Client](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-remote/src/main/java/com/diozero/internal/provider/remote/mqtt/ProtobufMqttProtocolHandler.java) | GPIO (read, write and events), SPI. I2C support not yet implemented. |
| Socket | [VoodooSpark](https://github.com/voodootikigod/voodoospark) | [Server](https://github.com/voodootikigod/voodoospark), [Client](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-remote/src/main/java/com/diozero/internal/provider/remote/voodoospark/VoodooSparkProtocolHandler.java) | GPIO (read, write and events). I2C support not yet implemented. SPI not currently supported by VoodooSpark. |
| Socket | [pigpiod](http://abyz.co.uk/rpi/pigpio/pigpiod.html) | [Server](http://abyz.co.uk/rpi/pigpio/pigpiod.html) | Client not yet ported to diozero-provider-remote. |
| USB | [firmata](https://github.com/firmata/protocol) | [Arduino Server](https://github.com/firmata/arduino), [Particle Spark Server](https://github.com/firmata/spark) | Client not yet ported to diozero-provider-remote. |

Performance is surprisingly good, I've been using my [SSD1331Test](https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/SSD1331Test.java) application to check frames per second when rendering Game of Life iterations to an SSD1331 colour OLED display via SPI; I've not seen much difference between the various protocols. Performance is approximately half of that when run on the device itself.
