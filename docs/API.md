# API

## Base classes

### GPIODevice

*class* **com.diozero.api.GPIODevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/GPIODevice.java)

: Base class for all GPIO related devices.

    **GPIODevice** (*pinNumber*)
    
    : Constructor
    
    * **pinNumber** (*int*) - Pin number to which the device is connected.
    
    *int* **getPinNumber** ()
    
    : Get the GPIO pin number for this device.
    

## Input Devices

### GPIOInputDevice

*class* **com.diozero.api.GPIOInputDevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/GPIOInputDevice.java)

: Common base class for digital and analog input devices, extends [GPIODevice](#gpiodevice).

    **GPIOInputDevice** (*pinNumber*)
    
    : Constructor
    
    * **pinNumber** (*int*) - Pin number to which the device is connected.

    **addListener** (*listener*)
    
    : Add a new listener.
    
    * listener (*InputEventListener&lt;T extends DeviceEvent&gt;)* - Callback instance.

    **removeListener** (*listener*)
    
    : Remove a specific listener.
    
    * listener (*InputEventListener&lt;T extends DeviceEvent&gt;)* - Callback instance to remove.

    **removeAllListeners** ()
    
    : Remove all listeners.


### Digital Input Device

*class* **com.diozero.api.DigitalInputDevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/DigitalInputDevice.java)

: Extends [GPIOInputDevice](#gpioinputdevice) to provide common support for digital devices.

    **DigitalInputDevice** (*pinNumber*, *pud*, *trigger*)
    
    : Constructor
    
    * **pinNumber** (*int*) - Pin number to which the device is connected.
    
    * **pud** (*GpioPullUpDown*) - Pull up/down configuration, values: NONE, PULL_UP, PULL_DOWN.
    
    * **trigger** (*GpioEventTrigger*) - Event trigger configuration, values: NONE, RISING, FALLING, BOTH
    
    *boolean* **getValue** ()
    
    : Read the current underlying state of the input pin
    
    *boolean* **isActive** ()
    
    : Read the current on/off state for this device taking into account the pull up / down configuration. If the input is pulled up **isActive**() will return `true` when when the value is `false`.


### Waitable Input Device

*class* **com.diozero.api.WaitableInputDevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/WaitableInputDevice.java)

: Extends [DigitalInputDevice](#digital-input-device) to support waiting for state changes.

    **WaitableInputDevice** (*pinNumber*, *pud*, *trigger*)
    
    : Constructor
    
    * **pinNumber** (*int*) - Pin number to which the device is connected.
    
    * **pud** (*GpioPullUpDown*) - Pull up/down configuration, values: NONE, PULL_UP, PULL_DOWN.
    
    * **trigger** (*GpioEventTrigger*) - Event trigger configuration, values: NONE, RISING, FALLING, BOTH
    
    **waitForActive** (*timeout=0*)
    
    : Wait for the input device to go active.
    
    * **timeout** (*int*) - Timeout value in milliseconds. Values &lt;= 0 represent an indefinite amount of time. Defaults to 0.
    
    **waitForInactive** (*timeout=0*)
    
    : Wait for the input device to go inactive.
    
    * **timeout** (*int*) - Timeout value in milliseconds. Values &lt;= 0 represent an indefinite amount of time. Defaults to 0.


### Smoothed Input Device

*class* **com.diozero.api.SmoothedInputDevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/SmoothedInputDevice.java)

