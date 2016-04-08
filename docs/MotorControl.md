# Motor Control

Currently supports the following types of motors:

1.  Motor controller boards with just two separate PWM connections; one for forwards / clockwise control, the other for backwards / ant-clockwise control. Examples: CamJam EduKit #3 Robotics Kit, Ryanteck RPi Motor Controller Board.
2.  H-Bridge style motor drivers with three connections; PWM proportionate control, digital forwards / clockwise control, and digital backwards / anti-clockwise control. Examples: Toshiba TB6612FNG Dual Motor Driver.
3.  Servos


## API


### MotorInterface

*interface* **com.diozero.api.motor.MotorInterface**{: .descname } [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/motor/MotorInterface.java){: .viewcode-link } [&para;](MotorControl.md#motorinterface "Permalink to this definition"){: .headerlink }

: Represents a single motor.
    
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
    
    **whenForward** (*action*)
    
    : Action to perform when going forward
    
    * **action** (*Action*) - Callback action object
    
    **whenBackward** (*action*)
    
    : Action to perform when going backward
    
    * **action** (*Action*) - Callback action object
    
    **whenStop** (*action*)
    
    : Action to perform when stopped
    
    * **action** (*Action*) - Callback action object
    
    **addListener** (*listener*)
    
    : Listener to be notified when the motor value changes
    
    * **listener** (*MotorListener*) - Listener instance
    
    **removeListener** (*listener*)
    
    : Remove a listener
    
    * **listener** (*MotorListener*) - Listener instance


### MotorBase

*interface* **com.diozero.api.motor.MotorBase**{: .descname } [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/motor/MotorBase.java){: .viewcode-link } [&para;](MotorControl.md#motorbase "Permalink to this definition"){: .headerlink }

: Base motor class, support for events. Implements [MotorInterface](MotorControl.md#motorinterface).

    **MotorBase** ()
    
    : Constructor.
    
    **reverse** ()
    
    : Reverse the direction of the motor.
    
    **setValue** (*value*)
    
    : Set the motor direction and speed.
    
    * **value** (*float*) - Range -1..1. Negative numbers backward.
    
    **whenForward** (*action*)
    
    : Action to perform when going forward
    
    * **action** (*Action*) - Callback action object
    
    **whenBackward** (*action*)
    
    : Action to perform when going backward
    
    * **action** (*Action*) - Callback action object
    
    **whenStop** (*action*)
    
    : Action to perform when stopped
    
    * **action** (*Action*) - Callback action object
    
    **addListener** (*listener*)
    
    : Listener to be notified when the motor value changes
    
    * **listener** (*MotorListener*) - Listener instance
    
    **removeListener** (*listener*)
    
    : Remove a listener
    
    * **listener** (*MotorListener*) - Listener instance
    

### Motor

*class* **com.diozero.api.motor.Motor**{: .descname } (*forwardPin*, *backwardPin*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/motor/Motor.java){: .viewcode-link } [&para;](MotorControl.md#motor "Permalink to this definition"){: .headerlink }

: Represents a single motor controlled by two separate PWM signals. Extends [MotorBase](#motorbase).
    
    * **forwardPin** (*int*) - PWM-capable GPIO for forward / clockwise control.
    
    * **backwardPin** (*int*) - PWM-capable GPIO for backward / anti-clockwise control.

    **Motor** (*deviceFactory*, *forwardPin*, *backwardPin*)
    
    : Constructor.
    
    * **deviceFactory** (*PwmOutputDeviceFactoryInterface*) - Device factory to use for constructing the [PWMOutputDevice](API#pwmoutputdevice) instances.
    
    * **forwardPin** (*int*) - PWM-capable GPIO for forward / clockwise control.
    
    * **backwardPin** (*int*) - PWM-capable GPIO for backward / anti-clockwise control.


### TB6612FNGMotor

*class* **com.diozero.sandpit.TB6612FNGMotor**{: .descname } (*motorForwardControlPin*, *motorBackwardControlPin*, *motorPwmControl*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sandpit/TB6612FNGMotor.java){: .viewcode-link } [&para;](MotorControl.md#tb6612fngmotor "Permalink to this definition"){: .headerlink }

: Represents a single motor controlled by one PWM signal and two separate forwards / backwards control digital signals. Extends [MotorBase](#motorbase).
    
    * **motorForwardControlPin** (*[DigitalOutputDevice](API.md#digitaloutputdevice)* - Digital device controlling forward movement.
    
    * **motorBackwardControlPin** (*[DigitalOutputDevice](API.md#digitaloutputdevice)* - Digital device controlling backward movement.
    
    * **motorPwmControl** (*[PwmOutputDevice](API.md#pwmoutputdevice)* - PWM output device controlling relative motor speed.


### DualMotor

*class* **com.diozero.api.motor.DualMotor**{: .descname } (*leftMotor*, *rightMotor*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/motor/DualMotor.java){: .viewcode-link } [&para;](MotorControl.md#dualmotor "Permalink to this definition"){: .headerlink }

: Convenience class for a robot with two [Motors](#motorinterface).
    
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
    
    **forwardLeft** (*speed*)
    
    : Turn the right motor forward at the specified speed, stop the left motor. Range 0..1.
    
    **forwardRight** (*speed*)
    
    : Turn the left motor forward at the specified speed, stop the right motor. Range 0..1.
    
    **backwardLeft** (*speed*)
    
    : Turn the right motor backward at the specified speed, stop the left motor. Range 0..1.
    
    **backwardRight** (*speed*)
    
    : Turn the left motor backward at the specified speed, stop the right motor. Range 0..1.
    
    **reverse** ()
    
    : Reverse the direction of both motors.
    
    **circleLeft** (*speed*, *turnRate*)
    
    : Circle to the left.
    
    * **speed** (*float*) - Range 0..1.
    
    * **turnRate** (*float*) - Range 0..speed.
    
    **circleRight** (*speed*, *turnRate*)
    
    : Circle to the right.
    
    * **speed** (*float*) - Range 0..1.
    
    * **turnRate** (*float*) - Range 0..speed.
    
    **stop** ()
    
    : Stop both motors.


### Servo

!!! Warning "Work in progress"
    Only tested with pigpio hence in the sandpit package.

*class* **com.diozero.sandpit.Servo**{: .descname } (*pinNumber*, *pwmFrequency*, *initialPulseWidthMs*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sandpit/Servo.java){: .viewcode-link } [&para;](MotorControl.md#servo "Permalink to this definition"){: .headerlink }

: Represents a pulse-width controlled servo.
    
    * **pinNumber** (*int*) - Pin number
    
    * **pwmFrequency** (*int*) - Desired PWM frequency for the servo, typically 50Hz
    
    * **initialPulseWidthMs** (*float*) - Starting pulse width (in milliseconds). Range is 0.5ms - 2.6ms
    
    **getPulseWidthMs** ()
    
    : Get the current pulse width value (milliseconds)
    
    *float* **getPulseWidthMs** (*float* pulseWidthMs)
    
    : Set the pulse width value (milliseconds)
    
    **pulseWidthMs** - New pulse width value (milliseconds)


## Common Motor Controllers

### CamJamKitDualMotor

*class* **com.diozero.CamJamKitDualMotor**{: .descname } [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/CamJamKitDualMotor.java){: .viewcode-link } [&para;](MotorControl.md#camjamkitdualmotor "Permalink to this definition"){: .headerlink }

: Extends [DualMotor](#dualmotor). Convenience class for interfacing with the [CamJam EduKit #3 Motor Controller Board](http://camjam.me/?page_id=1035) with pre-configured pin numbers (left -&gt; 9 and 10, right -&gt; 7 and 8).


### RyanteckDualMotor

*class* **com.diozero.RyanteckDualMotor**{: .descname } [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/RyanteckDualMotor.java){: .viewcode-link } [&para;](MotorControl.md#ryanteckdualmotor "Permalink to this definition"){: .headerlink }

: Extends [DualMotor](#dualmotor). Convenience class for interfacing with the Ryanteck RPi Motor Controller Board with pre-configured pin numbers (left -&gt; 17 and 18, right -&gt; 22 and 23).


### TB6612FNGDualMotorDriver

*class* **com.diozero.sandpit.TB6612FNGDualMotorDriver**{: .descname } [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sandpit/TB6612FNGDualMotorDriver.java){: .viewcode-link } [&para;](MotorControl.md#tb6612fngdualmotordriver "Permalink to this definition"){: .headerlink }

: Extends [DualMotor](#dualmotor). Convenience class for interfacing with dual H-bridge motor drivers such as the [Toshiba TB6612FNG Dual Motor Driver](http://toshiba.semicon-storage.com/info/lookup.jsp?pid=TB6612FNG&lang=en) as used in the [Pololu Dual Motor Driver Carrier](https://www.pololu.com/product/713).

