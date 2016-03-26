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

*class* com.diozero.**Button** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/Button.java){: .viewcode-link }

: Extends [DigitalInputDevice](API.md#digitalinputdevice].

    **Button** (*pinNumber*, *pud=NONE*, *trigger=BOTH*)
    
    : Constructor
    
    * **pinNumber** (*int*) - Pin number for the button.
    
    * **pud** (*GpioPullUpDown*) - Pull up / down configuration (NONE, PULL_UP, PULL_DOWN).
    
    * **trigger** (*GpioEventTrigger*) - What events to trigger (NONE, RISING, FALLING, BOTH).
    
    *boolean* **isPressed** ()
    
    : Return true if the button is currently pressed.
    
    *boolean* **isReleased** ()
    
    : Return true if the button is currently released.
    
    **whenPressed** (*action*)
    
    : Action to perform when the button is pressed.
    
    * **action** (*Action*) - Action function to invoke.
    
    **whenReleased** (*action*)
    
    : Action to perform when the button is released.
    
    * **action** (*Action*) - Action function to invoke.


### Motion Sensor

!!! Warning "Work-in-progress"
    Still under construction hence in the sandpit package.

*class* com.diozero.sandpit.**MotionSensor** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sandpit/MotionSensor.java){: .viewcode-link }

: Extends [SmoothedInputDevice](API.md#smoothedinputdevice] and represents a passive infra-red (PIR) motion sensor like the sort found in the [CamJam #2 EduKit](http://camjam.me/?page_id=623).

    **MotionSensor** (*pinNumber*, *threshold=10*, *eventAge=50*, *eventDetectPeriod=50*)
    
    : Constructor
    
    * **pinNumber** (*int*) - The GPIO pin which the button is attached to.
    
    * **threshold** (*int*) - The value above which the device will be considered "on".
    
    * **eventAge** (*int*) - The time in milliseconds to keep active events in the queue.
    
    * **eventDetectPeriod** (*int*) - How frequently to check for events.


## Analog Input Devices

### TMP36

*class* com.diozero.**TMP36** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/TMP36.java){: .viewcode-link }

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

*class* com.diozero.sandpit.**Potentiometer** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sandpit/Potentiometer.java){: .viewcode-link }

: Extends [AnalogInputDevice](API.md#analoginputdevice] for taking readings from a potentiometer.

    **Potentiometer** (*pinNumber*, *vRef*, *r1*)
    
    : Constructor
    
    * **pinNumber** (*int*) - Pin to which the potentiometer is connected.
    
    * **vRef** (*float*) - Reference voltage.
    
    * **r1** (*float*) - Resistor between the potentiometer and ground.

    *float* **getResistance** ()
    
    : Read the current resistance setting for the potentiometer.


### LDR

*class* com.diozero.**LDR** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/LDR.java){: .viewcode-link }

: Extends [AnalogInputDevce](API.md#analoginputdevice). Generic [Photoresistor / Light-Dependent-Resistor (LDR)](https://en.wikipedia.org/wiki/Photoresistor).

    **LDR** (*pinNumber*, *vRef*, *r1*)
    
    : Constructor
    
    * **pinNumber** (*int*) - Pin to which the LDR is connected.
    
    * **vRef** (*float*) - Reference voltage.
    
    * **r1** (*float*) - Resistor between the potentiometer and ground.

    *float* **getLdrResistance** ()
    
    : Read the current resistance across the LDR.

    *float* **getLuminosity** ()
    
    : Read the current luminosity.
    
    !!! warning "Not yet implemented"
        This operation currently just returns the resistance across the LDR.


### Sharp GP2Y0A21YK Distance Sensor

[Sharp GP2Y0A21YK](http://www.sharpsma.com/webfm_send/1208) Distance Sensor.

!!! Warning "Work-in-progress"
    Not yet tested hence in the sandpit package.

*class* com.diozero.sandpit.**GP2Y0A21YK** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sandpit/GP2Y0A21YK.java){: .viewcode-link }

: Extends [AnalogInputDevice](API.md#analoginputdevice] for taking object proximity readings.

    **GP2Y0A21YK** (*pinNumber*)
    
    : Constructor
    
    *float* **getDistanceCm** ()
    
    : Read distance, range 10 to 80cm.
    