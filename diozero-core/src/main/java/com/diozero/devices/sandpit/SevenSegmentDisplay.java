package com.diozero.devices.sandpit;

import java.util.Arrays;

import com.diozero.api.DeviceInterface;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.SleepUtil;

/**
 * <pre>
 *     A
 *   F   B    Col
 *     G
 *   E   C    Col
 *     D   DP
 * </pre>
 */
public class SevenSegmentDisplay implements DeviceInterface {
	private static final boolean[][] NUMBERS = { //
			{ true, true, true, true, true, true, false }, // 0
			{ false, true, true, false, false, false, false }, // 1
			{ true, true, false, true, true, false, true }, // 2
			{ true, true, true, true, false, false, true }, // 3
			{ false, true, true, false, false, true, true }, // 4
			{ true, false, true, true, false, true, true }, // 5
			{ true, false, true, true, true, true, true }, // 6
			{ true, true, true, false, false, false, false }, // 7
			{ true, true, true, true, true, true, true }, // 8
			{ true, true, true, true, false, true, true }, // 9
	};

	private DigitalOutputDevice[] segments;
	private DigitalOutputDevice[] digitControl;

	public SevenSegmentDisplay(int aGpio, int bGpio, int cGpio, int dGpio, int eGpio, int fGpio, int gGpio,
			int[] digitControlGpios) {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), aGpio, bGpio, cGpio, dGpio, eGpio, fGpio, gGpio,
				digitControlGpios);
	}

	public SevenSegmentDisplay(GpioDeviceFactoryInterface deviceFactory, int aGpio, int bGpio, int cGpio, int dGpio,
			int eGpio, int fGpio, int gGpio, int[] digitControlGpios) {
		// TODO Include DP and Colon
		segments = new DigitalOutputDevice[7];
		int i = 0;
		segments[i++] = new DigitalOutputDevice(deviceFactory, aGpio, true, false);
		segments[i++] = new DigitalOutputDevice(deviceFactory, bGpio, true, false);
		segments[i++] = new DigitalOutputDevice(deviceFactory, cGpio, true, false);
		segments[i++] = new DigitalOutputDevice(deviceFactory, dGpio, true, false);
		segments[i++] = new DigitalOutputDevice(deviceFactory, eGpio, true, false);
		segments[i++] = new DigitalOutputDevice(deviceFactory, fGpio, true, false);
		segments[i++] = new DigitalOutputDevice(deviceFactory, gGpio, true, false);

		digitControl = new DigitalOutputDevice[digitControlGpios.length];
		for (i = 0; i < digitControlGpios.length; i++) {
			digitControl[i] = new DigitalOutputDevice(deviceFactory, digitControlGpios[i], false, false);
		}
	}

	@Override
	public void close() {
		Arrays.asList(digitControl).forEach(DeviceInterface::close);
		Arrays.asList(segments).forEach(DeviceInterface::close);
	}

	public void displayNumbers(int value, boolean[] display) {
		// TODO A thread to allow separate numbers on each digit
		for (int i = 0; i < digitControl.length; i++) {
			digitControl[i].setOn(display[i]);
		}
		boolean[] values = NUMBERS[value];
		for (int i = 0; i < segments.length; i++) {
			segments[i].setOn(values[i]);
		}
	}

	public void enableDigit(int digit) {
		for (int i = 0; i < digitControl.length; i++) {
			digitControl[i].setOn(i == digit);
		}
	}

	public void displayNumber(int value) {
		boolean[] values = NUMBERS[value];
		for (int i = 0; i < segments.length; i++) {
			segments[i].setOn(values[i]);
		}
	}

	public static void main(String[] args) {
		try (SevenSegmentDisplay disp = new SevenSegmentDisplay(25, 23, 5, 6, 16, 24, 11,
				new int[] { 20, 21, 19, 26 })) {
			System.out.println("First only");
			boolean[] digits = new boolean[] { true, false, false, false };
			int delay_ms = 200;
			for (int i = 0; i < 10; i++) {
				disp.displayNumbers(i, digits);
				SleepUtil.sleepMillis(delay_ms);
			}

			System.out.println("All");
			digits = new boolean[] { true, true, true, true };
			for (int i = 0; i < 10; i++) {
				disp.displayNumbers(i, digits);
				SleepUtil.sleepMillis(delay_ms);
			}

			System.out.println("None");
			digits = new boolean[] { false, false, false, false };
			for (int i = 0; i < 10; i++) {
				disp.displayNumbers(i, digits);
				SleepUtil.sleepMillis(delay_ms);
			}

			System.out.println("Alternate");
			digits = new boolean[] { true, false, true, false };
			for (int i = 0; i < 10; i++) {
				disp.displayNumbers(i, digits);
				SleepUtil.sleepMillis(delay_ms);
			}

			System.out.println("Countdown");
			// Countdown from 9999
			int number = 9999;
			int decrement_delta_ms = 50;
			long last_change = System.currentTimeMillis();
			while (true) {
				for (int i = 0; i < 4; i++) {
					int digit = (number / (int) (Math.pow(10, 3 - i))) % 10;
					disp.enableDigit(i);
					disp.displayNumber(digit);
					SleepUtil.sleepMillis(5);
				}
				if (System.currentTimeMillis() - last_change > decrement_delta_ms) {
					number--;
					if (number < 0) {
						number = 9999;
					}
					last_change = System.currentTimeMillis();
				}
			}
		}
	}
}
