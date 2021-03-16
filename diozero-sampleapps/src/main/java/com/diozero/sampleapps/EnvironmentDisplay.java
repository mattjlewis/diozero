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
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.tinylog.Logger;

import com.diozero.api.AnalogInputDevice;
import com.diozero.api.DeviceInterface;
import com.diozero.api.DigitalInputDevice;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.devices.Ads1x15;
import com.diozero.devices.BME280;
import com.diozero.devices.PwmLed;
import com.diozero.devices.oled.ColourSsdOled;
import com.diozero.devices.oled.SSD1351;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.TemperatureUtil;

public class EnvironmentDisplay implements AutoCloseable {
	private static final Character DEGREES_CHARACTER = Character.valueOf('\u00B0');

	private float reading;
	private int bgImageIndex = 1;
	private BufferedImage[] backgroundImages;
	private DigitalOutputDevice oledDcPin;
	private DigitalOutputDevice oledResetPin;
	private ColourSsdOled oled;
	private BME280 bme280;
	private Ads1x15 adc;
	private AnalogInputDevice ain;
	private DigitalInputDevice adcDataReadyPin;
	private PwmLed pwmLed;
	private BufferedImage image;
	private Graphics2D g2d;
	private int displayWidth;
	private int displayHeight;
	private int fontLineHeight;
	private ScheduledFuture<?> future;

	public static void main(String[] args) {
		// For the SSD1351 OLED
		int spi_controller = 0;
		int spi_chip_select = 1;
		int dc_gpio = 21;
		int reset_gpio = 20;

		// For the ADS1115 ADC
		int i2c_controller = 1;
		int adc_read_channel = 3;
		int adc_ready_gpio = 24;

		try (EnvironmentDisplay display = new EnvironmentDisplay(spi_controller, spi_chip_select, dc_gpio, reset_gpio,
				i2c_controller, adc_read_channel, adc_ready_gpio)) {
			// Explicitly register this application for clean-up in case of
			DeviceFactoryHelper.registerForShutdown(display);
			
			display.start();
			
			display.waitToComplete();
		} catch (Exception e) {
			Logger.debug(e);
		}
	}

	public EnvironmentDisplay(int spiController, int spiChipSelect, int dcGpio, int resetGpio, int i2cController,
			int adcReadChannel, int adcReadyGpio) {
		oledDcPin = new DigitalOutputDevice(dcGpio);
		oledResetPin = new DigitalOutputDevice(resetGpio);
		oled = new SSD1351(spiController, spiChipSelect, oledDcPin, oledResetPin);

		bme280 = new BME280(i2cController, BME280.DEFAULT_I2C_ADDRESS);

		adc = new Ads1x15(i2cController, Ads1x15.Address.GND, Ads1x15.PgaConfig._4096MV, Ads1x15.Ads1115DataRate._8HZ);

		ain = new AnalogInputDevice(adc, adcReadChannel);
		adcDataReadyPin = new DigitalInputDevice(adcReadyGpio, GpioPullUpDown.PULL_UP, GpioEventTrigger.BOTH);

		pwmLed = new PwmLed(18);

		displayWidth = oled.getWidth();
		displayHeight = oled.getHeight();

		image = new BufferedImage(displayWidth, displayHeight, oled.getNativeImageType());
		g2d = image.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Font font = new Font("Serif", Font.PLAIN, 11);
		g2d.setFont(font);
		FontMetrics fm = g2d.getFontMetrics(font);
		fontLineHeight = fm.getMaxAscent() + fm.getMaxDescent();
		g2d.setBackground(Color.BLACK);

		backgroundImages = new BufferedImage[3];
		for (int i = 0; i < backgroundImages.length; i++) {
			try (InputStream is = EnvironmentDisplay.class
					.getResourceAsStream("/images/Background" + (i + 1) + ".jpg")) {
				if (is != null) {
					// The background image must have the same dimensions as the display
					BufferedImage bi = ImageIO.read(is);
					// Convert to the OLED image type
					backgroundImages[i] = new BufferedImage(oled.getWidth(), oled.getHeight(),
							oled.getNativeImageType());
					Graphics2D g = backgroundImages[i].createGraphics();
					g.drawImage(bi, 0, 0, oled.getWidth(), oled.getHeight(), null);
					g.dispose();
				}
			} catch (IOException e) {
				backgroundImages[i] = null;
			}
		}
	}

