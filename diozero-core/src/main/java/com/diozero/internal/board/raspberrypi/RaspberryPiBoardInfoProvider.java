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
	private static final String BCM_HARDWARE_ID = "BCM";
	
	public static enum Model {
		A(0), B(1), A_PLUS(2), B_PLUS(3), PI_2_B(4), ALPHA(5), COMPUTE_MODEL(6), UNKNWON(7), PI_3_B(8), ZERO(9);
		
		private int id;
		private Model(int id) {
			this.id = id;
		}
		
		public int getId() {
			return id;
		}
		
		public static Model forId(int id) {
			Model[] values = Model.values();
			if (id < 0 || id >= values.length) {
				throw new IllegalArgumentException("Illegal Model id " + id + ", must be 0.." + (values.length-1));
			}
			return values[id];
		}
	}
	public static enum Revision {
		REV_1(0), REV_1_1(1), REV_2(2), REV_1_2(3);
		
		private int id;
		private Revision(int id) {
			this.id = id;
		}
		
		public int getId() {
			return id;
		}
		
		public static Revision forId(int id) {
			Revision[] values = Revision.values();
			if (id < 0 || id >= values.length) {
				throw new IllegalArgumentException("Illegal Revision id " + id + ", must be 0.." + (values.length-1));
			}
			return values[id];
		}
	}
	public static enum Memory {
		MEM_256(0, 256), MEM_512(1, 512), MEM_1024(2, 1024);

		private int id;
		private int ram;
		private Memory(int id, int ram) {
			this.id = id;
			this.ram = ram;
		}
		
		public int getId() {
			return id;
		}
		
		public int getRam() {
			return ram;
		}
		
		public static Memory forId(int id) {
			Memory[] values = Memory.values();
			if (id < 0 || id >= values.length) {
				throw new IllegalArgumentException("Illegal Memory id " + id + ", must be 0.." + (values.length-1));
			}
			return values[id];
		}
	}
	public static enum Manufacturer {
		SONY(0), EGOMAN(1), EMBEST(2), QISDA(3), EMBEST2(4);
		
		private int id;
		private Manufacturer(int id) {
			this.id = id;
		}
		
		public int getId() {
			return id;
		}
		
		public static Manufacturer forId(int id) {
			Manufacturer[] values = Manufacturer.values();
			if (id < 0 || id >= values.length) {
				throw new IllegalArgumentException("Illegal Manufacturer id " + id + ", must be 0.." + (values.length-1));
			}
			return values[id];
		}
	}
	public static enum Processor {
		BCM_2835(0), BCM_2836(1), BCM_2837(2);
		
		private int id;
		private Processor(int id) {
			this.id = id;
		}
		
		public int getId() {
			return id;
		}
		
		public static Processor forId(int id) {
			Processor[] values = Processor.values();
			if (id < 0 || id >= values.length) {
				throw new IllegalArgumentException("Illegal Processor id " + id + ", must be 0.." + (values.length-1));
			}
			return values[id];
		}
	}
	
	private static Map<String, BoardInfo> PI_BOARDS;
	static {
		PI_BOARDS = new HashMap<>();
		PI_BOARDS.put("0002", new PiBRev1BoardInfo(Revision.REV_1,
				Memory.MEM_256, Manufacturer.EGOMAN));
		PI_BOARDS.put("0003", new PiBRev1BoardInfo(Revision.REV_1_1,
				Memory.MEM_256, Manufacturer.EGOMAN));
		PI_BOARDS.put("0004", new PiABRev2BoardInfo(Model.B,
				Memory.MEM_256, Manufacturer.SONY));
		PI_BOARDS.put("0005", new PiABRev2BoardInfo(Model.B,
				Memory.MEM_256, Manufacturer.QISDA));
		PI_BOARDS.put("0006", new PiABRev2BoardInfo(Model.B,
				Memory.MEM_256, Manufacturer.EGOMAN));
		PI_BOARDS.put("0007", new PiABRev2BoardInfo(Model.A,
				Memory.MEM_256, Manufacturer.EGOMAN));
		PI_BOARDS.put("0008", new PiABRev2BoardInfo(Model.A,
				Memory.MEM_256, Manufacturer.SONY));
		PI_BOARDS.put("0009", new PiABRev2BoardInfo(Model.A,
				Memory.MEM_256, Manufacturer.QISDA));
		PI_BOARDS.put("000d", new PiABRev2BoardInfo(Model.B,
				Memory.MEM_512, Manufacturer.EGOMAN));
		PI_BOARDS.put("000e", new PiABRev2BoardInfo(Model.B,
				Memory.MEM_512, Manufacturer.SONY));
		PI_BOARDS.put("000f", new PiABRev2BoardInfo(Model.B,
				Memory.MEM_512, Manufacturer.EGOMAN));
		PI_BOARDS.put("0010", new PiABPlusBoardInfo(Model.B_PLUS, Revision.REV_1_2,
				Memory.MEM_512, Manufacturer.SONY, Processor.BCM_2835));
		PI_BOARDS.put("0011", new PiComputeModuleBoardInfo(
				Memory.MEM_512, Manufacturer.SONY, Processor.BCM_2835));
		PI_BOARDS.put("0012", new PiABPlusBoardInfo(Model.A_PLUS, Revision.REV_1_2,
				Memory.MEM_256, Manufacturer.SONY, Processor.BCM_2835));
		PI_BOARDS.put("0013", new PiABPlusBoardInfo(Model.B_PLUS, Revision.REV_1_2,
				Memory.MEM_512, Manufacturer.EGOMAN, Processor.BCM_2835));
		PI_BOARDS.put("0014", new PiComputeModuleBoardInfo(
				Memory.MEM_512, Manufacturer.SONY, Processor.BCM_2835));
		PI_BOARDS.put("0015", new PiABPlusBoardInfo(Model.A_PLUS, Revision.REV_1_1,
				Memory.MEM_256, Manufacturer.SONY, Processor.BCM_2835));
	}
	
	@Override
	public BoardInfo lookup(String hardware, String revision) {
		if (hardware == null || revision == null) {
			return null;
		}
		if (! hardware.startsWith(BCM_HARDWARE_ID) || revision.length() < 4) {
			return null;
		}
		
		try {
			int rev_int = Integer.parseInt(revision, 16);
			// With the release of the Raspberry Pi 2, there is a new encoding of the
			// Revision field in /proc/cpuinfo
			if ((rev_int & (1 << 23)) != 0) {
				int pcb_rev = (rev_int & (0x0F << 0)) >> 0;
				int model = (rev_int & (0xFF << 4)) >> 4;
				int proc = (rev_int & (0x0F << 12)) >> 12;
				int mfr = (rev_int & (0x0F << 16)) >> 16;
				int mem = (rev_int & (0x07 << 20)) >> 20;
				//boolean warranty_void = (revision & (0x03 << 24)) != 0;
				
				return new PiABPlusBoardInfo(Model.forId(model), Revision.forId(pcb_rev),
						Memory.forId(mem), Manufacturer.forId(mfr), Processor.forId(proc));
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
		private Model model;
		private Revision revision;
		private Memory memory;
		private Manufacturer manufacturer;
		private Processor processor;
		
		public PiBoardInfo(Model model, Revision revision, Memory memory,
				Manufacturer manufacturer, Processor processor) {
			super(MAKE, model.toString(), memory.getRam(), MAKE.toLowerCase());
			
			this.model = model;
			this.revision = revision;
			this.memory = memory;
			this.manufacturer = manufacturer;
			this.processor = processor;
		}
	
		public Model getPiModel() {
			return model;
		}
	
		public Revision getRevision() {
			return revision;
		}
	
		public Memory getPiMemory() {
			return memory;
		}
	
		public Manufacturer getManufacturer() {
			return manufacturer;
		}
	
		public Processor getProcessor() {
			return processor;
		}
	
		@Override
		public String toString() {
			return "PiBoardInfo [" + super.toString() + ", revision=" + revision + ", memory=" + memory + ", manufacturer="
					+ manufacturer + ", processor=" + processor + "]";
		}
	}
	
	public static class PiBRev1BoardInfo extends PiBoardInfo {
		public PiBRev1BoardInfo(Revision revision, Memory memory, Manufacturer manufacturer) {
			super(Model.B, revision, memory, manufacturer, Processor.BCM_2835);

			// 3v3 1 | 2 5v0
			addGpioPinInfo(0, 3, PinInfo.DIGITAL_IN_OUT);  // I2C SDA
			// 4 5v0
			addGpioPinInfo(1, 5, PinInfo.DIGITAL_IN_OUT);  // I2C SCL
			// 6 GND
			addGpioPinInfo(4, 7, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(14, 8, PinInfo.DIGITAL_IN_OUT); // UART TXD
			// 9 GND
			addGpioPinInfo(15, 10, PinInfo.DIGITAL_IN_OUT); // UART RXD
			addGpioPinInfo(17, 11, PinInfo.DIGITAL_IN_OUT);
			addPwmPinInfo(18, 12, 0, PinInfo.DIGITAL_IN_OUT_PWM);
			addGpioPinInfo(21, 13, PinInfo.DIGITAL_IN_OUT);
			// 14 GND
			addGpioPinInfo(22, 15, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(23, 16, PinInfo.DIGITAL_IN_OUT);
			// 17 3v3
			addGpioPinInfo(24, 18, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(10, 19, PinInfo.DIGITAL_IN_OUT); // SPI MOSI
			// 20 GND
			addGpioPinInfo(9, 21, PinInfo.DIGITAL_IN_OUT);  // SPI MISO
			addGpioPinInfo(25, 22, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(11, 23, PinInfo.DIGITAL_IN_OUT); // SPI CLK
			addGpioPinInfo(8, 24, PinInfo.DIGITAL_IN_OUT);  // SPI CE0
			// 25 GND
			addGpioPinInfo(7, 26, PinInfo.DIGITAL_IN_OUT);  // SPI CE1
		}
	}
	
	public static class PiABRev2BoardInfo extends PiBoardInfo {
		public PiABRev2BoardInfo(Model model, Memory memory, Manufacturer manufacturer) {
			super(model, Revision.REV_2, memory, manufacturer, Processor.BCM_2835);

			// 3v3 1 | 2 5v0
			addGpioPinInfo(2, 3, PinInfo.DIGITAL_IN_OUT);  // I2C SDA
			// 4 5v0
			addGpioPinInfo(3, 5, PinInfo.DIGITAL_IN_OUT);  // I2C SCL
			// 6 GND
			addGpioPinInfo(4, 7, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(14, 8, PinInfo.DIGITAL_IN_OUT); // UART TXD
			// 9 GND
			addGpioPinInfo(15, 10, PinInfo.DIGITAL_IN_OUT); // UART RXD
			addGpioPinInfo(17, 11, PinInfo.DIGITAL_IN_OUT);
			addPwmPinInfo(18, 12, 0, PinInfo.DIGITAL_IN_OUT_PWM);
			addGpioPinInfo(27, 13, PinInfo.DIGITAL_IN_OUT);
			// 14 GND
			addGpioPinInfo(22, 15, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(23, 16, PinInfo.DIGITAL_IN_OUT);
			// 17 3v3
			addGpioPinInfo(24, 18, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(10, 19, PinInfo.DIGITAL_IN_OUT); // SPI MOSI
			// 20 GND
			addGpioPinInfo(9, 21, PinInfo.DIGITAL_IN_OUT);  // SPI MISO
			addGpioPinInfo(25, 22, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(11, 23, PinInfo.DIGITAL_IN_OUT); // SPI CLK
			addGpioPinInfo(8, 24, PinInfo.DIGITAL_IN_OUT);  // SPI CE0
			// 25 GND
			addGpioPinInfo(7, 26, PinInfo.DIGITAL_IN_OUT);  // SPI CE1
		}
	}
	
	public static class PiABPlusBoardInfo extends PiBoardInfo {
		public static final String P5_HEADER = "P5";
		
		public PiABPlusBoardInfo(Model model, Revision revision, Memory memory, Manufacturer manufacturer, Processor processor) {
			super(model, revision, memory, manufacturer, processor);

			// 3v3 1 | 2 5v0
			addGpioPinInfo(2, 3, PinInfo.DIGITAL_IN_OUT);					// I2C-SDA
			// 4 5v0
			addGpioPinInfo(3, 5, PinInfo.DIGITAL_IN_OUT);					// I2C-SCL
			// 6 GND
			addGpioPinInfo(4, 7, PinInfo.DIGITAL_IN_OUT);					// GPCLK0
			addGpioPinInfo(14, 8, PinInfo.DIGITAL_IN_OUT);					// UART-TXD
			// 9 GND
			addGpioPinInfo(15, 10, PinInfo.DIGITAL_IN_OUT);				// UART-RXD
			addGpioPinInfo(17, 11, PinInfo.DIGITAL_IN_OUT);				// Alt4 = SPI1-CE1
			addPwmPinInfo(18, "PCM-CLK", 12, 0, PinInfo.DIGITAL_IN_OUT_PWM);	// Alt0 = PCM-CLK, Alt4 = SPI1-CE0, Alt5 = PWM0
			addGpioPinInfo(27, 13, PinInfo.DIGITAL_IN_OUT);
			// 14 GND
			addGpioPinInfo(22, 15, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(23, 16, PinInfo.DIGITAL_IN_OUT);
			// 17 3v3
			addGpioPinInfo(24, 18, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(10, 19, PinInfo.DIGITAL_IN_OUT);				// SPI0-MOSI
			// 20 GND
			addGpioPinInfo(9, 21, PinInfo.DIGITAL_IN_OUT);					// SPI0-MISO
			addGpioPinInfo(25, 22, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(11, 23, PinInfo.DIGITAL_IN_OUT);				// SPI0-CLK
			addGpioPinInfo(8, 24, PinInfo.DIGITAL_IN_OUT);					// SPI0-CE0
			// 25 GND
			addGpioPinInfo(7, 26, PinInfo.DIGITAL_IN_OUT);					// SPI0-CE1
			// BCM_0 (ID_SD) 27 | 28 BMC_1 (ID_SC)
			addGpioPinInfo(5, 29, PinInfo.DIGITAL_IN_OUT);
			// 20 GND
			addGpioPinInfo(6, 31, PinInfo.DIGITAL_IN_OUT);
			addPwmPinInfo(12, "PWM0", 32, 0, PinInfo.DIGITAL_IN_OUT_PWM);	// Alt0 = PWM0
			addPwmPinInfo(13, "PWM1", 33, 1, PinInfo.DIGITAL_IN_OUT_PWM);	// Alt0 = PWM1
			// 34 GND
			addPwmPinInfo(19, "PCM-FS", 35, 1, PinInfo.DIGITAL_IN_OUT_PWM);	// Alt4 = SPI1-MISO, Alt5 = PWM1
			addGpioPinInfo(16, 36, PinInfo.DIGITAL_IN_OUT);				// Alt4 = SPI1-CE2
			addGpioPinInfo(26, 37, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(20, 38, PinInfo.DIGITAL_IN_OUT);				// Alt4 = SPI1-MOSI
			// 39 GND
			addGpioPinInfo(21, 40, PinInfo.DIGITAL_IN_OUT);				// Alt4 = SPI1-SCLK
			// P5 Header
			addGpioPinInfo(P5_HEADER, 28, 0, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P5_HEADER, 29, 1, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P5_HEADER, 30, 2, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P5_HEADER, 31, 3, PinInfo.DIGITAL_IN_OUT);
		}
	}
	
	public static class PiComputeModuleBoardInfo extends PiBoardInfo {
		public PiComputeModuleBoardInfo(Memory memory, Manufacturer manufacturer, Processor processor) {
			super(Model.COMPUTE_MODEL, Revision.REV_1_2, memory, manufacturer, processor);

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
