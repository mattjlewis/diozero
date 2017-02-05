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


import java.io.*;

import com.diozero.I2CLcd;
import com.diozero.api.Action;
import com.diozero.api.I2CConstants;
import com.diozero.util.RuntimeIOException;

/**
 * I2C LCD sample interactive application. To run:
 * <ul>
 * <li>sysfs:<br>
 *  {@code java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar com.diozero.sandpit.I2CLcdSampleAppInteractive [i2c_address] [i2c_controller]}</li>
 * <li>JDK Device I/O 1.0:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-jdkdio10-$DIOZERO_VERSION.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sandpit.I2CLcdSampleAppInteractive [i2c_address] [i2c_controller]}</li>
 * <li>JDK Device I/O 1.1:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-jdkdio11-$DIOZERO_VERSION.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sandpit.I2CLcdSampleAppInteractive [i2c_address] [i2c_controller]}</li>
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sandpit.I2CLcdSampleAppInteractive [i2c_address] [i2c_controller]}</li>
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sandpit.I2CLcdSampleAppInteractive [i2c_address] [i2c_controller]}</li>
 * <li>pigpgioJ:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-1.0.1.jar com.diozero.sandpit.I2CLcdSampleAppInteractive [i2c_address] [i2c_controller]}</li>
 * </ul>
 */
public class I2CLcdSampleAppInteractive implements Closeable {
	public static void main(String[] args) {
		int device_address = I2CLcd.DEFAULT_DEVICE_ADDRESS;
		if (args.length > 0) {
			device_address = Integer.decode(args[0]).intValue();
		}
		int controller = I2CConstants.BUS_1;
		if (args.length > 1) {
			controller = Integer.parseInt(args[1]);
		}
		
		try (I2CLcdSampleAppInteractive app = new I2CLcdSampleAppInteractive()) {
			app.run(controller, device_address);
		}
	}
	
	private BufferedReader reader;
	private boolean running;
	private I2CLcd lcd;
	
	private OptionsMenu screenSizeMenu = new OptionsMenu("Screen Size", new String[] { "16x2", "20x4" });
	private ActionMenu mainMenu = new ActionMenu("Main Menu", new ActionMenuItem[] {
			new ActionMenuItem("Exit", () -> running = false)
			, new ActionMenuItem("Clear", () -> lcd.clear())
			, new ActionMenuItem("Set Cursor Position", () -> {
				int column = integerInputPrompt("Column", 1, lcd.getColumnCount());
				int row = integerInputPrompt("Row", 1, lcd.getRowCount());
				lcd.setCursorPosition(column-1, row-1);
			})
			, new ActionMenuItem("Add Text", () -> lcd.addText(textInputPrompt("Text", 1, lcd.getRowCount()*lcd.getColumnCount())))
			, new ActionMenuItem("Set Text", () -> {
				int row = integerInputPrompt("Row Number", 1, lcd.getRowCount());
				String text = textInputPrompt("Text", 1, lcd.getColumnCount());
				lcd.setText(row-1, text);
			})
			, new ActionMenuItem("Define Special Character", () -> {
				int location = integerInputPrompt("Memory Location", 0, 7);
				byte[] codes = integerArrayInputPrompt("Character Codes", 8, 0, 255);
				lcd.createChar(location, codes);
			})
			, new ActionMenuItem("Add Special Character", () -> {
				int location = integerInputPrompt("Memory Location", 0, 7);
				lcd.addText((byte) location);
			})
			, new ActionMenuItem("Entry Mode Control", () -> {
				boolean increment = booleanInputPrompt("Increment (otherwise decrement)", lcd.isIncrementOn());
				boolean shift_display = booleanInputPrompt("Shift Display (otherwise shift cursor)", lcd.isShiftDisplayOn());
				lcd.entryModeControl(increment, shift_display);
			})
			, new ActionMenuItem("Cursor Control", () -> {
				boolean cursor_on = booleanInputPrompt("Cursor On", lcd.isCursorEnabled());
				boolean blink_on = booleanInputPrompt("Blink On", lcd.isBlinkEnabled());
				lcd.displayControl(true, cursor_on, blink_on);
			})
			, new ActionMenuItem("Shift Display", () -> {
				int option = prompt(new OptionsMenu("Direction", new String[] { "Left", "Right" }));
				if (option == 1) {
					lcd.shiftDisplayLeft();
				} else {
					lcd.shiftDisplayRight();
				}
			})
			, new ActionMenuItem("Shift Cursor", () -> {
				int option = prompt(new OptionsMenu("Direction", new String[] { "Left", "Right" }));
				if (option == 1) {
					lcd.moveCursorLeft();
				} else {
					lcd.moveCursorRight();
				}
			})
		});
	
	public void run(int controller, int deviceAddress) {
		reader = new BufferedReader(new InputStreamReader(System.in));
		
		int resp = prompt(screenSizeMenu);
		String[] resolution = screenSizeMenu.getOption(resp).split("x");
		
		int columns = Integer.parseInt(resolution[0]);
		int rows = Integer.parseInt(resolution[1]);
		lcd = new I2CLcd(controller, deviceAddress, columns, rows);
		
		running = true;
		while (running) {
			try {
				prompt(mainMenu);
			} catch (IllegalArgumentException iae) {
				System.out.println("Error: " + iae);
			}
		}
	}
	
