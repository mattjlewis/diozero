# Sensor Components

## HC-SRO4 Ultrasonic Ranging Module {: #hc-sr04 }

TODO Insert wiring diagram.

*class* **com.diozero.HCSR04**{: .descname } (*triggerGpioNum*, *echoGpioNum*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/HCSR04.java){: .viewcode-link } [&para;](SensorComponents.md#hc-sr04 "Permalink to this definition"){: .headerlink }

: Implements support for the [HC-SR04](http://www.micropik.com/PDF/HCSR04.pdf) Ultrasonic Ranging Module.
    
    * **triggerGpioNum** (*int*) - GPIO pin connected to the HC-SR04 trigger pin
    
    * **echoGpioNum** (*int*) - GPIO pin connected to the HC-SR04 echo pin

    *float* **getDistanceCm** ()

    : Get distance in cm


## Bosch BMP180

*class* **com.diozero.BMP180**{: .descname } (*mode*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/BMP180.java){: .viewcode-link } [&para;](SensorComponents.md#bosch-bmp180 "Permalink to this definition"){: .headerlink }

: Implements support for the [Bosch Sensortec BMP180](http://www.bosch-sensortec.com/bst/products/all_products/bmp180) temperature and pressure sensor.
    
    * **mode** (*BMPMode*) - BMP operating mode (Ultra-Low Power, Standard, High Resolution, Ultra-High Resolution)

    *float* **getPressure** ()

    : Read the barometric pressure (in hPa) from the device

    *float* **getTemperature** ()

    : Read the temperature (in &deg;C) from the device


## TSL2561 Digital Luminosity / Lux / Light Sensor {: #tsl2561 }

*class* **com.diozero.TSL2561**{: .descname } (*tsl2561Package*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/TSL2561.java){: .viewcode-link } [&para;](SensorComponents.md#tsl2561 "Permalink to this definition"){: .headerlink }

: Implements support for the [Adafruit](https://www.adafruit.com/products/439) Digital Luminosity / Lux / Light Sensor - [Datasheet](https://www.adafruit.com/datasheets/TSL2561.pdf).
    
    * **tsl2561Package** (*int*) - TSL package information (values are CS or T / FN / CL)

    **setAutoGain** (*autoGain*)

    : Enable or disable the auto-gain settings when reading data from the sensor
    
    * **autoGain** (*boolean*) - Auto-gain flag

    **setGain** (*gain*)

    : Adjusts the gain on the TSL2561 (adjusts the sensitivity to light)
    
    * **gain** (*int*) - Gain value

    *float* **getLuminosity** ()

    : Get luminosity in Lux

## 1-Wire Temperature Sensors

*class* **com.diozero.sandpit.W1ThermSensor**{: .descname } [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sandpit/W1ThermSensor.java){: .viewcode-link } [&para;](SensorComponents.md#1-wire-temperature-sensors "Permalink to this definition"){: .headerlink }

: Support for 1-wire temperature sensors such as the DS18B20. Adafruit has a good [article on connecting these](https://learn.adafruit.com/adafruits-raspberry-pi-lesson-11-ds18b20-temperature-sensing?view=all) to the Raspberry Pi. Currently supported types: DS18S20, DS1822, DS18B20, DS1825, DS28EA00, MAX31850K.

    static *List&lt;W1ThermSensor&gt;* **getAvailableSensors** (*folder="/sys/bus/w1/devices"*)
    
    : Class-level operation to get a list of all supported 1-wire device instances currently connected.
    
    * **folder** (*String*) - System folder containing the devices, defaults to `"/sys/bus/w1/devices"`.
    
    *float* **getTemperature** ()
    
    : Get the temperature in &#deg;C.
    
    *W1TermSensor.Type* **getType** ()
    
    : Get the type of this 1-wire sensor instance.
    
    *String* **getSerialNumber** ()
    
    : Get the serial number for this 1-wire sensor instance.
    