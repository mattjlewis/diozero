# Sensor Components

## HC-SRO4 Ultrasonic Ranging Module {: #hc-sr04 }

TODO Insert wiring diagram.

*class* **com.diozero.HCSR04** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/HCSR04.java)

: Implements support for the [HC-SR04](http://www.micropik.com/PDF/HCSR04.pdf) Ultrasonic Ranging Module.

    **HCSR04** (*triggerGpioNum*, *echoGpioNum*)

    : Constructor
    
    * **triggerGpioNum** (*int*) - GPIO pin connected to the HC-SR04 trigger pin
    
    * **echoGpioNum** (*int*) - GPIO pin connected to the HC-SR04 echo pin

    *float* **getDistanceCm** ()

    : Get distance in cm


## Bosch BMP180

*class* **com.diozero.BMP180** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/BMP180.java)

: Implements support for the [Bosch Sensortec BMP180](http://www.bosch-sensortec.com/bst/products/all_products/bmp180) temperature and pressure sensor.

    **BMP180** (*mode*)

    : Constructor
    
    * **mode** (*BMPMode*) - BMP operating mode (Ultra-Low Power, Standard, High Resolution, Ultra-High Resolution)

    *float* **getPressure** ()

    : Read the barometric pressure (in hPa) from the device

    *float* **getTemperature** ()

    : Read the temperature (in &#8451;) from the device


## TSL2561 Digital Luminosity / Lux / Light Sensor {: #tsl2561 }

*class* **com.diozero.TSL2561** [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/TSL2561.java)

: Implements support for the [Adafruit](https://www.adafruit.com/products/439) Digital Luminosity / Lux / Light Sensor - [Datasheet](https://www.adafruit.com/datasheets/TSL2561.pdf).

    **TSL2561** (*tsl2561Package*)

    : Constructor
    
    * **tsl2561Package** (*int*) - TSL package information (values are CS or T / FN / CL)

    **setAutoGain** (*autoGain*)

    : Enable or disable the auto-gain settings when reading data from the sensor
    
    * **autoGain** (*boolean*) - Auto-gain flag

    **setGain** (*gain*)

    : Adjusts the gain on the TSL2561 (adjusts the sensitivity to light)
    
    * **gain** (*int*) - Gain value

    *float* **getLuminosity** ()

    : Get luminosity in Lux