	private String readLine() {
		try {
			return reader.readLine();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
	
	private boolean booleanInputPrompt(String prompt, boolean currentVal) {
		while (true) {
			System.out.print(prompt + "? (y/n,yes/no,true/false,on/off) [currently " + (currentVal ? "on" : "off") + "]> ");
			System.out.flush();
			String s = readLine();
			if (s.equalsIgnoreCase("y") || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("on")) {
				return true;
			}
			if (s.equalsIgnoreCase("n") || s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false") || s.equalsIgnoreCase("off")) {
				return false;
			}
			System.out.println("Invalid response '" + s + "'");
		}
	}

	private int integerInputPrompt(String prompt, int min, int max) {
		while (true) {
			System.out.print(prompt + " (" + min + ".." + max + ")> ");
			System.out.flush();
			String s = readLine();
			try {
				return Integer.parseInt(s);
			} catch (NumberFormatException nfe) {
				System.out.println("Invalid number '" + s + "'");
			}
		}
	}

	private byte[] integerArrayInputPrompt(String prompt, int count, int min, int max) {
		while (true) {
			System.out.print(prompt + " (" + count + " comma separated numbers range " + min + ".." + max + ")> ");
			System.out.flush();
			String s = readLine();
			String[] codes = s.split(",");
			if (codes.length == count) {
				byte[] values = new byte[count];
				String val = "";
				try {
					for (int i=0; i<codes.length; i++) {
						val = codes[i].trim();
						values[i] = (byte) Integer.parseInt(val);
					}
					return values;
				} catch (NumberFormatException nfe) {
					System.out.println("Invalid number '" + val + "'");
				}
			} else {
				System.out.println("Not enough values (" + codes.length + "), expecting " + count);
			}
		}
	}

	private String textInputPrompt(String prompt, int minLength, int maxLength) {
		while (true) {
			System.out.print(prompt + " (" + minLength + ".." + maxLength + " chars)> ");
			System.out.flush();
			String s = readLine();
			if (s.length() >= minLength && s.length() <= maxLength) {
				return s;
			}
			
			System.out.println("Invalid input length (" + s.length() + "), must be " + minLength + ".." + maxLength);
		}
	}
	
	private int prompt(OptionsMenu menu) {
		int num_options = menu.getOptions().length;
		while (true) {
			System.out.println(menu.getPrompt() + ":");
			int i=1;
			for (String option : menu.getOptions()) {
				System.out.println(i++ + ") " + option);
			}
			System.out.print("> ");
			System.out.flush();
			
			String s = readLine();
			try {
				int resp = Integer.parseInt(s);
				if (resp >= 1 && resp <= num_options) {
					return resp;
				}
				System.out.println("Invalid input value " + resp + ". Must be 1.." + num_options);
			} catch (NumberFormatException nfe) {
				System.out.println("Invalid input value '" + s + "'. Must be 1.." + num_options);
			}
		}
	}
	
	private void prompt(ActionMenu menu) {
		int num_options = menu.getMenuItems().length;
		while (true) {
			System.out.println(menu.getPrompt() + ":");
			int i=1;
			for (ActionMenuItem menuItem : menu.getMenuItems()) {
				System.out.println(i++ + ") " + menuItem.getPrompt());
			}
			System.out.print("> ");
			System.out.flush();
			
			String s = readLine();
			try {
				int resp = Integer.parseInt(s);
				if (resp >= 1 && resp <= num_options) {
					ActionMenuItem menu_item = menu.getMenuItem(resp);
					System.out.println(menu_item.getPrompt() + ":");
					menu_item.getAction().action();
					return;
				}
				System.out.println("Invalid input value " + resp + ". Must be 1.." + num_options);
			} catch (NumberFormatException nfe) {
				System.out.println("Invalid input value '" + s + "'. Must be 1.." + num_options);
			}
		}
	}

	@Override
	public void close() {
		if (lcd != null) { lcd.close(); }
		if (reader != null) { try { reader.close(); } catch (IOException e) { } }
	}
}

class OptionsMenu {
	private String prompt;
	private String[] options;
	
	public OptionsMenu(String prompt, String[] options) {
		this.prompt = prompt;
		this.options = options;
	}

	public String getPrompt() {
		return prompt;
	}

	public String[] getOptions() {
		return options;
	}
	
	public String getOption(int resp) {
		return options[resp-1];
	}
}

class ActionMenu {
	private String prompt;
	private ActionMenuItem[] menuItems;

	public ActionMenu(String prompt, ActionMenuItem[] menuItems) {
		this.prompt = prompt;
		this.menuItems = menuItems;
	}

	public String getPrompt() {
		return prompt;
	}

	public ActionMenuItem[] getMenuItems() {
		return menuItems;
	}

	public ActionMenuItem getMenuItem(int resp) {
		return menuItems[resp-1];
	}
}

class ActionMenuItem {
	private String prompt;
	private Action action;
	
	public ActionMenuItem(String prompt, Action action) {
		this.prompt = prompt;
		this.action = action;
	}

	public String getPrompt() {
		return prompt;
	}

	public Action getAction() {
		return action;
	}
}
