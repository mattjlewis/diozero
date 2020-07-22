package com.diozero.sampleapps.mqtt;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - MQTT Sample App
 * Filename:     MqttClientApp.java  
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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.GpioPullUpDown;
import com.diozero.devices.Button;
import com.diozero.devices.HD44780Lcd;
import com.diozero.devices.HD44780Lcd.LcdConnection;
import com.diozero.devices.HD44780Lcd.PCF8574LcdConnection;
import com.diozero.devices.SSD1351;
import com.diozero.devices.SsdOled;
import com.diozero.util.DioZeroScheduler;
import com.diozero.util.SleepUtil;
import com.google.gson.Gson;

public class MqttClientApp implements AutoCloseable, IMqttMessageListener, Runnable {
	private static final int DEFAULT_RUNTIME_SECONDS = 60;
	private static final String DEFAULT_MQTT_URL = "tcp://192.168.1.155";
	private static final int LCD_I2C_CONTROLLER = 1;
	private static final int DEFAULT_BUTTON_GPIO = 17;
	private static final String BUTTON_MQTT_TOPIC = "event/button";
	private static final int MAX_IMAGES = 5;
	
	private MqttClient mqttClient;
	private Button button;
	private Gson gson;
	private List<NewsItem> newsItems;
	private LcdConnection lcdConnection;
	private HD44780Lcd lcd;
	private DigitalOutputDevice oledDcPin;
	private DigitalOutputDevice oledResetPin;
	private SsdOled oled;
	private int headlineIndex;
	private int imageIndex;
	
	public static void main(String[] args) throws UnknownHostException, MqttException {
		int delay = DEFAULT_RUNTIME_SECONDS;
		if (args.length > 0) {
			delay = Integer.parseInt(args[0]);
		}
		
		try (MqttClientApp app = new MqttClientApp(DEFAULT_MQTT_URL, InetAddress.getLocalHost().getHostName())) {
			Logger.info("Sleeping for {} seconds...", Integer.valueOf(delay));
			SleepUtil.sleepSeconds(delay);
		}
	}

	public MqttClientApp(final String mqttUrl, final String hostname) throws MqttException {
		gson = new Gson();
		newsItems = new ArrayList<>();
		
		mqttClient = new MqttClient(mqttUrl, hostname);
		mqttClient.connect();
		
		lcdConnection = new PCF8574LcdConnection(LCD_I2C_CONTROLLER);
		lcd = new HD44780Lcd(lcdConnection, 20, 4);
		
		mqttClient.subscribe("news/#", this);
		
		int spi_controller = 0;
		int chip_select = 0;
		int dc_gpio = 22;
		int reset_gpio = 27;
		oledDcPin = new DigitalOutputDevice(dc_gpio);
		oledResetPin = new DigitalOutputDevice(reset_gpio);
		oled = new SSD1351(spi_controller, chip_select, oledDcPin, oledResetPin);
		
		DioZeroScheduler.getDaemonInstance().scheduleAtFixedRate(this, 1, 10, TimeUnit.SECONDS);
	
		button = new Button(DEFAULT_BUTTON_GPIO, GpioPullUpDown.PULL_UP);
		button.addListener(event -> {
			if (event.isActive()) {
				ButtonPressMessage message = new ButtonPressMessage(hostname, event.getGpio(), event.getEpochTime(), event.getValue());
				String payload = gson.toJson(message);
				Logger.info("Sending message '{}'", payload);
				try {
					mqttClient.publish(BUTTON_MQTT_TOPIC, new MqttMessage(payload.getBytes()));
				} catch (MqttException e) {
					Logger.error(e, "Error publishing button press event to MQTT: {}", e);
				}
			}
		});
		
		mqttClient.subscribe("image/change", (topic, message) -> {
			try {
				BufferedImage image = ImageIO.read(new FileInputStream(new File("Eddie" + imageIndex + ".jpg")));
				oled.display(image);
				if (++imageIndex == MAX_IMAGES) {
					imageIndex = 0;
				}
			} catch (IOException e) {
				Logger.error(e, "Error displaying image to OLED: {}", e);
			}
		});
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		NewsItem item = gson.fromJson(new String(message.getPayload()), NewsItem.class);
		Logger.info("Got news item on topic {}, headline: {}", topic, item.getTitle());
		synchronized (newsItems) {
			if (! newsItems.contains(item)) {
				newsItems.add(item);
			}
		}
	}
	
	@Override
	public void run() {
		synchronized (newsItems) {
			if (newsItems.size() == 0) {
				Logger.info("No news items to display...");
			} else {
				if (headlineIndex >= newsItems.size()) {
					headlineIndex = 0;
				}
				
				lcd.clear();
				lcd.returnHome();
				lcd.entryModeControl(true, false);
				
				String title = newsItems.get(headlineIndex).getTitle();
				
				int index = 0;
				for (byte b : title.getBytes()) {
					if (index == lcd.getColumnCount()*lcd.getRowCount()) {
						lcd.entryModeControl(true, true);
					} else if (index == lcd.getColumnCount()) {
						lcd.setCursorPosition(0, 1);
					} else if (index == lcd.getColumnCount()*2) {
						lcd.setCursorPosition(0, 2);
					} else if (index == lcd.getColumnCount()*3) {
						lcd.setCursorPosition(0, 3);
					}
					char c = (char) b;
					if (Character.isAlphabetic(c) || Character.isDigit(c) || Character.isWhitespace(c)) {
						lcd.addText(b);
						SleepUtil.sleepSeconds(0.02);
					}
					index++;
				}
				
				headlineIndex++;
			}
		}
	}

	@Override
	public void close() throws MqttException {
		button.close();
		
		lcd.close();
		lcdConnection.close();
		
		oled.close();
		oledDcPin.close();
		oledResetPin.close();
		
		mqttClient.disconnect();
		mqttClient.close();
	}
}
