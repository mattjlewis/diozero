# Motor Control

Currently supports two basic types of motor controllers:

1.  Motor controller boards with just two separate PWM connections; one for forwards / clockwise control, the other for backwards / ant-clockwise control. Examples: CamJam EduKit #3 Robotics Kit, Ryanteck RPi Motor Controller Board.
2.  H-Bridge style motor drivers with three connections; PWM proportionate control, digital forwards / clockwise control, and digital backwards / anti-clockwise control. Examples: Toshiba TB6612FNG Dual Motor Driver.


## API


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


## Common Commercial Motor Controllers

### CamJamKitDualMotor

*class* com.diozero.**CamJamKitDualMotor** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/CamJamKitDualMotor.java)

: Extends [DualMotor](#dualmotor). Convenience class for interfacing with the [CamJam EduKit #3 Motor Controller Board](http://camjam.me/?page_id=1035) with pre-configured pin numbers (left -&gt; 9 and 10, right -&gt; 7 and 8).


### RyanteckDualMotor

*class* com.diozero.**RyanteckDualMotor** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/RyanteckDualMotor.java)

: Extends [DualMotor](#dualmotor). Convenience class for interfacing with the Ryanteck RPi Motor Controller Board with pre-configured pin numbers (left -&gt; 17 and 18, right -&gt; 22 and 23).


### TB6612FNGDualMotorDriver

*class* com.diozero.sandpit.**TB6612FNGDualMotorDriver** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sandpit/TB6612FNGDualMotorDriver.java)

: Extends [DualMotor](#dualmotor). Convenience class for interfacing with dual H-bridge motor drivers such as the [Toshiba TB6612FNG Dual Motor Driver](http://toshiba.semicon-storage.com/info/lookup.jsp?pid=TB6612FNG&lang=en) as used in the [Pololu Dual Motor Driver Carrier](https://www.pololu.com/product/713).

