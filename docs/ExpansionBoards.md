# Expansion Boards

## Microchip Analog to Digital Converters {: #mcp-adc }

*class* com.diozero.**McpAdc** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/McpAdc.java){: .viewcode-link }

: Provides support for the following Microchip analog-to-digital converter devices:

+ MCP300x: [MCP3001](http://www.microchip.com/wwwproducts/en/MCP3001), [MCP3002](http://www.microchip.com/wwwproducts/en/MCP3002), [MCP3004](http://www.microchip.com/wwwproducts/en/MCP3004), [MCP3008](http://www.microchip.com/wwwproducts/en/MCP3008)
+ MCP320x: [MCP3201](http://www.microchip.com/wwwproducts/en/MCP3201), [MCP3202](http://www.microchip.com/wwwproducts/en/MCP3202), [MCP3204](http://www.microchip.com/wwwproducts/en/MCP3204), [MCP3208](http://www.microchip.com/wwwproducts/en/MCP3208)
+ MCP330x: [MCP3301](http://www.microchip.com/wwwproducts/en/MCP3301), [MCP3302](http://www.microchip.com/wwwproducts/en/MCP3302), [MCP3304](http://www.microchip.com/wwwproducts/en/MCP3304)

Usage example: An LDR controlled LED (using a 10k&#8486; resistor for the LDR and a 220&#8486; resistor for the LED):

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

## Microchip MCP23017 GPIO Expansion Board {: #mcp-gpio-expansion-board }

*class* com.diozero.**MCP23017** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/MCP23017.java){: .viewcode-link }

: Provides support for the Microchip [MCP23017](http://www.microchip.com/wwwproducts/Devices.aspx?product=MCP23017) 16-bit input/output port expander. Input device state change notifications will only work if at least one of the MCP23017 interrupt pins is connected to the Raspberry Pi.

    **MCP23017** (*interruptPinNumber*)
    
    : Constructor
    
    * **interruptPinNumber** (*int*) - The pin on the Raspberry Pi to be used for input interrupt notifications. This sets the device into mirrored interrupt mode whereby interrupts for either bank get mirrored on both of the MCP23017 interrupt outputs.

    **MCP23017** (*interruptPinNumberA*, *interruptPinNumberB*)
    
    : Constructor
    
    * **interruptPinNumberA** (*int*) - The pin on the Raspberry Pi to be used for input interrupt notifications for bank A.
    
    * **interruptPinNumberB** (*int*) - The pin on the Raspberry Pi to be used for input interrupt notifications for bank B.

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

## PCA9685 PWM / Servo Driver {: #pwm-servo-driver }

*class* com.diozero.**PCA9685** [source](https://github.com/mattjlewis/diozero/blob/master/src/main/java/com/diozero/PCA9685.java){: .viewcode-link }

: Provides support for the [PCA9685](http://www.nxp.com/products/power-management/lighting-driver-and-controller-ics/i2c-led-display-control/16-channel-12-bit-pwm-fm-plus-ic-bus-led-controller:PCA9685) 12-bit 16-channel PWM driver as used by the [Adafruit PWM Servo Driver](https://www.adafruit.com/product/815). Implements [PwmOutputDeviceFactoryInterface](https://github.com/mattjlewis/diozero/blob/master/src/main/java/com/diozero/internal/spi/PwmOutputDeviceFactoryInterface.java) hence can be passed into the constructor of PWM output devices.

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
