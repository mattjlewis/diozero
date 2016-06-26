package com.diozero.internal.board.pi;

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

import com.diozero.util.BoardInfo;
import com.diozero.util.BoardInfoProvider;

/**
 * See <a href="https://github.com/AndrewFromMelbourne/raspberry_pi_revision">this c library</a>.
 */
public class PiBoardInfoProvider implements BoardInfoProvider {
	public static final String MAKE = "Raspberry Pi";
	
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
		VERSION_1(0), VERSION_1_1(1), VERSION_2(2), VERSION_1_2(3);
		
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
		PI_BOARDS.put("0002", new PiBoardInfo(Model.B, Revision.VERSION_1,
				Memory.MEM_256, Manufacturer.EGOMAN, Processor.BCM_2835));
		PI_BOARDS.put("0003", new PiBoardInfo(Model.B, Revision.VERSION_1_1,
				Memory.MEM_256, Manufacturer.EGOMAN, Processor.BCM_2835));
		PI_BOARDS.put("0004", new PiBoardInfo(Model.B, Revision.VERSION_2,
				Memory.MEM_256, Manufacturer.SONY, Processor.BCM_2835));
		PI_BOARDS.put("0005", new PiBoardInfo(Model.B, Revision.VERSION_2,
				Memory.MEM_256, Manufacturer.QISDA, Processor.BCM_2835));
		PI_BOARDS.put("0006", new PiBoardInfo(Model.B, Revision.VERSION_2,
				Memory.MEM_256, Manufacturer.EGOMAN, Processor.BCM_2835));
		PI_BOARDS.put("0007", new PiBoardInfo(Model.A, Revision.VERSION_2,
				Memory.MEM_256, Manufacturer.EGOMAN, Processor.BCM_2835));
		PI_BOARDS.put("0008", new PiBoardInfo(Model.A, Revision.VERSION_2,
				Memory.MEM_256, Manufacturer.SONY, Processor.BCM_2835));
		PI_BOARDS.put("0009", new PiBoardInfo(Model.A, Revision.VERSION_2,
				Memory.MEM_256, Manufacturer.QISDA, Processor.BCM_2835));
		PI_BOARDS.put("000d", new PiBoardInfo(Model.B, Revision.VERSION_2,
				Memory.MEM_512, Manufacturer.EGOMAN, Processor.BCM_2835));
		PI_BOARDS.put("000e", new PiBoardInfo(Model.B, Revision.VERSION_2,
				Memory.MEM_512, Manufacturer.SONY, Processor.BCM_2835));
		PI_BOARDS.put("000f", new PiBoardInfo(Model.B, Revision.VERSION_2,
				Memory.MEM_512, Manufacturer.EGOMAN, Processor.BCM_2835));
		PI_BOARDS.put("0010", new PiBoardInfo(Model.B_PLUS, Revision.VERSION_1_2,
				Memory.MEM_512, Manufacturer.SONY, Processor.BCM_2835));
		PI_BOARDS.put("0011", new PiBoardInfo(Model.COMPUTE_MODEL, Revision.VERSION_1_2,
				Memory.MEM_512, Manufacturer.SONY, Processor.BCM_2835));
		PI_BOARDS.put("0012", new PiBoardInfo(Model.A_PLUS, Revision.VERSION_1_2,
				Memory.MEM_256, Manufacturer.SONY, Processor.BCM_2835));
		PI_BOARDS.put("0013", new PiBoardInfo(Model.B_PLUS, Revision.VERSION_1_2,
				Memory.MEM_512, Manufacturer.EGOMAN, Processor.BCM_2835));
		PI_BOARDS.put("0014", new PiBoardInfo(Model.COMPUTE_MODEL, Revision.VERSION_1_2,
				Memory.MEM_512, Manufacturer.SONY, Processor.BCM_2835));
		PI_BOARDS.put("0015", new PiBoardInfo(Model.A_PLUS, Revision.VERSION_1_1,
				Memory.MEM_256, Manufacturer.SONY, Processor.BCM_2835));
	}
	
	@Override
	public BoardInfo lookup(String revisionString) {
		try {
			int revision = Integer.parseInt(revisionString, 16);
			// With the release of the Raspberry Pi 2, there is a new encoding of the
			// Revision field in /proc/cpuinfo
			if ((revision & (1 << 23)) != 0) {
				int pcb_rev = (revision & (0x0F << 0)) >> 0;
				int model = (revision & (0xFF << 4)) >> 4;
				int proc = (revision & (0x0F << 12)) >> 12;
				int mfr = (revision & (0x0F << 16)) >> 16;
				int mem = (revision & (0x07 << 20)) >> 20;
				//boolean warranty_void = (revision & (0x03 << 24)) != 0;
				
				return new PiBoardInfo(Model.forId(model), Revision.forId(pcb_rev),
						Memory.forId(mem), Manufacturer.forId(mfr), Processor.forId(proc));
			}
		} catch (NumberFormatException nfe) {
			// Ignore
		}
		if (revisionString.length() < 4) {
			return null;
		}
		
		return PI_BOARDS.get(revisionString.substring(revisionString.length()-4));
	}
	
	public static class PiBoardInfo extends BoardInfo {
		private Model model;
		private Revision revision;
		private Memory memory;
		private Manufacturer manufacturer;
		private Processor processor;
		
		public PiBoardInfo(Model model, Revision revision, Memory memory, Manufacturer manufacturer, Processor processor) {
			super(MAKE, model.toString(), memory.getRam());
			
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
}
