package com.diozero.sampleapps;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     SSD1306Test.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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


import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.devices.SSD1306;
import com.diozero.util.SleepUtil;

/**
 * <ul>
 * <li>sysfs:<br>
 * {@code java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar com.diozero.sampleapps.SSD1306Test}</li>
 * <li>JDK Device I/O 1.0:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-jdkdio10-$DIOZERO_VERSION.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.SSD1306Test}</li>
 * <li>JDK Device I/O 1.1:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-jdkdio11-$DIOZERO_VERSION.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.SSD1306Test}</li>
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.1.jar com.diozero.sampleapps.SSD1306Test}</li>
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.1.jar com.diozero.sampleapps.SSD1306Test}</li>
 * <li>pigpio:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-1.0.1.jar com.diozero.sampleapps.SSD1306Test}</li>
 * </ul>
 */
public class SSD1306Test {
	public static void main(String[] args) {
		Graphics2D g2d = null;
		Random random = new Random();
		try (DigitalOutputDevice dc_pin = new DigitalOutputDevice(22);
				DigitalOutputDevice reset_pin = new DigitalOutputDevice(27);
				SSD1306 oled = new SSD1306(0, 0, dc_pin, reset_pin)) {
			int width = oled.getWidth();
			int height = oled.getHeight();
			Logger.info("Sierpinski triangle");
			int[][] corners = { { width/2, 0 }, { 0, height-1 }, { width-1, height-1 } };
			int[] start_corner = corners[random.nextInt(3)];
			int x = start_corner[0];
			int y = start_corner[1];
			for (int i=0; i<1_000; i++) {
				int[] target_corner = corners[random.nextInt(3)];
				x += (target_corner[0] - x) / 2;
				y += (target_corner[1] - y) / 2;
				oled.setPixel(x, y, true);
				oled.display();
				SleepUtil.sleepSeconds(0.005);
			}
			
			Logger.info("Displaying custom image");
			BufferedImage image = new BufferedImage(width, height, oled.getNativeImageType());
			g2d = image.createGraphics();
			g2d.setColor(Color.white);
			g2d.setBackground(Color.black);
			g2d.clearRect(0, 0, width, height);
			g2d.drawLine(0, 0, width, height);
			g2d.drawLine(width, 0, 0, height);
			g2d.drawLine(width/2, 0, width/2, height);
			g2d.drawLine(0, height/2, width, height/2);
			g2d.drawRect(0, 0, width/4, height/4);
			g2d.draw3DRect(width/4, height/4, width/2, height/2, true);
			g2d.drawOval(width/2, height/2, width/3, height/3);
			g2d.fillRect(width/4, 0, width/4, height/4);
			g2d.fillOval(0, height/4, width/4, height/4);
			oled.display(image);
			Logger.debug("Sleeping for 2 seconds");
			SleepUtil.sleepSeconds(2);
			
			Logger.debug("Inverting");
			oled.invertDisplay(true);
			SleepUtil.sleepSeconds(1);
			Logger.debug("Restoring to normal");
			oled.invertDisplay(false);
			SleepUtil.sleepSeconds(1);
			
			for (int i=0; i<255; i++) {
				oled.setContrast((byte) i);
				SleepUtil.sleepSeconds(0.01);
			}
			
			animateText(oled, "SSD1306 Organic LED Display demo scroller. Java implementation by diozero (diozero.com).");
		} finally {
			if (g2d != null) {
				g2d.dispose();
			}
		}
	}

	private static void animateText(SSD1306 oled, String text) {
		int width = oled.getWidth();
		int height = oled.getHeight();
		BufferedImage image = new BufferedImage(width, height, oled.getNativeImageType());
		Graphics2D g2d = image.createGraphics();
		
		g2d.setColor(Color.white);
		g2d.setBackground(Color.black);
		
		Font f = g2d.getFont();
		Logger.info("Font name={}, family={}, size={}, style={}", f.getFontName(), f.getFamily(),
				Integer.valueOf(f.getSize()), Integer.valueOf(f.getStyle()));
		FontMetrics fm = g2d.getFontMetrics();
		int maxwidth = fm.stringWidth(text);
		
		int amplitude = height/4;
		int offset = height/2 - 4;
		int velocity = -2;
		int startpos = width;
		int pos = startpos;
		int x;
		for (int i=0; i<500; i++) {
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
		        int y = (int) (offset + Math.floor(amplitude * Math.sin(x / ((float)width) * 2.0 * Math.PI)));
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
