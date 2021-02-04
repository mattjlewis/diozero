---
parent: Concepts
nav_order: 1
permalink: /concepts/providers.html
title: Device Factories
---

# Device Factories

Iternally, all devices are provisioned via a
[Device Factory](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/internal/spi/DeviceFactoryInterface.java) -
all of the classes in com.diozero.api and com.diozero.devices include an additional constructor
parameter that allows a device factory to be specified. If a device factory isn't specified, the
host board itself is used to provision the device using
[DeviceFactoryHelper.getNativeDeviceFactory()](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sbc/DeviceFactoryHelper.java).
This is particularly useful for GPIO expansion boards and Analog-to-Digital converters.
The [Providers](2_concepts/1_Providers.md) section provides further details on board device providers.

> Device Factories
>{: .admonition-title }
>
> Unless you are implementing a new device you shouldn't need to use any of the Device 
> Factory interfaces or helper classes (within the `com.diozero.internal` package).
{: .admonition .note }

Some boards like the Raspberry Pi provide no analog input pins; attempting to create an 
AnalogInputDevice such as an LDR using the Raspberry Pi default native device factory 
would result in a runtime error (`UnsupportedOperationException`). However, support for 
Analog to Digital Converter expansion devices such as the 
[MCP3008](http://rtd.diozero.com/en/latest/ExpansionBoards/#mcp-adc) has been added 
which are implemented as analog input device factories hence can be used in the 
constructor of analog devices like LDRs:

```java
try (McpAdc adc = new McpAdc(McpAdc.Type.MCP3008, chipSelect); LDR ldr = new LDR(adc, pin, vRef, r1)) {
	System.out.println(ldr.getUnscaledValue());
}
```

Repeating the previous example of controlling an LED when you press a button but with 
all devices connected via an 
[MCP23017](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/MCP23017.java) 
GPIO expansion board:

```java
try (MCP23017 mcp23017 = new MCP23017(12);
		Button button = new Button(mcp23017, 1, GpioPullUpDown.PULL_UP);
		LED led = new LED(mcp23017, 2)) {
	button.whenPressed(epochTime -> led.on());
	button.whenReleased(epochTime -> led.off());
	SleepUtil.sleepSeconds(10);
}
```

Analog input devices also provide an event notification mechanism. To control the 
brightness of an LED based on ambient light levels:

```java
try (McpAdc adc = new McpAdc(McpAdc.Type.MCP3008, 1);
		LDR ldr = new LDR(adc, 1, 3.3, 1000);
		PwmLed led = new PwmLed(18)) {
	// Detect variations of 10%, get values every 50ms (the default)
	ldr.addListener(event -> led.setValue(1-event.getUnscaledValue()), .1f);
	SleepUtil.sleepSeconds(20);
}
```

The builtin provider is designed to be portable across different boards. 

## Device Factory Lookup

The class [DeviceFactoryHelper](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/util/DeviceFactoryHelper.java)
encapsulates the logic for accessing the configured service provider.
Interfaces for implementing a new service provider are in the
[com.diozero.internal.provider](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/internal/provider)
package. Developing a new service provider is relatively straightforward given the provided APIs and base classes.

The device native service provider library is defined in the following order:

1. System property com.diozero.devicefactory, e.g. `-Dcom.diozero.devicefactory=com.diozero.internal.provider.pi4j.Pi4jDeviceFactory`
2. Service definition file on the classpath, file: `/META-INF/services/com.diozero.internal.provider.NativeDeviceFactoryInterface`. For example [the one for pi4j](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-pi4j/src/main/resources/META-INF/services/com.diozero.internal.provider.NativeDeviceFactoryInterface)

See below for provider specific details.

## Additional Device Factories

In addition to the default builtin device factory, the following service providers are also available via additional JAR files:

+ [BBBIOlib](https://github.com/VegetableAvenger/BBBIOlib)
+ [Firmata4j](https://github.com/kurbatov/firmata4j)
+ mmap
+ [pigpio](http://abyz.co.uk/rpi/pigpio/index.html) via my [JNI wrapper library](https://github.com/mattjlewis/pigpioj)
+ [VoodooSpark](https://github.com/voodootikigod/voodoospark)

### pigpio

Uses the excellent pigpio C library to provide fully optimised GPIO / SPI and I2C support for all 
Raspberry Pi models. Unfortunately this library requires root access.
Make sure that pigpio is installed:
```
sudo apt update && sudo apt -y install pigpio
```

The pigpioj library has two mechanisms for interfacing with pigpio - JNI and Sockets.

### Creating a Device Factory

TODO Describe steps for creating a new provider.
