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

*class* **com.diozero.LED** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/LED.java)

: Extends [DigitalOutputDevice](API.md#digital-output-device) and provides utility methods for controlling a Light Emitting Diode (LED). [Source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/LED.java).

    **LED** (*pinNumber*)

    : Constructor
    
    * **pinNumber** (*int*) - GPIO pin to which the LED is connected.

    **blink** ()

    : Blink indefinitely with 1 second on and 1 second off.
    
    **blink** (*onTime*, *offTime*, *n*, *background*)
    
    : Blink
    
    * **onTime** (*float*) - On time in seconds.
    
    * **offtime** (*float*) - Off time in seconds.
    
    * **n** (*int*) - Number of iterations. Set to -1 to blink indefinitely.
    
    * **background** (*boolean*) - If true start a background thread to control the blink and return immediately. If false, only return once the blink iterations have finished.


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

## RGB LED

## Buzzer

## PWM Output inc. servo control
