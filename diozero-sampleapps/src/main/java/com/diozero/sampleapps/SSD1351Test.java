package com.diozero.sampleapps;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     SSD1351Test.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2021 diozero
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
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.tinylog.Logger;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.devices.oled.ColourSsdOled;
import com.diozero.devices.oled.SSD1351;
import com.diozero.devices.oled.SsdOled;
import com.diozero.sampleapps.gol.GameOfLife;
import com.diozero.sbc.DeviceFactoryHelper;

/**
 * <ul>
 * <li>Built-in:<br>
 * {@code java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar com.diozero.sampleapps.SSD1351Test}</li>
 * <li>pigpioj:<br>
 * {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-2.4.jar com.diozero.sampleapps.SSD1351Test}</li>
 * </ul>
 */
public class SSD1351Test {
	public static void main(String[] args) {
		/*-
		try (LED led = new LED(16)) {
			led.on();
			Thread.sleep(500);
			led.off();
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
		 */
		// int dc_gpio = 185;
		int dc_gpio = 21;
		// int reset_gpio = 224;
		int reset_gpio = 20;
		if (args.length > 1) {
			dc_gpio = Integer.parseInt(args[0]);
			reset_gpio = Integer.parseInt(args[1]);
		}
		// int spi_controller = 2;
		int spi_controller = 0;
		int chip_select = 1;
		if (args.length > 2) {
			spi_controller = Integer.parseInt(args[2]);
			chip_select = Integer.parseInt(args[3]);
		}

		try (DigitalOutputDevice dc_pin = new DigitalOutputDevice(dc_gpio);
				DigitalOutputDevice reset_pin = new DigitalOutputDevice(reset_gpio);
				ColourSsdOled oled = new SSD1351(spi_controller, chip_select, dc_pin, reset_pin)) {
			gameOfLife(oled, 10_000);
			displayImages(oled);
			sierpinskiTriangle(oled, 250);
			drawText(oled);
			testJava2D(oled);
			animateText(oled,
					"SSD1351 Organic LED Display demo scroller. Java implementation by diozero (diozero.com).");
		} catch (RuntimeException e) {
			Logger.error(e, "Error: {}", e);
		} finally {
			// Required if there are non-daemon threads that will prevent the
			// built-in clean-up routines from running
			DeviceFactoryHelper.getNativeDeviceFactory().close();
		}
	}

	public static void animateText(SsdOled oled, String text) {
		int width = oled.getWidth();
		int height = oled.getHeight();
		BufferedImage image = new BufferedImage(width, height, oled.getNativeImageType());
		Graphics2D g2d = image.createGraphics();
		Random random = new Random();

		Color[] colours = { Color.WHITE, Color.BLUE, Color.CYAN, Color.DARK_GRAY, Color.GRAY, Color.LIGHT_GRAY,
				Color.MAGENTA, Color.ORANGE, Color.PINK, Color.RED, Color.YELLOW };
		g2d.setBackground(Color.BLACK);

		Font f = g2d.getFont();
		Logger.info("Font name={}, family={}, size={}, style={}", f.getFontName(), f.getFamily(),
				Integer.valueOf(f.getSize()), Integer.valueOf(f.getStyle()));
		FontMetrics fm = g2d.getFontMetrics();
		int maxwidth = fm.stringWidth(text);

		int amplitude = height / 4;
		int offset = height / 2 - 4;
		int velocity = -2;
		int startpos = width;
		int pos = startpos;
		int x;
		for (int i = 0; i < 200; i++) {
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
				int y = (int) (offset + Math.floor(amplitude * Math.sin(x / ((float) width) * 2.0 * Math.PI)));
				// Draw text.
				g2d.setColor(colours[random.nextInt(colours.length)]);
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
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
		}
	}

	public static void gameOfLife(ColourSsdOled oled, long duration) {
		Logger.info("Game of Life");
		oled.clearDisplay();

		GameOfLife gol = new GameOfLife(oled.getWidth(), oled.getHeight());
		gol.randomise();
		long start = System.currentTimeMillis();
		long current_duration;
		int iterations = 0;
		do {
			render(oled, gol);
			gol.iterate();
			current_duration = System.currentTimeMillis() - start;
			iterations++;
		} while (current_duration < duration);
		double fps = iterations / (duration / 1000.0);
		Logger.info("FPS: {0.##}", Double.valueOf(fps));
	}

	private static void render(ColourSsdOled oled, GameOfLife gol) {
		for (int i = 0; i < gol.getWidth(); i++) {
			for (int j = 0; j < gol.getHeight(); j++) {
				if (gol.isAlive(i, j)) {
					oled.setPixel(i, j, ColourSsdOled.MAX_RED, ColourSsdOled.MAX_GREEN, ColourSsdOled.MAX_BLUE, false);
				} else {
					oled.setPixel(i, j, (byte) 0, (byte) 0, (byte) 0, false);
				}
			}
		}
		oled.display();
	}

