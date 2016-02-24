# LED Strips

## API

## Supported Devices

### WS2811B / WS2812B / Adafruit NeoPixel

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
