# Expansion Boards

## Microchip Analog to Digital Converters {: #mcp-adc }

The class [McpAdc](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/McpAdc.java) supports the following Microchip analog-to-digital converter devices:

+ MCP300x: [MCP3001](http://www.microchip.com/wwwproducts/en/MCP3001), [MCP3002](http://www.microchip.com/wwwproducts/en/MCP3002), [MCP3004](http://www.microchip.com/wwwproducts/en/MCP3004), [MCP3008](http://www.microchip.com/wwwproducts/en/MCP3008)
+ MCP320x: [MCP3201](http://www.microchip.com/wwwproducts/en/MCP3201), [MCP3202](http://www.microchip.com/wwwproducts/en/MCP3202), [MCP3204](http://www.microchip.com/wwwproducts/en/MCP3204), [MCP3208](http://www.microchip.com/wwwproducts/en/MCP3208)
+ MCP330x: [MCP3301](http://www.microchip.com/wwwproducts/en/MCP3301), [MCP3302](http://www.microchip.com/wwwproducts/en/MCP3302), [MCP3304](http://www.microchip.com/wwwproducts/en/MCP3304)

An LDR controlled LED (using a 10k&#8486; resistor for the LDR and a 220&#8486; resistor for the LED):

![MCP3008 LDR Controlled LED](images/MCP3008_LDR_LED.png "MCP3008 LDR Controlled LED")

Code for the above circuit is implemented in [LdrControlledLed](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/LdrControlledLed.java), the important bit:

```java
try (McpAdc adc = new McpAdc(type, chipSelect); LDR ldr = new LDR(adc, pin, vRef, r1); PwmLed led = new PwmLed(ledPin)) {
	// Detect variations of 5%, taking a reading every 20ms
	ldr.addListener((event) -> led.setValue(1-event.getUnscaledValue()), .05f, 20);
	Logger.debug("Sleeping for 20s");
	SleepUtil.sleepSeconds(20);
}
```

## Microchip GPIO Expansion Board

Support for the Microchip [MCP23017](http://www.microchip.com/wwwproducts/Devices.aspx?product=MCP23017) 16-bit input/output port expander is provided by the [MCP23017 class](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/MCP23017.java). This class provides for support for interrupt driven state change callbacks.

An example circuit for controlling an LED with a button, all connected via an MCP23017:

![MCP23017 Button controlled LED](images/MCP23017_LED_Button.png "MCP23017 Button controlled LED")

Code for the above circuit is implemented in [MCP23017Test](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/MCP23017Test.java), the important bit:

```java
try (MCP23017 mcp23017 = new MCP23017(intAPin, intBPin);
		Button button = new Button(mcp23017, inputPin, GpioPullUpDown.PULL_UP);
		LED led = new LED(mcp23017, outputPin)) {
	led.on();
	SleepUtil.sleepSeconds(1);
	led.off();
	SleepUtil.sleepSeconds(1);
	led.blink(0.5f, 0.5f, 10, false);
	button.whenPressed(led::on);
	button.whenReleased(led::off);
	Logger.debug("Waiting for 10s - *** Press the button connected to MCP23017 pin {} ***",
			Integer.valueOf(inputPin));
	SleepUtil.sleepSeconds(10);
	button.whenPressed(null);
	button.whenReleased(null);
}
```

## PWM / Servo Driver {: #pwm-servo-driver }

TODO Documentation pending...

+ [PCA9685](http://www.nxp.com/products/power-management/lighting-driver-and-controller-ics/i2c-led-display-control/16-channel-12-bit-pwm-fm-plus-ic-bus-led-controller:PCA9685) 12-bit 16-channel PWM driver as used by the [Adafruit PWM Servo Driver](https://www.adafruit.com/product/815)
