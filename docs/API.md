# API

## Base classes

### GPIODevice

*class* com.diozero.api.**GPIODevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/GPIODevice.java)

: Abstract base class for all GPIO related devices.

    **GPIODevice** (*pinNumber*)
    
    : Constructor
    
    * **pinNumber** (*int*) - Pin number to which the device is connected.
    
    *int* **getPinNumber** ()
    
    : Get the GPIO pin number for this device.
    

## Input Devices

### GPIOInputDevice

*class* com.diozero.api.**GPIOInputDevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/GPIOInputDevice.java)

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


### DigitalInputDevice

*class* com.diozero.api.**DigitalInputDevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/DigitalInputDevice.java)

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


### WaitableInputDevice

*class* com.diozero.api.**WaitableInputDevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/WaitableInputDevice.java)

: Extends [DigitalInputDevice](#digitalinputdevice) to support waiting for state changes.

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


### SmoothedInputDevice

*class* com.diozero.api.**SmoothedInputDevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/SmoothedInputDevice.java)

: Extends [DigitalInputDevice](#digitalinputdevice) to support waiting for state changes.

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


### AnalogInputDevice

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

*class* com.diozero.api.**AnalogInputDevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/AnalogInputDevice.java)

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

### DigitalOutputDevice

*class* com.diozero.api.**DigitalOutputDevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/DigitalOutputDevice.java)

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


### PWMOutputDevice

*class* com.diozero.api.**PWMOutputDevice** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/PWMOutputDevice.java)

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


## Motors

Divided into two distinct types;

1.  Motor controller boards with just two separate PWM connections; one for forwards / clockwise control, the other for backwards / ant-clockwise control. Examples: CamJam EduKit #3 Robotics Kit, Ryanteck RPi Motor Controller Board.
2.  H-Bridge style motor drivers with three connections; PWM proportionate control, digital forwards / clockwise control, and digital backwards / anti-clockwise control. Examples: Toshiba TB6612FNG Dual Motor Driver.


### MotorInterface

*interface* **com.diozero.api.MotorInterface** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/MotorInterface.java)

: Represents a single motor.

    **Motor** (*forwardPin*, *backwardPin*)
    
    : Constructor.
    
    * **forwardPin** (*int*) - PWM-capable GPIO for forward / clockwise control.
    
    * **backwardPin** (*int*) - PWM-capable GPIO for backward / anti-clockwise control.
    
    **forward** (*speed=1*)
    
    : Turn the motor forward at the specified speed (range 0..1).
    
    * **speed** (*float*) - Speed, range 0..1. Defaults to 1.
    
    **backward** (*speed=1*)
    
    : Turn the motor backward at the specified speed (range 0..1).
    
    * **speed** (*float*) - Speed, range 0..1. Defaults to 1.
    
    **stop** ()
    
    : Stop the motor.
    
    **reverse** ()
    
    : Reverse the direction of the motor.
    
    *float* **getValue** ()
    
    : Get the current motor direction and speed, between -1 (full speed backward) and 1 (full speed forward).
    
    **setValue** (*value*)
    
    : Set the motor direction and speed.
    
    * **value** (*float*) - Relative value between -1 (full speed backward) and 1 (full speed forward).
    
    *boolean* **isActive** ()
    
    : Return `true` if the motor is moving in either direction, `false` if stopped.


### Motor

*class* com.diozero.api.**Motor** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/Motor.java)

