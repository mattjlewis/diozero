package com.diozero.sampleapps.oled;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     SH1106Test.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import com.diozero.devices.oled.SH1106;
import com.diozero.devices.oled.SsdOledCommunicationChannel;

/**
 * <ul>
 *     <li>SPI (default)<br>
 *     {@code java -cp diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.oled.SH1106Test}</li>
 *     <li>I2C<br>
 *     {@code java -cp diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.oled.SH1106Test i2c}</li>
 * </ul>
 */
public class SH1106Test extends MonochromeSsdOledBase {
    public static void main(String[] args) {
        SsdOledCommunicationChannel channel = getChannel(args);
        try (SH1106 display = new SH1106(channel)) {
            sierpinski(display);
            customImage(display);
            animateText(display, "SH1106");
            display.clear();
        }
    }
}
