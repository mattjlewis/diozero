---
parent: Concepts
nav_order: 1
permalink: /concepts/providers.html
title: Device Factories & Providers
redirect_from:
  - /en/latest/Providers/index.html
  - /en/stable/Providers/index.html
---

# Device Factories & Providers

## Device Factories

Iternally, all devices are provisioned via a
[Device Factory](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/internal/spi/DeviceFactoryInterface.java) -
all of the classes in `com.diozero.api` and `com.diozero.devices` include a constructor parameter
that allows a device factory to be specified. If a device factory isn't specified, the
host board itself is used to provision the device using
[DeviceFactoryHelper.getNativeDeviceFactory()](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sbc/DeviceFactoryHelper.java).
This is particularly useful for GPIO expansion boards and Analog-to-Digital converters -
the device factory that is specific to the expansion board can be used to provision the device
instead of the host board.

Some boards like the Raspberry Pi provide no analog input pins; attempting to create an 
AnalogInputDevice such as an LDR using the Raspberry Pi default native device factory 
would result in a runtime error (`UnsupportedOperationException`). However, diozero includes
support for Analog to Digital Converter expansion devices such as the 
[MCP3008](https://www.microchip.com/wwwproducts/en/MCP3008); such devices have been
implemented as analog input device factories hence can be used in the constructor of analog
devices like LDRs:

```java
try (McpAdc ain_df = new McpAdc(McpAdc.Type.MCP3008, chipSelect); LDR ldr = new LDR(ain_df, pin, vRef, r1)) {
	System.out.println(ldr.getUnscaledValue());
}
```

In this example, the `ain_df` [McpAdc](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/devices/McpAdc.java)
object handles all of the logic for interfacing with the MCP3008 ADC chip as well as implementing
[AnalogInputDeviceFactoryInterface](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/internal/spi/AnalogInputDeviceFactoryInterface.java)
to enable it to provision AnalogInputDevice instances.

Repeating the previous example of controlling an LED when you press a button but with 
all devices connected via an 
[MCP23017](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/devices/MCP23017.java) 
GPIO expansion board:

```java
try (MCP23017 mcp23017 = new MCP23017(12);
		Button button = new Button(mcp23017, 1, GpioPullUpDown.PULL_UP);
		LED led = new LED(mcp23017, 2)) {
	button.whenPressed(nanoTime -> led.on());
	button.whenReleased(nanoTime -> led.off());
	SleepUtil.sleepSeconds(10);
}
```

## Providers

The Device Factory section introd
To maximise the portability across the broad spectrum of boards and devices, diozero
has the concept of providers that are  base native device factory 

Provider | Dependency | diozero Provider Library | SBC
--- | -------- | ------------ | -----------------------
built-in (default) | None | Built-in | All
pigpio | pigpioj-java | diozero-provider-pigio | Raspberry Pi (all flavours)
bbbiolib | bbbiolib | diozero-provider-bbiolib | BeagleBone Green / Black
firmata | firmata4j | diozero-provider-firmata | Arduino compatible boards
voodoospark | None | diozero-provider-voodoospark | Arduino compatible boards
remote | None | diozero-provider-remote | All

The built-in provider is designed to be portable across different boards. 

{: .note-title }
> Providers
>
> Unless you are implementing a new provider or device factory you shouldn't need to use any
> of the interfaces or helper classes (within the `com.diozero.internal` package).

### pigpio

Uses the excellent [pigpio](http://abyz.me.uk/rpi/pigpio/) C library to provide fully optimised
GPIO / SPI and I2C support for all Raspberry Pi models.
Makes use of the [pigpioj](https://github.com/mattjlewis/pigpioj) library to access pigpio.

Make sure that pigpio is installed:
```
sudo apt update && sudo apt -y install pigpio
```

The pigpioj library has two mechanisms for interfacing with pigpio:

1. JNI - default mode of operation; requires root access; **the pigpiod daemon process must be stopped**
1. Sockets - activate when the environment variable `PIGPIOD_HOST` is set; does not require root access

The [pigpioj](https://github.com/mattjlewis/pigpioj) project provides additional details.

## Provider Lookup

The class [DeviceFactoryHelper](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/util/DeviceFactoryHelper.java)
encapsulates the logic for accessing the configured service provider.
Interfaces for implementing a new service provider are in the
[com.diozero.internal.provider](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/internal/provider)
package. Developing a new service provider is relatively straightforward given the provided APIs and base classes.

The device native service provider library is defined in the following order:

1. System property com.diozero.devicefactory, e.g. `-Dcom.diozero.devicefactory=com.diozero.internal.provider.pi4j.Pi4jDeviceFactory`
2. Service definition file on the classpath, file: `/META-INF/services/com.diozero.internal.provider.NativeDeviceFactoryInterface`. For example [the one for pi4j](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-pi4j/src/main/resources/META-INF/services/com.diozero.internal.provider.NativeDeviceFactoryInterface)

See below for provider specific details.
