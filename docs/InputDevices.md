# Input Devices

## Digital Input Devices

### Button

![Button](images/Button.png "Button") 

Code taken from [ButtonTest](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/ButtonTest.java):

```java
try (Button button = new Button(inputPin, GpioPullUpDown.PULL_UP)) {
	button.addListener(event -> Logger.debug("valueChanged({})", event));
	Logger.debug("Waiting for 10s - *** Press the button connected to input pin " + inputPin + " ***");
	SleepUtil.sleepSeconds(10);
}
```

Controlling an LED with a button:

![Button controlled LED](images/Button_LED.png "Button controlled LED") 

Code taken from [ButtonControlledLed](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/ButtonControlledLed.java):

```java
try (Button button = new Button(buttonPin, GpioPullUpDown.PULL_UP); LED led = new LED(ledPin)) {
	button.whenPressed(led::on);
	button.whenReleased(led::off);
	Logger.info("Waiting for 10s - *** Press the button connected to pin {} ***", Integer.valueOf(buttonPin));
	SleepUtil.sleepSeconds(10);
}
```

### PIR Motion Sensor

### Line Sensor

## Analog Input Devices

The [AnalogInputDevice](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/AnalogInputDevice.java) base class encapsulates logic for interfacing with analog devices. This class provides access to unscaled (-1..1) and scaled (e.g. voltage, temperature, distance) readings. For scaled readings is important to pass the ADC voltage range in the device constructor - all raw analog readings are normalised (i.e. -1..1).

!!! note
    Note the Raspberry Pi does not natively support analog input devices, see [expansion boards](ExpansionBoards.md) for connecting to analog-to-digital converters.

The following analog devices are supported via subclasses of AnalogInputDevice:

* [com.diozero.TMP36](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/TMP36.java): Temperature Sensor [TMP36 by Analog Devices](http://www.analog.com/en/products/analog-to-digital-converters/integrated-special-purpose-converters/integrated-temperature-sensors/tmp36.html)
* [com.diozero.sandpit.Potentiometer](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sandpit/Potentiometer.java): Generic [potentiometer](https://en.wikipedia.org/wiki/Potentiometer)
* [com.diozero.LDR](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/LDR.java): Generic [Photoresistor / Light-Dependent-Resistor (LDR)](https://en.wikipedia.org/wiki/Photoresistor)
* [com.diozero.sandpit.GP2Y0A21YK](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sandpit/GP2Y0A21YK.java): [Sharp GP2Y0A21YK](http://www.sharpsma.com/webfm_send/1208) Distance Sensor

Example: Temperature readings using an MCP3008 and TMP36:

![MCP3008 TMP36](images/MCP3008_TMP36.png "MCP3008 TMP36") 

Code taken from [TMP36Test](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/TMP36Test.java):
```java
try (McpAdc adc = new McpAdc(type, chipSelect);
		TMP36 tmp36 = new TMP36(adc, pin, tempOffset, vRef)) {
	for (int i=0; i<ITERATIONS; i++) {
		double tmp = tmp36.getTemperature();
		Logger.info("Temperature: {}", String.format("%.2f", Double.valueOf(tmp)));
		SleepUtil.sleepSeconds(.5);
	}
}
```
