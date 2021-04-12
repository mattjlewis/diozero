---
Title: Performance
nav_order: 7
permalink: /performance.html
has_children: true
---

# Performance

I've done some limited performance tests (turning a GPIO on then off, see
[GpioPerfTest](https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/perf/GpioPerfTest.java))
on a Raspberry Pi Zero, 2B, 3B and 4B using the various [provider implementations](1_Providers.md).
I've also run tests using the underlying APIs directly without going via the diozero APIs to assess the overhead of diozero.

The diozero performance test applications are all in the [com.diozero.sampleapps.perf](https://github.com/mattjlewis/diozero/tree/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/perf) package.
See [PigpioPerfTest](https://github.com/mattjlewis/pigpioj/blob/master/pigpioj-java/src/main/java/uk/pigpioj/test/PigpioPerfTest.java) for a similar performance test application for pigpioj without diozero.
The [mmap-tests]() folder contains some basic applications for testing mmap and gpio chardev performance without diozero.

Results (with diozero 1.2.0 and pigpioj 2.5.7):

![Performance](/assets/images/Performance.png "Performance") 

{: .note }
> * All tests were performed with OpenJDK 11 ("11.0.9.1" 2020-11-04) using default settings.
> * An LED was connected to the GPIO to verify that the GPIO was being toggled.
> * The Pi 3B diozero (builtin) results have been verified with an oscilloscope.
> * Operating System and libraries were all current as of April 2021 (RaspiOS 5.10.17-v7+, Armbian 5.10.21).
> * Operating Systems are 32-bit unless otherwise stated.
> * mmap controlled GPIO is not currently available for the BeagleBone Black using the built-in provider - it is via the BBBioLib provider.

## Conclusion

* While pigpio itself is extremely fast, the JNI layer introduces significant overhead
* The builtin memory mapped GPIO implementation is extremely fast
* 64-bit O/S delivers additional performance for the memory mapped GPIO implementation

## Raw Numbers

| Library (Provider) | Device | Frequency (kHz) |
| -------- |:------:| :---|
| pigpioj (JNI) | Pi Zero | 909 |
| pigpioj (JNI) | Pi 2B | 1,667 |
| pigpioj (JNI) | Pi 3B | 2,354 |
| pigpioj (JNI) | CM 4 | 2,751 |
| pigpioj (JNI) | Pi 4B (32-bit) | 2,771 |
| pigpioj (JNI) | Pi 4B (64-bit) | 2,623 |
| diozero (pigpio) | Pi Zero | 805 |
| diozero (pigpio) | Pi 2B | 1,533 |
| diozero (pigpio) | Pi 3B | 2,217 |
| diozero (pigpio) | CM 4 | 2,763 |
| diozero (pigpio) | Pi 4B (32-bit) | 2,858 |
| diozero (pigpio) | Pi 4B (64-bit) | 2,598 |
| diozero (builtin) | TinkerBoard | 515 |
| diozero (builtin) | BeagleBone Black | 191 |
| diozero (BBBioLib) | BeagleBone Black | 1,453 |
| diozero (builtin) | Odroid C2 | 1,668 |
| diozero (builtin) | NanoPi Neo | 2,328 |
| diozero (builtin) | NanoPi Duo 2 | 2,605 |
| diozero (builtin) | Pi Zero | 2,644 |
| diozero (builtin) | Pi 2B | 8,503 |
| diozero (builtin) | Pi 3B | 13,028 |
| diozero (builtin) | CM 4 | 21,115 |
| diozero (builtin) | Pi 4B (32-bit) | 21,110 |
| diozero (builtin) | Pi 4B (64-bit) | 27,785 |
