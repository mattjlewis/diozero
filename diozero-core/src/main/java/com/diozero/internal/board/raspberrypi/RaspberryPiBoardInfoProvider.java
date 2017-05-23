package com.diozero.internal.board.raspberrypi;

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
import java.util.HashMap;
import java.util.Map;

import com.diozero.api.PinInfo;
import com.diozero.util.BoardInfo;
import com.diozero.util.BoardInfoProvider;

/**
 * See <a href="https://github.com/AndrewFromMelbourne/raspberry_pi_revision">this c library</a>.
 */
public class RaspberryPiBoardInfoProvider implements BoardInfoProvider {
	public static final String MAKE = "RaspberryPi";
	private static final String BCM_HARDWARE_PREFIX = "BCM";
	
	private static final String MODEL_A = "A";
	private static final String MODEL_B = "B";
	private static final String MODEL_A_PLUS = "A+";
	private static final String MODEL_B_PLUS = "B+";
	private static final String COMPUTE_MODULE = "CM";
	private static Map<Integer, String> MODELS;
	static {
		MODELS = new HashMap<>();
		MODELS.put(Integer.valueOf(0), MODEL_A);
		MODELS.put(Integer.valueOf(1), MODEL_B);
		MODELS.put(Integer.valueOf(2), MODEL_A_PLUS);
		MODELS.put(Integer.valueOf(3), MODEL_B_PLUS);
		MODELS.put(Integer.valueOf(4), "2B");
		MODELS.put(Integer.valueOf(5), "Alpha");
		MODELS.put(Integer.valueOf(6), COMPUTE_MODULE);
		MODELS.put(Integer.valueOf(8), "3B");
		MODELS.put(Integer.valueOf(9), "Zero");
		MODELS.put(Integer.valueOf(10), "CM3");
		MODELS.put(Integer.valueOf(12), "ZeroW");
	}
	
	private static final String PCB_REV_1_0 = "1.0";
	private static final String PCB_REV_1_1 = "1.1";
	private static final String PCB_REV_1_2 = "1.2";
	private static final String PCB_REV_2_0 = "2.0";
	
	private static Map<Integer, Integer> MEMORY;
	static {
		MEMORY = new HashMap<>();
		MEMORY.put(Integer.valueOf(0), Integer.valueOf(256));
		MEMORY.put(Integer.valueOf(1), Integer.valueOf(512));
		MEMORY.put(Integer.valueOf(2), Integer.valueOf(1024));
	}
	
	private static final String SONY = "Sony";
	private static final String EGOMAN = "Egoman";
	private static final String EMBEST = "Embest";
	private static final String SONY_JAPAN = "Sony Japan";
	private static final String EMBEST_2 = "Embest-2";
	private static final String QISDA = "Qisda";
	private static Map<Integer, String> MANUFACTURERS;
	static {
		MANUFACTURERS = new HashMap<>();
		MANUFACTURERS.put(Integer.valueOf(0), SONY);
		MANUFACTURERS.put(Integer.valueOf(1), EGOMAN);
		MANUFACTURERS.put(Integer.valueOf(2), EMBEST);
		MANUFACTURERS.put(Integer.valueOf(3), SONY_JAPAN);
		MANUFACTURERS.put(Integer.valueOf(4), EMBEST_2);
		MANUFACTURERS.put(Integer.valueOf(99), QISDA);
	}
	
	private static final String BCM2835 = "BCM2835";
	private static final String BCM2836 = "BCM2836";
	private static final String BCM2837 = "BCM2837";
	private static Map<Integer, String> PROCESSORS;
	static {
		PROCESSORS = new HashMap<>();
		PROCESSORS.put(Integer.valueOf(0), BCM2835);
		PROCESSORS.put(Integer.valueOf(1), BCM2836);
		PROCESSORS.put(Integer.valueOf(2), BCM2837);
	}
	
