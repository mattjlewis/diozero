# Other Components

## HC-SRO4 Ultrasonic Ranging Module

TODO Insert wiring diagram.

*class* **com.diozero.HCSR04**

: Implements support for the [HC-SR04](http://www.micropik.com/PDF/HCSR04.pdf) Ultrasonic Ranging Module.

    **HCSR04** (*triggerGpioNum*, *echoGpioNum*)

    : Constructor
    
    * **triggerGpioNum** (*int*) - GPIO pin connected to the HC-SR04 trigger pin
    
    * **echoGpioNum** (*int*) - GPIO pin connected to the HC-SR04 echo pin

    *float* **getDistanceCm**()

    : Get distance in cm


## Bosch Sensortec BMP180

*class* **com.diozero.BMP180**

: Implements support for the [Bosch Sensortec BMP180](http://www.bosch-sensortec.com/bst/products/all_products/bmp180) temperature and pressure sensor.

    **BMP180** (*mode*)

    : Constructor
    
    * **mode** (*BMPMode*) - BMP operating mode (Ultra-Low Power, Standard, High Resolution, Ultra-High Resolution)

    *float* **getPressure**()

    : Read the barometric pressure (in hPa) from the device

    *float* **getTemperature**()

    : Read the temperature (in &#8451;) from the device


## TSL2561 Digital Luminosity / Lux / Light Sensor

*class* **com.diozero.TSL2561**

: Implements support for the [Adafruit](https://www.adafruit.com/products/439) Digital Luminosity / Lux / Light Sensor - [Datasheet](https://www.adafruit.com/datasheets/TSL2561.pdf).

    **TSL2561**(*tsl2561Package*)

    : Constructor
    
    * **tsl2561Package** (*int*) - TSL package information (values are CS or T / FN / CL)

    **setAutoGain** (*autoGain*)

    : Enable or disable the auto-gain settings when reading data from the sensor
    
    * **autoGain** (*boolean*) - Auto-gain flag

    **setGain** (*gain*)

    : Adjusts the gain on the TSL2561 (adjusts the sensitivity to light)
    
    * **gain** (*int*) - Gain value

    *float* **getLuminosity**()

    : Get luminosity in Lux


## NeoPixel

TODO Insert wiring diagram.

<dl class="class">
<dt id="com.diozero.ws281xj.WS281x">
<em class="property">class</em> <strong>com.diozero.ws281xj</strong><code class="descclassname">WS281x</code><span class="sig-paren">(</span><em>frequency</em>, <em>dmaNum</em>, <em>gpioNum</em>, <em>brightness</em>, <em>numPixels</em><span class="sig-paren">)</span><a href="https://github.com/mattjlewis/diozero/blob/master/diozero-ws281x-java/src/main/java/com/diozero/ws281xj/WS281x.java"><span class="viewcode-link">[source]</span></a></dt>
<dd>Support for [WS281x / NeoPixel LEDs](https://learn.adafruit.com/adafruit-neopixel-uberguide) is available through a [JNI wrapper](WS281xNative.java) around the [rpi_ws281x C library](https://github.com/jgarff/rpi_ws281x).</dd>
</dl>
<dl class="method">
<dt id="com.diozero.ws281xj.WS281x.render"><code class="descname">render</code><span class="sig-paren">(</span><span class="sig-paren">)</span><a class="headerlink" href="#com.diozero.ws281xj.WS281x.render" title="Permalink to this definition">@para;</a></dt>
<dd><p>Push any updated colours to the LED strip.</p><dd>
</dl>

## Inertial Measurement Units

TODO Describe IMU API
