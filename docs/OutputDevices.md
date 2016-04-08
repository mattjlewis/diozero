# Output Devices

## Digital LED

Connect the cathode (short leg, flat side) of the LED to a ground pin; connect the anode (longer leg) to a limiting resistor; connect the other side of the limiting resistor to a GPIO pin (the limiting resistor can be placed either side of the LED).

TODO Wiring Diagram?

Example LED control, taken from [LEDTest](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/LEDTest.java):

```java
try (LED led = new LED(pin)) {
	led.on();
	SleepUtil.sleepSeconds(.5);
	led.off();
	SleepUtil.sleepSeconds(.5);
	led.toggle();
	SleepUtil.sleepSeconds(.5);
	led.toggle();
	SleepUtil.sleepSeconds(.5);
	led.blink(0.5f, 0.5f, 10, false);
}
```

*class* **com.diozero.LED**{: .descname } (*pinNumber*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/LED.java){: .viewcode-link } [&para;](OutputDevices.md#led "Permalink to this definition"){: .headerlink }

: Extends [DigitalOutputDevice](API.md#digitaloutputdevice) and provides utility methods for controlling a Light Emitting Diode (LED).
    
    * **pinNumber** (*int*) - GPIO pin to which the LED is connected.

    **blink** ()

    : Blink indefinitely with 1 second on and 1 second off.
    
    **blink** (*onTime*, *offTime*, *n*, *background*)
    
    : Blink
    
    * **onTime** (*float*) - On time in seconds.
    
    * **offtime** (*float*) - Off time in seconds.
    
    * **n** (*int*) - Number of iterations. Set to -1 to blink indefinitely.
    
    * **background** (*boolean*) - If true start a background thread to control the blink and return immediately. If false, only return once the blink iterations have finished.


## Buzzer

*class* **com.diozero.Buzzer**{: .descname } (*pinNumber*, *activeHigh*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/Buzzer.java){: .viewcode-link } [&para;](OutputDevices.md#buzzer "Permalink to this definition"){: .headerlink }

: Extends [DigitalOutputDevice](API.md#digitaloutputdevice) and represents a digital buzzer component.
    
    * **pinNumber** (*int*) - The GPIO pin which the buzzer is attached to.
    
    * **activeHigh** (*boolean*) - Set to `true` if the circuit is wired such that the buzzer will beep when the output is high.
    
    **beep** (*onTime=1*, *offTime=1*, *n=-1*, *background=true*)
    
    : Turn the device turn on and off repeatedly.
    
    * **onTime** (*float*) - Number of seconds on. Defaults to 1 second.
    
    * **offTime** (*float*) - Number of seconds off. Defaults to 1 second.
    
    * **n** (*int*) - Number of times to blink; any value &lt; 0 (the default) means forever.
    
    * **background** (*boolean*) - If `True` (the default), start a background thread to continue blinking and return immediately. If `False`, only return when the blink is finished (warning: the default value of n will result in this method never returning).


## PWM LED

Code, taken from [PwmLedTest](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/PwmLedTest.java):

```java
float delay = 0.5f;
try (PwmLed led = new PwmLed(pin)) {
	led.on();
	SleepUtil.sleepSeconds(delay);
	led.off();
	SleepUtil.sleepSeconds(delay);
	led.toggle();
	SleepUtil.sleepSeconds(delay);
	led.toggle();
	SleepUtil.sleepSeconds(delay);
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

*class* **com.diozero.PwmLed**{: .descname } (*pinNumber*, *initialValue=0*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/PwmLed.java){: .viewcode-link } [&para;](OutputDevices.md#pwm-led "Permalink to this definition"){: .headerlink }

: Extends [DigitalOutputDevice](API.md#digitaloutputdevice) and represents a PWM controlled LED.
    
    * **pinNumber** (*int*) - The GPIO pin which the LED is attached to.
    
    * **initialValue=0** (*float*) - Initial PWM output value (range 0..1).
    
    **blink** ()
    
    : Blink the LED on and off indefinitely.
    
    **blink** (*onTime*, *offTime*, *iterations*, *background*)
    
    : Blink the LED.
    
    * **onTime** (*float*) - On time in seconds.
    
    * **offTime** (*float*) - Off time in seconds.
    
    * **iterations** (*int*) - Number of iterations. Set to &lt;0 to blink indefinitely.
    
    * **background** (*boolean*) - If true start a background thread to control the blink and return immediately. If false, only return once the blink iterations have finished.

    **pulse** (*fadeTime=1*, *steps=50*, *iterations=-1*, *background=true*)
    
    : Pulse the LED on and off repeatedly.
    
    * **fadeTime** (*float*) - Time from fully on to fully off. Default 1.
    
    * **steps** (*int*) - Number of steps between fully on to fully off. Default 50.
    
    * **iterations** (*int*) - Number of times to fade in and out. Default infinite.
    
    * **background** (*boolean*) - If true start a background thread to control the blink and return immediately. If false, only return once the blink iterations have finished.

    **isLit** ()
    
    : Return true if the PWM value is &gt;0, false if 0.


## RGB LED

*class* **com.diozero.RgbLed**{: .descname } (*pinNumber*, *initialValue=0*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/RgbLed.java){: .viewcode-link } [&para;](OutputDevices.md#rgb-led "Permalink to this definition"){: .headerlink }

: Extends [DigitalOutputDevice](API.md#digitaloutputdevice) and represents a PWM controlled LED.
    