	public void start() {
		adc.setContinousMode(adcDataReadyPin, ain.getGpio(), new_reading -> reading = new_reading);

		int period_ms = 200;
		// Use the non-daemon thread pool so that the application doesn't quit
		future = DiozeroScheduler.getNonDaemonInstance().scheduleAtFixedRate(this::update, 0, period_ms,
				TimeUnit.MILLISECONDS);
	}

	public void stop() {
		if (future != null) {
			future.cancel(true);
			try {
				future.get();
			} catch (Exception e) {
				// Ignore
			}
		}
	}

	@Override
	public synchronized void close() {
		Logger.trace("Closing...");
		
		stop();
		
		// Close all device interfaces
		for (DeviceInterface device : Arrays.asList(oled, oledDcPin, oledResetPin, bme280, ain, adcDataReadyPin, adc,
				pwmLed)) {
			if (device != null) {
				try {
					device.close();
				} catch (Exception e) {
					// Ignore
				}
			}
		}

		// Set all objects to null so that close can be idempotent - don't want to
		// attempt to close these objects twice
		oled = null;
		oledDcPin = null;
		oledResetPin = null;
		bme280 = null;
		ain = null;
		adcDataReadyPin = null;
		adc = null;
		pwmLed = null;

		// Dispose of the Graphics2D object
		if (g2d != null) {
			g2d.dispose();
			g2d = null;
		}
		image = null;
		backgroundImages = null;
		
		Logger.trace("Closed.");
	}

	private void update() {
		pwmLed.setValue(reading);

		if (backgroundImages[bgImageIndex] == null) {
			g2d.clearRect(0, 0, displayWidth, displayHeight);
		} else {
			g2d.drawImage(backgroundImages[bgImageIndex], 0, 0, null);
		}

		int index = 1;

		bme280.waitDataAvailable(10, 5);
		float[] tph = bme280.getValues();

		String t_text = String.format("T: %.2f%cC (%.2f%cF)", Float.valueOf(tph[0]), DEGREES_CHARACTER,
				Float.valueOf(TemperatureUtil.toFahrenheit(tph[0])), DEGREES_CHARACTER);
		g2d.setColor(Color.red);
		g2d.drawString(t_text, 0, index++ * fontLineHeight);
		String p_text = String.format("P: %.2f hPa", Float.valueOf(tph[1]));
		g2d.setColor(Color.green);
		g2d.drawString(p_text, 0, index++ * fontLineHeight);
		String h_text = String.format("H: %.2f %%rH", Float.valueOf(tph[2]));
		g2d.setColor(Color.blue);
		g2d.drawString(h_text, 0, index++ * fontLineHeight);

		String adc_text = String.format("Pot: %.2f%% (%.2fv)", Float.valueOf(reading),
				Float.valueOf(ain.convertToScaledValue(reading)));
		g2d.setColor(Color.lightGray);
		g2d.drawString(adc_text, 0, displayHeight);

		int radius = 40;
		int baseline_y = displayHeight - fontLineHeight;

		drawSpeedometer(g2d, (displayWidth - radius) / 2, baseline_y, radius, reading, Color.white, Color.blue,
				Color.red);

		Logger.debug("Updating image");
		oled.display(image);
	}

	private void waitToComplete() throws InterruptedException, ExecutionException {
		future.get();
	}

	public static void drawSpeedometer(Graphics2D g2d, int x, int y, int radius, float value, Color background,
			Color outline, Color indicator) {
		g2d.setColor(background);
		g2d.fillArc(x, y - radius / 2, radius, radius, 0, 180);
		g2d.setColor(outline);
		g2d.drawArc(x, y - radius / 2, radius, radius, 0, 180);
		g2d.drawLine(x, y, x + radius, y);

		double radians = Math.toRadians((value - 0.5) * 180);
		double sin_alpha = Math.sin(radians);
		double cos_alpha = Math.cos(radians);
		g2d.setColor(indicator);
		g2d.drawLine(x + radius / 2, y, x + radius / 2 + (int) (radius / 2 * sin_alpha),
				y - (int) (radius / 2 * cos_alpha));
	}
}