	private static Map<String, BoardInfo> PI_BOARDS;
	static {
		PI_BOARDS = new HashMap<>();
		PI_BOARDS.put("0002", new PiBRev1BoardInfo("0002", PCB_REV_1_0, 256, EGOMAN));
		PI_BOARDS.put("0003", new PiBRev1BoardInfo("0003", PCB_REV_1_1, 256, EGOMAN));
		PI_BOARDS.put("0004", new PiABRev2BoardInfo("0004", MODEL_B, 256, SONY));
		PI_BOARDS.put("0005", new PiABRev2BoardInfo("0005", MODEL_B, 256, QISDA));
		PI_BOARDS.put("0006", new PiABRev2BoardInfo("0006", MODEL_B, 256, EGOMAN));
		PI_BOARDS.put("0007", new PiABRev2BoardInfo("0007", MODEL_A, 256, EGOMAN));
		PI_BOARDS.put("0008", new PiABRev2BoardInfo("0008", MODEL_A, 256, SONY));
		PI_BOARDS.put("0009", new PiABRev2BoardInfo("0009", MODEL_A, 256, QISDA));
		PI_BOARDS.put("000d", new PiABRev2BoardInfo("000d", MODEL_B, 512, EGOMAN));
		PI_BOARDS.put("000e", new PiABRev2BoardInfo("000e", MODEL_B, 512, SONY));
		PI_BOARDS.put("000f", new PiABRev2BoardInfo("000f", MODEL_B, 512, QISDA));
		PI_BOARDS.put("0010", new PiABPlusBoardInfo("0010", MODEL_B_PLUS, PCB_REV_1_2, 512, SONY, BCM2835));
		PI_BOARDS.put("0011", new PiComputeModuleBoardInfo("0011", 512, SONY, BCM2835));
		PI_BOARDS.put("0012", new PiABPlusBoardInfo("0012", MODEL_A_PLUS, PCB_REV_1_2, 256, SONY, BCM2835));
		PI_BOARDS.put("0013", new PiABPlusBoardInfo("0013", MODEL_B_PLUS, PCB_REV_1_2, 512, EGOMAN, BCM2835));
		PI_BOARDS.put("0014", new PiComputeModuleBoardInfo("0014", 512, EMBEST, BCM2835));
		PI_BOARDS.put("0015", new PiABPlusBoardInfo("0015", MODEL_A_PLUS, PCB_REV_1_1, 256, EMBEST, BCM2835));
	}
	
	@Override
	public BoardInfo lookup(String hardware, String revision, Integer memoryKb) {
		if (hardware == null || revision == null) {
			return null;
		}
		if (! hardware.startsWith(BCM_HARDWARE_PREFIX) || revision.length() < 4) {
			return null;
		}
		
		try {
			int rev_int = Integer.parseInt(revision, 16);
			if ((rev_int & 0x800000) != 0) {
				// With the release of the Raspberry Pi 2, there is a new encoding of the
				// Revision field in /proc/cpuinfo
				if ((rev_int & (1 << 23)) != 0) {
					String pcb_revision;
					int pcb_rev = rev_int & 0x0F;
					switch (pcb_rev) {
					case 0:
						pcb_revision = PCB_REV_1_0;
						break;
					case 1:
						pcb_revision = PCB_REV_1_1;
						break;
					case 2:
						pcb_revision = PCB_REV_2_0;
						break;
					default:
						pcb_revision = "1." + pcb_rev;
					}
					int model = (rev_int & (0xFF << 4)) >> 4;
					int proc = (rev_int & (0x0F << 12)) >> 12;
					int mfr = (rev_int & (0x0F << 16)) >> 16;
					int mem = (rev_int & (0x07 << 20)) >> 20;
					//boolean warranty_void = (revision & (0x03 << 24)) != 0;
					
					String model_val = MODELS.get(Integer.valueOf(model));
					if (model_val == null) {
						model_val = "UNKNOWN-" + model;
					}
					String proc_val = PROCESSORS.get(Integer.valueOf(proc));
					if (proc_val == null) {
						proc_val = "UNKNOWN-" + proc;
					}
					String mfr_val = MANUFACTURERS.get(Integer.valueOf(mfr));
					if (mfr_val == null) {
						mfr_val = "UNKNOWN-" + mfr;
					}
					Integer mem_val = MEMORY.get(Integer.valueOf(mem));
					if (mem_val == null) {
						mem_val = Integer.valueOf(0);
					}
					return new PiABPlusBoardInfo(revision, model_val, pcb_revision,
							mem_val.intValue(), mfr_val, proc_val);
				}
			}
		} catch (NumberFormatException nfe) {
			// Ignore
		}
		
		return PI_BOARDS.get(revision.substring(revision.length()-4));
	}
	
