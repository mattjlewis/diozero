package com.diozero.sampleapps;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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


import java.io.Closeable;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.pmw.tinylog.Logger;

import com.diozero.Button;
import com.diozero.HD44780Lcd;
import com.diozero.HD44780Lcd.LcdConnection;
import com.diozero.HD44780Lcd.PCF8574LcdConnection;
import com.diozero.api.I2CConstants;
import com.diozero.util.RangeUtil;
import com.diozero.util.SleepUtil;

public class LcdGame implements Closeable {
	private static final char PLAYER_CHAR = '>';
	private static final char BLOCK_CHAR = '#';
	private static final char COLLISION_CHAR = 'X';
	private static final int START_DELAY_MS = 200;
	private static final int DELAY_STEP_MS = 5;
	private static final int DELAY_DECREMENT_STEP = 20;

	public static void main(String[] args) {
		int device_address = HD44780Lcd.PCF8574LcdConnection.DEFAULT_DEVICE_ADDRESS;
		if (args.length > 0) {
			device_address = Integer.decode(args[0]).intValue();
		}
		int controller = I2CConstants.BUS_1;
		if (args.length > 1) {
			controller = Integer.parseInt(args[1]);
		}
		
		try (LcdGame lcd_game = new LcdGame(new PCF8574LcdConnection(controller, device_address), 35, 193, 0)) {
			lcd_game.run();
		}
	}
	
	private static enum State {
		NONE, WAITING_FOR_INPUT, PLAYING_GAME;
	}
	
	private HD44780Lcd lcd;
	private Button leftButton;
	private Button rightButton;
	private Button okButton;
	private int[][] cursorBounds;
	private Lock lock;
	private Condition cond;
	private State state = State.NONE;
	private Random random;
	private int playerPosY;
	private int[] cursorPos;
	
	public LcdGame(LcdConnection lcdConnection, int leftGpio, int rightGpio, int okGpio) {
		random = new Random();
		lcd = new HD44780Lcd(lcdConnection, 20, 4);
		leftButton = new Button(leftGpio);
		rightButton = new Button(rightGpio);
		okButton = new Button(okGpio);
		
		lock = new ReentrantLock();
		cond = lock.newCondition();
		
		leftButton.whenReleased(this::leftReleased);
		rightButton.whenReleased(this::rightReleased);
		okButton.whenReleased(this::okReleased);
	}
	
	private void movePlayer(int delta) {
		int new_player_pos_y = playerPosY+delta;
		new_player_pos_y = RangeUtil.constrain(new_player_pos_y, 0, lcd.getRowCount()-1);
		if (new_player_pos_y != playerPosY) {
			synchronized (lcd) {
				lcd.setCursorPosition(0, playerPosY);
				lcd.addText(' ');
				lcd.setCursorPosition(0, new_player_pos_y);
				lcd.addText('>');
				playerPosY = new_player_pos_y;
			}
		}
	}
	
	public void leftReleased() {
		switch (state) {
		case WAITING_FOR_INPUT:
			// Move to left prompt
			synchronized (lcd) {
				lcd.setCursorPosition(cursorBounds[0][0], cursorBounds[0][1]);
				cursorPos = cursorBounds[0];
			}
			break;
		case PLAYING_GAME:
			movePlayer(1);
			break;
		default:
		}
	}
	
	public void rightReleased() {
		switch (state) {
		case WAITING_FOR_INPUT:
			// Move to left prompt
			synchronized (lcd) {
				lcd.setCursorPosition(cursorBounds[1][0], cursorBounds[1][1]);
				cursorPos = cursorBounds[1];
			}
			break;
		case PLAYING_GAME:
			movePlayer(-1);
			break;
		default:
		}
	}
	
