---
parent: Devices
nav_order: 3
permalink: /devices/expansionboards.html
redirect_from:
  - /en/latest/ExpansionBoards/index.html
  - /en/stable/ExpansionBoards/index.html
---

# Expansion Boards

All of these devices act as Device Factories and should be accessed as such.

## ADS112C04 ADC

16-Bit, 4 Channel, 2,000 samples per second delta-sigma ADC.

[Datasheet](https://www.ti.com/lit/ds/symlink/ads112c04.pdf),
[Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/devices/Ads112C04.html).

## ADS1015 / ADS1115 ADC

Ads1x15 [Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/devices/Ads1x15.html).

Datasheets:

* ADS1015 - 3,300 samples per second, 12-Bit, 4 channel ADC [Datasheet](https://www.ti.com/lit/ds/symlink/ads1015.pdf)
* ADS1115 - 860 samples per second, 16-Bit, 4 channel ADC [Datasheet](https://www.ti.com/lit/ds/symlink/ads1115.pdf)

## Microchip Analog to Digital Converters
{: #mcp-adc }

Provides support for the following Microchip analog-to-digital converter devices:

+ MCP300x: [MCP3001](http://www.microchip.com/wwwproducts/en/MCP3001), [MCP3002](http://www.microchip.com/wwwproducts/en/MCP3002), [MCP3004](http://www.microchip.com/wwwproducts/en/MCP3004), [MCP3008](http://www.microchip.com/wwwproducts/en/MCP3008)
+ MCP320x: [MCP3201](http://www.microchip.com/wwwproducts/en/MCP3201), [MCP3202](http://www.microchip.com/wwwproducts/en/MCP3202), [MCP3204](http://www.microchip.com/wwwproducts/en/MCP3204), [MCP3208](http://www.microchip.com/wwwproducts/en/MCP3208)
+ MCP330x: [MCP3301](http://www.microchip.com/wwwproducts/en/MCP3301), [MCP3302](http://www.microchip.com/wwwproducts/en/MCP3302), [MCP3304](http://www.microchip.com/wwwproducts/en/MCP3304)

Usage example: An LDR controlled LED (using a 10k&#8486; resistor for the LDR and a 220&#8486; resistor for the LED):

![MCP3008 LDR Controlled LED](/assets/images/MCP3008_LDR_LED.png "MCP3008 LDR Controlled LED")

Code for the above circuit is implemented in [LdrControlledLed](https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/LdrControlledLed.java), the important bit:

```java
try (McpAdc adc = new McpAdc(type, chipSelect); LDR ldr = new LDR(adc, pin, r1); PwmLed led = new PwmLed(ledPin)) {
	// Detect variations of 5%, taking a reading every 20ms
	ldr.addListener((event) -> led.setValue(1-event.getUnscaledValue()), .05f, 20);
	Logger.debug("Sleeping for 20s");
	SleepUtil.sleepSeconds(20);
}
```

McpAdc [Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/devices/McpAdc.html).

## Microchip MCP23xxx GPIO Expansion Board
{: #mcp23xxx }

Supports MCP23008, MCP23017 and MCP23S17.

An example circuit for controlling an LED with a button, all connected via an [MCP23017](https://www.microchip.com/wwwproducts/en/mcp23017):

![MCP23017 Button controlled LED](/assets/images/MCP23017_LED_Button.png "MCP23017 Button controlled LED")

Code for the above circuit is implemented in [MCP23017Test](https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/MCP23017Test.java), the important bit:

```java
try (MCP23017 mcp23017 = new MCP23017(intAPin, intBPin);
		Button button = new Button(mcp23017, inputPin, GpioPullUpDown.PULL_UP);
		LED led = new LED(mcp23017, outputPin)) {
	led.on();
	SleepUtil.sleepSeconds(1);
	led.off();
	SleepUtil.sleepSeconds(1);
	led.blink(0.5f, 0.5f, 10, false);
	button.whenPressed(nanoTime -> led.on());
	button.whenReleased(nanoTime -> led.off());
	Logger.debug("Waiting for 10s - *** Press the button connected to MCP23017 pin {} ***",
			Integer.valueOf(inputPin));
	SleepUtil.sleepSeconds(10);
	button.whenPressed(null);
	button.whenReleased(null);
}
```

Implementations:

* MCP23008 [Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/devices/MCP23008.html)
* MCP23017 [Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/devices/MCP23017.html)
* MCP23S17 [Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/devices/MCP23S17.html)

## PCA9685 PWM / Servo Driver
{: #pca9685 }

Provides support for the [PCA9685](http://www.nxp.com/products/power-management/lighting-driver-and-controller-ics/i2c-led-display-control/16-channel-12-bit-pwm-fm-plus-ic-bus-led-controller:PCA9685)
12-bit 16-channel PWM driver as used by the [Adafruit PWM Servo Driver](https://www.adafruit.com/product/815).
Implements [PwmOutputDeviceFactoryInterface](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/internal/spi/PwmOutputDeviceFactoryInterface.html)
hence can be passed into the constructor of PWM output devices.

Usage example:

```java
float delay = 0.5f;
try (PwmOutputDeviceFactoryInterface df = new PCA9685(); PwmLed led = new PwmLed(df, pin)) {
	led.setValue(.25f);
	SleepUtil.sleepSeconds(delay);
	led.toggle();
	SleepUtil.sleepSeconds(delay);
	led.setValue(.5f);
	SleepUtil.sleepSeconds(delay);
	led.blink(0.5f, 0.5f, 5, false);
	led.pulse(1, 50, 5, false);
} catch (RuntimeIOException e) {
	Logger.error(e, "Error: {}", e);
}
```

## PCF8574 GPIO Expansion Board
{: #pcf8574 }

Support for the [PCF8574](https://www.ti.com/lit/ds/symlink/pcf8574.pdf) 8-Bit I/O Expander.

PCF8574 [Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/devices/PCF8574.html).

## PCF8591 ADC / DAC
{: #pcf8591 }

Support for the [PCF8591](https://www.nxp.com/docs/en/data-sheet/PCF8591.pdf) 8-bit A/D and D/A converter.

PCF8591 [Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/devices/PCF8591.html).

## Picon Zero Intelligent Robotics Controller

[Picon Zero](https://shop.pimoroni.com/products/picon-zero-intelligent-robotics-controller-for-raspberry-pi).

[Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/devices/PiconZero.html).

## Output Shift Registers
{: #osr }

E.g. SN74HC595. See OutputShiftRegisterDeviceFactory.
