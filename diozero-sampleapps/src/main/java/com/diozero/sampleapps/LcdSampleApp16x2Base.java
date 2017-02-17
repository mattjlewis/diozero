package com.diozero.sampleapps;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 mattjlewis
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

import org.pmw.tinylog.Logger;

import com.diozero.HD44780Lcd;
import com.diozero.util.SleepUtil;

public class LcdSampleApp16x2Base {
	// Main program block
	public static void test(HD44780Lcd lcd) {
		lcd.setBacklightEnabled(true);
		
		/*Logger.info("Calling setText");
		lcd.setText(0, "Hello World!");
		SleepUtil.sleepSeconds(2);*/
		
		// 0, 14, 21, 31, 10, 4, 10, 17
		byte[] space_invader = HD44780Lcd.Characters.get("space_invader");
		byte[] smilie = HD44780Lcd.Characters.get("smilie");
		byte[] frownie = HD44780Lcd.Characters.get("frownie");
		lcd.createChar(0, space_invader);
		lcd.createChar(1, smilie);
		lcd.createChar(2, frownie);
		lcd.clear();

		Logger.info("Adding text");
		lcd.setCursorPosition(0, 0);
		lcd.addText('H');
		lcd.addText('e');
		lcd.addText('l');
		lcd.addText('l');
		lcd.addText('o');
		lcd.addText(' ');
		lcd.addText(' ');
		lcd.addText(0);
		lcd.addText(1);
		lcd.addText(2);
		lcd.setCursorPosition(0, 1);
		lcd.addText('W');
		lcd.addText('o');
		lcd.addText('r');
		lcd.addText('l');
		lcd.addText('d');
		lcd.addText('!');
		lcd.addText(' ');
		lcd.addText(0);
		lcd.addText(1);
		lcd.addText(2);
		SleepUtil.sleepSeconds(2);
		lcd.clear();
		
		lcd.createChar(3, HD44780Lcd.Characters.get("runninga"));
		lcd.createChar(4, HD44780Lcd.Characters.get("runningb"));
		lcd.clear();
		lcd.displayControl(true, false, false);
		for (int i=0; i<40; i++) {
			lcd.setCursorPosition(0, 0);
			lcd.addText(3);
			SleepUtil.sleepMillis(100);
			lcd.setCursorPosition(0, 0);
			lcd.addText(4);
			SleepUtil.sleepMillis(100);
		}
		SleepUtil.sleepSeconds(1);
		lcd.displayControl(true, true, true);
		lcd.clear();
		
		for (int i=0; i<4; i++) {
			// Send some text
			lcd.setText(0, "Hello -         ");
			lcd.setText(1, "World! " + i);
			SleepUtil.sleepSeconds(0.5);
			
			lcd.clear();

			// Send some more text
			lcd.setText(0, ">        diozero");
			lcd.setText(1, ">    HD44780 LCD");
			SleepUtil.sleepSeconds(0.5);
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
				SleepUtil.sleepSeconds(.1);
			}
		}
		SleepUtil.sleepSeconds(1);
		lcd.clear();
		lcd.entryModeControl(true, false);
		
		lcd.setCursorPosition(0, 0);
		lcd.addText('H');
		lcd.addText('e');
		lcd.addText('l');
		lcd.addText('l');
		lcd.addText('o');
		lcd.addText(' ');
		lcd.addText(' ');
		lcd.addText(0);
		lcd.addText(1);
		lcd.addText(2);
		lcd.setCursorPosition(0, 1);
		lcd.addText('W');
		lcd.addText('o');
		lcd.addText('r');
		lcd.addText('l');
		lcd.addText('d');
		lcd.addText('!');
		lcd.addText(' ');
		lcd.addText(0);
		lcd.addText(1);
		lcd.addText(2);
		
		Logger.info("Sleeping for 4 seconds...");
		SleepUtil.sleepSeconds(4);
		
		lcd.clear();
	}
}