: Represents a single motor controlled by two separate PWM signals. Implements [MotorInterface](#motorinterface).

    **Motor** (*forwardPin*, *backwardPin*)
    
    : Constructor.
    
    * **forwardPin** (*int*) - PWM-capable GPIO for forward / clockwise control.
    
    * **backwardPin** (*int*) - PWM-capable GPIO for backward / anti-clockwise control.

    **Motor** (*deviceFactory*, *forwardPin*, *backwardPin*)
    
    : Constructor.
    
    * **deviceFactory** (*PwmOutputDeviceFactoryInterface*) - Device factory to use for constructing the [PWMOutputDevice](API#pwmoutputdevice) instances.
    
    * **forwardPin** (*int*) - PWM-capable GPIO for forward / clockwise control.
    
    * **backwardPin** (*int*) - PWM-capable GPIO for backward / anti-clockwise control.

    **Motor** (*forward*, *backward*)
    
    : Constructor.
    
    * **forward** (*[PwmOutputDevice](API.md#pwmoutputdevice)*) - PWM output device for forward / clockwise control.
    
    * **backward** (*[PwmOutputDevice](API.md#pwmoutputdevice)*) - PWM output device for backward / anti-clockwise control.


### TB6612FNGMotor

*class* com.diozero.sandpit.**TB6612FNGMotor** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sandpit/TB6612FNGMotor.java)

: Represents a single motor controlled by one PWM signal and two separate forwards / backwards control digital signals. Implements [MotorInterface](#motorinterface).

    **TB6612FNGMotor** (*motorForwardControlPin*, *motorBackwardControlPin*, *motorPwmControl*)
    
    : Constructor
    
    * **motorForwardControlPin** (*[DigitalOutputDevice](API.md#digitaloutputdevice*) - Digital device controlling forward movement.
    
    * **motorBackwardControlPin** (*[DigitalOutputDevice](API.md#digitaloutputdevice*) - Digital device controlling backward movement.
    
    * **motorPwmControl** (*[PwmOutputDevice](API.md#pwmoutputdevice*) - PWM output device controlling relative motor speed.


### DualMotor

*class* com.diozero.api.**DualMotor** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/DualMotor.java)

: Convenience class for a robot with two [Motors](#motorinterface).

    **DualMotor** (*leftMotor*, *rightMotor*)
    
    : Constructor
    
    * **leftMotor** (*[MotorInterface](#motorinterface)*) - The left motor
    
    * **rightMotor** (*[MotorInterface](#motorinterface)*) - The right motor
    
    *float[]* **getValues** ()
    
    : Get the directional values for both motors (-1 backwards .. 1 forwards).
    
    **setValues** (*leftValue*, *rightValue*)
    
    : Set the speed and direction for both let and right motors (clockwise / counter-clockwise).
    
    * **leftValue** (*float*) - Range -1 .. 1. Positive numbers for clockwise, Negative numbers for counter clockwise.
    
    * **rightValue** (*float*) - Range -1 .. 1. Positive numbers for anti-clockwise, Negative numbers for counter clockwise.
    
    **forward** (*speed*)
    
    : Set both motors forward at the specific speed. Range 0..1.
    
    **backward** (*speed*)
    
    : Set both motors backward at the specific speed. Range 0..1.
    
    **rotateLeft** (*speed*)
    
    : Turn the left motor backward and the right motor forward at the specific speed. Range 0..1.
    
    **rotateRight** (*speed*)
    
    : Turn the left motor forward and the right motor backward at the specific speed. Range 0..1.
    
    **reverse** ()
    
    : Reverse the direction of both motors.
    
    **stop** ()
    
    : Stop both motors.


### TB6612FNGDualMotorDriver

*class* com.diozero.sandpit.**TB6612FNGDualMotorDriver** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sandpit/TB6612FNGDualMotorDriver.java)

: Extends [DualMotor](#dualmotor). Convenience class for interfacing with dual H-bridge motor drivers.


### CamJamKitDualMotor

*class* com.diozero.**CamJamKitDualMotor** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/CamJamKitDualMotor.java)

: Extends [DualMotor](#dualmotor). Convenience class for interfacing with the CamJam Edit Kit #3 motor controller board with pre-configured pin numbers (left -&gt; 9 and 10, right -&gt; 7 and 8).


### RyanteckDualMotor

*class* com.diozero.**RyanteckDualMotor** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/RyanteckDualMotor.java)

: Extends [DualMotor](#dualmotor). Convenience class for interfacing with the Ryanteck motor controller board with pre-configured pin numbers (left -&gt; 17 and 18, right -&gt; 22 and 23).


## I2C Support

## SPI Support
