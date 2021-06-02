---
parent: Concepts
nav_order: 3
permalink: /concepts/gettingstarted.html
title: Getting Started
---

# Getting Started

Snapshot builds of the library are available in the [Nexus Repository Manager](https://oss.sonatype.org/index.html#nexus-search;gav~com.diozero~~~~).
A ZIP file of diozero and all dependencies can also be downloaded via the [diozero-distribution artifact on Nexus](https://oss.sonatype.org/index.html#nexus-search;gav~com.diozero~diozero-distribution~~~~kw,versionexpand).

Javadoc for the core library is also available via [javadoc.io](http://www.javadoc.io/doc/com.diozero/diozero-core/). 

Java doesn't provide a convenient deployment-time dependency manager 
such as Python's `pip` therefore you will need to manually download all dependencies 
and setup your classpath correctly. You can do this either via setting the `CLASSPATH` 
environment variable or as a command-line option (`java -cp <jar1>:<jar2>`). 
The dependencies have been deliberately kept to as few libraries as possible -
diozero is only dependent on [tinylog v2](http://www.tinylog.org).

To compile a diozero application you will need 3 JAR files - [tinylog](http://www.tinylog.org/)
(both API and Impl), and diozero-core. 
To run a diozero application, you can also optionally include one of the supported device provider 
libraries and the corresponding diozero provider wrapper library. Note that the built-in default
device provider gives maximum portability but has some limitations such as not supporting hardware
PWM on the Raspberry Pi. If you need hardware PWM on a Raspberry Pi then you must use the pigpio provider.

{: .note-title }
> Memory Mapped GPIO Control (mmap)
>
> Very high performance GPIO control can be achieved by directly accessing the GPIO registers via
> the `/dev/gpiomem` device file.
> The built-in default provider makes use of this feature, allowing it to achieve GPIO toggle
> rates as high as 24MHz on the Raspberry Pi 4.
> 
> mmap is currently supported on the following SBCs:
>
> * Raspberry Pi (all variants)
> * FriendlyArm / Allwinner H3 Sun8i CPU (as used in the NanoPi Duo2 / NanoPi Neo amongst others)
> * OrangePi Zero+ (Allwinner H5) / One+ (Allwinner H6)
> * ASUS TinkerBoard
> * Odroid C2
> * Next Think Co CHIP (Allwinner sun4i/sun5i)
>
> Some boards / Operating Systems _may_ require you to run as root to access this file.

To get started I recommend first looking at the classes in 
[com.diozero.sampleapps](https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/). 
To run the [LEDTest](https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/LEDTest.java) 
sample application using the pigpioj provider:

```shell
sudo java -cp tinylog-api-2.2.1.jar:tinylog-impl-2.2.1.jar:diozero-core-{{ site.version }}.jar:diozero-sampleapps-{{ site.version }}.jar:diozero-provider-pigpio-{{ site.version }}.jar:pigpioj-java-2.5.5.jar com.diozero.sampleapps.LEDTest 12
```

For an experience similar to Python where source code is interpreted rather than compiled try 
[Groovy](http://www.groovy-lang.org/) (`sudo apt update && sudo apt install groovy`). 
With the `CLASSPATH` environment variable set as per the instructions above, a simple test 
application can be run via the command `groovy <filename>`. There is also a Groovy shell environment `groovysh`.

A Groovy equivalent of the LED controlled button example:

```groovy
import com.diozero.devices.Button
import com.diozero.devices.LED
import com.diozero.util.SleepUtil

led = new LED(12)
button = new Button(25)

button.whenPressed({ led.on() })
button.whenReleased({ led.off() })

println("Waiting for button presses. Press CTRL-C to quit.")
SleepUtil.sleepSeconds(5)
```

To run:

```shell
sudo groovy -cp $CLASSPATH test.groovy
```