: Extends [DigitalInputDevice](#digital-input-device) to support waiting for state changes.

    **SmoothedInputDevice** (*pinNumber*, *pud*, *threshold*, *age*)
    
    : Constructor
    
    * **pinNumber** (*int*) - Pin number to which the device is connected.
    
    * **pud** (*GpioPullUpDown*) - Pull up/down configuration, values: NONE, PULL_UP, PULL_DOWN.
    
    * **threshold** (*int*) - The value above which the device will be considered "on".
    
    * **age** (*int*) - The time in milliseconds to keep items in the queue.
    
    *int* **getThreshold** ()
    
    : If the number of on events younger than age exceeds this amount, then 'isActive' will return 'True'.
    
    **setThreshold** (threshold)
    
    : Set the threshold.
    
    * **threshold** (*int*) - New threshold value in terms of number of on events within the specified time period that will trigger an on event to any listeners.


### Analog Input Device

The [AnalogInputDevice](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/AnalogInputDevice.java) base class encapsulates logic for interfacing with analog devices. This class provides access to unscaled (-1..1) and scaled (e.g. voltage, temperature, distance) readings. For scaled readings is important to pass the ADC voltage range in the device constructor - all raw analog readings are normalised (i.e. -1..1).

!!! note
    Note the Raspberry Pi does not natively support analog input devices, see [expansion boards](ExpansionBoards.md#mcp-adc) for connecting to analog-to-digital converters.

Example: Temperature readings using an MCP3008 and TMP36:

![MCP3008 TMP36](images/MCP3008_TMP36.png "MCP3008 TMP36") 

Code taken from [TMP36Test](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/TMP36Test.java):
```java
try (McpAdc adc = new McpAdc(type, chipSelect);
		TMP36 tmp36 = new TMP36(adc, pin, vRef, tempOffset)) {
	for (int i=0; i<ITERATIONS; i++) {
		double tmp = tmp36.getTemperature();
		Logger.info("Temperature: {}", String.format("%.2f", Double.valueOf(tmp)));
		SleepUtil.sleepSeconds(.5);
	}
}
```

*class* **com.diozero.api.AnalogInputDevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/AnalogInputDevice.java)

: Extends [GPIOInputDevice](#gpioinputdevice) to provide common support for analog devices.

    **AnalogInputDevice** (*pinNumber*, *range*)
    
    : Constructor
    
    * **pinNumber** (*int*) - Pin number to which the device is connected.
    
    * **range** (*float*) - To be used for taking scaled readings for this device.
    
    *float* **getUnscaledValue** ()
    
    : Get the unscaled normalised value in the range 0..1 (if unsigned) or -1..1 (if signed).
    
    *float* **getScaledValue** ()
    
    : Get the scaled value in the range 0..range (if unsigned) or -range..range (if signed).
    
    **addListener** (*listener*, *percentChange*, *pollInterval=50*)
    
    : Register a listener for value changes.
    
    * **listener** (*InputEventListener&lt;AnalogInputEvent&gt;*) - The listener callback.
    
    * **percentChange** (*float*) - Degree of change required to trigger an event.
    
    * **pollInterval** (*int=50*) - Time in milliseconds at which reading should be taken.


## Output Devices

### Digital Output Device

*class* **com.diozero.api.DigitalOutputDevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/DigitalOutputDevice.java)

: Extends [GPIODevice](#gpiodevice) to provide generic digital (on/off) output control.

    **DigitalOutputDevice** (*pinNumber*, *activeHigh=true*, *initialValue=false*)
    
    : Constructor
    
    * **pinNumber** (*int*) - GPIO pin to which the output device is connected.
    
    * **activeHigh** (*boolean*) - If true then setting the value to true will turn on the connected device.
    
    * **initialValue** (*boolean*) - Initial output value.
    
    **on** ()
    
    : Turn on the device.
    
    **off** ()
    
    : Turn off the device.
    
    **toggle** ()
    
    : Toggle the state of the device
    
    *boolean* **isOn** ()
    
    : Returns true if the device is currently on.
    
    **setOn** (*on*)
    
    : Turn the device on or off.
    
    * **value** (*boolean*) - New value, true == on, false == off.
    
    **setValueUnsafe** (*value*)
    
    : Unsafe operation that has no synchronisation checks and doesn't factor in active low logic. Included primarily for performance tests.
    
    * **value** (*boolean*) - New value, true == on, false == off.


### PWM Output Device

*class* **com.diozero.api.PWMOutputDevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/PWMOutputDevice.java)

: Extends [GPIODevice](#gpiodevice) to provide generic [Pulse Width Modulation](https://en.wikipedia.org/wiki/Pulse-width_modulation) (PWM) output control.

    **PWMOutputDevice** (*pinNumber*, *initialValue=0*)
    
    : Constructor
    
    * **pinNumber** (*int*) - GPIO pin to which the output device is connected.
    
    * **initialValue** (*float*) - Initial output value (0..1).
    
    *float* **getValue** ()
    
    : Get the current PWM output value (0..1).
    
    **setValue** (*value*)
    
    : Set the PWM output value (0..1).
    
    *float* **value** - the new PWM output value (0..1).
    
    **on** ()
    
    : Turn on the device (same as `setValue(1)]`.
    
    **off** ()
    
    : Turn off the device (same as `setValue(0)`.
    
    **toggle** ()
    
    : Toggle the state of the device (same as `setValue(1 - getValue()`).
    
    *boolean* **isOn** ()
    
    : Returns true if the device currently has a value &gt; 0.


### Motors (Digital and PWM)

## I2C Support

## SPI Support
