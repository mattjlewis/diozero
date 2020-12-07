---
parent: API
nav_order: 1
permalink: /api/gpio.html
---

# General Purpose Input / Output (GPIO)

* [API](API.md) for lower-level interactions
    * [Input](API.md#input-devices), [Output](API.md#output-devices), [I2C](API.md#i2c-support), [SPI](API.md#spi-support)
* [Input Devices](InputDevices.md)
    * [Digital](InputDevices.md#digital-input-devices) and [Analog](InputDevices.md#analog-input-devices)
* [Output Devices](OutputDevices.md)
    * [Digital](OutputDevices.md#digital-led) and [PWM](OutputDevices.md#pwm-led)

## Base classes

### GpioDevice

*class* **com.diozero.api.GpioDevice**{: .descname } (*gpio*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/GpioDevice.java){: .viewcode-link } [&para;](API.md#gpiodevice "Permalink to this definition"){: .headerlink }

: Abstract base class for all GPIO related devices.
    
    * **gpio** (*int*) - GPIO to which the device is connected.
    
    *int* **getGpio** ()
    
    : Get the GPIO for this device.
    

## Input Devices

### GpioInputDevice

*class* **com.diozero.api.GpioInputDevice**{: .descname } (*gpio*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/GpioInputDevice.java){: .viewcode-link } [&para;](API.md#gpioinputdevice "Permalink to this definition"){: .headerlink }

: Common base class for digital and analog input devices, extends [GpioDevice](#gpiodevice).
    
    * **gpio** (*int*) - GPIO to which the device is connected.

    **addListener** (*listener*)
    
    : Add a new listener.
    
    * **listener** (*InputEventListener&lt;T extends DeviceEvent&gt;)* - Callback instance.

    **removeListener** (*listener*)
    
    : Remove a specific listener.
    
    * **listener** (*InputEventListener&lt;T extends DeviceEvent&gt;)* - Callback instance to remove.

    **removeAllListeners** ()
    
    : Remove all listeners.


### DigitalInputDevice

*class* **com.diozero.api.DigitalInputDevice**{: .descname } (*gpio*, *pud=NONE*, *trigger=BOTH*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/DigitalInputDevice.java){: .viewcode-link } [&para;](API.md#digitalinputdevice "Permalink to this definition"){: .headerlink }

: Extends [GpioInputDevice](#gpioinputdevice) to provide common support for digital devices.
    
    * **gpio** (*int*) - GPIO to which the device is connected.
    
    * **pud** (*GpioPullUpDown*) - Pull up/down configuration, values: NONE, PULL_UP, PULL_DOWN. Defaults to NONE.
    
    * **trigger** (*GpioEventTrigger*) - Event trigger configuration, values: NONE, RISING, FALLING, BOTH. Defaults to BOTH.
    
    *GpioPullUpDown* **getPullUpDown** ()
    
    : Get pull up / down configuration.
    
    *GpioEventTrigger* **getTrigger** ()
    
    : Get event trigger configuration.
    
    *boolean* **isActiveHigh** ()
    
    : Returns false if configured as pull-up, true for all other pull up / down options.
    
    *boolean* **getValue** ()
    
    : Read the current underlying state of the input pin. Does not factor in active high logic.
    
    *boolean* **isActive** ()
    
    : Read the current on/off state for this device taking into account the pull up / down configuration. If the input is pulled up **isActive** () will return `true` when when the value is `false`.
    
    **whenActivated** (*action*)
    
    : Action to perform when the device state is active.
    
    * **action** (*Action*) - Action callback object.
    
    **whenDectivated** (*action*)
    
    : Action to perform when the device state is inactive.
    
    * **action** (*Action*) - Action callback object.


### WaitableDigitalInputDevice

*class* **com.diozero.api.WaitableDigitalInputDevice**{: .descname } (*gpio*, *pud=NONE*, *trigger=BOTH*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/WaitableDigitalInputDevice.java){: .viewcode-link } [&para;](API.md#waitabledigitalinputdevice "Permalink to this definition"){: .headerlink }

: Extends [DigitalInputDevice](#digitalinputdevice) to support waiting for state changes.
    
    * **gpio** (*int*) - GPIO to which the device is connected.
    
    * **pud** (*GpioPullUpDown*) - Pull up/down configuration, values: NONE, PULL_UP, PULL_DOWN.. Defaults to NONE.
    
    * **trigger** (*GpioEventTrigger*) - Event trigger configuration, values: NONE, RISING, FALLING, BOTH. Defaults to BOTH.
    
    **waitForActive** (*timeout=0*)
    
    : Wait for the specified time period (in millieconds) for the device to go active.
    
    * **timeout** (*int*) - Timeout value in milliseconds. Timeout values &lt;= 0 represent an indefinite amount of time.
    
    **waitForInactive** (*timeout=0*)
    
    : Wait for the specified time period (in millieconds) for the device to go inactive.
    
    * **timeout** (*int*) - Timeout value in milliseconds. Timeout values &lt;= 0 represent an indefinite amount of time.
    
    **waitForValue** (*value*, *timeout*)
    
    : Wait for a specific input high / low state.
    
    * **timeout** (*int*) - Timeout value in milliseconds. Timeout values &lt;= 0 represent an indefinite amount of time.


### SmoothedInputDevice

*class* **com.diozero.api.SmoothedInputDevice**{: .descname } (*gpio*, *pud*, *threshold*, *eventAge*, *eventDetectPeriod*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/SmoothedInputDevice.java){: .viewcode-link } [&para;](API.md#smoothedinputdevice "Permalink to this definition"){: .headerlink }

: Represents a generic input device which takes its value from the number of active events over a specific time period.
This class extends [WaitableDigitalInputDevice](#waitabledigitalinputdevice) with a queue which is added to whenever the input device is active. The number of the active events in the queue is compared to a threshold which is used to determine the state of the 'active' property.
Any active events over the specified eventAge are removed by a background thread.
This class is intended for use with devices which exhibit "twitchy" behaviour (such as certain motion sensors).
    
    * **gpio** (*int*) - GPIO to which the device is connected.
    
    * **pud** (*GpioPullUpDown*) - Pull up/down configuration, values: NONE, PULL_UP, PULL_DOWN.
    
    * **threshold** (*int*) - The value above which the device will be considered "on".
    
    * **eventAge** (*int*) - The time in milliseconds to keep items in the queue.
    
    * **eventDetectPeriod** (*int*) - How frequently to check for events.
    
    *int* **getThreshold** ()
    
    : If the number of on events younger than age exceeds this amount, then `isActive` will return `true`.
    
    **setThreshold** (threshold)
    
    : Set the threshold.
    
    * **threshold** (*int*) - New threshold value in terms of number of on events within the specified time period that will trigger an on event to any listeners.
    
    *int* **getEventAge** ()
    
    : The time in milliseconds to keep items in the queue.
    
    **setEventAge** (eventAge)
    
    : Set the event age (milliseconds).
    
    * **eventAge** (*int*) - New event age value (milliseconds).
    
    *int* **getEventDetectPeriod** ()
    
    : How frequently (in milliseconds) to check the state of the queue.


### AnalogInputDevice

The [AnalogInputDevice](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/AnalogInputDevice.java) base class encapsulates logic for interfacing with analog devices. This class provides access to unscaled (-1..1) and scaled (e.g. voltage, temperature, distance) readings. For scaled readings is important to pass the ADC voltage range in the device constructor - all raw analog readings are normalised (i.e. -1..1).

> ❗ **Analog Device Support**
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

*class* **com.diozero.api.AnalogInputDevice** (*gpio*, *range*){: .descname } [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/AnalogInputDevice.java){: .viewcode-link } [&para;](API.md#analoginputdevice "Permalink to this definition"){: .headerlink }

: Extends [GpioInputDevice](#gpioinputdevice) to provide common support for analog devices.
    
    * **gpio** (*int*) - GPIO to which the device is connected.
    
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


### DigitalInputOutputDevice

*class* **com.diozero.api.DigitalInputOutputDevice**{: .descname } (*gpio*, *mode*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/DigitalInputOutputDevice.java){: .viewcode-link } [&para;](API.md#digitalinputoutputdevice "Permalink to this definition"){: .headerlink }

: Extends [GpioDevice](#gpiodevice) that allows switching between input and output modes. Note currently only provides basic get/set functionality.
    
    * **gpio** (*int*) - GPIO pin to which the output device is connected.
    
    * **mode** (*DeviceMode*) - Initial input / output mode.
    
    *DeviceMode* **getMode** ()
    
    : Get the current input / output mode.
    
    **setMode** (mode)
    
    : Change input / output mode.
    
    **mode** (*DeviceMode*) - new mode for the device.
    
    *boolean* **getValue** ()
    
    : Get the current device state.
    
    **setValue** (value)
    
    : Set the output value.
    
    **value** (*boolean*) - output on/off state.


## Output Devices

### DigitalOutputDevice

*class* **com.diozero.api.DigitalOutputDevice**{: .descname } (*gpio*, *activeHigh=true*, *initialValue=false*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/DigitalOutputDevice.java){: .viewcode-link } [&para;](API.md#digitaloutputdevice "Permalink to this definition"){: .headerlink }

: Extends [GpioDevice](#gpiodevice) to provide generic digital (on/off) output control.
    
    * **gpio** (*int*) - GPIO pin to which the output device is connected.
    
    * **activeHigh** (*boolean*) - If true then setting the value to true will turn on the connected device.
    
    * **initialValue** (*boolean*) - Initial output value.
    
    **on** ()
    
    : Turn on the device.
    
    **off** ()
    
    : Turn off the device.
    
    **toggle** ()
    
    : Toggle the state of the device.
    
    *boolean* **isOn** ()
    
    : Returns true if the device is currently on.
    
    **setOn** (*on*)
    
    : Turn the device on or off.
    
    * **on** (*boolean*) - New on/off value.
    
    **setValueUnsafe** (*value*)
    
    : Unsafe operation that has no synchronisation checks and doesn't factor in active low logic. Included primarily for performance tests.
    
    * **value** (*boolean*) - New value, true == on, false == off.
    
    **onOffLoop** (*ontime*, *offTime*, *n*, *background*)
    
    : Toggle the device on-off.
    
    * **onTime** (*float*) - On time in seconds.
    
    * **offtime** (*float*) - Off time in seconds.
    
    * **n** (*int*) - Number of iterations. Set to -1 to blink indefinitely.
    
    * **background** (*boolean*) - If true start a background thread to control the blink and return immediately. If false, only return once the blink iterations have finished.


### PwmOutputDevice

*class* **com.diozero.api.PwmOutputDevice**{: .descname } (*gpio*, *initialValue=0*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/PwmOutputDevice.java){: .viewcode-link } [&para;](API.md#pwmoutputdevice "Permalink to this definition"){: .headerlink }

: Extends [GpioDevice](#gpiodevice) to provide generic [Pulse Width Modulation](https://en.wikipedia.org/wiki/Pulse-width_modulation) (PWM) output control.
    
    * **gpio** (*int*) - GPIO pin to which the output device is connected.
    
    * **initialValue** (*float*) - Initial output value (0..1).
    
    *float* **getValue** ()
    
    : Get the current PWM output value (0..1).
    
    **setValue** (*value*)
    
    : Set the PWM output value (0..1).
    
    * **value** (*float*) - the new PWM output value (0..1).
    
    **on** ()
    
    : Turn on the device (same as `setValue(1)`).
    
    **off** ()
    
    : Turn off the device (same as `setValue(0)`).
    
    **toggle** ()
    
    : Toggle the state of the device (same as `setValue(1 - getValue()`).
    
    *boolean* **isOn** ()
    
    : Returns true if the device currently has a value &gt; 0.
