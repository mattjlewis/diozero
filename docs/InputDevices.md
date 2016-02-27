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

### TMP36

*class* com.diozero.**TMP36** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/TMP36.java)

: Extends [AnalogInputDevice](API.md#analoginputdevice] for reading temperature values from a [TMP36 Temperature Sensor by Analog Devices](http://www.analog.com/en/products/analog-to-digital-converters/integrated-special-purpose-converters/integrated-temperature-sensors/tmp36.html).

    **TMP36** (*pinNumber*, *vRef*, *tempOffset*)
    
    : Constructor
    
    * **pinNumber** (*int*) - Pin number on the ADC device.
    
    * **vRef*** (*float*) - Voltage range for the ADC - essential for scaled readings.
    
    * **tempOffset*** (*float*) - Compensate for potential temperature reading variations between different TMP36 devices.
    
    *float* **getTemperature** ()
    
    : Get the current temperature in &#8451;


### Potentiometer

Generic [potentiometer](https://en.wikipedia.org/wiki/Potentiometer).

TODO Wiring diagram.

!!! Warning "Work-in-progress"
    Still under construction hence in the sandpit package.

*class* com.diozero.sandpit.**Potentiometer** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sandpit/Potentiometer.java)

: Extends [AnalogInputDevice](API.md#analoginputdevice] for taking readings from a potentiometer.


### LDR

[com.diozero.LDR](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/LDR.java): Generic [Photoresistor / Light-Dependent-Resistor (LDR)](https://en.wikipedia.org/wiki/Photoresistor)

### Sharp GP2Y0A21YK Distance Sensor

[Sharp GP2Y0A21YK](http://www.sharpsma.com/webfm_send/1208) Distance Sensor.

!!! Warning "Work-in-progress"
    Not yet tested hence in the sandpit package.

*class* com.diozero.sandpit.**GP2Y0A21YK** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sandpit/GP2Y0A21YK.java)

: Extends [AnalogInputDevice](API.md#analoginputdevice] for taking object proximity readings. 
