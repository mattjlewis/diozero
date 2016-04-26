package com.diozero.sampleapps;

import org.pmw.tinylog.Logger;

import com.diozero.I2CLcd;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * I2C LCD sample application. To run:
 * <ul>
 * <li>JDK Device I/O 1.0:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-jdkdio10-$DIOZERO_VERSION.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sandpit.I2CLcdSampleApp20x4}</li>
 * <li>JDK Device I/O 1.1:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-jdkdio11-$DIOZERO_VERSION.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sandpit.I2CLcdSampleApp20x4}</li>
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sandpit.I2CLcdSampleApp20x4}</li>
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sandpit.I2CLcdSampleApp20x4}</li>
 * <li>pigpgioJ:<br>
 *  {@code sudo java -cp tinylog-1.0.3.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-1.0.0.jar com.diozero.sandpit.I2CLcdSampleApp20x4}</li>
 * </ul>
 */
public class I2CLcdSampleApp20x4 {
	// Main program block
	public static void main(String[] args) {
		// Initialise display
		try (I2CLcd lcd = new I2CLcd(20, 4)) {
			
			byte[] space_invader = new byte[] { 0x00, 0x0e, 0x15, 0x1f, 0x0a, 0x04, 0x0a, 0x11 };
			byte[] smilie = new byte[] { 0x00, 0x00, 0x0a, 0x00, 0x00, 0x11, 0x0e, 0x00 };
			byte[] frownie = new byte[] { 0x00, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x0e, 0x11 };
			lcd.createChar(0, space_invader);
			lcd.createChar(1, smilie);
			lcd.createChar(2, frownie);
			
			for (int i=0; i<2; i++) {
				lcd.setCursorPosition(0, i*2);
				lcd.addText((byte) 'H');
				lcd.addText((byte) 'e');
				lcd.addText((byte) 'l');
				lcd.addText((byte) 'l');
				lcd.addText((byte) 'o');
				lcd.addText((byte) ' ');
				lcd.addText((byte) ' ');
				lcd.addText((byte) 0);
				lcd.addText((byte) 1);
				lcd.addText((byte) 2);
				lcd.setCursorPosition(0, i*2+1);
				lcd.addText((byte) 'W');
				lcd.addText((byte) 'o');
				lcd.addText((byte) 'r');
				lcd.addText((byte) 'l');
				lcd.addText((byte) 'd');
				lcd.addText((byte) '!');
				lcd.addText((byte) ' ');
				lcd.addText((byte) 0);
				lcd.addText((byte) 1);
				lcd.addText((byte) 2);
			}
			SleepUtil.sleepSeconds(5);
			lcd.clear();
			
			for (int i=0; i<1; i++) {
				// Send some text
				lcd.setText(0, "Hello -");
				lcd.setText(1, "World! " + i);
				lcd.setText(2, "Hello -");
				lcd.setText(3, "World! " + i);
				SleepUtil.sleepSeconds(1);
				
				lcd.clear();
				SleepUtil.sleepSeconds(1);
			  
				// Send some more text
				lcd.setText(0, ">             RPiSpy");
				lcd.setText(1, ">            I2C LCD");
				lcd.setText(2, ">             RPiSpy");
				lcd.setText(3, ">            I2C LCD");
				SleepUtil.sleepSeconds(1);
			}
			
			SleepUtil.sleepSeconds(1);
			lcd.clear();
			
			for (byte b : "Hello Matt!".getBytes()) {
				lcd.addText(b);
				SleepUtil.sleepSeconds(.2);
			}
			
			SleepUtil.sleepSeconds(1);
			lcd.clear();

			int x=0;
			for (int i=0; i<3; i++) {
				for (byte b : "Hello World! ".getBytes()) {
					if (x++ == lcd.getColumnCount()) {
						lcd.entryModeControl(true, true);
					}
					lcd.addText(b);
					SleepUtil.sleepSeconds(.2);
				}
			}
			SleepUtil.sleepSeconds(1);
			lcd.clear();
			lcd.entryModeControl(true, false);

			for (int i=0; i<2; i++) {
				lcd.setCursorPosition(0, i*2);
				lcd.addText((byte) 'H');
				lcd.addText((byte) 'e');
				lcd.addText((byte) 'l');
				lcd.addText((byte) 'l');
				lcd.addText((byte) 'o');
				lcd.addText((byte) ' ');
				lcd.addText((byte) ' ');
				lcd.addText((byte) 0);
				lcd.addText((byte) 1);
				lcd.addText((byte) 2);
				lcd.setCursorPosition(0, i*2+1);
				lcd.addText((byte) 'W');
				lcd.addText((byte) 'o');
				lcd.addText((byte) 'r');
				lcd.addText((byte) 'l');
				lcd.addText((byte) 'd');
				lcd.addText((byte) '!');
				lcd.addText((byte) ' ');
				lcd.addText((byte) 0);
				lcd.addText((byte) 1);
				lcd.addText((byte) 2);
			}
			Logger.info("Sleeping for 60 seconds...");
			SleepUtil.sleepSeconds(60);
			
			lcd.clear();
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
		}
	}
}
