# LED Strips

## WS2811B / WS2812B / Adafruit NeoPixel {: #ws281x }

TODO Insert wiring diagram.

*class* **com.diozero.ws281xj.WS281x**{: .descname } (*frequency=800,000*, *dmaNum=5*, *gpioNum*, *brightness*, *numPixels*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-ws281x-java/src/main/java/com/diozero/ws281xj/WS281x.java){: .viewcode-link } [&para;](LEDStrips.md#ws281x "Permalink to this definition"){: .headerlink }

: Provides support for [WS2811B / WS2812B aka Adafriut NeoPixel LEDs](https://learn.adafruit.com/adafruit-neopixel-uberguide) via a JNI wrapper around the [rpi_ws281x C library](https://github.com/jgarff/rpi_ws281x).

    !!! note
        All colours are represented as 24bit RGB values.
    
    * **frequency** (*int*) - Communication frequency, 800,000 or 400,000.
    
    * **dmaNum** (*int*) - DMA number.
    
    * **gpioNum** (*int*) - GPIO pin to use to drive the LEDs.
    
    * **brightness** (*int*) - Brightness level.
    
    * **numPixels** (*int*) - The number of pixels connected.

    **render** ()
    
    : Push any updated colours to the LED strip.
    
    **allOff** ()
    
    : Turn off all pixels.
    
    *int* **getPixelColour** (*pixel*)
    
    : Get the current colour for the specified **pixel** (*int*).
    
    **setPixelColour** (*pixel*, *colour*)
    
    : Set the colour for the specified pixel.
    
    * **pixel** (*int*) - Pixel number.
    
    * **colour** (*int*) - Colour represented as a 24bit RGB integer (0x0RGB).
    
    **setPixelColourRGB** (*pixel*, *red*, *green*, *blue*)
    
    : Set the colour for the specified pixel using individual red / green / blue values.
    
    * **pixel** (*int*) - Pixel number.
    
    * **red** (*int*) - 8-bit value for the red component.
    
    * **green** (*int*) - 8-bit value for the green component.
        
    * **blue** (*int*) - 8-bit value for the blue component.
    
    **setPixelColourHSB** (*pixel*, *hue*, *saturation*, *brightness*)
    
    : Set the colour for the specified pixel using Hue Saturation Brightness (HSB) values.
    
    * **pixel** (*int*) - Pixel number.
    
    * **hue** (*float*) - Float value in the range 0..1 representing the hue.
    
    * **saturation** (*float*) - Float value in the range 0..1 representing the colour saturation.
    
    * **brightness** (*float*) - Float value in the range 0..1 representing the colour brightness.
    
    **setPixelColourHSL** (*pixel*, *hue*, *saturation*, *luminance*)
    
    : Set the colour for the specified pixel using Hue Saturation Luminance (HSL) values.
    
    !!! warning "In development"
        I'm not convinced this is working correctly. Code for the colour mapping taken [from this article written in 2009](https://tips4java.wordpress.com/2009/07/05/hsl-color/). Also not entirely sure how this is different from HSB colour mapping.
    
    * **pixel** (*int*) - Pixel number.
    
    * **hue** (*float*) - Represents the colour (think colours of the rainbow), specified in degrees from 0 - 360. Red is 0, green is 120 and blue is 240.
    
    * **saturation** (*float*) - Represents the purity of the colour. Range is 0..1 with 1 fully saturated and 0 gray.
    
    * **luminance** (*float*) - Represents the brightness of the colour. Range is 0..1 with 1 white 0 black.