	@SuppressWarnings("boxing")
	public static void displayImages(SsdOled oled) {
		Logger.info("Images");
		// https://github.com/rm-hull/luma.examples
		String[] images = { "/images/Background.png", "/images/balloon.png", "/images/pi_logo.png",
				"/images/pixelart1.png", "/images/pixelart2.png", "/images/pixelart3.jpg", "/images/pixelart4.jpg",
				"/images/pixelart5.jpg", "/images/starwars.png", "not found" };
		for (String image : images) {
			try (InputStream is = SSD1351Test.class.getResourceAsStream(image)) {
				if (is != null) {
					BufferedImage br = ImageIO.read(is);
					if (br.getWidth() != oled.getWidth() || br.getHeight() != oled.getHeight()) {
						Logger.debug("Rescaling image {} from {}x{} to {}x{}", image, br.getWidth(), br.getHeight(),
								oled.getWidth(), oled.getHeight());
						br = Scalr.resize(br, Scalr.Method.AUTOMATIC, Scalr.Mode.FIT_EXACT, oled.getWidth(),
								oled.getHeight(), Scalr.OP_ANTIALIAS);
					}
					oled.display(br);
				}
			} catch (IOException e) {
				Logger.error(e, "Error: {}", e);
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
		}
	}

	public static void sierpinskiTriangle(ColourSsdOled oled, int iterations) {
		Logger.info("Sierpinski triangle");
		int width = oled.getWidth();
		int height = oled.getHeight();
		final Random random = new Random();
		oled.clearDisplay();

		final Point[] corners = { new Point(width / 2, 0), new Point(0, height - 1), new Point(width - 1, height - 1) };
		Point point = new Point(corners[random.nextInt(corners.length)]);
		for (int i = 0; i < iterations; i++) {
			final Point target_corner = corners[random.nextInt(corners.length)];
			point.x += (target_corner.x - point.x) / 2;
			point.y += (target_corner.y - point.y) / 2;
			oled.setPixel(point.x, point.y, ColourSsdOled.MAX_RED, (byte) 0, (byte) 0, true);
			/*-
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
			}
			 */
		}
	}

	private static final class Point {
		int x, y;

		Point(int x, int y) {
			this.x = x;
			this.y = y;
		}

		Point(Point p) {
			this.x = p.x;
			this.y = p.y;
		}
	}

	public static void drawText(SsdOled oled) {
		Logger.info("Coloured text");
		int width = oled.getWidth();
		int height = oled.getHeight();
		BufferedImage image = new BufferedImage(width, height, oled.getNativeImageType());
		Graphics2D g2d = image.createGraphics();

		g2d.setBackground(Color.BLACK);
		g2d.clearRect(0, 0, width, height);

		g2d.setColor(Color.RED);
		g2d.drawString("Red", 10, 10);
		g2d.setColor(Color.GREEN);
		g2d.drawString("Green", 10, 20);
		g2d.setColor(Color.BLUE);
		g2d.drawString("Blue", 10, 30);

		oled.display(image);

		g2d.dispose();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
	}

	public static void testJava2D(ColourSsdOled oled) {
		Logger.info("Displaying custom image");
		int width = oled.getWidth();
		int height = oled.getHeight();
		BufferedImage image = new BufferedImage(width, height, oled.getNativeImageType());
		Graphics2D g2d = image.createGraphics();

		g2d.setBackground(Color.BLACK);
		g2d.clearRect(0, 0, width, height);

		g2d.setColor(Color.WHITE);
		g2d.drawLine(0, 0, width, height);
		g2d.drawLine(width, 0, 0, height);
		g2d.drawLine(width / 2, 0, width / 2, height);
		g2d.drawLine(0, height / 2, width, height / 2);

		g2d.setColor(Color.RED);
		g2d.drawRect(0, 0, width / 4, height / 4);

		g2d.setColor(Color.ORANGE);
		g2d.draw3DRect(width / 4, height / 4, width / 2, height / 2, true);

		g2d.setColor(Color.BLUE);
		g2d.drawOval(width / 2, height / 2, width / 3, height / 3);

		g2d.setColor(Color.GREEN);
		g2d.fillRect(width / 4, 0, width / 4, height / 4);

		g2d.setColor(Color.YELLOW);
		g2d.fillOval(0, height / 4, width / 4, height / 4);

		oled.display(image);
		g2d.dispose();

		Logger.debug("Sleeping for 2 seconds");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}

		Logger.debug("Inverting");
		oled.invertDisplay(true);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		Logger.debug("Restoring to normal");
		oled.invertDisplay(false);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}

		for (int i = 0; i < 255; i++) {
			oled.setContrast((byte) i);
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}
	}
}
