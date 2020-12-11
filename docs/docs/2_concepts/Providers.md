---
parent: Concepts
nav_order: 1
permalink: /concepts/providers.html
---

# Device Factories and Providers

The builtin provider is designed to be portable across different boards. 

## Device Factories

The class [DeviceFactoryHelper](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/util/DeviceFactoryHelper.java) encapsulates the logic for accessing the configured service provider. Interfaces for implementing a new service provider are in the [com.diozero.internal.provider](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/internal/provider) package. Developing a new service provider is relatively straightforward given the provided APIs and base classes.

In theory the OpenJDK Device I/O service provider should provide the best platform support, however, the JDK Device I/O library [doesn't support PWM](http://mail.openjdk.java.net/pipermail/dio-dev/2015-November/000650.html). Also it appears it is no longer maintained.

The device native service provider library is defined in the following order:

1. System property com.diozero.devicefactory, e.g. `-Dcom.diozero.devicefactory=com.diozero.internal.provider.pi4j.Pi4jDeviceFactory`
2. Service definition file on the classpath, file: `/META-INF/services/com.diozero.internal.provider.NativeDeviceFactoryInterface`. For example [the one for pi4j](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-pi4j/src/main/resources/META-INF/services/com.diozero.internal.provider.NativeDeviceFactoryInterface)

**Currently implemented service providers:**

+ [BBBIOlib](https://github.com/VegetableAvenger/BBBIOlib)
+ [Firmata4j](https://github.com/kurbatov/firmata4j)
+ mmap
+ [pigpio](http://abyz.co.uk/rpi/pigpio/index.html) via my [JNI wrapper library](https://github.com/mattjlewis/pigpioj)
+ [VoodooSpark](https://github.com/voodootikigod/voodoospark)

See below for provider specific details.

TODO Describe steps for creating a new provider.

## pigpio

TBD
