package com.diozero.internal.provider.pi4j;

import java.util.HashMap;
import java.util.Map;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

/**
 *  +-----+-----+---------+------+---+---Pi 2---+---+------+---------+-----+-----+
 *  | BCM | wPi |   Name  | Mode | V | Physical | V | Mode | Name    | wPi | BCM |
 *  +-----+-----+---------+------+---+----++----+---+------+---------+-----+-----+
 *  |     |     |    3.3v |      |   |  1 || 2  |   |      | 5v      |     |     |
 *  |   2 |   8 |   SDA.1 | ALT0 | 1 |  3 || 4  |   |      | 5V      |     |     |
 *  |   3 |   9 |   SCL.1 | ALT0 | 1 |  5 || 6  |   |      | 0v      |     |     |
 *  |   4 |   7 | GPIO. 7 |   IN | 1 |  7 || 8  | 1 | ALT0 | TxD     | 15  | 14  |
 *  |     |     |      0v |      |   |  9 || 10 | 1 | ALT0 | RxD     | 16  | 15  |
 *  |  17 |   0 | GPIO. 0 |   IN | 0 | 11 || 12 | 1 | IN   | GPIO. 1 | 1   | 18  |
 *  |  27 |   2 | GPIO. 2 |   IN | 0 | 13 || 14 |   |      | 0v      |     |     |
 *  |  22 |   3 | GPIO. 3 |   IN | 0 | 15 || 16 | 0 | IN   | GPIO. 4 | 4   | 23  |
 *  |     |     |    3.3v |      |   | 17 || 18 | 0 | IN   | GPIO. 5 | 5   | 24  |
 *  |  10 |  12 |    MOSI | ALT0 | 0 | 19 || 20 |   |      | 0v      |     |     |
 *  |   9 |  13 |    MISO | ALT0 | 0 | 21 || 22 | 0 | IN   | GPIO. 6 | 6   | 25  |
 *  |  11 |  14 |    SCLK | ALT0 | 0 | 23 || 24 | 1 | OUT  | CE0     | 10  | 8   |
 *  |     |     |      0v |      |   | 25 || 26 | 1 | OUT  | CE1     | 11  | 7   |
 *  |   0 |  30 |   SDA.0 |   IN | 1 | 27 || 28 | 1 | IN   | SCL.0   | 31  | 1   |
 *  |   5 |  21 | GPIO.21 |   IN | 1 | 29 || 30 |   |      | 0v      |     |     |
 *  |   6 |  22 | GPIO.22 |   IN | 1 | 31 || 32 | 0 | IN   | GPIO.26 | 26  | 12  |
 *  |  13 |  23 | GPIO.23 |   IN | 0 | 33 || 34 |   |      | 0v      |     |     |
 *  |  19 |  24 | GPIO.24 |   IN | 0 | 35 || 36 | 0 | IN   | GPIO.27 | 27  | 16  |
 *  |  26 |  25 | GPIO.25 |   IN | 0 | 37 || 38 | 0 | IN   | GPIO.28 | 28  | 20  |
 *  |     |     |      0v |      |   | 39 || 40 | 0 | IN   | GPIO.29 | 29  | 21  |
 *  +-----+-----+---------+------+---+----++----+---+------+---------+-----+-----+
 *  | BCM | wPi |   Name  | Mode | V | Physical | V | Mode | Name    | wPi | BCM |
 *  +-----+-----+---------+------+---+---Pi 2---+---+------+---------+-----+-----+
 */
public class RaspiGpioBcm {
	public static final Pin BCM_GPIO_2 = RaspiPin.GPIO_08;
	public static final Pin BCM_GPIO_3 = RaspiPin.GPIO_09;
	public static final Pin BCM_GPIO_4 = RaspiPin.GPIO_07;
	public static final Pin BCM_GPIO_5 = RaspiPin.GPIO_21;
	public static final Pin BCM_GPIO_6 = RaspiPin.GPIO_22;
	public static final Pin BCM_GPIO_7 = RaspiPin.GPIO_11;
	public static final Pin BCM_GPIO_8 = RaspiPin.GPIO_10;
	public static final Pin BCM_GPIO_9 = RaspiPin.GPIO_13;
	public static final Pin BCM_GPIO_10 = RaspiPin.GPIO_12;
	public static final Pin BCM_GPIO_11 = RaspiPin.GPIO_14;
	public static final Pin BCM_GPIO_12 = RaspiPin.GPIO_26;
	public static final Pin BCM_GPIO_13 = RaspiPin.GPIO_23;
	public static final Pin BCM_GPIO_14 = RaspiPin.GPIO_15;
	public static final Pin BCM_GPIO_15 = RaspiPin.GPIO_16;
	public static final Pin BCM_GPIO_16 = RaspiPin.GPIO_27;
	public static final Pin BCM_GPIO_17 = RaspiPin.GPIO_00;
	public static final Pin BCM_GPIO_18 = RaspiPin.GPIO_01;
	public static final Pin BCM_GPIO_19 = RaspiPin.GPIO_24;
	public static final Pin BCM_GPIO_20 = RaspiPin.GPIO_28;
	public static final Pin BCM_GPIO_21 = RaspiPin.GPIO_29;
	public static final Pin BCM_GPIO_22 = RaspiPin.GPIO_03;
	public static final Pin BCM_GPIO_23 = RaspiPin.GPIO_04;
	public static final Pin BCM_GPIO_24 = RaspiPin.GPIO_05;
	public static final Pin BCM_GPIO_25 = RaspiPin.GPIO_06;
	public static final Pin BCM_GPIO_26 = RaspiPin.GPIO_25;
	public static final Pin BCM_GPIO_27 = RaspiPin.GPIO_02;
	public static final Pin BCM_GPIO_28 = RaspiPin.GPIO_17;
	public static final Pin BCM_GPIO_29 = RaspiPin.GPIO_18;
	public static final Pin BCM_GPIO_30 = RaspiPin.GPIO_19;
	public static final Pin BCM_GPIO_31 = RaspiPin.GPIO_20;
	
	private static final Map<Integer, Pin> BCM_TO_WIRINGPI_MAPPING = new HashMap<>();
	static {
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(2), BCM_GPIO_2);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(3), BCM_GPIO_3);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(4), BCM_GPIO_4);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(5), BCM_GPIO_5);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(6), BCM_GPIO_6);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(7), BCM_GPIO_7);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(8), BCM_GPIO_8);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(9), BCM_GPIO_9);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(10), BCM_GPIO_10);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(11), BCM_GPIO_11);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(12), BCM_GPIO_12);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(13), BCM_GPIO_13);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(14), BCM_GPIO_14);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(15), BCM_GPIO_15);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(16), BCM_GPIO_16);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(17), BCM_GPIO_17);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(18), BCM_GPIO_18);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(19), BCM_GPIO_19);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(20), BCM_GPIO_20);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(21), BCM_GPIO_21);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(22), BCM_GPIO_22);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(23), BCM_GPIO_23);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(24), BCM_GPIO_24);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(25), BCM_GPIO_25);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(26), BCM_GPIO_26);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(27), BCM_GPIO_27);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(28), BCM_GPIO_28);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(29), BCM_GPIO_29);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(30), BCM_GPIO_30);
		BCM_TO_WIRINGPI_MAPPING.put(Integer.valueOf(31), BCM_GPIO_31);
	}
	
	public static Pin getPin(int pinNumer) {
		return BCM_TO_WIRINGPI_MAPPING.get(Integer.valueOf(pinNumer));
	}
}