	/**
	 * <p>See <a href="https://pinout.xyz/">https://pinout.xyz/</a></p>
	 * <pre>
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
	 *  </pre>
	 */
	static abstract class PiBoardInfo extends BoardInfo {
		private String code;
		private String pcbRevision;
		private String manufacturer;
		private String processor;
		
		public PiBoardInfo(String code, String model, String pcbRevision, int memory,
				String manufacturer, String processor) {
			super(MAKE, model, memory, MAKE.toLowerCase());
			
			this.code = code;
			this.pcbRevision = pcbRevision;
			this.manufacturer = manufacturer;
			this.processor = processor;
		}

		public String getRevision() {
			return pcbRevision;
		}
	
		public String getManufacturer() {
			return manufacturer;
		}
	
		public String getProcessor() {
			return processor;
		}
	
		@Override
		public String toString() {
			return "PiBoardInfo [" + super.toString() + ", code=" + code + ", pcbRevision=" + pcbRevision
					+ ", manufacturer=" + manufacturer + ", processor=" + processor + "]";
		}
	}
	
	public static class PiBRev1BoardInfo extends PiBoardInfo {
		public PiBRev1BoardInfo(String code, String pcbRevision, int memory, String manufacturer) {
			super(code, MODEL_B, pcbRevision, memory, manufacturer, BCM2835);

			int pin = 1;
			addGeneralPinInfo(pin++, PinInfo.VCC_3V3);
			addGeneralPinInfo(pin++, PinInfo.VCC_5V);
			addGpioPinInfo(0, pin++, PinInfo.DIGITAL_IN_OUT);	// I2C SDA
			addGeneralPinInfo(pin++, PinInfo.VCC_5V);
			addGpioPinInfo(1, pin++, PinInfo.DIGITAL_IN_OUT);	// I2C SCL
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(4, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(14, pin++, PinInfo.DIGITAL_IN_OUT);	// UART TXD
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(15, pin++, PinInfo.DIGITAL_IN_OUT);	// UART RXD
			addGpioPinInfo(17, pin++, PinInfo.DIGITAL_IN_OUT);
			// TODO Try enabling sysfs PWM, see http://www.jumpnowtek.com/rpi/Using-the-Raspberry-Pi-Hardware-PWM-timers.html
			//addPwmPinInfo(18, pin++, 0, PinInfo.DIGITAL_IN_OUT_PWM);
			addGpioPinInfo(18, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(21, pin++, PinInfo.DIGITAL_IN_OUT);
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(22, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(23, pin++, PinInfo.DIGITAL_IN_OUT);
			addGeneralPinInfo(pin++, PinInfo.VCC_3V3);
			addGpioPinInfo(24, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(10, pin++, PinInfo.DIGITAL_IN_OUT);	// SPI MOSI
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(9, pin++, PinInfo.DIGITAL_IN_OUT);	// SPI MISO
			addGpioPinInfo(25, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(11, pin++, PinInfo.DIGITAL_IN_OUT);	// SPI CLK
			addGpioPinInfo(8, pin++, PinInfo.DIGITAL_IN_OUT);	// SPI CE0
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(7, pin++, PinInfo.DIGITAL_IN_OUT);	// SPI CE1
		}
	}
	
	public static class PiABRev2BoardInfo extends PiBoardInfo {
		public PiABRev2BoardInfo(String code, String model, int memory, String manufacturer) {
			super(code, model, PCB_REV_2_0, memory, manufacturer, BCM2835);

			int pin = 1;
			addGeneralPinInfo(pin++, PinInfo.VCC_3V3);
			addGeneralPinInfo(pin++, PinInfo.VCC_5V);
			addGpioPinInfo(2, pin++, PinInfo.DIGITAL_IN_OUT);	// I2C SDA
			addGeneralPinInfo(pin++, PinInfo.VCC_5V);
			addGpioPinInfo(3, pin++, PinInfo.DIGITAL_IN_OUT);	// I2C SCL
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(4, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(14, pin++, PinInfo.DIGITAL_IN_OUT);	// UART TXD
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(15, pin++, PinInfo.DIGITAL_IN_OUT);	// UART RXD
			addGpioPinInfo(17, pin++, PinInfo.DIGITAL_IN_OUT);
			// TODO Try enabling sysfs PWM, see http://www.jumpnowtek.com/rpi/Using-the-Raspberry-Pi-Hardware-PWM-timers.html
			//addPwmPinInfo(18, pin++, 0, PinInfo.DIGITAL_IN_OUT_PWM);
			addGpioPinInfo(18, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(27, pin++, PinInfo.DIGITAL_IN_OUT);
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(22, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(23, pin++, PinInfo.DIGITAL_IN_OUT);
			addGeneralPinInfo(pin++, PinInfo.VCC_3V3);
			addGpioPinInfo(24, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(10, pin++, PinInfo.DIGITAL_IN_OUT);	// SPI MOSI
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(9, pin++, PinInfo.DIGITAL_IN_OUT);	// SPI MISO
			addGpioPinInfo(25, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(11, pin++, PinInfo.DIGITAL_IN_OUT);	// SPI CLK
			addGpioPinInfo(8, pin++, PinInfo.DIGITAL_IN_OUT);	// SPI CE0
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(7, pin++, PinInfo.DIGITAL_IN_OUT);	// SPI CE1
		}
	}
	
	public static class PiABPlusBoardInfo extends PiBoardInfo {
		public static final String P5_HEADER = "P5";
		
		public PiABPlusBoardInfo(String code, String model, String pcbRevision, int memory, String manufacturer, String processor) {
			super(code, model, pcbRevision, memory, manufacturer, processor);

			int pin = 1;
			addGeneralPinInfo(pin++, PinInfo.VCC_3V3);
			addGeneralPinInfo(pin++, PinInfo.VCC_5V);
			addGpioPinInfo(2, pin++, PinInfo.DIGITAL_IN_OUT);					// I2C-SDA
			addGeneralPinInfo(pin++, PinInfo.VCC_5V);
			addGpioPinInfo(3, pin++, PinInfo.DIGITAL_IN_OUT);					// I2C-SCL
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(4, pin++, PinInfo.DIGITAL_IN_OUT);					// GPCLK0
			addGpioPinInfo(14, pin++, PinInfo.DIGITAL_IN_OUT);					// UART-TXD
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(15, pin++, PinInfo.DIGITAL_IN_OUT);					// UART-RXD
			addGpioPinInfo(17, pin++, PinInfo.DIGITAL_IN_OUT);					// Alt4 = SPI1-CE1
			// TODO Try enabling sysfs PWM, see http://www.jumpnowtek.com/rpi/Using-the-Raspberry-Pi-Hardware-PWM-timers.html
			//addPwmPinInfo(18, pin++, 0, PinInfo.DIGITAL_IN_OUT_PWM);
			addGpioPinInfo(18, pin++, PinInfo.DIGITAL_IN_OUT);					// Alt0 = PCM-CLK, Alt4 = SPI1-CE0, Alt5 = PWM0
			addGpioPinInfo(27, pin++, PinInfo.DIGITAL_IN_OUT);
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(22, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(23, pin++, PinInfo.DIGITAL_IN_OUT);
			addGeneralPinInfo(pin++, PinInfo.VCC_3V3);
			addGpioPinInfo(24, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(10, pin++, PinInfo.DIGITAL_IN_OUT);					// SPI0-MOSI
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(9, pin++, PinInfo.DIGITAL_IN_OUT);					// SPI0-MISO
			addGpioPinInfo(25, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(11, pin++, PinInfo.DIGITAL_IN_OUT);					// SPI0-CLK
			addGpioPinInfo(8, pin++, PinInfo.DIGITAL_IN_OUT);					// SPI0-CE0
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(7, pin++, PinInfo.DIGITAL_IN_OUT);					// SPI0-CE1
			addGeneralPinInfo(pin++, "BCM 0 (ID_SD)");
			addGeneralPinInfo(pin++, "BCM 1 (ID_SC)");
			addGpioPinInfo(5, pin++, PinInfo.DIGITAL_IN_OUT);
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(6, pin++, PinInfo.DIGITAL_IN_OUT);
			// TODO Try enabling sysfs PWM, see http://www.jumpnowtek.com/rpi/Using-the-Raspberry-Pi-Hardware-PWM-timers.html
			//addPwmPinInfo(12, pin++, 0, PinInfo.DIGITAL_IN_OUT_PWM);
			addGpioPinInfo(12, pin++, PinInfo.DIGITAL_IN_OUT);					// Alt0 = PWM0
			// TODO Try enabling sysfs PWM, see http://www.jumpnowtek.com/rpi/Using-the-Raspberry-Pi-Hardware-PWM-timers.html
			//addPwmPinInfo(13, pin++, 1, PinInfo.DIGITAL_IN_OUT_PWM);
			addGpioPinInfo(13, pin++, PinInfo.DIGITAL_IN_OUT);					// Alt0 = PWM1
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			// TODO Try enabling sysfs PWM, see http://www.jumpnowtek.com/rpi/Using-the-Raspberry-Pi-Hardware-PWM-timers.html
			//addPwmPinInfo(19, pin++, 1, PinInfo.DIGITAL_IN_OUT_PWM);
			addGpioPinInfo(19, pin++, PinInfo.DIGITAL_IN_OUT);					// Alt4 = SPI1-MISO, Alt5 = PWM1
			addGpioPinInfo(16, pin++, PinInfo.DIGITAL_IN_OUT);					// Alt4 = SPI1-CE2
			addGpioPinInfo(26, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(20, pin++, PinInfo.DIGITAL_IN_OUT);					// Alt4 = SPI1-MOSI
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(21, pin++, PinInfo.DIGITAL_IN_OUT);					// Alt4 = SPI1-SCLK
			// P5 Header
			pin = 1;
			addGpioPinInfo(P5_HEADER, 28, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P5_HEADER, 29, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P5_HEADER, 30, pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P5_HEADER, 31, pin++, PinInfo.DIGITAL_IN_OUT);
		}
	}
	
	public static class PiComputeModuleBoardInfo extends PiBoardInfo {
		public PiComputeModuleBoardInfo(String code, int memory, String manufacturer, String processor) {
			super(code, COMPUTE_MODULE, PCB_REV_1_2, memory, manufacturer, processor);

			// See https://www.raspberrypi.org/documentation/hardware/computemodule/RPI-CM-DATASHEET-V1_0.pdf
			addGpioPinInfo(0, 3, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(1, 5, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(2, 9, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(3, 11, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(4, 15, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(5, 17, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(6, 21, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(7, 23, PinInfo.DIGITAL_IN_OUT);			// SPI0-CE1
			addGpioPinInfo(8, 27, PinInfo.DIGITAL_IN_OUT);			// SPI0-CE0
			addGpioPinInfo(9, 29, PinInfo.DIGITAL_IN_OUT);			// SPI0-MISO
			addGpioPinInfo(10, 33, PinInfo.DIGITAL_IN_OUT);			// SPI0-MOSI
			addGpioPinInfo(11, 35, PinInfo.DIGITAL_IN_OUT);			// SPI0-SCLK
			addPwmPinInfo(12, 45, 0, PinInfo.DIGITAL_IN_OUT_PWM);	// PWM0
			addPwmPinInfo(13, 47, 1, PinInfo.DIGITAL_IN_OUT_PWM);	// PWM1
			// TODO Complete this (up to GPIO45)
		}
	}
}
