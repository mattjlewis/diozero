# Other Components

## HC-SRO4 Ultrasonic Ranging Module

[HC-SR04](http://www.micropik.com/PDF/HCSR04.pdf).

## Bosch Sensortec BMP180

[Bosch Sensortec BMP180](http://www.bosch-sensortec.com/bst/products/all_products/bmp180) temperature and pressure sensor

## TSL2561 Digital Luminosity / Lux / Light Sensor

[Adafruit](https://www.adafruit.com/products/439)
[Datasheet](https://www.adafruit.com/datasheets/TSL2561.pdf) light-to-digital converter

## NeoPixel

Support for [WS281x / NeoPixel LEDs](https://learn.adafruit.com/adafruit-neopixel-uberguide) is available through a [JNI wrapper](WS281xNative.java) around the [rpi_ws281x C library](https://github.com/jgarff/rpi_ws281x).

<dl class="class">
<dt id="com.diozero.ws281xj.WS281x">
<em class="property">class</em> <code class="descclassname">com.diozero.ws281xj.WS281x</code><span class="sig-paren">(</span><em>frequency</em>, <em>dmaNum</em>, <em>gpioNum</em>, <em>brightness</em>, <em>numPixels</em><span class="sig-paren">)</span></dt>
</dl>


## Inertial Measurement Units

* [InvenSense MPU-9150](http://www.invensense.com/products/motion-tracking/9-axis/mpu-9150/) Nine-axis motion tracking device. Currently a fully working Java port of the InvenSense C library but could do with some Object Orientation related improvements.
