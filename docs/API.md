# API

## Analog Input Device

## Digital Input Device

## Motors (Digital and PWM)

## Digital Output Device

*class* **com.diozero.api.DigitalOutputDevice**

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
    
    **setValue** (*value*)
    
    : Turn the device on or off.
    
    * **value** (*boolean*) - New value, true == on, false == off.
    
    **setValueUnsafe** (*value*)
    
    : Unsafe operation that has no synchronisation checks and doesn't factor in active low logic. Included primarily for performance tests.
    
    * **value** (*boolean*) - New value, true == on, false == off.


## I2C Device Support

## SPI Device Support

## PWM Output Device

## Smoothed Input Device

## Waitable Input Device
