---
title: GPIO
parent: API
nav_order: 1
permalink: /api/gpio.html
redirect_from:
  - /en/latest/InputDevices/index.html
  - /en/stable/InputDevices/index.html
  - /en/latest/OutputDevices/index.html
  - /en/stable/OutputDevices/index.html
---

# General Purpose Input / Output (GPIO)

[Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/api/package-summary.html).

Key concepts:

* Use the provided Builder static classes rather than the constructors.
* Can be provisioned either via the GPIO number or a
 [PinInfo](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/api/PinInfo.html) object.
 A [PinInfo](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/api/PinInfo.html)
 object can be retrieved via [one of the lookup methods](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/sbc/BoardPinInfo.html#getByGpioNumberOrThrow(int))
 in the [BoardPinInfo](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/sbc/BoardPinInfo.html)
 instance that is returned from the
 [device factory](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/internal/spi/DeviceFactoryInterface.html)
 [getBoardPinInfo()](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/internal/spi/DeviceFactoryInterface.html#getBoardPinInfo()) method.
* The [GPIO](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/internal/spi/GpioDeviceFactoryInterface.html)
 or [Analog Input](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/internal/spi/AnalogInputDeviceFactoryInterface.html)
 device factory can be specified when using GPIO expansion boards such as the MCP3008, otherwise it
 defaults to the [automatically detected](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/sbc/DeviceFactoryHelper.html#getNativeDeviceFactory()) host board.

## Digital

### Input

Key concepts:

* The `activeHigh` property is optional; it will default to `false` if `pud` is set to pull-up, otherwise `true`.

DigitalInputDevice [Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/api/DigitalInputDevice.html).

SmoothedInputDevice [Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/api/SmoothedInputDevice.html).

### Input and Output

A GPIO device that can be dynamically switched between input and output mode.

DigitalInputOutputDevice [Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/api/DigitalInputOutputDevice.html).

### Output

DigitalOutputDevice [Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/api/DigitalOutputDevice.html).

### PWM Output

PwmOutputDevice [Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/api/PwmOutputDevice.html).

## Analog

### Input

The [AnalogInputDevice](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/api/AnalogInputDevice.html)
class provides the mechanism for interfacing with analog devices.
This class provides access to unscaled (-1..1) as well as scaled (e.g. voltage, temperature, distance) readings.
For scaled readings is important to set the ADC voltage range in the device constructor -
all raw analog readings are normalised (i.e. -1..1).

{: .note-title }
> Analog Device Support
>
> A lot of boards, including the Raspberry Pi, do not natively support analog input devices, see
[expansion boards](../4_devices/3_ExpansionBoards.md) for connecting to analog-to-digital converters.

Example: Temperature readings using an MCP3008 and TMP36:

![MCP3008 TMP36](/assets/images/MCP3008_TMP36.png "MCP3008 TMP36") 

Code taken from [TMP36Test](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/TMP36Test.java):
```java
try (McpAdc adc = new McpAdc(McpAdc.Type.MCP3008, chipSelect);
		TMP36 tmp36 = new TMP36(adc, pin, vRef, tempOffset)) {
	for (int i=0; i<ITERATIONS; i++) {
		double tmp = tmp36.getTemperature();
		Logger.info("Temperature: {}", String.format("%.2f", Double.valueOf(tmp)));
		SleepUtil.sleepSeconds(.5);
	}
}
```

Analog input devices also provide an event notification mechanism. To control the 
brightness of an LED based on ambient light levels:

```java
try (McpAdc adc = new McpAdc(McpAdc.Type.MCP3008, 1);
		LDR ldr = new LDR(adc, 1, 1000);
		PwmLed led = new PwmLed(18)) {
	// Detect variations of 10%, get values every 50ms (the default)
	ldr.addListener(event -> led.setValue(1-event.getUnscaledValue()), .1f);
	SleepUtil.sleepSeconds(20);
}
```

### Output

The [AnalogOutputDevice](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/api/AnalogOutputDevice.html)
class provides support for analog output via an Digital to Analog Converter.

Example:
```java
try (PCF8591 dac = new PCF8591();
		AnalogOutputDevice aout = AnalogOutputDevice.Builder.builder(0).setDeviceFactory(dac).build()) {
	for (float f = 0; f < 1; f += 0.1) {
		aout.setValue(f);
		SleepUtil.sleepMillis(100);
	}
	for (float f = 1; f >= 0; f -= 0.1) {
		aout.setValue(f);
		SleepUtil.sleepMillis(100);
	}
}
```
