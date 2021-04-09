---
Title: Performance
nav_order: 7
permalink: /performance.html
has_children: true
---

# Performance

I've done some limited performance tests (turning a GPIO on then off, see
[GpioPerfTest](https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/perf/GpioPerfTest.java))
on a Raspberry Pi 2 and 3 using the various [provider implementations](1_Providers.md).
I've also run tests using the underlying APIs directly without going via diozero to assess the overhead of diozero (see
[WiringPiRawPerfTest](https://github.com/mattjlewis/diozero/blob/master/diozero-provider-wiringpi/src/main/java/com/diozero/internal/provider/wiringpi/WiringPiRawPerfTest.java) and
[PigpioPerfTest](https://github.com/mattjlewis/pigpioj/blob/master/pigpioj-java/src/main/java/uk/pigpioj/test/PigpioPerfTest.java)) -
the overhead of diozero is approximately 1% for pigpio. Here are the results:

| Provider | Device | Frequency (kHz) |
| -------- |:------:| ---------------:|
| pigpio | Pi2 | 2,019 |
| pigpio | Pi3 | 2,900 |
| pigpio (JNI) | Pi2 | 2,509 |
| pigpio (JNI) | Pi3 | 3,537 |
| mmap | Pi3 |  7,686 |
| mmap (JNI) | Pi3 |   11,007 |

![Performance](/assets/images/Performance.png "Performance") 
