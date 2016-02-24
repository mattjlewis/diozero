# API

## Input Devices

### Digital Input Device

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

: Extends GPIODevice to provide common support for analog devices.

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
    
    * **pollInterval** (*int*) - Time in milliseconds at which reading should be taken.


### Smoothed Input Device

### Waitable Input Device

## Output Devices

### Digital Output Device

*class* **com.diozero.api.DigitalOutputDevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/DigitalOutputDevice.java)

: Extends GpioDevice to provide generic digital (on/off) output control.

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

### Motors (Digital and PWM)

## I2C Support

## SPI Support
