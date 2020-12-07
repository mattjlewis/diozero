---
parent: Concepts
nav_order: 1
permalink: /concepts/providers.html
---

# Device Factories and Providers

## Device Factories

The class [DeviceFactoryHelper](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/util/DeviceFactoryHelper.java) encapsulates the logic for accessing the configured service provider. Interfaces for implementing a new service provider are in the [com.diozero.internal.provider](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/internal/provider) package. Developing a new service provider is relatively straightforward given the provided APIs and base classes.

In theory the OpenJDK Device I/O service provider should provide the best platform support, however, the JDK Device I/O library [doesn't support PWM](http://mail.openjdk.java.net/pipermail/dio-dev/2015-November/000650.html). Also it appears it is no longer maintained.

The device native service provider library is defined in the following order:

1. System property com.diozero.devicefactory, e.g. `-Dcom.diozero.devicefactory=com.diozero.internal.provider.pi4j.Pi4jDeviceFactory`
2. Service definition file on the classpath, file: `/META-INF/services/com.diozero.internal.provider.NativeDeviceFactoryInterface`. For example [the one for pi4j](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-pi4j/src/main/resources/META-INF/services/com.diozero.internal.provider.NativeDeviceFactoryInterface)

**Currently implemented service providers:**

+ [BBBIOlib](https://github.com/VegetableAvenger/BBBIOlib)
+ [Firmata4j](https://github.com/kurbatov/firmata4j)
+ [JDK Device I/O](https://wiki.openjdk.java.net/display/dio/Main) - versions 1.0 and 1.1
+ mmap
+ [Pi4j](http://pi4j.com/)
+ [pigpio](http://abyz.co.uk/rpi/pigpio/index.html) via my [JNI wrapper library](https://github.com/mattjlewis/pigpioj)
+ [VoodooSpark](https://github.com/voodootikigod/voodoospark)
+ [wiringPi](http://wiringpi.com/) via the Pi4j JNI wrapper classes

See below for provider specific details.

TODO Describe steps for creating a new provider.

## JDK Device I/O

This library has device providers for [JDK Device I/O](https://wiki.openjdk.java.net/display/dio/Main) v1.0 (in Mercurial [master repository](http://hg.openjdk.java.net/dio/master)) and v1.1 Mercurial (in [dev repository](http://hg.openjdk.java.net/dio/dev)). Unfortunately these libraries aren't in Maven repositories; to build the JDK Device I/O v1.0 library on the Raspberry Pi:

```sh
sudo apt-get install mercurial
mkdir deviceio
cd deviceio
hg clone http://hg.openjdk.java.net/dio/master
cd master
export JAVA_HOME=/usr/lib/jvm/jdk-8-oracle-arm-vfp-hflt
export PATH=$JAVA_HOME/bin:$PATH
export PI_TOOLS=/usr
make bundle PI_TOOLS=$PI_TOOLS osgi
cp build/deviceio/lib/ext/dio.jar ../dio-1.0.jar
sudo cp build/deviceio/lib/ext/dio.jar $JAVA_HOME/jre/lib/ext/.
sudo cp build/deviceio/lib/arm/libdio.so $JAVA_HOME/jre/lib/arm/.
```

To add Java Device I/O JAR to your local Maven repository (on your development machine) run:

	mvn install:install-file -Dfile=dio-1.0.jar -DgroupId=jdk.dio -DartifactId=device-io -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true

Similar instructions should be followed for installing v1.1, the only difference being how it is built:

```sh
hg clone http://hg.openjdk.java.net/dio/dev
cd dev
export JAVA_HOME=/usr/lib/jvm/jdk-8-oracle-arm-vfp-hflt
export PATH=$JAVA_HOME/bin:$PATH
export CROSS_TOOL=/usr/bin/
make osgi
cp build/jar/dio.jar ../dio-1.1.jar
```

## wiringPi & Pi4j

Make sure you have wiringPi installed (`sudo apt-get update && sudo apt-get install wiringpi`). You also need to include [Pi4j 1.2](https://pi4j.com/download/pi4j-1.2.zip) on the classpath.

## pigpio

TBD
