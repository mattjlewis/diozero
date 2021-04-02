---
title: GPIO
parent: API
nav_order: 1
permalink: /api/gpio.html
---

# General Purpose Input / Output (GPIO)

[Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core).

## Digital

## Analog

### Input

The [AnalogInputDevice](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/AnalogInputDevice.java) base class encapsulates logic for interfacing with analog devices. This class provides access to unscaled (-1..1) and scaled (e.g. voltage, temperature, distance) readings. For scaled readings is important to pass the ADC voltage range in the device constructor - all raw analog readings are normalised (i.e. -1..1).

{: .note-title }
> Analog Device Support
>
> The Raspberry Pi does not natively support analog input devices, see [expansion boards](ExpansionBoards.md#mcp-adc) for connecting to analog-to-digital converters.

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
		LDR ldr = new LDR(adc, 1, 3.3, 1000);
		PwmLed led = new PwmLed(18)) {
	// Detect variations of 10%, get values every 50ms (the default)
	ldr.addListener(event -> led.setValue(1-event.getUnscaledValue()), .1f);
	SleepUtil.sleepSeconds(20);
}
```
