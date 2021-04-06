---
parent: Devices
nav_order: 8
permalink: /devices/ledstrips.html
---

# LED Strips

## WS2811B / WS2812B / Adafruit NeoPixel
{: #ws281x }

Provides support for [WS2811B / WS2812B aka Adafriut NeoPixel LEDs](https://learn.adafruit.com/adafruit-neopixel-uberguide)
via a JNI wrapper around the [rpi_ws281x C library](https://github.com/jgarff/rpi_ws281x)
as well as a native Java implementation that uses SPI.
Also controls APA102 LED strips using SPI.

{: .note-title }
> Colour Coding
>
> All colours are represented as 24bit RGB values.

TODO Insert wiring diagram.
