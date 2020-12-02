package com.diozero.sampleapps;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.tinylog.Logger;

import com.diozero.api.AnalogInputDevice;
import com.diozero.api.DigitalInputDevice;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.devices.Ads1x15;
import com.diozero.devices.BME280;
import com.diozero.devices.oled.ColourSsdOled;
import com.diozero.devices.oled.SSD1351;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.SleepUtil;
import com.diozero.util.TemperatureUtil;

public class EnvironmentDisplay {
	private static float reading;

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

				Ads1x15 adc = new Ads1x15(i2c_controller, Ads1x15.Address.GND, Ads1x15.PgaConfig.PGA_4096MV,
						Ads1x15.Ads1115DataRate.DR_8HZ);
				AnalogInputDevice ain = new AnalogInputDevice(adc, adc_read_channel);
				DigitalInputDevice adc_ready_pin = new DigitalInputDevice(adc_ready_gpio, GpioPullUpDown.PULL_UP,
						GpioEventTrigger.BOTH)) {
			int width = oled.getWidth();
			int height = oled.getHeight();
			final BufferedImage image = new BufferedImage(width, height, oled.getNativeImageType());
			final Graphics2D g2d = image.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Font font = new Font("Serif", Font.PLAIN, 12);
			g2d.setFont(font);
			FontMetrics fm = g2d.getFontMetrics(font);
			float line_height = fm.getMaxAscent() + fm.getMaxDescent();
			g2d.setBackground(Color.BLACK);

			adc.setContinousMode(adc_ready_pin, ain.getGpio(), new_reading -> EnvironmentDisplay.reading = new_reading);

			int period_ms = 100;
			DiozeroScheduler.getDaemonInstance().scheduleAtFixedRate(() -> {
				g2d.clearRect(0, 0, width, height);

				int index = 1;

				bme280.waitDataAvailable(10, 5);
				float[] tph = bme280.getValues();

				String t_text = String.format("T: %.2f C (%.2f F)", Float.valueOf(tph[0]),
						Float.valueOf(TemperatureUtil.toFahrenheit(tph[0])));
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
				g2d.drawString(adc_text, 0, index++ * line_height);

				int radius = 40;
				int baseline_y = height - 5;
				g2d.setColor(Color.white);
				g2d.fillArc(0, baseline_y - radius / 2, radius, radius, 0, 180);
				g2d.setColor(Color.blue);
				g2d.drawArc(0, baseline_y - radius / 2, radius, radius, 0, 180);
				g2d.drawLine(0, baseline_y, radius, baseline_y);
				g2d.setColor(Color.lightGray);
				double radians = Math.toRadians(reading * 180);
				// soh cah toa
				double sin_alpha = Math.sin(radians);
				double cos_alpha = Math.cos(radians);
				g2d.drawLine(radius / 2, baseline_y, radius / 2 + (int) (radius * sin_alpha),
						baseline_y - (int) (radius * cos_alpha));

				Logger.debug("Updating image");

				oled.display(image);
			}, 0, period_ms, TimeUnit.MILLISECONDS);

			while (running.get()) {
				Logger.debug("Sleeping");
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