	public void okReleased() {
		lock.lock();
		try {
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}
	
	public void run() {
		while (true) {
			if (! prompt("Ready?")) {
				break;
			}
			int loop_count = play();
			lcd.setText(1, "Score: " + loop_count);
			if (! prompt("Again?")) {
				break;
			}
		}
	}
	
	private boolean prompt(String question) {
		int[] yes_pos;
		int[] no_pos;
		synchronized (lcd) {
			lcd.setCursorPosition(0, 0);
			String prompt = question + " Y/N";
			yes_pos = new int[] { prompt.length() - 3, 0 };
			no_pos = new int [] { prompt.length() - 1, 0 };
			cursorBounds = new int[][] { yes_pos, no_pos };
			lcd.addText(prompt);
			lcd.setCursorPosition(prompt.length()-2, 0);
			lcd.displayControl(true, true, true);
			state = State.WAITING_FOR_INPUT;
		}
		Boolean response = null;
		while (response == null) {
			lock.lock();
			try {
				cond.await();
			} catch (InterruptedException e) {
			} finally {
				lock.unlock();
			}
			if (cursorPos[0] == yes_pos[0] && cursorPos[1] == yes_pos[1]) {
				response = Boolean.TRUE;
			} else if (cursorPos[0] == no_pos[0] && cursorPos[1] == no_pos[1]) {
				response = Boolean.FALSE;
			}
		}
		state = State.NONE;
		lcd.displayControl(true, false, false);
		
		return response.booleanValue();
	}
	
	private int play() {
		// Turn off cursor
		lcd.displayControl(true, false, false);
		
		// Initialise the blocks ([y][x])
		boolean[][] objects = null;
		state = State.PLAYING_GAME;
		int delay = START_DELAY_MS;
		boolean collision = false;
		int loop_count = 0;
		while (! collision) {
			// 1. Prepare the game objects
			if (objects == null) {
				// 1.1 Initialise
				objects = new boolean[lcd.getRowCount()][lcd.getColumnCount()];
				for (int x=lcd.getColumnCount()/2; x<lcd.getColumnCount(); x++) {
					// TODO Need to make sure there is always a gap, i.e. must be at least 1 clear column
					objects[random.nextInt(lcd.getRowCount())][x] = random.nextBoolean();
				}
			} else {
				// 1.2.1 Shift all blocks left by 1
				// TODO Is it possible / quicker to do this?
				//for (int y=0; y<lcd.getRowCount(); y++) {
				//	System.arraycopy(objects[y], 1, objects[y], 0, objects[y].length-1);
				//}
				for (int y=0; y<lcd.getRowCount(); y++) {
					for (int x=0; x<lcd.getColumnCount()-1; x++) {
						objects[y][x] = objects[y][x+1];
					}
				}
				// 1.2.2 Add in a new column on the right
				for (int y=0; y<lcd.getRowCount(); y++) {
					objects[y][lcd.getColumnCount()-1] = false;
				}
				// TODO Need to make sure there is always a gap, i.e. must be at least 1 clear column
				objects[random.nextInt(lcd.getRowCount())][lcd.getColumnCount()-1] = random.nextBoolean();
			}
			
			// 2. Render the screen
			synchronized (lcd) {
				// 2.1 Clear the display
				lcd.clear();
				
				// 2.2 Add the blocks
				for (int y=0; y<lcd.getRowCount(); y++) {
					for (int x=0; x<lcd.getColumnCount(); x++) {
						if (objects[y][x]) {
							lcd.setCursorPosition(x, y);
							// Collision detection
							if (x == 0 && playerPosY == y) {
								// Collision!
								collision = true;
								lcd.addText(COLLISION_CHAR);
							} else  {
								lcd.addText(BLOCK_CHAR);
							}
						}
					}
				}
				
				// 2.3 Add the player (if no collision)
				if (! collision) {
					lcd.setCursorPosition(0, playerPosY);
					lcd.addText(PLAYER_CHAR);
				}
			}
			
			// 3. Pause and increment loop counter if there wasn't a collision
			if (! collision) {
				SleepUtil.sleepMillis(delay);
				
				if (++loop_count % DELAY_DECREMENT_STEP == 0) {
					delay -= DELAY_STEP_MS;
					Logger.info("Faster! delay={}", Integer.valueOf(delay));
				}
			}
		}
		state = State.NONE;
		
		return loop_count;
	}
	
	@Override
	public void close() {
		lcd.close();
	}
}
