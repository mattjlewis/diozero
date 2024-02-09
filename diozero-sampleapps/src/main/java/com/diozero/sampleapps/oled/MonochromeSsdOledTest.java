package com.diozero.sampleapps.oled;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     MonochromeSsdOledTest.java
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
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

import org.tinylog.Logger;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.I2CDevice;
import com.diozero.devices.oled.MonochromeSsdOled;
import com.diozero.devices.oled.SSD1306;
import com.diozero.devices.oled.SsdOledCommunicationChannel;
import com.diozero.devices.oled.SsdOledCommunicationChannel.I2cCommunicationChannel;
import com.diozero.devices.oled.SsdOledCommunicationChannel.SpiCommunicationChannel;
import com.diozero.util.SleepUtil;

/**
 * Tests for grey/black and white OLEDs
 */
abstract public class MonochromeSsdOledTest {
    /**
     * Get the comm channel to use: if args == "i2c" use the default channel
     *
     * @param args program args
     * @return the channel to use
     */
    public static SsdOledCommunicationChannel getChannel(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("i2c")) {
            I2CDevice device = new I2CDevice(1, SSD1306.DEFAULT_I2C_ADDRESS);
            return new I2cCommunicationChannel(device);
        }

        DigitalOutputDevice dc_pin = new DigitalOutputDevice(22);
        DigitalOutputDevice reset_pin = new DigitalOutputDevice(27);
        return new SpiCommunicationChannel(0, 0, SpiCommunicationChannel.SPI_FREQUENCY, dc_pin, reset_pin);
    }

    public static void sierpinski(MonochromeSsdOled oled) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int width = oled.getWidth();
        int height = oled.getHeight();
        Logger.info("Sierpinski triangle for w/h {}/{}", width, height);
        int[][] corners = { { width / 2, 0 }, { 0, height - 1 }, { width - 1, height - 1 } };
        int[] start_corner = corners[random.nextInt(3)];
        int x = start_corner[0];
        int y = start_corner[1];
        for (int i = 0; i < 1_000; i++) {
            int[] target_corner = corners[random.nextInt(3)];
            x += (target_corner[0] - x) / 2;
            y += (target_corner[1] - y) / 2;
            oled.setPixel(x, y, true);
            oled.show();
            SleepUtil.sleepSeconds(0.005);
        }

    }

    public static void customImage(MonochromeSsdOled oled) {
        Logger.info("Displaying custom image");
        int width = oled.getWidth();
        int height = oled.getHeight();

        BufferedImage image = new BufferedImage(width, height, oled.getNativeImageType());
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.white);
        g2d.setBackground(Color.black);
        g2d.clearRect(0, 0, width, height);
        // TODO use this to find the bounds of the display for debugging
//        g2d.drawRect(0,0,width-1,height-1);
        g2d.drawLine(0, 0, width, height);
        g2d.drawLine(width, 0, 0, height);
        g2d.drawLine(width / 2, 0, width / 2, height);
        g2d.drawLine(0, height / 2, width, height / 2);
        g2d.drawRect(0, 0, width / 4, height / 4);
        g2d.draw3DRect(width / 4, height / 4, width / 2, height / 2, true);
        g2d.drawOval(width / 2, height / 2, width / 3, height / 3);
        g2d.fillRect(width / 4, 0, width / 4, height / 4);
        g2d.fillOval(0, height / 4, width / 4, height / 4);
        oled.display(image);
        Logger.debug("Sleeping for 2 seconds");
        SleepUtil.sleepSeconds(2);


        Logger.info("Inverting");
        oled.invertDisplay(true);
        SleepUtil.sleepSeconds(1);
        Logger.info("Restoring to normal");
        oled.invertDisplay(false);
        SleepUtil.sleepSeconds(1);

        Logger.info("Constrast changes");
        for (int i = 0; i < 255; i++) {
            oled.setContrast((byte)i);
            SleepUtil.sleepSeconds(0.01);
        }
    }

    public static void animateText(MonochromeSsdOled oled, String text) {
        text = text + " Organic LED Display demo scroller. Java implementation by diozero (diozero.com).";
        int width = oled.getWidth();
        int height = oled.getHeight();
        BufferedImage image = new BufferedImage(width, height, oled.getNativeImageType());
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(Color.white);
        g2d.setBackground(Color.black);

        Font f = g2d.getFont();
        Logger.info("Font name={}, family={}, size={}, style={}", f.getFontName(), f.getFamily(),
                    f.getSize(), f.getStyle());
        FontMetrics fm = g2d.getFontMetrics();
        int maxwidth = fm.stringWidth(text);

        int amplitude = height / 4;
        int offset = height / 2 - 4;
        int velocity = -2;
        int startpos = width;
        int pos = startpos;
        int x;
        for (int i = 0; i < 500; i++) {
            g2d.clearRect(0, 0, width, height);
            x = pos;

            for (char c : text.toCharArray()) {
                if (x > width) {
                    break;
                }

                if (x < -10) {
                    x += fm.charWidth(c);
                    continue;
                }
                // Calculate offset from sine wave.
                int y = (int)(offset + Math.floor(amplitude * Math.sin(x / ((float)width) * 2.0 * Math.PI)));
                // Draw text.
                g2d.drawString(String.valueOf(c), x, y);
                // Increment x position based on chacacter width.
                x += fm.charWidth(c);
            }
            // Draw the image buffer.
            oled.display(image);
            // Move position for next frame.
            pos += velocity;
            // Start over if text has scrolled completely off left side of screen.
            if (pos < -maxwidth) {
                pos = startpos;
            }

            // Pause briefly before drawing next frame.
            SleepUtil.sleepSeconds(0.05);
        }
    }
}
