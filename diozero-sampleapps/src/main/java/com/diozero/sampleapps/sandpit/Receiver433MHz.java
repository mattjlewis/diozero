package com.diozero.sampleapps.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     Receiver433MHz.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import java.util.LinkedList;
import java.util.List;

import org.tinylog.Logger;

import com.diozero.api.DigitalInputDevice;
import com.diozero.util.SleepUtil;

public class Receiver433MHz {
	// See https://github.com/sui77/rc-switch/blob/c5645170be8cb3044f4a8ca8565bfd2d221ba182/RCSwitch.cpp
	// 1 By One doorbell:
	// Expected: 0.35 ms
	//private static final int SHORT_DELAY_MAX_NS = 900_000;
	// Expected: 0.35 * 3 = 1.05 ms
	//private static final int LONG_DELAY_MAX_NS = 2_000_000;
	// Expected: ~6.2 ms
	//private static final int BLOCK_DELAY_MIN_NS = 6_000_000;
	public static enum Protocol {
		ONE_BY_ONE_DOORBELL(350_000, 1, 3, 18),
		VW(300_000, 1, 2, 25);
		
		private int shortNs;
		private int longNs;
		private int blockNs;
		private int shortMaxNs;
		private int longMaxNs;
		private int blockMinNs;
		
		private Protocol(int periodNs, int shortPulseCount, int longPulseCount, int blockPulseCount) {
			this.shortNs = periodNs * shortPulseCount;
			this.longNs = periodNs * longPulseCount;
			this.blockNs = periodNs * blockPulseCount;
			
			shortMaxNs = (int) (longNs * 0.86);
			longMaxNs = 2 * longNs;
			blockMinNs = (int) (blockNs * 0.86);
		}

		public int getShortNs() {
			return shortNs;
		}

		public int getLongNs() {
			return longNs;
		}

		public int getBlockNs() {
			return blockNs;
		}
		
		public int getShortMaxNs() {
			return shortMaxNs;
		}
		
		public int getLongMaxNs() {
			return longMaxNs;
		}
		
		public int getBlockMinNs() {
			return blockMinNs;
		}
	}
	
	private static boolean lastValue;
	private static long lastEventTimeNs = 0;
	private static boolean blockStarted = false;
	private static List<Boolean> data;
	
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <input gpio> [duration] [protocol name]", Receiver433MHz.class.getName());
		}
		int gpio = Integer.parseInt(args[0]);
		int duration = 10;
		if (args.length > 1) {
			duration = Integer.parseInt(args[1]);
		}
		Protocol protocol = Protocol.ONE_BY_ONE_DOORBELL;
		if (args.length > 2) {
			protocol = Protocol.valueOf(args[2]);
		}
		
		test(gpio, protocol, duration);
	}
	
	private static void test(int gpio, Protocol protocol, int duration) {
		data = new LinkedList<>();
		
		try (DigitalInputDevice input = new DigitalInputDevice(gpio)) {
			input.addListener(event -> {
				boolean value = event.getValue();
				long nano_time = event.getNanoTime();
				
				if (lastValue == value) {
					//Logger.debug("Lost an event");
					blockStarted = false;
				} else {
					int period_ns = (int) (nano_time - lastEventTimeNs);
					
					// New block signalled by data held low for ~ 6.2ms
					if (value && period_ns >= protocol.getBlockMinNs()) {
						if (blockStarted) {
							//Logger.debug("End of block");
							// Process captured data
							if (data.size() % 2 != 0) {
								Logger.error("Bad data length: {}", Integer.valueOf(data.size()));
							} else {
								String data_string = "";
								for (int i=0; i<data.size(); i+=2) {
									if (data.get(i).equals(data.get(i+1))) {
										data_string += data.get(i).booleanValue() ? "X" : "F";
									} else {
										data_string += data.get(i).booleanValue() ? "0" : "1";
									}
								}
								Logger.info("Got data: {}", data_string);
							}
						}
						//Logger.debug("Starting new block");
						// Clear any captured data
						data.clear();
						blockStarted = true;
						
						// Add implicit long pulse
						data.add(Boolean.TRUE);
					} else if (blockStarted) {
						if (period_ns < protocol.getShortMaxNs()) {
							//Logger.debug("short " + lastValue);
							data.add(Boolean.FALSE);
						} else if (period_ns < protocol.getLongMaxNs()) {
							//Logger.debug("long " + lastValue);
							data.add(Boolean.TRUE);
						} else {
							Logger.debug("Bad delay {} for event value {}", Integer.valueOf(period_ns), Boolean.valueOf(event.getValue()));
							blockStarted = false;
						}
					} else {
						//Logger.debug("Bad event: " + event);
						blockStarted = false;
					}
					
					lastValue = value;
				}
				
				lastEventTimeNs = nano_time;
			});
			
			Logger.info("Sleeping for {} seconds", Integer.valueOf(duration));
			SleepUtil.sleepSeconds(duration);
		}
	}
}
