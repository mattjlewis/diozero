# Sensor Components

## HC-SRO4 Ultrasonic Ranging Module {: #hc-sr04 }

TODO Insert wiring diagram.

*class* **com.diozero.HCSR04**{: .descname } (*triggerGpioNum*, *echoGpioNum*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/HCSR04.java){: .viewcode-link } [&para;](SensorComponents.md#hc-sr04 "Permalink to this definition"){: .headerlink }

: Implements support for the [HC-SR04](http://www.micropik.com/PDF/HCSR04.pdf) Ultrasonic Ranging Module.
    
    * **triggerGpioNum** (*int*) - GPIO pin connected to the HC-SR04 trigger pin.
    
    * **echoGpioNum** (*int*) - GPIO pin connected to the HC-SR04 echo pin.

    *float* **getDistanceCm** ()

    : Get distance in cm.


## Bosch BMP180

*class* **com.diozero.BMP180**{: .descname } (*mode*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/BMP180.java){: .viewcode-link } [&para;](SensorComponents.md#bosch-bmp180 "Permalink to this definition"){: .headerlink }

: Implements support for the [Bosch Sensortec BMP180](http://www.bosch-sensortec.com/bst/products/all_products/bmp180) temperature and pressure sensor.
    
    * **mode** (*BMPMode*) - BMP operating mode (Ultra-Low Power, Standard, High Resolution, Ultra-High Resolution).

    *float* **getPressure** ()

    : Read the barometric pressure (in hPa) from the device.

    *float* **getTemperature** ()

    : Read the temperature (in &deg;C) from the device.


## Bosch BME280

*class* **com.diozero.BME280**{: .descname } (*bus=1*, *address=0x76*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/BME280.java){: .viewcode-link } [&para;](SensorComponents.md#bosch-bme280 "Permalink to this definition"){: .headerlink }

: Implements support for the [Bosch Sensortec BME280](http://www.bosch-sensortec.com/bst/products/all_products/bme280) pressure, humidity and temperature sensor.
    
    * **bus** (*int*) - I2C bus number.
    
    * **address** (*int*) - I2C device address.
    
    **setOperatingModes** (tempOversampling, pressOversampling, humOversampling, operatingMode)
    
    : Set the oversampling and operating modes
    
    * **tempOversampling** (*TemperatureOversampling*) - Temperature oversampling mode (1, 2, 4, 8, 16)
    
    * **pressOversampling** (*PressureOversampling*) - Pressure oversampling mode (1, 2, 4, 8, 16)
    
    * **humOversampling** (*HumidityOversampling*) - Humidity oversampling mode (1, 2, 4, 8, 16)
    
    * **operatingMode** (*OperatingMode*) - Device operating mode (Sleep, Forced, Normal)
    
    **setStandbyAndFilterModes** (*standbyMode*, *filterMode*)
    
    : Set the standby and filter modes
    
    * **standbyMode** (*StandbyMode*) - The standby mode (0.5ms, 62.5ms, 125ms, 250ms, 500ms, 1000ms, 10ms, 20ms)
    
    * **filterMode** (*FilterMode*) - The filter mode (Off, 2, 4, 8, 16 samples)

    *float* **getTemperature** ()

    : Read the temperature (in &deg;C) from the device.

    *float* **getPressure** ()

    : Read the barometric pressure (in hPa) from the device.

    *float* **getHumidity** ()

    : Read the humidity from the device.


## TSL2561 Digital Luminosity / Lux / Light Sensor {: #tsl2561 }

*class* **com.diozero.TSL2561**{: .descname } (*tsl2561Package*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/TSL2561.java){: .viewcode-link } [&para;](SensorComponents.md#tsl2561 "Permalink to this definition"){: .headerlink }

: Implements support for the [Adafruit](https://www.adafruit.com/products/439) Digital Luminosity / Lux / Light Sensor - [Datasheet](https://www.adafruit.com/datasheets/TSL2561.pdf).
    
    * **tsl2561Package** (*int*) - TSL package information (values are CS or T / FN / CL).

    **setAutoGain** (*autoGain*)

    : Enable or disable the auto-gain settings when reading data from the sensor.
    
    * **autoGain** (*boolean*) - Auto-gain flag.

    **setGain** (*gain*)

    : Adjusts the gain on the TSL2561 (adjusts the sensitivity to light).
    
    * **gain** (*int*) - Gain value.

    *float* **getLuminosity** ()

    : Get luminosity in Lux.


## STMicroelectronics HTS221 Humidity and Temperature Sensor {: #hts221 }

*class* **com.diozero.HTS221**{: .descname } [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/HTS221.java){: .viewcode-link } [&para;](SensorComponents.md#hts221 "Permalink to this definition"){: .headerlink }

: Implements support for the [STMicroelectronics]() Humidity and Temperature Sensor - [Datasheet](http://www2.st.com/content/ccc/resource/technical/document/datasheet/4d/9a/9c/ad/25/07/42/34/DM00116291.pdf/files/DM00116291.pdf/jcr:content/translations/en.DM00116291.pdf).

    *float* **getHumidity** ()

    : Read the humidity from the device.

    *float* **getTemperature** ()

    : Read the temperature (in &deg;C) from the device.


## STMicroelectronics LPS25H Pressure and Temperature Sensor {: #lps25h }

*class* **com.diozero.LPS25H**{: .descname } [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/LPS25H.java){: .viewcode-link } [&para;](SensorComponents.md#lps25h "Permalink to this definition"){: .headerlink }

: Implements support for the [STMicroelectronics](http://www.st.com/content/st_com/en/products/mems-and-sensors/pressure-sensors/lps25h.html) Pressure and Temperature Sensor - [Datasheet](http://www2.st.com/content/ccc/resource/technical/document/datasheet/58/d2/33/a4/42/89/42/0b/DM00066332.pdf/files/DM00066332.pdf/jcr:content/translations/en.DM00066332.pdf).

    *float* **getPressure** ()

    : Read the barometric pressure (in hPa) from the device.

    *float* **getTemperature** ()

    : Read the temperature (in &deg;C) from the device.


## 1-Wire Temperature Sensors

*class* **com.diozero.W1ThermSensor**{: .descname } [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/W1ThermSensor.java){: .viewcode-link } [&para;](SensorComponents.md#1-wire-temperature-sensors "Permalink to this definition"){: .headerlink }

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


## Sharp GP2Y0A21YK {: #gp2y0a21yk }

*class* **com.diozero.GP2Y0A21YK**{: .descname } (*pinNumber*, *vRef*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/GP2Y0A21YK.java){: .viewcode-link } [&para;](SensorComponents.md#gp2y0a21yk "Permalink to this definition"){: .headerlink }

: Implements support for the [Sharp GP2Y0A21YK](http://www.sharpsma.com/webfm_send/1208) distance sensor - [Datasheet](http://oomlout.com/parts/IC-PROX-01-guide.pdf).

    * **pinNumber** (*int*) - Pin number

    * **vRef** (*float*) - Reference voltage for the ADC

    *float* **getDistanceCm** ()

    : Get the distance in cm.
