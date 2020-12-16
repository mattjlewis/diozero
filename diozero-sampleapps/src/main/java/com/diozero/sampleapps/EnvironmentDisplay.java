package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     EnvironmentDisplay.java  
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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

import org.tinylog.Logger;

import com.diozero.api.AnalogInputDevice;
import com.diozero.api.DigitalInputDevice;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.devices.Ads1x15;
import com.diozero.devices.BME280;
import com.diozero.devices.PwmLed;
import com.diozero.devices.oled.ColourSsdOled;
import com.diozero.devices.oled.SSD1351;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.SleepUtil;
import com.diozero.util.TemperatureUtil;

public class EnvironmentDisplay {
	private static final Character DEGREES_CHARACTER = Character.valueOf('\u00B0');

	private static float reading;
	private static BufferedImage backgroundImage;

	public static void main(String[] args) {
		// For the SSD1351 OLED
		int spi_controller = 0;
		int chip_select = 1;
		int dc_gpio = 21;
		int reset_gpio = 20;

		// For the ADS1115 ADC
		int i2c_controller = 1;
		int adc_read_channel = 3;
		int adc_ready_gpio = 24;

		AtomicBoolean running = new AtomicBoolean(true);

		try (DigitalOutputDevice dc_pin = new DigitalOutputDevice(dc_gpio);
				DigitalOutputDevice reset_pin = new DigitalOutputDevice(reset_gpio);
				ColourSsdOled oled = new SSD1351(spi_controller, chip_select, dc_pin, reset_pin);

				BME280 bme280 = new BME280(i2c_controller, BME280.DEFAULT_I2C_ADDRESS);

				Ads1x15 adc = new Ads1x15(i2c_controller, Ads1x15.Address.GND, Ads1x15.PgaConfig._4096MV,
						Ads1x15.Ads1115DataRate._8HZ);
				AnalogInputDevice ain = new AnalogInputDevice(adc, adc_read_channel);
				DigitalInputDevice adc_ready_pin = new DigitalInputDevice(adc_ready_gpio, GpioPullUpDown.PULL_UP,
						GpioEventTrigger.BOTH);

				PwmLed pwm_led = new PwmLed(18)) {
			int width = oled.getWidth();
			int height = oled.getHeight();
			final BufferedImage image = new BufferedImage(width, height, oled.getNativeImageType());
			final Graphics2D g2d = image.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Font font = new Font("Serif", Font.PLAIN, 11);
			g2d.setFont(font);
			FontMetrics fm = g2d.getFontMetrics(font);
			int line_height = fm.getMaxAscent() + fm.getMaxDescent();
			g2d.setBackground(Color.BLACK);

			try (InputStream is = EnvironmentDisplay.class
					.getResourceAsStream("/images/Background" + (new Random().nextInt(3) + 1) + ".png")) {
				if (is != null) {
					// The background image must have the same dimensions as the display
					BufferedImage i = ImageIO.read(is);
					// Convert to the OLED image type
					backgroundImage = new BufferedImage(oled.getWidth(), oled.getHeight(), oled.getNativeImageType());
					Graphics2D g = backgroundImage.createGraphics();
					g.drawImage(i, 0, 0, oled.getWidth(), oled.getHeight(), null);
					g.dispose();
				}
			} catch (IOException e) {
				backgroundImage = null;
			}

			adc.setContinousMode(adc_ready_pin, ain.getGpio(), new_reading -> reading = new_reading);

			int period_ms = 200;
			DiozeroScheduler.getDaemonInstance().scheduleAtFixedRate(() -> {
				pwm_led.setValue(reading);

				if (backgroundImage == null) {
					g2d.clearRect(0, 0, width, height);
				} else {
					g2d.drawImage(backgroundImage, 0, 0, null);
				}

				int index = 1;

				bme280.waitDataAvailable(10, 5);
				float[] tph = bme280.getValues();

				String t_text = String.format("T: %.2f%cC (%.2f%cF)", Float.valueOf(tph[0]), DEGREES_CHARACTER,
						Float.valueOf(TemperatureUtil.toFahrenheit(tph[0])), DEGREES_CHARACTER);
				g2d.setColor(Color.red);
				g2d.drawString(t_text, 0, index++ * line_height);
				String p_text = String.format("P: %.2f hPa", Float.valueOf(tph[1]));
				g2d.setColor(Color.green);
				g2d.drawString(p_text, 0, index++ * line_height);
				String h_text = String.format("H: %.2f %%rH", Float.valueOf(tph[2]));
				g2d.setColor(Color.blue);
				g2d.drawString(h_text, 0, index++ * line_height);

				String adc_text = String.format("Pot: %.2f%% (%.2fv)", Float.valueOf(reading),
						Float.valueOf(ain.convertToScaledValue(reading)));
				g2d.setColor(Color.lightGray);
				g2d.drawString(adc_text, 0, height);

				int radius = 40;
				int baseline_y = height - line_height;

				drawSpeedometer(g2d, (width - radius) / 2, baseline_y, radius, reading, Color.white, Color.blue,
						Color.red);

				Logger.debug("Updating image");

				oled.display(image);
			}, 0, period_ms, TimeUnit.MILLISECONDS);

			while (running.get()) {
				Logger.debug("Sleeping");
				SleepUtil.sleepSeconds(1);
			}
		}
	}

	public static void drawSpeedometer(Graphics2D g2d, int x, int y, int radius, float value, Color background,
			Color outline, Color indicator) {
		g2d.setColor(background);
		g2d.fillArc(x, y - radius / 2, radius, radius, 0, 180);
		g2d.setColor(outline);
		g2d.drawArc(x + 1, (y - radius / 2) - 1, radius - 1, radius - 1, 0, 180);
		g2d.drawLine(x, y, x + radius, y);

		double radians = Math.toRadians((value - 0.5) * 180);
		double sin_alpha = Math.sin(radians);
		double cos_alpha = Math.cos(radians);
		g2d.setColor(indicator);
		g2d.drawLine(x + radius / 2, y, x + radius / 2 + (int) (radius / 2 * sin_alpha),
				y - (int) (radius / 2 * cos_alpha));
	}
}
