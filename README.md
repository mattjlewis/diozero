# DIO-Zero - a Java Device I/O wrapper for GPIO / I2C / SPI devices
A Device I/O library that provides object-orientated APIs for a range of GPIO/I2C/SPI devices such as LEDs, buttons and other various sensors. The library uses Java ServiceLoader to load low-level libraries for actually interfacing with the underlying hardware. I've only tested this library on the Raspberry Pi (B+, Zero and 2) using Oracle's Java SE 1.8 however it should work on any device that the currently bundled libraries support.

Detailed document is available on [read the docs](http://rtd.diozero.com/).

The class [DeviceFactoryHelper](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/DeviceFactoryHelper.java) encapsulates the logic for accessing the configured service provider. Interfaces for implementing a new service provider are in the [com.diozero.internal.spi](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/internal/spi) package. Developing a new service provider is relatively straightforward given the provided APIs and base classes.

In theory the OpenJDK Device I/O service provider should provide the best platform support, however, the JDK Device I/O library [doesn't support PWM](http://mail.openjdk.java.net/pipermail/dio-dev/2015-November/000650.html).

As well as providing classes for interfacing with some I2C and SPI devices, the library also provides a simple object-orientated GPIO wrapper layer that replicates the APIs from the excellent [GPIO Zero Python library](http://pythonhosted.org/gpiozero/).

All pin numbers are device native, i.e. Broadcom pin numbering on the Raspberry Pi.

Snapshot builds of the library are available in the [Nexus Repository Manager](https://oss.sonatype.org/index.html#nexus-search;gav~com.diozero~~~~).
This library uses [tinylog](www.tinylog.org) [v1.0](https://github.com/pmwmedia/tinylog/releases/download/1.0.3/tinylog-1.0.3.zip).

To run the [LED sample application](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/LEDTest.java) using Pi4j:

	sudo java -classpath diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pi4j-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar:tinylog-1.0.3.jar com.diozero.sampleapps.LEDTest 18

The device native service provider library is defined in the following order:
1.  System property com.diozero.devicefactory, e.g. `-Dcom.diozero.devicefactory=com.diozero.internal.provider.pi4j.Pi4jDeviceFactory`
2.  Service definition file on the classpath, file /META-INF/services/com.diozero.internal.spi.NativeDeviceFactoryInterface. For example [the one for pi4j](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-pi4j/src/main/resources/META-INF/services/com.diozero.internal.spi.NativeDeviceFactoryInterface)

**Currently implemented service providers:**
* [JDK Device I/O](https://wiki.openjdk.java.net/display/dio/Main) - versions 1.0 and 1.1
* [Pi4j](http://pi4j.com/)
* [wiringPi](http://wiringpi.com/) via the Pi4j JNI wrapper classes
* [pigpio](http://abyz.co.uk/rpi/pigpio/index.html) via my [JNI wrapper library](https://github.com/mattjlewis/pigpioj)
See below for provider specific details.

**Currently supported I2C devices:**
* [InvenSense MPU-9150](http://www.invensense.com/products/motion-tracking/9-axis/mpu-9150/) Nine-axis motion tracking device. Currently a fully working Java port of the InvenSense C library but could do with some Object Orientation related improvements
* [TSL2561](https://www.adafruit.com/datasheets/TSL2561.pdf) light-to-digital converter
* [Bosch Sensortec BMP180](https://www.bosch-sensortec.com/en/homepage/products_3/environmental_sensors_1/bmp180_1/bmp180) temperature and pressure sensor
* [MCP23017](http://www.microchip.com/wwwproducts/Devices.aspx?product=MCP23017) 16-bit input/output port expander with interrupt output
* [PCA9685](http://www.nxp.com/products/power-management/lighting-driver-and-controller-ics/i2c-led-display-control/16-channel-12-bit-pwm-fm-plus-ic-bus-led-controller:PCA9685) 12-bit 16-channel PWM driver as used by the [Adafruit PWM Servo Driver](https://www.adafruit.com/product/815)

**Currently supported SPI devices:**
* [MCP3008 Analogue to Digital Converter](https://www.adafruit.com/datasheets/MCP3008.pdf)

**Analogue device support:**
* [TMP36](http://www.analog.com/en/products/analog-to-digital-converters/integrated-special-purpose-converters/integrated-temperature-sensors/tmp36.html) temperature sensor
* [Photo Resistor/LDR](https://en.wikipedia.org/wiki/Photoresistor)
* [Sharp GP2Y0A21YK](http://www.sharpsma.com/webfm_send/1208) distance sensor

**Basic GPIO Output devices:**
* Button
* Buzzer
* Generic bi-directional motors (Digital and PWM)
* LED (Digital and PWM) 

**Basic GPIO Input devices:**
* Line sensor
* Motion sensor

**More complex GPIO devices:**
* [HC-SR04](http://www.micropik.com/PDF/HCSR04.pdf) Ultrasonic Ranging Module
* PWM-controlled motor drivers, e.g. [CamJam EduKit #3 - Robotics](http://camjam.me/?page_id=1035) and the [Toshiba TB6612FNG Dual Motor Driver](http://toshiba.semicon-storage.com/info/lookup.jsp?pid=TB6612FNG&lang=en) as used in the [Pololu Dual Motor Driver Carrier](https://www.pololu.com/product/713)

##Performance
I've done some limited performance tests (turning a GPIO on then off, see [GpioPerfTest](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/GpioPerfTest.java)) on a Raspberry Pi 2 using the various native device factory providers as well as a test using Pi4j's wiringPi JNI API directly without going via my DIO-Zero wrapper (see [WiringPiRawPerfTest](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-wiringpi/src/main/java/com/diozero/internal/provider/wiringpi/WiringPiRawPerfTest.java)); here are the results:

| Provider | Iterations | Frequency (kHz) |
| -------- | ---------- | --------------- |
| Pi4j 1.0 | 10,000 | 0.91 |
| JDK DIO 1.1 | 100,000 | 8.23 |
| Pi4j 1.1 | 10,000,000 | 622 |
| wiringPi | 5,000,000 | 1,683 |
| wiringPi (direct) | 10,000,000 | 2,137 |
| pigpio | 5,000,000 | 1,266 |
| pigpio (direct) 10,000,000 | 1,649 |

For a discussion on why Pi4j 1.0 is so slow, see this [issue](https://github.com/Pi4J/pi4j/issues/158). These results are in-line with those documented in the book ["Raspberry Pi with Java: Programming the Internet of Things"](http://www.amazon.co.uk/Raspberry-Pi-Java-Programming-Internet/dp/0071842012). For reference, the author's results were:

| Library | Frequency (kHz) |
| ------- | --------------- |
| Pi4j 1.0 | 0.751 |
| JDK DIO 1.0 | 3.048 |
| wiringPi (direct) | 1,662 |

##Providers
###JDK Device I/O
This library has device providers for [JDK Device I/O](https://wiki.openjdk.java.net/display/dio/Main) v1.0 (in Mercurial [master repository](http://hg.openjdk.java.net/dio/master)) and v1.1 Mercurial (in [dev repository](http://hg.openjdk.java.net/dio/dev)). Unfortunately these libraries aren't in Maven repositories; to build the JDK Device I/O v1.0 library on the Raspberry Pi:

	sudo apt-get install mercurial
	mkdir deviceio
	cd deviceio
	hg clone http://hg.openjdk.java.net/dio/master
	cd master
	export JAVA_HOME=/usr/lib/jvm/jdk-8-oracle-arm-vfp-hflt
	export PATH=$JAVA_HOME/bin:$PATH
	export PI_TOOLS=/usr
	make bundle PI_TOOLS=$PI_TOOLS osgi
	cp build/deviceio/lib/ext/dio.jar dio-1.0.jar
	sudo cp build/deviceio/lib/ext/dio.jar $JAVA_HOME/jre/lib/ext/.
	sudo cp build/deviceio/lib/arm/libdio.so $JAVA_HOME/jre/lib/arm/.

To add Java Device I/O JAR to your local Maven repository (on your development machine) run:

	mvn install:install-file -Dfile=dio-1.0.jar -DgroupId=jdk.dio -DartifactId=device-io -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true

Similar instructions should be followed for installing v1.1, the only difference being how it is built:

	hg clone http://hg.openjdk.java.net/dio/dev
	cd dev
	export JAVA_HOME=/usr/lib/jvm/jdk-8-oracle-arm-vfp-hflt
	export PATH=$JAVA_HOME/bin:$PATH
	export PI_TOOLS=/usr
	make osgi

###wiringPi & Pi4j
Make sure you have wiringPi installed (`sudo apt-get update && sudo apt-get install wiringpi`). I recommend using the latest [Pi4j 1.1 snapshot build](https://oss.sonatype.org/service/local/repositories/snapshots/content/com/pi4j/pi4j-core/1.1-SNAPSHOT/pi4j-core-1.1-20151214.215847-34.jar).

###pigpio
TBD

##To-Do
There is still a lot left to do, in particular:
* Thorough testing (various types of devices using each service provider)
* Testing on different devices (all flavours of Raspberry Pi, BeagleBone, ...)
* GPIO input debouncing
* Other I2C & SPI devices, including those on the SenseHAT
* A clean object-orientated API for IMUs
