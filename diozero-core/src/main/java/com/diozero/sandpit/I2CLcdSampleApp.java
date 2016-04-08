package com.diozero.sandpit;

import java.io.IOException;

import org.pmw.tinylog.Logger;

import com.diozero.util.SleepUtil;

public class I2CLcdSampleApp {
	// Main program block
	public static void main(String[] args) {
		// Initialise display
		try (I2CLcd lcd = new I2CLcd(2, 16)) {
			while (true) {
				// Send some text
				lcd.setText(0, "RPiSpy         <");
				lcd.setText(1, "I2C LCD        <");
	
				SleepUtil.sleepSeconds(3);
			  
				// Send some more text
				lcd.setText(0, ">         RPiSpy");
				lcd.setText(1, ">        I2C LCD");
	
				SleepUtil.sleepSeconds(3);
			}
		} catch (IOException e) {
			Logger.error(e, "Error: {}", e);
		}
	}
}
