package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     MFRC522.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.tinylog.Logger;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.SPIConstants;
import com.diozero.api.SpiClockMode;
import com.diozero.api.SpiDevice;
import com.diozero.util.Hex;
import com.diozero.util.SleepUtil;

/**
 * <p><a href="http://www.nxp.com/documents/data_sheet/MFRC522.pdf">Datasheet</a><br>
 * <a href="https://github.com/mxgxw/MFRC522-python/blob/master/MFRC522.py">Example Python code</a><br>
 * <a href="https://github.com/nfc-tools/mfcuk">MiFare Classic Universal toolKit (MFCUK)</a><br>
 * Work-in-progress!</p>
 * <p>Wiring:</p>
 * <ul>
 * <li>SDA:  SPI0 CE0 (GPIO8)</li>
 * <li>SCK:  SPI0 SCLK (GPIO11)</li>
 * <li>MOSI: SPI0 MOSI (GPIO10)</li>
 * <li>MISO: SPI0 MISO (GPIO9)</li>
 * <li>IRQ:  Not connected</li>
 * <li>GND:  GND</li>
 * <li>RST:  Any free GPIO (GPIO25)</li>
 * <li>3v3:  3v3</li>
 * </ul>
 *
 * <p>Java port from <a href="https://github.com/miguelbalboa/rfid/blob/master/src/MFRC522.cpp">MFRC522 Library to use ARDUINO RFID MODULE KIT 13.56 MHZ WITH TAGS SPI W AND R BY COOQROBOT</a> that was created by Miguel Balboa (circuitito.com).</p>
 * 
 * <p>There are three hardware components involved:</p>
 * <ol>
 * <li>The controller, e.g. Arduino, Raspberry Pi</li>
 * <li>The PCD (Proximity C	oupling Device), e.g. NXP MFRC522 Contactless Reader</li>
 * <li>The PICC (Proximity Integrated Circuit Card): A card or tag using the ISO 14443A interface, e.g. Mifare or NTAG203.</li>
 * </ol>
 * 
 * <p>The microcontroller and card reader uses SPI for communication.
 * The protocol is described in the <a href="http://www.nxp.com/documents/data_sheet/MFRC522.pdf">MFRC522 datasheet</a>.</p>
 * 
 * <p>The card reader and the tags communicate using a 13.56MHz electromagnetic field.
 * The protocol is defined in <a href="http://wg8.de/wg8n1496_17n3613_Ballot_FCD14443-3.pdf">http://wg8.de/wg8n1496_17n3613_Ballot_FCD14443-3.pdf</a>:</p>
 * <ul>
 * <li>ISO/IEC 14443-3 Identification cards</li>
 * <li>Contactless integrated circuit cards</li>
 * <li>Proximity cards</li>
 * <li>Part 3: Initialization and anticollision"</li>
 * <li>Chapter 6, Type A ? Initialization and anticollision</li>
 * </ul>
 * 
 * <p>If only the PICC UID is wanted, the above documents have all the information needed.<br>
 * To read and write from MIFARE PICCs, the MIFARE protocol is used after the PICC has been selected.<br>
 * The MIFARE Classic and Ultralight chips and protocols are described in the datasheets:</p>
 * <ul>
 * <li><a href="http://www.mouser.com/ds/2/302/MF1S503x-89574.pdf">1K</a></li>
 * <li><a href="http://datasheet.octopart.com/MF1S7035DA4,118-NXP-Semiconductors-datasheet-11046188.pdf">4K</a></li>
 * <li><a href="http://www.idcardmarket.com/download/mifare_S20_datasheet.pdf">Mini</a></li>
 * <li><a href="http://www.nxp.com/documents/data_sheet/MF0ICU1.pdf">Ultralight</a></li>
 * <li><a href="http://www.nxp.com/documents/short_data_sheet/MF0ICU2_SDS.pdf">Ultralight C</a></li>
 * </ul>
 * 
 * <h2>MIFARE Classic 1K (MF1S503x)</h2>
 * <p>Has 16 sectors * 4 blocks/sector * 16 bytes/block = 1024 bytes.<br>
 * The blocks are numbered 0-63.<br>
 * Block 3 in each sector is the Sector Trailer. See <a href="http://www.mouser.com/ds/2/302/MF1S503x-89574.pdf">sections 8.6 and 8.7</a>:</p>
 * <pre>
 *   Bytes 0-5:   Key A
 *   Bytes 6-8:   Access Bits
 *   Bytes 9:     User data
 *   Bytes 10-15: Key B (or user data)
 * </pre>
 * <p>Block 0 is read-only manufacturer data, sometimes as follows:</p>
 * <pre>
 * 0 1 2 3 |4  | 5 |6   |7 8 9 A B C D E F
 * UID     |BCC|SAK|ATAQ|Manufacturer data
 * </pre>
 * <p>To access a block, an authentication using a key from the block's sector must be performed first.<br>
 * Example: To read from block 10, first authenticate using a key from sector 3 (blocks 8-11).<br>
 * All keys are set to FFFFFFFFFFFFh at chip delivery.<br>
 * <em>Warning</em>: Please read section 8.7 "Memory Access". It includes this text: if the PICC detects a format violation the whole sector is irreversibly blocked.<br>
 * To use a block in "value block" mode (for Increment/Decrement operations) you need to change the sector trailer. Use setAccessBits() to calculate the bit patterns.</p>
 *
 * <h2>MIFARE Classic 4K (MF1S703x)</h2>
 * <p>Has (32 sectors * 4 blocks/sector + 8 sectors * 16 blocks/sector) * 16 bytes/block = 4096 bytes.<br>
 * The blocks are numbered 0-255.<br>
 * The last block in each sector is the Sector Trailer like above.</p>
 * 
 * <h2>MIFARE Classic Mini (MF1 IC S20)</h2>
 * <p>Has 5 sectors * 4 blocks/sector * 16 bytes/block = 320 bytes.<br>
 * The blocks are numbered 0-19.<br>
 * The last block in each sector is the Sector Trailer like above.</p>
 * 
 * <h2>MIFARE Ultralight (MF0ICU1)</h2>
 * <p>Has 16 pages of 4 bytes = 64 bytes.<br>
 * Pages 0 + 1 is used for the 7-byte UID.<br>
 * Page 2 contains the last check digit for the UID, one byte manufacturer internal data, and the lock bytes (see http://www.nxp.com/documents/data_sheet/MF0ICU1.pdf section 8.5.2).<br>
 * Page 3 is OTP, One Time Programmable bits. Once set to 1 they cannot revert to 0.<br>
 * Pages 4-15 are read/write unless blocked by the lock bytes in page 2.</p>
 * 
 * <h2>MIFARE Ultralight C (MF0ICU2)</h2>
 * <p>Has 48 pages of 4 bytes = 192 bytes.<br>
 * Pages 0 + 1 is used for the 7-byte UID.<br>
 * Page 2 contains the last check digit for the UID, one byte manufacturer internal data, and the lock bytes (see http://www.nxp.com/documents/data_sheet/MF0ICU1.pdf section 8.5.2).<br>
 * Page 3 is OTP, One Time Programmable bits. Once set to 1 they cannot revert to 0.<br>
 * Pages 4-39 are read/write unless blocked by the lock bytes in page 2.<br>
 * Page 40 Lock bytes<br>
 * Page 41 16 bit one way counter<br>
 * Pages 42-43 Authentication configuration<br>
 * Pages 44-47 Authentication key</p>
 * 
 * <h2>Access conditions for sector trailer</h2>
 * <pre>
 *   KeyA  Bits  KeyB
 *   R  W  R  W  R  W
 * 0 -  A  A  -  A  A
 * 1 -  A  A  A  A  A Default
 * 2 -  -  A  -  A  -
 * 3 -  B  AB B  -  B
 * 4 -  B  AB -  -  B
 * 5 -  -  AB B  -  -
 * 6 -  -  AB -  -  -
 * 7 -  -  AB -  -  -
 * </pre>
 * 
 * <h2>Access conditions for data blocks</h2>
 * <pre>
 *     R   W   +  -X
 * 0  AB  AB  AB  AB Data  Default
 * 1  AB  --  --  AB Value
 * 2  AB  --  --  -- Data
 * 3   B   B  --  -- Data
 * 4  AB   B  --  -- Data
 * 5   B  --  --  -- Data
 * 6  AB   B   B  AB Value
 * 7  --  --  --  -- Data
 * </pre>
 * 
 * <h2>Workflow</h2>
 * <ol>
 * <li>Call ISO_Request() to check to see if a tag is in range and if so get its ATQA.</li>
 * <li>Upon success call ISO_Anticollision() which returns the UID of the active tag in range.</li>
 * <li>Upon success call ISO_Select(UID) which selects the active tag in range by its UID and returns its SAK.</li>
 * <li>If the card needs authentication call ISO_Authenticate(blockAddr, keyId, key, UID) to authenticate access to a block.</li>
 * <li>Call ISO_StopCrypto() when you have finished talking to a card which requires authentication.</li>
 * </ol>
 */
public class MFRC522 implements Closeable {
	public static final byte[] DEFAULT_KEY = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };
	
	// Firmware data for self-test
	// Reference values based on firmware version
	// Hint: if needed, you can remove unused self-test data to save flash memory
	//
	// Version 0.0 (0x90)
	// Philips Semiconductors; Preliminary Specification Revision 2.0 - 01 August 2005; 16.1 self-test
	public static final byte[] MFRC522_firmware_referenceV0_0 = {
		0x00, (byte) 0x87, (byte) 0x98, 0x0f, 0x49, (byte) 0xFF, 0x07, 0x19,
		(byte) 0xBF, 0x22, 0x30, 0x49, 0x59, 0x63, (byte) 0xAD, (byte) 0xCA,
		0x7F,(byte)  0xE3, 0x4E, 0x03, 0x5C, 0x4E, 0x49, 0x50,
		0x47, (byte) 0x9A, 0x37, 0x61, (byte) 0xE7, (byte) 0xE2, (byte) 0xC6, 0x2E,
		0x75, 0x5A, (byte) 0xED, 0x04, 0x3D, 0x02, 0x4B, 0x78,
		0x32, (byte) 0xFF, 0x58, 0x3B, 0x7C, (byte) 0xE9, 0x00, (byte) 0x94,
		(byte) 0xB4, 0x4A, 0x59, 0x5B, (byte) 0xFD, (byte) 0xC9, 0x29, (byte) 0xDF,
		0x35, (byte) 0x96, (byte) 0x98, (byte) 0x9E, 0x4F, 0x30, 0x32, (byte) 0x8D
	};
	// Version 1.0 (0x91)
	// NXP Semiconductors; Rev. 3.8 - 17 September 2014; 16.1.1 self-test
	public static final byte[] MFRC522_firmware_referenceV1_0 = {
		0x00, (byte) 0xC6, 0x37, (byte) 0xD5, 0x32, (byte) 0xB7, 0x57, 0x5C,
		(byte) 0xC2, (byte) 0xD8, 0x7C, 0x4D, (byte) 0xD9, 0x70, (byte) 0xC7, 0x73,
		0x10, (byte) 0xE6, (byte) 0xD2, (byte) 0xAA, 0x5E, (byte) 0xA1, 0x3E, 0x5A,
		0x14, (byte) 0xAF, 0x30, 0x61, (byte) 0xC9, 0x70, (byte) 0xDB, 0x2E,
		0x64, 0x22, 0x72, (byte) 0xB5, (byte) 0xBD, 0x65, (byte) 0xF4, (byte) 0xEC,
		0x22, (byte) 0xBC, (byte) 0xD3, 0x72, 0x35, (byte) 0xCD, (byte) 0xAA, 0x41,
		0x1F, (byte) 0xA7, (byte) 0xF3, 0x53, 0x14, (byte) 0xDE, 0x7E, 0x02,
		(byte) 0xD9, 0x0F, (byte) 0xB5, 0x5E, 0x25, 0x1D, 0x29, 0x79
	};
	// Version 2.0 (0x92)
	// NXP Semiconductors; Rev. 3.8 - 17 September 2014; 16.1.1 self-test
	public static final byte[] MFRC522_firmware_referenceV2_0 = {
		0x00, (byte) 0xEB, (byte) 0x66, (byte) 0xBA, 0x57, (byte) 0xBF, 0x23, (byte) 0x95,
		(byte) 0xD0, (byte) 0xE3, 0x0D, 0x3D, 0x27, (byte) 0x89, 0x5C, (byte) 0xDE,
		(byte) 0x9D, 0x3B, (byte) 0xA7, 0x00, 0x21, 0x5B, (byte) 0x89, (byte) 0x82,
		0x51, 0x3A, (byte) 0xEB, 0x02, 0x0C, (byte) 0xA5, 0x00, 0x49,
		0x7C, (byte) 0x84, 0x4D, (byte) 0xB3, (byte) 0xCC, (byte) 0xD2, 0x1B, (byte) 0x81,
		0x5D, 0x48, 0x76, (byte) 0xD5, 0x71, 0x61, 0x21, (byte) 0xA9,
		(byte) 0x86, (byte) 0x96, (byte) 0x83, 0x38, (byte) 0xCF, (byte) 0x9D, 0x5B, 0x6D,
		(byte) 0xDC, 0x15, (byte) 0xBA, 0x3E, 0x7D, (byte) 0x95, 0x3B, 0x2F
	};
	// Clone
	// Fudan Semiconductor FM17522 (0x88)
	public static final byte[] FM17522_firmware_reference = {
		0x00, (byte) 0xD6, 0x78, (byte) 0x8C, (byte) 0xE2, (byte) 0xAA, 0x0C, 0x18,
		0x2A, (byte) 0xB8, 0x7A, 0x7F, (byte) 0xD3, (byte) 0x6A, (byte) 0xCF, 0x0B,
		(byte) 0xB1, 0x37, 0x63, 0x4B, 0x69, (byte) 0xAE, (byte) 0x91, (byte) 0xC7,
		(byte) 0xC3, (byte) 0x97, (byte) 0xAE, 0x77, (byte) 0xF4, 0x37, (byte) 0xD7, (byte) 0x9B,
		0x7C, (byte) 0xF5, 0x3C, 0x11, (byte) 0x8F, 0x15, (byte) 0xC3, (byte) 0xD7,
		(byte) 0xC1, 0x5B, 0x00, 0x2A, (byte) 0xD0, 0x75, (byte) 0xDE, (byte) 0x9E,
		0x51, 0x64, (byte) 0xAB, 0x3E, (byte) 0xE9, 0x15, (byte) 0xB5, (byte) 0xAB,
		0x56, (byte) 0x9A, (byte) 0x98, (byte) 0x82, 0x26, (byte) 0xEA, 0x2A, 0x62
	};
	
	// The MIFARE Classic uses a 4 bit ACK/NAK. Any other value than 0xA is NAK.
	public static final byte MF_ACK = 0xA;
	// A Mifare Crypto1 key is 6 bytes.
	public static final byte MF_KEY_SIZE = 6;
	
	
	
	// AddicoreRFID error codes
	public static final byte MI_OK = 0;
	public static final byte MI_NOTAGERR = 1;
	public static final byte MI_ERR = 2;
	
	private static final int SPI_CLOCK_FREQUENCY = 1_000_000;
	
	private SpiDevice device;
	private DigitalOutputDevice resetPin;

	private boolean logReadsAndWrites = false;
	
	public MFRC522(int chipSelect, int resetGpio) {
		this(SPIConstants.DEFAULT_SPI_CONTROLLER, chipSelect, resetGpio);
	}
	
	@SuppressWarnings("resource")
	public MFRC522(int controller, int chipSelect, int resetGpio) {
		this(controller, chipSelect, new DigitalOutputDevice(resetGpio, true, false));
	}
	
	public MFRC522(int controller, int chipSelect, DigitalOutputDevice resetPin) {
		device = new SpiDevice(controller, chipSelect, SPI_CLOCK_FREQUENCY, SpiClockMode.MODE_0, false);
		this.resetPin = resetPin;
		
		init();
	}
	
	@Override
	public void close() {
		if (device != null) {
			device.close();
		}
	}
	
	public void setLogReadsAndWrites(boolean logReadsAndWrites) {
		this.logReadsAndWrites = logReadsAndWrites;
	}
	
	
	/////////////////////////////////////////////////////////////////////////////////////
	// Basic interface functions for communicating with the MFRC522
	/////////////////////////////////////////////////////////////////////////////////////
	
	private void writeRegister(PcdRegister register, PcdCommand command) {
		writeRegister(register, command.getValue());
	}
	
	private void writeRegister(PcdRegister register, byte value) {
		if (logReadsAndWrites) {
			Logger.debug("(0x{}, 0x{})", Integer.toHexString(register.getValue() & 0xff),
					Integer.toHexString(value & 0xff));
		}
		
		byte[] tx = new byte[2];
		// Address Format: 0XXXXXX0, the left most "0" indicates a write
		tx[0] = register.getAddress();
		tx[1] = value;
		
		device.write(tx);
	}
	
	private void writeRegister(PcdRegister register, byte[] values) {
		writeRegister(register, values, values.length);
	}
	
	private void writeRegister(PcdRegister register, byte[] values, int length) {
		if (logReadsAndWrites) {
			Logger.debug("(0x{}, 0x{}, {} bytes)", Integer.toHexString(register.getValue() & 0xff),
					Hex.encodeHexString(values), Integer.valueOf(length));
		}
		
		byte[] tx = new byte[length+1];
		// Address Format: 0XXXXXX0, the left most "0" indicates a write
		tx[0] = register.getAddress();
		System.arraycopy(values, 0, tx, 1, length);
		
		device.write(tx);
	}
	
	private byte readRegister(PcdRegister register) {
		byte[] tx = new byte[2];
		// Address Format: 1XXXXXX0, the first "1" indicates a read
		tx[0] = (byte) (register.getAddress() | 0x80);
		tx[1] = (byte) 0;
		
		byte[] rx = device.writeAndRead(tx);
		
		if (logReadsAndWrites) {
			Logger.debug("(0x{}): 0x{}", Integer.toHexString(register.getValue() & 0xff),
					Integer.toHexString(rx[1] & 0xff));
		}
		
		return rx[1];
	}
	
	private byte[] readRegister(PcdRegister register, int count, byte rxAlign) {
		if (count == 0) {
			return null;
		}
		
		// MSB == 1 is for reading. LSB is not used in address. Datasheet section 8.1.2.3.
		byte address = (byte) (register.getAddress() | 0x80);
		byte[] tx = new byte[count+1];
		int i;
		for (i=0; i<count; i++) {
			tx[i] = address;
		}
		tx[i] = (byte) 0;
		
		byte[] rx = device.writeAndRead(tx);
		
		byte[] values = new byte[count];
		
		// Index in values array.
		int index = 0;
		int length = count;
		i = 1;
		// One read is performed outside of the loop
		count--;
		// Only update bit positions rxAlign..7 in values[0]
		if (rxAlign != 0) {
			// Create bit mask for bit positions rxAlign..7
			byte mask = (byte) ((0xFF << rxAlign) & 0xFF);
			// Read value and tell that we want to read the same address again.
			byte value = rx[i++];
			// Apply mask to both current value of values[0] and the new data in value.
			values[0] = (byte) ((values[0] & ~mask) | (value & mask));
			index++;
		}
		while (index < count) {
			// Read value and tell that we want to read the same address again.
			values[index] = rx[i++];
			index++;
		}
		// Read the final byte. Send 0 to stop reading.
		values[index] = rx[i++];
		
		if (logReadsAndWrites) {
			Logger.debug("(0x{}, {} bytes, {}): 0x{}", Integer.toHexString(register.getValue() & 0xff),
					Integer.valueOf(length), Integer.valueOf(rxAlign), Hex.encodeHexString(values));
		}
	
		return values;
	}
	
	private void setBitMask(PcdRegister register, byte mask) {
		byte current = readRegister(register);
		//Logger.debug("Current: 0x" + Integer.toHexString(current&0xff) + ", mask: 0x" + Integer.toHexString(mask&0xff));
		// Already set?
		//if ((current & mask) != mask) {
			//Logger.debug("Setting bit mask 0x" + Integer.toHexString(mask&0xff));
			writeRegister(register, (byte) (current | mask));
		//}
	}
	
	private void clearBitMask(PcdRegister register, byte mask) {
		byte current = readRegister(register);
		//Logger.debug("Current: 0x" + Integer.toHexString(current&0xff) + ", mask: 0x" + Integer.toHexString(mask&0xff));
		// Already clear?
		//if ((current & mask) != 0) {
			//Logger.debug("Clearing bit mask 0x" + Integer.toHexString(mask&0xff));
			writeRegister(register, (byte) (current & ~mask));
		//}
	}
	
	private byte[] calculateCRC(byte[] data) {
		return calculateCRC(data, data.length);
	}
	
	private byte[] calculateCRC(byte[] data, int length) {
		writeRegister(PcdRegister.COMMAND_REG, PcdCommand.IDLE);		// Stop any active command.
		writeRegister(PcdRegister.DIV_IRQ_REG, (byte) 0x04);			// Clear the CRCIRq interrupt request bit
		writeRegister(PcdRegister.FIFO_LEVEL_REG, (byte) 0x80);			// FlushBuffer = 1, FIFO initialization
		writeRegister(PcdRegister.FIFO_DATA_REG, data, length);			// Write data to the FIFO
		writeRegister(PcdRegister.COMMAND_REG, PcdCommand.CALC_CRC);	// Start the calculation
		
		// Wait for the CRC calculation to complete (up to 100ms)
		long start_ms = System.currentTimeMillis();
		do {
			// DivIrqReg[7..0] bits are: Set2 reserved reserved MfinActIRq reserved CRCIRq reserved reserved
			byte n = readRegister(PcdRegister.DIV_IRQ_REG);
			if ((n & 0x04) != 0) {						// CRCIRq bit set - calculation done
				writeRegister(PcdRegister.COMMAND_REG, PcdCommand.IDLE);	// Stop calculating CRC for new content in the FIFO.
				// Transfer the result from the registers to the result buffer
				byte[] result = new byte[2];
				result[0] = readRegister(PcdRegister.CRC_RESULT_REG_LSB);
				result[1] = readRegister(PcdRegister.CRC_RESULT_REG_MSB);
				return result;
			}
		} while ((System.currentTimeMillis() - start_ms) < 100);

		// 100ms passed and nothing happend. Communication with the MFRC522 might be down.
		Logger.error("*** Timed out waiting for CalcCRC to complete");
		return null;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	// Functions for manipulating the MFRC522
	/////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Initializes the MFRC522 chip.
	 */
	public void init() {
		if (! resetPin.isOn()) {
			Logger.debug("reset pin was off");
			// Exit power down mode. This triggers a hard reset.
			resetPin.on();
			// Section 8.8.2 in the datasheet says the oscillator start-up time
			// is the start up time of the crystal + 37,74us. Let us be
			// generous: 50ms.
			SleepUtil.sleepMillis(50);
		} else {
			// Perform a soft reset if we haven't triggered a hard reset above.
			reset();
		}
		
		// Reset baud rates
		writeRegister(PcdRegister.TX_MODE_REG, (byte) 0x00);
		writeRegister(PcdRegister.RX_MODE_REG, (byte) 0x00);
		// Reset ModWidthReg
		writeRegister(PcdRegister.MOD_WIDTH_REG, (byte) 0x26);
		
		// OLD CODE - START
		// The following formula is used to calculate the timer frequency if the 
		// DEMOD_REG register?s TPrescalEven bit is set to logic 0:
		// fTimer = 13.56 MHz / (2*TPreScaler+1).
		// The following formula is used to calculate the timer frequency if the 
		// DEMOD_REG register?s TPrescalEven bit inDemoReg is set to logic 1:
		// fTimer = 13.56 MHz / (2*TPreScaler+2).
		
		// 110100111110 = 3390; 13_560_000 / 6781 -> fTimer = 1999
		// total time delay = ((TPrescaler * 2 + 1) * (TReloadVal + 1)) / 13.56 MHz
		// 203430 / 13560000 = 0.015 = 0.015
		
		// Timer: TPrescaler*TReloadVal/6.78MHz = 24ms
		/*
		writeRegister(T_MODE_REG, (byte) 0x8D);			// Tauto=1; f(Timer) = 6.78MHz/TPreScaler
		writeRegister(T_PRESCALER_REG, (byte) 0x3E);	// TModeReg[3..0] + TPrescalerReg
	
		// 30
		writeRegister(T_RELOAD_REG_MSB, (byte) 0);
		writeRegister(T_RELOAD_REG_LSB, (byte) 0x1E);
		
		writeRegister(TX_ASK_REG, (byte) 0x40);			// 100%ASK
		writeRegister(MODE_REG, (byte) 0x3D);			// CRC Initial value 0x6363	???
		*/
		
		// TPrescaler: 000010101001 = 169;  13_560_000 / 169  -> fTimer = 80236
		// TReload: 11111101000 = 2024
		// ((169 * 2 + 1) * (2024 + 1)) / 13.56 MHz = 0.050
		// joan:
		//self._PCDWrite(self._TModeReg,	  0x80)
		//self._PCDWrite(self._TPrescalerReg, 0xA9)
		//self._PCDWrite(self._TReloadRegH,   0x03)
		//self._PCDWrite(self._TReloadRegL,   0xe8)
	
		//self._PCDWrite(self._TxASKReg,	  0x40)
		//#self._PCDWrite(self._ModeReg,	   0x3D)
		//self._PCDWrite(self._ModeReg,	   0x29)
		// OLD CODE - END
	
		// When communicating with a PICC we need a timeout if something goes wrong.
		// f_timer = 13.56 MHz / (2*TPreScaler+1) where TPreScaler = [TPrescaler_Hi:TPrescaler_Lo].
		// TPrescaler_Hi are the four low bits in TModeReg. TPrescaler_Lo is TPrescalerReg.
		writeRegister(PcdRegister.T_MODE_REG, (byte) 0x80);			// TAuto=1; timer starts automatically at the end of the transmission in all communication modes at all speeds
		writeRegister(PcdRegister.T_PRESCALER_REG, (byte) 0xA9);	// TPreScaler = TModeReg[3..0]:TPrescalerReg, ie 0x0A9 = 169 => f_timer=40kHz, ie a timer period of 25us.
		writeRegister(PcdRegister.T_RELOAD_REG_MSB, (byte) 0x03);	// Reload timer with 0x03E8 = 1000, ie 25ms before timeout.
		writeRegister(PcdRegister.T_RELOAD_REG_LSB, (byte) 0xE8);
		
		writeRegister(PcdRegister.TX_ASK_REG, (byte) 0x40);			// Default 0x00. Force a 100 % ASK modulation independent of the ModGsPReg register setting
		writeRegister(PcdRegister.MODE_REG, (byte) 0x3D);			// Default 0x3F. Set the preset value for the CRC coprocessor for the CalcCRC command to 0x6363 (ISO 14443-3 part 6.2.4)
		
		setAntennaOn(true);								// Enable the antenna driver pins TX1 and TX2 (they were disabled by the reset)
	}

	/**
	 * Perform soft reset of AddicoreRFID Module
	 */
	private void reset() {
		writeRegister(PcdRegister.COMMAND_REG, PcdCommand.SOFT_RESET);
		// The datasheet does not mention how long the SoftRest command takes to complete.
		// But the MFRC522 might have been in soft power-down mode (triggered by bit 4 of CommandReg) 
		// Section 8.8.2 in the datasheet says the oscillator start-up time is the start up time of the crystal + 37,74us. Let us be generous: 50ms.
		SleepUtil.sleepMillis(50);
		while ((readRegister(PcdRegister.COMMAND_REG) & (1<<4)) != 0) {
			// PCD still restarting - unlikely after waiting 50ms, but better safe than sorry.
		}
	}

	/**
	 * Open antennas, each time you start or shut down the natural barrier
	 * between the transmitter should be at least 1ms interval
	 * 
	 * @param on
	 *            on/off value
	 */
	public void setAntennaOn(boolean on) {
		if (on) {
			byte value = readRegister(PcdRegister.TX_CONTROL_REG);
			if ((value & 0x03) != 0x03) {
				writeRegister(PcdRegister.TX_CONTROL_REG, (byte) (value | 0x03));
			}
		} else {
			clearBitMask(PcdRegister.TX_CONTROL_REG, (byte) 0x03);
		}
	}
	
	/**
	 * Get the current MFRC522 Receiver Gain (RxGain[2:0]) value.
	 * See 9.3.3.6 / table 98 in http://www.nxp.com/documents/data_sheet/MFRC522.pdf
	 * NOTE: Return value scrubbed with (0x07&lt;&lt;4)=01110000b as RCFfgReg may use reserved bits.
	 * 
	 * @return Value of the RxGain, scrubbed to the 3 bits used.
	 */
	public AntennaGain getAntennaGain() {
		return AntennaGain.forValue((byte) (readRegister(PcdRegister.RF_CONFIG_REG) & (0x07<<4)));
	}
	
	/**
	 * Set the MFRC522 Receiver Gain (RxGain) to value specified by given mask.
	 * See 9.3.3.6 / table 98 in http://www.nxp.com/documents/data_sheet/MFRC522.pdf
	 * NOTE: Given mask is scrubbed with (0x07&lt;&lt;4)=01110000b as RCFfgReg may use reserved bits.
	 * @param gain New antenna gain value
	 */
	public void setAntennaGain(AntennaGain gain) {
		if (getAntennaGain() != gain) {									// only bother if there is a change
			clearBitMask(PcdRegister.RF_CONFIG_REG, (byte) (0x07<<4));	// clear needed to allow 000 pattern
			setBitMask(PcdRegister.RF_CONFIG_REG, gain.getValue());		// only set RxGain[2:0] bits
		}
	}
	
	/**
	 * Performs a self-test of the MFRC522
	 * See 16.1.1 in http://www.nxp.com/documents/data_sheet/MFRC522.pdf
	 * 
	 * @return Whether or not the test passed. Or false if no firmware reference is available.
	 */
	public boolean performSelfTest() {
		Logger.debug("Self test - START");
		// This follows directly the steps outlined in 16.1.1
		// 1. Perform a soft reset.
		reset();
		
		// 2. Clear the internal buffer by writing 25 bytes of 00h
		byte[] ZEROES = new byte[25];
		writeRegister(PcdRegister.FIFO_LEVEL_REG, (byte) 0x80);	// flush the FIFO buffer
		writeRegister(PcdRegister.FIFO_DATA_REG, ZEROES);		// write 25 bytes of 00h to FIFO
		writeRegister(PcdRegister.COMMAND_REG, PcdCommand.MEM);		// transfer to internal buffer
		
		// 3. Enable self-test
		writeRegister(PcdRegister.AUTO_TEST_REG, (byte) 0x09);
		
		// 4. Write 00h to FIFO buffer
		writeRegister(PcdRegister.FIFO_DATA_REG, (byte) 0x00);
		
		// 5. Start self-test by issuing the CalcCRC command
		writeRegister(PcdRegister.COMMAND_REG, PcdCommand.CALC_CRC);
		
		// 6. Wait for self-test to complete
		byte n;
		for (int i=0; i<0xFF; i++) {
			// The datasheet does not specify exact completion condition except
			// that FIFO buffer should contain 64 bytes.
			// While selftest is initiated by CalcCRC command
			// it behaves differently from normal CRC computation,
			// so one can't reliably use DivIrqReg to check for completion.
			// It is reported that some devices does not trigger CRCIRq flag
			// during selftest.
			n = readRegister(PcdRegister.FIFO_LEVEL_REG);
			if (n >= 64) {
				break;
			}
		}
		writeRegister(PcdRegister.COMMAND_REG, PcdCommand.IDLE);		// Stop calculating CRC for new content in the FIFO.
		
		// 7. Read out resulting 64 bytes from the FIFO buffer.
		//byte[] result = readRegister(FIFO_DATA_REG, 64);
		byte[] result = new byte[64];
		for (int i=0; i<result.length; i++) {
			result[i] = readRegister(PcdRegister.FIFO_DATA_REG);
		}
		
		// Auto self-test done
		// Reset AutoTestReg register to be 0 again. Required for normal operation.
		writeRegister(PcdRegister.AUTO_TEST_REG, (byte) 0x00);
		
		// Determine firmware version (see section 9.3.4.8 in spec)
		int version = readRegister(PcdRegister.VERSION_REG) & 0xff;
		Logger.debug("version: 0x" + Integer.toHexString(version));
		
		// Pick the appropriate reference values
		byte[] reference;
		switch (version) {
		case 0x88:	// Fudan Semiconductor FM17522 clone
			reference = FM17522_firmware_reference;
			break;
		case 0x90:	// Version 0.0
			reference = MFRC522_firmware_referenceV0_0;
			break;
		case 0x91:	// Version 1.0
			reference = MFRC522_firmware_referenceV1_0;
			break;
		case 0x92:	// Version 2.0
			reference = MFRC522_firmware_referenceV2_0;
			break;
		default:	// Unknown version
			Logger.debug("Self test - END - FAIL");
			return false; // abort test
		}
		
		// Verify that the results match up to our expectations
		for (int i=0; i<64; i++) {
			if (result[i] != reference[i]) {
				Logger.debug("Self test - END - FAIL");
				return false;
			}
		}
		Logger.debug("Self test - END - PASS");
		
		// Test passed; all is good.
		return true;
	} // End PCD_PerformSelfTest()
	
	public int getVersion() {
		return readRegister(PcdRegister.VERSION_REG) & 0xff;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	// Functions for communicating with PICCs
	/////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Executes the Transceive command. CRC validation can only be done if
	 * backData and backLen are specified.
	 * 
	 * @param sendData
	 *            Pointer to the data to transfer to the FIFO.
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	private Response transceiveData(byte[] sendData) {
		return transceiveData(sendData, (byte) 0, (byte) 0, false);
	}

	/**
	 * Executes the Transceive command. CRC validation can only be done if
	 * backData and backLen are specified.
	 * 
	 * @param sendData
	 *            Pointer to the data to transfer to the FIFO.
	 * @param rxAlign
	 *            Defines the bit position in backData[0] for the first bit
	 *            received. Default 0.
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	private Response transceiveData(byte[] sendData, byte rxAlign) {
		return transceiveData(sendData, rxAlign, (byte) 0, false);
	}

	/**
	 * Executes the Transceive command. CRC validation can only be done if
	 * backData and backLen are specified.
	 * 
	 * @param sendData
	 *            Pointer to the data to transfer to the FIFO.
	 * @param validBits
	 *            The number of valid bits in the last byte. 0 for 8 valid bits
	 * @param rxAlign
	 *            Defines the bit position in backData[0] for the first bit
	 *            received. Default 0.
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	private Response transceiveData(byte[] sendData, byte validBits, byte rxAlign) {
		return transceiveData(sendData, validBits, rxAlign, false);
	}

	/**
	 * Executes the Transceive command. CRC validation can only be done if
	 * backData and backLen are specified.
	 * 
	 * @param sendData
	 *            Pointer to the data to transfer to the FIFO.
	 * @param validBits
	 *            The number of valid bits in the last byte. 0 for 8 valid bits
	 * @param rxAlign
	 *            Defines the bit position in backData[0] for the first bit
	 *            received. Default 0.
	 * @param checkCRC
	 *            True => The last two bytes of the response is assumed to be a
	 *            CRC_A that must be validated.
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	private Response transceiveData(byte[] sendData, byte validBits, byte rxAlign, boolean checkCRC) {
		byte waitIRq = 0x30; // RxIRq and IdleIRq
		return communicateWithPICC(PcdCommand.TRANSCEIVE, waitIRq, sendData, validBits, rxAlign, checkCRC);
	}

	private Response communicateWithPICC(PcdCommand command, byte waitIRq, byte[] sendData) {
		return communicateWithPICC(command, waitIRq, sendData, (byte) 0, (byte) 0, false);
	}

	private Response communicateWithPICC(PcdCommand command, byte waitIRq, byte[] sendData, byte validBits) {
		return communicateWithPICC(command, waitIRq, sendData, validBits, (byte) 0, false);
	}

	/**
	 * Transfers data to the MFRC522 FIFO, executes a command, waits for
	 * completion and transfers data back from the FIFO. CRC validation can only
	 * be done if backData and backLen are specified.
	 * 
	 * @param command
	 *            The command to execute. One of the PCD_Command enums.
	 * @param waitIRq
	 *            The bits in the ComIrqReg register that signals successful
	 *            completion of the command.
	 * @param sendData
	 *            Data to transfer to the FIFO.
	 * @param validBits
	 *            The number of valid bits in the last byte. 0 for 8 valid bits.
	 * @param rxAlign
	 *            Defines the bit position in backData[0] for the first bit
	 *            received. Default 0.
	 * @param checkCRC
	 *            True => The last two bytes of the response is assumed to be a
	 *            CRC_A that must be validated.
	 * @return response
	 */
	private Response communicateWithPICC(PcdCommand command, byte waitIRq, byte[] sendData, byte validBits,
			byte rxAlign, boolean checkCRC) {
		// Prepare values for BitFramingReg
		byte tx_last_bits = validBits;
		byte bit_framing = (byte) ((rxAlign << 4) + tx_last_bits);		// RxAlign = BitFramingReg[6..4]. TxLastBits = BitFramingReg[2..0]
		
		writeRegister(PcdRegister.COMMAND_REG, PcdCommand.IDLE);	// Stop any active command.
		writeRegister(PcdRegister.COM_IRQ_REG, (byte) 0x7f);		// Clear all seven interrupt request bits
		writeRegister(PcdRegister.FIFO_LEVEL_REG, (byte) 0x80);		// FlushBuffer=1, FIFO Initialisation
		writeRegister(PcdRegister.FIFO_DATA_REG, sendData);			// Write sendData to the FIFO
		writeRegister(PcdRegister.BIT_FRAMING_REG, bit_framing);	// Bit adjustments
		writeRegister(PcdRegister.COMMAND_REG, command);			// Execute the command
		if (command == PcdCommand.TRANSCEIVE) {
			setBitMask(PcdRegister.BIT_FRAMING_REG, (byte) 0x80);		// StartSend=1, transmission of data starts
		}
		
		// Wait for the command to complete.
		// In PCD_Init() we set the TAuto flag in TModeReg. This means the timer
		// automatically starts when the PCD stops transmitting.
		long start_ms = System.currentTimeMillis();
		boolean timeout = true;
		do {
			byte n = readRegister(PcdRegister.COM_IRQ_REG);
			if ((n & waitIRq) != 0) {
				// One of the interrupts that signal success has been set.
				timeout = false;
				break;
			}
			// Timer interrupt - nothing received in 25ms
			if ((n & 0x01) != 0) {
				Logger.debug("timer interrupt, n: 0x" + Integer.toHexString(n & 0xff));
				break;
			}
		} while ((System.currentTimeMillis() - start_ms) < 200);
		
		// 35.7ms and nothing happend. Communication with the MFRC522 might be down.
		if (timeout) {
			Logger.debug("Timed out waiting for interrupt");
			return new Response(StatusCode.TIMEOUT);
		}
		
		// StartSend=0
		//clearBitMask(PcdRegister.BIT_FRAMING_REG, (byte) 0x80);
	
		// Stop now if any errors except collisions were detected.
		// ErrorReg[7..0] bits are: WrErr TempErr reserved BufferOvfl CollErr CRCErr ParityErr ProtocolErr
		byte error_reg_val = readRegister(PcdRegister.ERROR_REG);
		// BufferOvfl ParityErr ProtocolErr
		if ((error_reg_val & 0x13) != 0) {
			Logger.error("*** Error reg val: 0x" + Integer.toHexString(error_reg_val & 0xff));
			return new Response(StatusCode.ERROR);
		}
		
		int back_len = 0;
		byte valid_bits = 0;
		
		// If the caller wants data back, get it from the MFRC522.
		byte[] back_data = new byte[0];
		// TODO Check this is equivalent
		//if (backData && backLen) {
		// FIXME Also TRANSCEIVE when called from haltA()
		if (command != PcdCommand.MF_AUTHENT) {
			// Number of bytes in the FIFO
			back_len = readRegister(PcdRegister.FIFO_LEVEL_REG) & 0xff;
			Logger.debug("FIFO Level: " + back_len);
			// Get received data from FIFO
			/*
			back_data = new byte[back_len];
			for (int index=0; index<back_len; index++) {
				back_data[index] = readRegister(FIFO_DATA_REG);
			}
			*/
			back_data = readRegister(PcdRegister.FIFO_DATA_REG, back_len, rxAlign);
			
			// RxLastBits[2:0] indicates the number of valid bits in the last received byte.
			// If this value is 000b, the whole byte is valid.
			valid_bits = (byte) (readRegister(PcdRegister.CONTROL_REG) & 0x07);
		}
		
		// Tell about collisions
		if ((error_reg_val & 0x08) != 0) {		// CollErr
			return new Response(StatusCode.COLLISION);
		}
		
		// Perform CRC_A validation if requested.
		//if (backData && backLen && checkCRC) {
		if (back_len > 0 && checkCRC) {
			Logger.debug("Checking CRC");
			// In this case a MIFARE Classic NAK is not OK.
			if (back_len == 1 && valid_bits == 4) {
				Logger.error("*** MIFARE Classic NAK is not ok");
				return new Response(StatusCode.MIFARE_NACK);
			}
			// We need at least the CRC_A value and all 8 bits of the last byte must be received.
			if (back_len < 2 || valid_bits != 0) {
				Logger.error("*** CRC was wrong");
				return new Response(StatusCode.CRC_WRONG);
			}
			// Verify CRC_A - do our own calculation and store the control in controlBuffer.
			byte[] control_buffer = calculateCRC(back_data, back_len-2);
			if (control_buffer == null) {
				Logger.error("*** Control buffer from PCD_CalculateCRC was null");
				return new Response(StatusCode.TIMEOUT);
			}
			
			if ((back_data[back_len-2] != control_buffer[0]) || (back_data[back_len-1] != control_buffer[1])) {
				Logger.error("*** CRC was wrong");
				return new Response(StatusCode.CRC_WRONG);
			}
		}
		Logger.debug("ok");
	
		return new Response(StatusCode.OK, back_data, back_len, valid_bits);
	}
	
	/**
	 * Transmits a REQuest command, Type A. Invites PICCs in state IDLE to go to READY and prepare for anticollision or selection. 7 bit frame.
	 * Beware: When two PICCs are in the field at the same time I often get STATUS_TIMEOUT - probably due do bad antenna design.
	 * 
	 * @param bufferATQA The buffer to store the ATQA (Answer to request) in
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	public StatusCode requestA(byte[] bufferATQA) {
		return requestAOrWakeUpA(PiccCommand.REQUEST_A, bufferATQA);
	}
	
	/**
	 * Transmits a Wake-UP command, Type A. Invites PICCs in state IDLE and HALT to go to READY(*) and prepare for anticollision or selection. 7 bit frame.
	 * Beware: When two PICCs are in the field at the same time I often get STATUS_TIMEOUT - probably due do bad antenna design.
	 * 
	 * @param bufferATQA The buffer to store the ATQA (Answer to request) in
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	public StatusCode wakeupA(byte[] bufferATQA) {
		return requestAOrWakeUpA(PiccCommand.WAKE_UP_A, bufferATQA);
	}
	
	/**
	 * Transmits REQA or WUPA commands.
	 * Beware: When two PICCs are in the field at the same time I often get STATUS_TIMEOUT - probably due do bad antenna design.
	 * 
	 * @param command The command to send - PICC_CMD_REQA or PICC_CMD_WUPA
	 * @param bufferATQA The buffer to store the ATQA (Answer to request) in
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */ 
	public StatusCode requestAOrWakeUpA(PiccCommand command, byte[] bufferATQA) {
		// The ATQA response is 2 bytes long.
		if (bufferATQA == null || bufferATQA.length != 2) {
			return StatusCode.NO_ROOM;
		}
		// ValuesAfterColl=1 => Bits received after collision are cleared.
		clearBitMask(PcdRegister.COLL_REG, (byte) 0x80);
		// For REQA and WUPA we need the short frame format - transmit only 7 bits of the last (and only) byte. TxLastBits = BitFramingReg[2..0]
		byte valid_bits = 7;
		Response response = transceiveData(new byte[] { command.getValue() }, valid_bits);
		valid_bits = response.getValidBits();
		if (response.getStatus() != StatusCode.OK) {
			return response.getStatus();
		}
		
		byte[] back_data = response.getBackData();
		int back_len = response.getBackLen();
		// ATQA must be exactly 16 bits.
		if (back_len != 2 || response.getValidBits() != 0) {
			return StatusCode.ERROR;
		}
		
		System.arraycopy(back_data, 0, bufferATQA, 0, back_len);
		
		return StatusCode.OK;
	}

	public UID select() {
		return select((byte) 0);
	}
	
	/**
	 * Transmits SELECT/ANTICOLLISION commands to select a single PICC.
	 * Before calling this function the PICCs must be placed in the READY(*) state by calling PICC_RequestA() or PICC_WakeupA().
	 * On success:
	 * 		- The chosen PICC is in state ACTIVE(*) and all other PICCs have returned to state IDLE/HALT. (Figure 7 of the ISO/IEC 14443-3 draft.)
	 * 		- The UID size and value of the chosen PICC is returned in *uid along with the SAK.
	 * 
	 * A PICC UID consists of 4, 7 or 10 bytes.
	 * Only 4 bytes can be specified in a SELECT command, so for the longer UIDs two or three iterations are used:
	 * 		UID size	Number of UID bytes		Cascade levels		Example of PICC
	 * 		========	===================		==============		===============
	 * 		single				 4						1				MIFARE Classic
	 * 		double				 7						2				MIFARE Ultralight
	 * 		triple				10						3				Not currently in use?
	 * 
	 * @param validBits The number of known UID bits supplied in *uid. Normally 0. If set you must also supply uid-&gt;size.
	 * @return UID object or null if there was an error.
	 */
	public UID select(byte validBits) {
		byte[] buffer = new byte[9];	// The SELECT/ANTICOLLISION commands uses a 7 byte standard frame + 2 bytes CRC_A
		// Description of buffer structure:
		//		Byte 0: SEL 				Indicates the Cascade Level: PICC_CMD_SEL_CL1, PICC_CMD_SEL_CL2 or PICC_CMD_SEL_CL3
		//		Byte 1: NVB					Number of Valid Bits (in complete command, not just the UID): High nibble: complete bytes, Low nibble: Extra bits. 
		//		Byte 2: UID-data or CT		See explanation below. CT means Cascade Tag.
		//		Byte 3: UID-data
		//		Byte 4: UID-data
		//		Byte 5: UID-data
		//		Byte 6: BCC					Block Check Character - XOR of bytes 2-5
		//		Byte 7: CRC_A
		//		Byte 8: CRC_A
		// The BCC and CRC_A are only transmitted if we know all the UID bits of the current Cascade Level.
		//
		// Description of bytes 2-5: (Section 6.5.4 of the ISO/IEC 14443-3 draft: UID contents and cascade levels)
		//		UID size	Cascade level	Byte2	Byte3	Byte4	Byte5
		//		========	=============	=====	=====	=====	=====
		//		 4 bytes		1			uid0	uid1	uid2	uid3
		//		 7 bytes		1			CT		uid0	uid1	uid2
		//						2			uid3	uid4	uid5	uid6
		//		10 bytes		1			CT		uid0	uid1	uid2
		//						2			CT		uid3	uid4	uid5
		//						3			uid6	uid7	uid8	uid9
		
		// Sanity checks
		if (validBits > 80) {
			Logger.error("*** Error: validBits ({}) was > 80", Byte.valueOf(validBits));
			return null;
		}
		
		// Prepare MFRC522
		clearBitMask(PcdRegister.COLL_REG, (byte) 0x80);		// ValuesAfterColl=1 => Bits received after collision are cleared.
		
		// Repeat Cascade Level loop until we have a complete UID.
		boolean uid_complete = false;
		boolean use_cascade_tag;
		byte cascade_level = 1;
		List<Byte> uid_bytes = new ArrayList<>();
		byte uid_sak = 0;
		byte uid_index;					// The first index in uid->uidByte[] that is used in the current Cascade Level.
		int current_level_known_bits;	// The number of known UID bits in the current Cascade Level.
		int response_buffer_offset;
		while (!uid_complete) {
			Logger.debug("uid_complete: " + uid_complete + ", cascade_level: " + cascade_level);
			// Set the Cascade Level in the SEL byte, find out if we need to use the Cascade Tag in byte 2.
			switch (cascade_level) {
			case 1:
				buffer[0] = PiccCommand.SEL_CL1.getValue();
				uid_index = 0;
				use_cascade_tag = (validBits != 0) && (uid_bytes.size() > 4);	// When we know that the UID has more than 4 bytes
				break;
			
			case 2:
				buffer[0] = PiccCommand.SEL_CL2.getValue();
				uid_index = 3;
				use_cascade_tag = (validBits != 0) && (uid_bytes.size() > 7);	// When we know that the UID has more than 7 bytes
				break;
			
			case 3:
				buffer[0] = PiccCommand.SEL_CL3.getValue();
				uid_index = 6;
				use_cascade_tag = false;						// Never used in CL3.
				break;
			
			default:
				Logger.error("*** Error: invalid cascade_level ()", Byte.valueOf(cascade_level));
				return null;
			}
			
			// How many UID bits are known in this Cascade Level?
			current_level_known_bits = validBits - (8 * uid_index);
			if (current_level_known_bits < 0) {
				current_level_known_bits = 0;
			}
			// Copy the known bits from uid->uidByte[] to buffer[]
			int index = 2; // destination index in buffer[]
			if (use_cascade_tag) {
				buffer[index++] = PiccCommand.CASCADE_TAG.getValue();
			}
			// The number of bytes needed to represent the known bits for this level.
			int bytesToCopy = current_level_known_bits / 8 + (((current_level_known_bits % 8) != 0) ? 1 : 0);
			if (bytesToCopy != 0) {
				// Max 4 bytes in each Cascade Level. Only 3 left if we use the Cascade Tag
				int maxBytes = use_cascade_tag ? 3 : 4;
				if (bytesToCopy > maxBytes) {
					bytesToCopy = maxBytes;
				}
				for (int count=0; count<bytesToCopy; count++) {
					buffer[index++] = uid_bytes.get(uid_index + count).byteValue();
				}
			}
			// Now that the data has been copied we need to include the 8 bits in CT in currentLevelKnownBits
			if (use_cascade_tag) {
				current_level_known_bits += 8;
			}
			
			// Repeat anti collision loop until we can transmit all UID bits + BCC and receive a SAK - max 32 iterations.
			boolean select_done = false;
			int buffer_used;					// The number of bytes used in the buffer, ie the number of bytes to transfer to the FIFO.
			int tx_last_bits = 0;				// Used in BitFramingReg. The number of valid bits in the last transmitted byte. 
			int response_length = 0;
			byte[] response_buffer = {};
			while (!select_done) {
				Logger.debug("select_done: " + select_done);
				// Find out how many bits and bytes to send and receive.
				if (current_level_known_bits >= 32) { // All UID bits in this Cascade Level are known. This is a SELECT.
					Logger.debug("SELECT: current_level_known_bits={}", Integer.valueOf(current_level_known_bits));
					buffer[1] = 0x70; // NVB - Number of Valid Bits: Seven whole bytes
					// Calculate BCC - Block Check Character
					buffer[6] = (byte) (buffer[2] ^ buffer[3] ^ buffer[4] ^ buffer[5]);
					// Calculate CRC_A
					//result = PCD_CalculateCRC(buffer, 7, &buffer[7]);
					byte[] crc = calculateCRC(buffer, 7);
					if (crc == null) {
						Logger.error("*** Error calculating CRC");
						return null;
					}
					// Note C++ code stores CRC in the last 2 bytes of buffer
					System.arraycopy(crc, 0, buffer, 7, crc.length);
					tx_last_bits = 0; // 0 => All 8 bits are valid.
					buffer_used = 9;
					// Store response in the last 3 bytes of buffer (BCC and CRC_A - not needed after tx)
					//responseBuffer = &buffer[6];
					//responseLength = 3;
					response_buffer_offset = 6;
					response_length = crc.length + 1;
				} else { // This is an ANTICOLLISION.
					Logger.debug("ANTICOLLISION: current_level_known_bits={}", Integer.valueOf(current_level_known_bits));
					tx_last_bits	= current_level_known_bits % 8;
					int count		= current_level_known_bits / 8;			// Number of whole bytes in the UID part.
					index			= 2 + count;							// Number of whole bytes: SEL + NVB + UIDs
					buffer[1]		= (byte) ((index << 4) + tx_last_bits);	// NVB - Number of Valid Bits
					buffer_used		= index + ((tx_last_bits != 0) ? 1 : 0);
					// Store response in the unused part of buffer
					//responseBuffer = &buffer[index];
					//responseLength = sizeof(buffer) - index;
					response_buffer_offset = index;
					response_length	= buffer.length - index;
				}
				
				// Set bit adjustments
				// Having a separate variable is overkill. But it makes the next line easier to read.
				int rx_align = tx_last_bits;
				// RxAlign = BitFramingReg[6..4]. TxLastBits = BitFramingReg[2..0]
				writeRegister(PcdRegister.BIT_FRAMING_REG, (byte) ((rx_align << 4) + tx_last_bits));
				
				Logger.debug("tx_last_bits: " + tx_last_bits);
				
				// Transmit the buffer and receive the response.
				byte[] tx_data = new byte[buffer_used];
				System.arraycopy(buffer, 0, tx_data, 0, buffer_used);
				Response response = transceiveData(tx_data, (byte) tx_last_bits, (byte) rx_align);
				//result = PCD_TransceiveData(buffer, bufferUsed, responseBuffer, &responseLength, &txLastBits, rxAlign);
				if (response.getBackData() == null) {
					Logger.error("*** got no back data from transcieveData, aborting");
					return null;
				}
				System.arraycopy(response.getBackData(), 0, buffer, response_buffer_offset, response.getBackLen());
				response_buffer = response.getBackData();
				response_length = response.getBackLen();
				tx_last_bits = response.getValidBits();
				if (response.getStatus() == StatusCode.COLLISION) { // More than one PICC in the field => collision.
					byte valueOfCollReg = readRegister(PcdRegister.COLL_REG); // CollReg[7..0] bits are: ValuesAfterColl reserved CollPosNotValid CollPos[4:0]
					if ((valueOfCollReg & 0x20) != 0) { // CollPosNotValid
						Logger.error("*** valueOfCollReg ({}) has bit 0x20 set", Byte.valueOf(valueOfCollReg));
						return null; // Without a valid collision position we cannot continue
					}
					int collisionPos = valueOfCollReg & 0x1F; // Values 0-31, 0 means bit 32.
					if (collisionPos == 0) {
						collisionPos = 32;
					}
					if (collisionPos <= current_level_known_bits) { // No progress - should not happen 
						Logger.error("*** collisionPos ({}) is <= current_level_known_bits ({})",
								Integer.valueOf(collisionPos), Integer.valueOf(current_level_known_bits));
						//return StatusCode.INTERNAL_ERROR;
						return null;
					}
					// Choose the PICC with the bit set.
					current_level_known_bits = collisionPos;
					int count = (current_level_known_bits - 1) % 8; // The bit to modify
					index = 1 + (current_level_known_bits / 8) + (count != 0 ? 1 : 0); // First byte is index 0.
					buffer[index] |= (1 << count);
				} else if (response.getStatus() != StatusCode.OK) {
					Logger.error("*** Invalid response from PCD_TransceiveData: {}", response.getStatus());
					//return response.getStatus();
					return null;
				} else { // STATUS_OK
					if (current_level_known_bits >= 32) { // This was a SELECT.
						select_done = true; // No more anticollision 
						// We continue below outside the while.
					} else { // This was an ANTICOLLISION.
						// We now have all 32 bits of the UID in this Cascade Level
						current_level_known_bits = 32;
						// Run loop again to do the SELECT.
					}
				}
			} // End of while (!selectDone)
			
			// We do not check the CBB - it was constructed by us above.
			
			// Copy the found UID bytes from buffer[] to uid->uidByte[]
			index			= (buffer[2] == PiccCommand.CASCADE_TAG.getValue()) ? 3 : 2; // source index in buffer[]
			bytesToCopy		= (buffer[2] == PiccCommand.CASCADE_TAG.getValue()) ? 3 : 4;
			for (int count=0; count<bytesToCopy; count++) {
				uid_bytes.add(Byte.valueOf(buffer[index++]));
				if (uid_bytes.size() != uid_index + count + 1) {
					Logger.error("*** Error, expected uid_bytes size to be " + (uid_index + count + 1) + ", but was " + uid_bytes.size());
				}
			}
			
			// Check response SAK (Select Acknowledge)
			if (response_length != 3 || tx_last_bits != 0) { // SAK must be exactly 24 bits (1 byte + CRC_A).
				Logger.error("*** SAK must be exactly 24 bits (1 byte + CRC_A), response_length={}", Integer.valueOf(response_length));
				//return StatusCode.ERROR;
				return null;
			}
			// Verify CRC_A - do our own calculation
			byte[] crc = calculateCRC(response_buffer, 1);
			if (crc == null) {
				Logger.error("*** Error in PCD_CalculateCRC");
				//return StatusCode.CRC_WRONG;
				return null;
			}
			if ((crc[0] != response_buffer[1]) || (crc[1] != response_buffer[2])) {
				Logger.error("*** CRC was wrong");
				//return StatusCode.CRC_WRONG;
				return null;
			}
			if ((response_buffer[0] & 0x04) != 0) { // Cascade bit set - UID not complete yes
				cascade_level++;
			} else {
				uid_complete = true;
				uid_sak = response_buffer[0];
			}
		} // End of while (!uidComplete)
		UID uid = new UID(uid_bytes, uid_sak);
		Logger.debug("End of while (!uidComplete) loop, uid: " + uid);
		
		// Set correct uid->size
		//uid.setSize(3 * cascade_level + 1);
		if (uid.getSize() != (3 * cascade_level + 1)) {
			Logger.error("*** Expected UID size to be " + (3 * cascade_level + 1) + " but was " + uid.getSize());
		}
		
		return uid;
	}

	/**
	 * Instructs a PICC in state ACTIVE(*) to go to state HALT.
	 *
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */ 
	public StatusCode haltA() {
		byte[] buffer = new byte[4];
		
		// Build command buffer
		buffer[0] = PiccCommand.HALT_A.getValue();
		buffer[1] = 0;
		// Calculate CRC_A
		byte[] crc = calculateCRC(buffer);
		if (crc == null) {
			return StatusCode.TIMEOUT;
		}
		System.arraycopy(crc, 0, buffer, 2, crc.length);
		
		// Send the command.
		// The standard says:
		//		If the PICC responds with any modulation during a period of 1 ms after the end of the frame containing the
		//		HLTA command, this response shall be interpreted as 'not acknowledge'.
		// We interpret that this way: Only STATUS_TIMEOUT is a success.
		Response response = transceiveData(buffer);
		StatusCode result = response.getStatus();
		if (result == StatusCode.TIMEOUT) {
			return StatusCode.OK;
		}
		if (result == StatusCode.OK) { // That is ironically NOT ok in this case ;-)
			return StatusCode.ERROR;
		}
		return result;
	}

	/**
	 * Executes the MFRC522 MFAuthent command.
	 * This command manages MIFARE authentication to enable a secure communication to any MIFARE Mini, MIFARE 1K and MIFARE 4K card.
	 * The authentication is described in the MFRC522 datasheet section 10.3.1.9 and http://www.nxp.com/documents/data_sheet/MF1S503x.pdf section 10.1.
	 * For use with MIFARE Classic PICCs.
	 * The PICC must be selected - ie in state ACTIVE(*) - before calling this function.
	 * Remember to call PCD_StopCrypto1() after communicating with the authenticated PICC - otherwise no new communications can start.
	 * 
	 * All keys are set to FFFFFFFFFFFFh at chip delivery.
	 * 
	 * @param authKeyA PICC_CMD_MF_AUTH_KEY_A or PICC_CMD_MF_AUTH_KEY_B
	 * @param blockAddr The block number. See numbering in the comments in the .h file.
	 * @param key Crypto1 key to use (6 bytes)
	 * @param uid Pointer to Uid struct. The first 4 bytes of the UID is used.
	 * @return STATUS_OK on success, STATUS_??? otherwise. Probably STATUS_TIMEOUT if you supply the wrong key.
	 */
	public StatusCode authenticate(boolean authKeyA, byte blockAddr, byte[] key, UID uid) {
		Logger.debug("blockAddr: " + blockAddr);
		
		// Build command buffer
		byte[] sendData = new byte[12];
		sendData[0] = authKeyA ? PiccCommand.MF_AUTH_KEY_A.getValue() : PiccCommand.MF_AUTH_KEY_B.getValue();
		sendData[1] = blockAddr;
		System.arraycopy(key, 0, sendData, 2, key.length);
		/*
		for (byte i=0; i<key.length; i++) {	// 6 key bytes
			sendData[2+i] = key[i];
		}
		*/
		// Use the last uid bytes as specified in http://cache.nxp.com/documents/application_note/AN10927.pdf
		// section 3.2.5 "MIFARE Classic Authentication".
		// The only missed case is the MF1Sxxxx shortcut activation,
		// but it requires cascade tag (CT) byte, that is not part of uid.
		System.arraycopy(uid.getUidBytes(), uid.getUidBytes().length-4, sendData, 8, 4);
		/*
		for (byte i=0; i<4; i++) {				// The last 4 bytes of the UID
			sendData[8+i] = uid.getUidByte(i + uid.getSize() - 4);
		}
		*/
		
		byte waitIRq = 0x10;		// IdleIRq
		// Start the authentication.
		return communicateWithPICC(PcdCommand.MF_AUTHENT, waitIRq, sendData).getStatus();
		//return PCD_CommunicateWithPICC(PCD_MFAuthent, waitIRq, &sendData[0], sizeof(sendData));
	} // End PCD_Authenticate()

	/**
	 * Used to exit the PCD from its authenticated state.
	 * Remember to call this function after communicating with an authenticated PICC - otherwise no new communications can start.
	 */
	public void stopCrypto1() {
		// Clear MFCrypto1On bit
		clearBitMask(PcdRegister.STATUS2_REG, (byte) 0x08); // Status2Reg[7..0] bits are: TempSensClear I2CForceHS reserved reserved MFCrypto1On ModemState[2:0]
	} // End PCD_StopCrypto1()
	
	/**
	 * Reads 16 bytes (+ 2 bytes CRC_A) from the active PICC.
	 * 
	 * For MIFARE Classic the sector containing the block must be authenticated before calling this function.
	 * 
	 * For MIFARE Ultralight only addresses 00h to 0Fh are decoded.
	 * The MF0ICU1 returns a NAK for higher addresses.
	 * The MF0ICU1 responds to the READ command by sending 16 bytes starting from the page address defined by the command argument.
	 * For example; if blockAddr is 03h then pages 03h, 04h, 05h, 06h are returned.
	 * A roll-back is implemented: If blockAddr is 0Eh, then the contents of pages 0Eh, 0Fh, 00h and 01h are returned.
	 * 
	 * The buffer must be at least 18 bytes because a CRC_A is also returned.
	 * Checks the CRC_A before returning STATUS_OK.
	 * 
	 * @param blockAddr MIFARE Classic: The block (0-0xff) number. MIFARE Ultralight: The first page to return data from.
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	public byte[] mifareRead(byte blockAddr) {
		Logger.debug("blockAddr: " + blockAddr);
		// Build command buffer
		byte[] buffer = { PiccCommand.MF_READ.getValue(), blockAddr };
		// Calculate CRC_A
		byte[] crc = calculateCRC(buffer);
		if (crc == null) {
			return null;
		}
		byte[] tx_buffer = new byte[4];
		System.arraycopy(buffer, 0, tx_buffer, 0, buffer.length);
		System.arraycopy(crc, 0, tx_buffer, 2, crc.length);
		
		// Transmit the buffer and receive the response, validate CRC_A.
		Response response = transceiveData(tx_buffer, (byte) 0, (byte) 0, true);
		//return PCD_TransceiveData(buffer, 4, buffer, bufferSize, nullptr, 0, true);
		
		if (response.getStatus() != StatusCode.OK) {
			return null;
		}
		
		return response.getBackData();
	} // End MIFARE_Read()

	/**
	 * Writes 16 bytes to the active PICC.
	 * 
	 * For MIFARE Classic the sector containing the block must be authenticated before calling this function.
	 * 
	 * For MIFARE Ultralight the operation is called "COMPATIBILITY WRITE".
	 * Even though 16 bytes are transferred to the Ultralight PICC, only the least significant 4 bytes (bytes 0 to 3)
	 * are written to the specified address. It is recommended to set the remaining bytes 04h to 0Fh to all logic 0.
	 * 
	 * @param blockAddr MIFARE Classic: The block (0-0xff) number. MIFARE Ultralight: The page (2-15) to write to.
	 * @param buffer The 16 bytes to write to the PICC
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	public StatusCode mifareWrite(byte blockAddr, byte[] buffer) {
		// Sanity check
		//if (buffer == null || buffer.length != 16) {
		//	return StatusCode.INVALID;
		//}
		
		// Mifare Classic protocol requires two communications to perform a write.
		// Step 1: Tell the PICC we want to write to block blockAddr.
		byte[] cmdBuffer = { PiccCommand.MF_WRITE.getValue(), blockAddr };
		StatusCode result = mifareTransceive(cmdBuffer); // Adds CRC_A and checks that the response is MF_ACK.
		if (result != StatusCode.OK) {
			return result;
		}
		
		// Step 2: Transfer the data
		return mifareTransceive(buffer); // Adds CRC_A and checks that the response is MF_ACK.
	} // End MIFARE_Write()

	/**
	 * Writes a 4 byte page to the active MIFARE Ultralight PICC.
	 * 
	 * @param page The page (2-15) to write to.
	 * @param buffer The 4 bytes to write to the PICC
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	public StatusCode mifareUltralightWrite(byte page, byte[] buffer) {
		// Sanity check
		if (buffer == null || buffer.length != 4) {
			return StatusCode.INVALID;
		}
		
		// Build commmand buffer
		byte[] cmdBuffer = new byte[6];
		cmdBuffer[0] = PiccCommand.UL_WRITE.getValue();
		cmdBuffer[1] = page;
		System.arraycopy(buffer, 0, cmdBuffer, 2, 4);
		//memcpy(&cmdBuffer[2], buffer, 4);
		
		// Perform the write
		return mifareTransceive(cmdBuffer); // Adds CRC_A and checks that the response is MF_ACK.
	} // End MIFARE_Ultralight_Write()

	/**
	 * MIFARE Decrement subtracts the delta from the value of the addressed block, and stores the result in a volatile memory.
	 * For MIFARE Classic only. The sector containing the block must be authenticated before calling this function.
	 * Only for blocks in "value block" mode, ie with access bits [C1 C2 C3] = [110] or [001].
	 * Use MIFARE_Transfer() to store the result in a block.
	 * 
	 * @param blockAddr The block (0-0xff) number.
	 * @param delta This number is subtracted from the value of block blockAddr.
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	public StatusCode mifareDecrement(byte blockAddr, int delta) {
		return mifareTwoStepHelper(PiccCommand.MF_DECREMENT, blockAddr, delta);
	} // End MIFARE_Decrement()

	/**
	 * MIFARE Increment adds the delta to the value of the addressed block, and stores the result in a volatile memory.
	 * For MIFARE Classic only. The sector containing the block must be authenticated before calling this function.
	 * Only for blocks in "value block" mode, ie with access bits [C1 C2 C3] = [110] or [001].
	 * Use MIFARE_Transfer() to store the result in a block.
	 * 
	 * @param blockAddr The block (0-0xff) number.
	 * @param delta This number is added to the value of block blockAddr.
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	public StatusCode mifareIncrement(byte blockAddr, int delta) {
		return mifareTwoStepHelper(PiccCommand.MF_INCREMENT, blockAddr, delta);
	} // End MIFARE_Increment()

	/**
	 * MIFARE Restore copies the value of the addressed block into a volatile memory.
	 * For MIFARE Classic only. The sector containing the block must be authenticated before calling this function.
	 * Only for blocks in "value block" mode, ie with access bits [C1 C2 C3] = [110] or [001].
	 * Use MIFARE_Transfer() to store the result in a block.
	 * 
	 * @param blockAddr The block (0-0xff) number.
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	public StatusCode mifareRestore(byte blockAddr) {
		// The datasheet describes Restore as a two step operation, but does not explain what data to transfer in step 2.
		// Doing only a single step does not work, so I chose to transfer 0L in step two.
		return mifareTwoStepHelper(PiccCommand.MF_RESTORE, blockAddr, 0);
	} // End MIFARE_Restore()

	/**
	 * Helper function for the two-step MIFARE Classic protocol operations Decrement, Increment and Restore.
	 * 
	 * @param command The command to use
	 * @param blockAddr The block (0-0xff) number.
	 * @param data The data to transfer in step 2
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	public StatusCode mifareTwoStepHelper(PiccCommand command, byte blockAddr, int data) {
		// Step 1: Tell the PICC the command and block address
		byte[] cmdBuffer = { command.getValue(), blockAddr };
		StatusCode result = mifareTransceive(cmdBuffer); // Adds CRC_A and checks that the response is MF_ACK.
		if (result != StatusCode.OK) {
			return result;
		}
		
		// Step 2: Transfer the data
		
		byte[] buffer = new byte[4];
		// Translate the int32_t into 4 bytes; repeated 2x in value block
		buffer[0] = (byte) (data & 0xFF);
		buffer[1] = (byte) ((data & 0xFF00) >> 8);
		buffer[2] = (byte) ((data & 0xFF0000) >> 16);
		buffer[3] = (byte) ((data & 0xFF000000) >> 24);
		
		return mifareTransceive(buffer, true); // Adds CRC_A and accept timeout as success.
	} // End MIFARE_TwoStepHelper()

	/**
	 * MIFARE Transfer writes the value stored in the volatile memory into one MIFARE Classic block.
	 * For MIFARE Classic only. The sector containing the block must be authenticated before calling this function.
	 * Only for blocks in "value block" mode, ie with access bits [C1 C2 C3] = [110] or [001].
	 * 
	 * @param blockAddr The block (0-0xff) number.
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	public StatusCode mifareTransfer(byte blockAddr) {
		// Tell the PICC we want to transfer the result into block blockAddr.
		byte[] cmdBuffer = { PiccCommand.MF_TRANSFER.getValue(), blockAddr };
		return mifareTransceive(cmdBuffer); // Adds CRC_A and checks that the response is MF_ACK.
	} // End MIFARE_Transfer()

	/**
	 * Helper routine to read the current value from a Value Block.
	 * 
	 * Only for MIFARE Classic and only for blocks in "value block" mode, that
	 * is: with access bits [C1 C2 C3] = [110] or [001]. The sector containing
	 * the block must be authenticated before calling this function. 
	 * 
	 * @param blockAddr The block (0x00-0xff) number.
	 * @return Integer value or null if error.
	 */
	public Integer mifareGetValue(byte blockAddr) {
		// Read the block
		byte[] buffer = mifareRead(blockAddr);
		if (buffer == null) {
			return null;
		}
		
		// Extract the value
		return Integer.valueOf(((buffer[3] & 0xff)<<24) | ((buffer[2] & 0xff)<<16) | ((buffer[1] & 0xff)<<8) | (buffer[0] & 0xff));
	} // End MIFARE_GetValue()

	/**
	 * Helper routine to write a specific value into a Value Block.
	 * 
	 * Only for MIFARE Classic and only for blocks in "value block" mode, that
	 * is: with access bits [C1 C2 C3] = [110] or [001]. The sector containing
	 * the block must be authenticated before calling this function. 
	 * 
	 * @param blockAddr The block (0x00-0xff) number.
	 * @param value New value of the Value Block.
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	public StatusCode mifareSetValue(byte blockAddr, int value) {
		byte[] buffer = new byte[16];
		
		// Translate the int32_t into 4 bytes; repeated 2x in value block
		buffer[0] = buffer[ 8] = (byte) (value & 0xFF);
		buffer[1] = buffer[ 9] = (byte) ((value & 0xFF00) >> 8);
		buffer[2] = buffer[10] = (byte) ((value & 0xFF0000) >> 16);
		buffer[3] = buffer[11] = (byte) ((value & 0xFF000000) >> 24);
		// Inverse 4 bytes also found in value block
		buffer[4] = (byte) ~buffer[0];
		buffer[5] = (byte) ~buffer[1];
		buffer[6] = (byte) ~buffer[2];
		buffer[7] = (byte) ~buffer[3];
		// Address 2x with inverse address 2x
		buffer[12] = buffer[14] = blockAddr;
		buffer[13] = buffer[15] = (byte) ~blockAddr;
		
		// Write the whole data block
		return mifareWrite(blockAddr, buffer);
	} // End MIFARE_SetValue()

	/*
	 * Authenticate with a NTAG216.
	 * 
	 * Only for NTAG216. First implemented by Gargantuanman.
	 * 
	 * @param passWord   password.
	 * @param pACK       result success???.
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	/*
	public StatusCode ntag216Auth(byte[] passWord, byte pACK[]) //Authenticate with 32bit password
	{
		// TODO: Fix cmdBuffer length and rxlength. They really should match.
		//       (Better still, rxlength should not even be necessary.)

		StatusCode result;
		byte				cmdBuffer[18]; // We need room for 16 bytes data and 2 bytes CRC_A.
		
		cmdBuffer[0] = 0x1B; //Comando de autentificacion
		
		for (byte i = 0; i<4; i++)
			cmdBuffer[i+1] = passWord[i];
		
		result = PCD_CalculateCRC(cmdBuffer, 5, &cmdBuffer[5]);
		
		if (result!=STATUS_OK) {
			return result;
		}
		
		// Transceive the data, store the reply in cmdBuffer[]
		byte waitIRq		= 0x30;	// RxIRq and IdleIRq
//		byte cmdBufferSize	= sizeof(cmdBuffer);
		byte validBits		= 0;
		byte rxlength		= 5;
		result = PCD_CommunicateWithPICC(PCD_Transceive, waitIRq, cmdBuffer, 7, cmdBuffer, &rxlength, &validBits);
		
		pACK[0] = cmdBuffer[0];
		pACK[1] = cmdBuffer[1];
		
		if (result!=STATUS_OK) {
			return result;
		}
		
		return STATUS_OK;
	} // End PCD_NTAG216_AUTH()
	*/


	/////////////////////////////////////////////////////////////////////////////////////
	// Support functions
	/////////////////////////////////////////////////////////////////////////////////////

	public StatusCode mifareTransceive(byte[] sendData) {
		return mifareTransceive(sendData, false);
	}
	
	/**
	 * Wrapper for MIFARE protocol communication.
	 * Adds CRC_A, executes the Transceive command and checks that the response is MF_ACK or a timeout.
	 * 
	 * @param sendData Data to transfer to the FIFO. Do NOT include the CRC_A.
	 * @param acceptTimeout	A timeout is also success
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	public StatusCode mifareTransceive(byte[] sendData, boolean acceptTimeout) {
		// Sanity check
		if (sendData == null || sendData.length != 16) {
			return StatusCode.INVALID;
		}
		
		/* C++ Code:
		byte cmdBuffer[18]; // We need room for 16 bytes data and 2 bytes CRC_A.
		// Copy sendData[] to cmdBuffer[] and add CRC_A
		memcpy(cmdBuffer, sendData, sendLen);
		result = PCD_CalculateCRC(cmdBuffer, sendLen, &cmdBuffer[sendLen]);
		if (result != STATUS_OK) { 
			return result;
		}
		sendLen += 2;
		*/
		byte[] crc = calculateCRC(sendData);
		if (crc == null) { 
			return StatusCode.TIMEOUT;
		}
		
		// Transceive the data
		byte waitIRq = 0x30;		// RxIRq and IdleIRq
		byte validBits = 0;
		byte[] cmdBuffer = new byte[sendData.length+2];
		System.arraycopy(sendData, 0, cmdBuffer, 0, sendData.length);
		System.arraycopy(crc, 0, cmdBuffer, sendData.length, crc.length);
		//result = PCD_CommunicateWithPICC(PCD_Transceive, waitIRq, cmdBuffer, sendLen, cmdBuffer, &cmdBufferSize, &validBits);
		Response result = communicateWithPICC(PcdCommand.TRANSCEIVE, waitIRq, cmdBuffer, validBits);
		validBits = result.getValidBits();
		if (acceptTimeout && result.getStatus() == StatusCode.TIMEOUT) {
			return StatusCode.OK;
		}
		if (result.getStatus() != StatusCode.OK) {
			return result.getStatus();
		}
		// The PICC must reply with a 4 bit ACK
		if (result.getBackData().length != 1 || validBits != 4) {
			return StatusCode.ERROR;
		}
		if (result.getBackData()[0] != MF_ACK) {
			return StatusCode.MIFARE_NACK;
		}
		return StatusCode.OK;
	} // End PCD_MIFARE_Transceive()

	/**
	 * Translates the SAK (Select Acknowledge) to a PICC type.
	 * 
	 * @param sak The SAK byte returned from PICC_Select().
	 * @return PICC_Type
	 */
	public static PiccType getPiccType(byte sak) {
		// http://www.nxp.com/documents/application_note/AN10833.pdf 
		// 3.2 Coding of Select Acknowledge (SAK)
		// ignore 8-bit (iso14443 starts with LSBit = bit 1)
		// fixes wrong type for manufacturer Infineon (http://nfc-tools.org/index.php?title=ISO14443A)
		return PiccType.forId(sak &= 0x7F);
	} // End PICC_GetType()

	/**
	 * Calculates the bit pattern needed for the specified access bits. In the [C1 C2 C3] tuples C1 is MSB (=4) and C3 is LSB (=1).
	 */
	/*
	public void mifareSetAccessBits(	byte *accessBitBuffer,	///< Pointer to byte 6, 7 and 8 in the sector trailer. Bytes [0..2] will be set.
										byte g0,				///< Access bits [C1 C2 C3] for block 0 (for sectors 0-31) or blocks 0-4 (for sectors 32-39)
										byte g1,				///< Access bits C1 C2 C3] for block 1 (for sectors 0-31) or blocks 5-9 (for sectors 32-39)
										byte g2,				///< Access bits C1 C2 C3] for block 2 (for sectors 0-31) or blocks 10-14 (for sectors 32-39)
										byte g3					///< Access bits C1 C2 C3] for the sector trailer, block 3 (for sectors 0-31) or block 15 (for sectors 32-39)
									) {
		byte c1 = ((g3 & 4) << 1) | ((g2 & 4) << 0) | ((g1 & 4) >> 1) | ((g0 & 4) >> 2);
		byte c2 = ((g3 & 2) << 2) | ((g2 & 2) << 1) | ((g1 & 2) << 0) | ((g0 & 2) >> 1);
		byte c3 = ((g3 & 1) << 3) | ((g2 & 1) << 2) | ((g1 & 1) << 1) | ((g0 & 1) << 0);
		
		accessBitBuffer[0] = (~c2 & 0xF) << 4 | (~c1 & 0xF);
		accessBitBuffer[1] =          c1 << 4 | (~c3 & 0xF);
		accessBitBuffer[2] =          c3 << 4 | c2;
	} // End MIFARE_SetAccessBits()
	*/
	
	/**
	 * Performs the "magic sequence" needed to get Chinese UID changeable
	 * Mifare cards to allow writing to sector 0, where the card UID is stored.
	 *
	 * Note that you do not need to have selected the card through REQA or WUPA,
	 * this sequence works immediately when the card is in the reader vicinity.
	 * This means you can use this method even on "bricked" cards that your reader does
	 * not recognise anymore (see MFRC522::MIFARE_UnbrickUidSector).
	 * 
	 * Of course with non-bricked devices, you're free to select them before calling this function.
	 * @return Status
	 */
	public boolean mifareOpenUidBackdoor() {
		// Magic sequence:
		// > 50 00 57 CD (HALT + CRC)
		// > 40 (7 bits only)
		// < A (4 bits only)
		// > 43
		// < A (4 bits only)
		// Then you can write to sector 0 without authenticating
		
		haltA(); // 50 00 57 CD
		
		byte[] cmd = { 0x40 };
		byte validBits = 7; /* Our command is only 7 bits. After receiving card response,
							  this will contain amount of valid response bits. */
		Response resp = transceiveData(cmd, validBits, (byte) 0, false); // 40
		byte[] response = resp.getBackData();
		int received = resp.getBackLen();
		validBits = resp.getValidBits();
		if (resp.getStatus() != StatusCode.OK) {
			Logger.error(
					"Card did not respond to 0x40 after HALT command. Are you sure it is a UID changeable one? Error: {}",
					resp.getStatus());
			return false;
		}
		if (received != 1 || response[0] != 0x0A) {
			Logger.error("Got bad response on backdoor 0x40 command: 0x{} ({} valid bits)",
					Integer.toHexString(response[0]), Integer.valueOf(validBits));
			return false;
		}
		
		cmd = new byte[] { 0x43 };
		validBits = 8;
		resp = transceiveData(cmd, validBits, (byte) 0, false); // 43
		response = resp.getBackData();
		received = resp.getBackLen();
		validBits = resp.getValidBits();
		if (resp.getStatus() != StatusCode.OK) {
				Logger.error("Error in communication at command 0x43, after successfully executing 0x40. Error: {}",
						resp.getStatus());
			return false;
		}
		if (received != 1 || response[0] != 0x0A) {
			Logger.error("Got bad response on backdoor 0x43 command: 0x{} ({} valid bits)",
					Integer.toHexString(response[0]), Integer.valueOf(validBits));
			return false;
		}
		
		// You can now write to sector 0 without authenticating!
		return true;
	} // End MIFARE_OpenUidBackdoor()
	
	/**
	 * Reads entire block 0, including all manufacturer data, and overwrites
	 * that block with the new UID, a freshly calculated BCC, and the original
	 * manufacturer data.
	 *
	 * Make sure to have selected the card before this function is called.
	 * @param newUid New UID.
	 * @param uid Current UID
	 * @param authKey Authentication key
	 * @return Status
	 */
	public boolean mifareSetUid(byte[] newUid, UID uid, byte[] authKey) {
		// UID + BCC byte can not be larger than 16 together
		if (newUid == null || newUid.length == 0 || newUid.length > 15) {
			Logger.error("New UID buffer empty, size 0, or size > 15 given");
			return false;
		}
		
		// Authenticate for reading
		StatusCode status = authenticate(true, (byte) 1, authKey, uid);
		if (status != StatusCode.OK) {
			if (status == StatusCode.TIMEOUT) {
				// We get a read timeout if no card is selected yet, so let's select one
				
				// Wake the card up again if sleeping
				//byte atqa_answer[2];
				//byte atqa_size = 2;
				//wakeupA(atqa_answer, &atqa_size);
				
				if (! isNewCardPresent() || readCardSerial() == null) {
					Logger.error("No card was previously selected, and none are available. Failed to set UID.");
					return false;
				}
				
				status = authenticate(true, (byte) 1, authKey, uid);
				if (status != StatusCode.OK) {
					// We tried, time to give up
					Logger.error("Failed to authenticate to card for reading, could not set UID: {}", status);
					return false;
				}
			} else {
				Logger.error("PCD_Authenticate() failed: {}", status);
				return false;
			}
		}
		
		// Read block 0
		byte[] block0_buffer = mifareRead((byte) 0);
		if (block0_buffer == null) {
			Logger.error("MIFARE_Read() failed: {}. Are you sure your KEY A for sector 0 is 0x{}?", status, Hex.encodeHexString(authKey));
			return false;
		}
		
		// Write new UID to the data we just read, and calculate BCC byte
		byte bcc = 0;
		for (int i=0; i<uid.getSize(); i++) {
			block0_buffer[i] = newUid[i];
			bcc ^= newUid[i];
		}
		
		// Write BCC byte to buffer
		block0_buffer[uid.getSize()] = bcc;
		
		// Stop encrypted traffic so we can send raw bytes
		stopCrypto1();
		
		// Activate UID backdoor
		if (! mifareOpenUidBackdoor()) {
			Logger.error("Activating the UID backdoor failed.");
			return false;
		}
		
		// Write modified block 0 back to card
		status = mifareWrite((byte) 0, block0_buffer);
		if (status != StatusCode.OK) {
			Logger.error("MIFARE_Write() failed: {}", status);
			return false;
		}
		
		// Wake the card up again
		byte[] atqa_answer = new byte[2];
		wakeupA(atqa_answer);
		
		return true;
	}
	
	/**
	 * Resets entire sector 0 to zeroes, so the card can be read again by readers.
	 * @return Status
	 */
	public boolean mifareUnbrickUidSector() {
		mifareOpenUidBackdoor();
		
		byte[] block0_buffer = { 0x01, 0x02, 0x03, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
		
		// Write modified block 0 back to card
		StatusCode status = mifareWrite((byte) 0, block0_buffer);
		if (status != StatusCode.OK) {
			Logger.error("MIFARE_Write() failed: {}", status);
			return false;
		}
		return true;
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// Convenience functions - does not add extra functionality
	/////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns true if a PICC responds to PICC_CMD_REQA.
	 * Only "new" cards in state IDLE are invited. Sleeping cards in state HALT are ignored.
	 * 
	 * @return bool
	 */
	public boolean isNewCardPresent() {
		// Reset baud rates
		writeRegister(PcdRegister.TX_MODE_REG, (byte) 0x00);
		writeRegister(PcdRegister.RX_MODE_REG, (byte) 0x00);
		// Reset ModWidthReg
		writeRegister(PcdRegister.MOD_WIDTH_REG, (byte) 0x26);

		byte[] bufferATQA = new byte[2];
		StatusCode result = requestA(bufferATQA);
		return (result == StatusCode.OK || result == StatusCode.COLLISION);
	} // End PICC_IsNewCardPresent()

	/**
	 * Simple wrapper around PICC_Select.
	 * Returns the UID of the card if present, otherwise null.
	 * Remember to call PICC_IsNewCardPresent(), PICC_RequestA() or PICC_WakeupA() first.
	 * 
	 * @return The UID is a card could be read, otherwise null
	 */
	public UID readCardSerial() {
		return select();
	}

	
	// DEBUG METHODS


	/**
	 * Dumps debug info about the connected PCD to Serial.
	 * Shows all known firmware versions
	 */
	public void dumpVersionToConsole() {
		// Get the MFRC522 firmware version
		int v = readRegister(PcdRegister.VERSION_REG) & 0xff;
		System.out.print("Firmware Version: 0x");
		System.out.print(Integer.toHexString(v & 0xff));
		// Lookup which version
		switch (v) {
			case 0x88: System.out.println(" = (clone)");  break;
			case 0x90: System.out.println(" = v0.0");     break;
			case 0x91: System.out.println(" = v1.0");     break;
			case 0x92: System.out.println(" = v2.0");     break;
			default:   System.out.println(" = (unknown)");
		}
		// When 0x00 or 0xFF is returned, communication probably failed
		if ((v == 0x00) || (v == 0xFF)) {
			System.out.println("WARNING: Communication failure, is the MFRC522 properly connected?");
		}
	} // End PCD_DumpVersionToSerial()

	/**
	 * Dumps debug info about the selected PICC to Serial.
	 * On success the PICC is halted after dumping the data.
	 * For MIFARE Classic the factory default key of 0xFFFFFFFFFFFF is tried.  
	 *
	 * @param uid UID returned from a successful PICC_Select().
	 * @deprecated Kept for backward compatibility
	 */
	@Deprecated
	public void dumpToConsole(UID uid) {
		dumpToConsole(uid, DEFAULT_KEY);
	}
	
	@Deprecated
	public void dumpToConsole(UID uid, byte[] key) {	
		// Dump UID, SAK and Type
		dumpDetailsToConsole(uid);
		
		// Dump contents
		PiccType piccType = uid.getType();
		switch (piccType) {
		case MIFARE_MINI:
		case MIFARE_1K:
		case MIFARE_4K:
			// All keys are set to FFFFFFFFFFFFh at chip delivery from the factory.
			dumpMifareClassicToConsole(uid, key);
			break;
			
		case MIFARE_UL:
			dumpMifareUltralightToConsole();
			break;
			
		case ISO_14443_4:
		case MIFARE_DESFIRE:
		case ISO_18092:
		case MIFARE_PLUS:
		case TNP3XXX:
			Logger.warn("Dumping memory contents not implemented for that PICC type.");
			break;
			
		case UNKNOWN:
		case NOT_COMPLETE:
		default:
			break; // No memory dump here
		}
		
		haltA(); // Already done if it was a MIFARE Classic PICC.
	} // End PICC_DumpToSerial()

	/**
	 * Dumps card info (UID,SAK,Type) about the selected PICC to Serial.
	 *
	 * @param uid UID struct returned from a successful PICC_Select().
	 * @deprecated kept for backward compatibility
	 */
	@Deprecated
	public static void dumpDetailsToConsole(UID uid) {
		// UID
		System.out.println("Card UID: 0x" + Hex.encodeHexString(uid.getUidBytes()));
		
		// SAK
		System.out.println("Card SAK: 0x" + Integer.toHexString(uid.getSak() & 0xff));
		
		// (suggested) PICC type
		System.out.println("PICC type: " + uid.getType().getName());
	} // End PICC_DumpDetailsToSerial()

	/**
	 * Dumps memory contents of a MIFARE Classic PICC.
	 * On success the PICC is halted after dumping the data.
	 * @param uid UID returned from a successful PICC_Select().
	 * @param key Key A used for all sectors.
	 */
	public void dumpMifareClassicToConsole(UID uid, byte[] key) {
		byte no_of_sectors = 0;
		switch (uid.getType()) {
		case MIFARE_MINI:
			// Has 5 sectors * 4 blocks/sector * 16 bytes/block = 320 bytes.
			no_of_sectors = 5;
			break;
			
		case MIFARE_1K:
			// Has 16 sectors * 4 blocks/sector * 16 bytes/block = 1024 bytes.
			no_of_sectors = 16;
			break;
			
		case MIFARE_4K:
			// Has (32 sectors * 4 blocks/sector + 8 sectors * 16 blocks/sector) * 16 bytes/block = 4096 bytes.
			no_of_sectors = 40;
			break;
			
		default: // Should not happen. Ignore.
			break;
		}
		
		// Dump sectors, highest address first.
		if (no_of_sectors != 0) {
			System.out.println("Sector Block   0  1  2  3   4  5  6  7   8  9 10 11  12 13 14 15  AccessBits");
			for (int i=no_of_sectors - 1; i>=0; i--) {
				dumpMifareClassicSectorToConsole(uid, key, (byte) i);
			}
		}
		haltA(); // Halt the PICC before stopping the encrypted session.
		stopCrypto1();
	} // End PICC_DumpMifareClassicToSerial()

	/**
	 * Dumps memory contents of a sector of a MIFARE Classic PICC.
	 * Uses PCD_Authenticate(), MIFARE_Read() and PCD_StopCrypto1.
	 * Always uses PICC_CMD_MF_AUTH_KEY_A because only Key A can always read the sector trailer access bits.
	 * @param uid UID returned from a successful select().
	 * @param key Key A for the sector.
	 * @param sector The sector to dump, 0..39.
	 */
	public void dumpMifareClassicSectorToConsole(UID uid, byte[] key, byte sector) {
		int firstBlock;				// Address of lowest address to dump actually last block dumped)
		byte no_of_blocks;			// Number of blocks in sector
		boolean isSectorTrailer;	// Set to true while handling the "last" (ie highest address) in the sector.
		
		// The access bits are stored in a peculiar fashion.
		// There are four groups:
		//		g[3]	Access bits for the sector trailer, block 3 (for sectors 0-31) or block 15 (for sectors 32-39)
		//		g[2]	Access bits for block 2 (for sectors 0-31) or blocks 10-14 (for sectors 32-39)
		//		g[1]	Access bits for block 1 (for sectors 0-31) or blocks 5-9 (for sectors 32-39)
		//		g[0]	Access bits for block 0 (for sectors 0-31) or blocks 0-4 (for sectors 32-39)
		// Each group has access bits [C1 C2 C3]. In this code C1 is MSB and C3 is LSB.
		// The four CX bits are stored together in a nible cx and an inverted nible cx_.
		int c1, c2, c3;		// Nibbles
		int c1_, c2_, c3_;		// Inverted nibbles
		int[] g = new int[4];	// Access bits for each of the four groups.
		byte group;				// 0-3 - active group for access bits
		boolean firstInGroup;	// True for the first block dumped in the group
		
		// Determine position and size of sector.
		if (sector < 32) { // Sectors 0..31 has 4 blocks each
			no_of_blocks = 4;
			firstBlock = sector * no_of_blocks;
		} else if (sector < 40) { // Sectors 32-39 has 16 blocks each
			no_of_blocks = 16;
			firstBlock = 128 + (sector - 32) * no_of_blocks;
		} else { // Illegal input, no MIFARE Classic PICC has more than 40 sectors.
			return;
		}
			
		// Dump blocks, highest address first.
		byte[] buffer;
		byte blockAddr;
		isSectorTrailer = true;
		// True if one of the inverted nibbles did not match
		boolean invertedError = false;	// Avoid "unassiged variable" warning.
		for (byte blockOffset=(byte) (no_of_blocks - 1); blockOffset>=0; blockOffset--) {
			blockAddr = (byte) (firstBlock + blockOffset);
			// Sector number - only on first line
			if (isSectorTrailer) {
				if (sector < 10) {
					System.out.print("   "); // Pad with spaces
				} else {
					System.out.print("  "); // Pad with spaces
				}
				System.out.print(sector);
				System.out.print("   ");
			} else {
				System.out.print("       ");
			}
			// Block number
			if (blockAddr < 10) {
				System.out.print("   "); // Pad with spaces
			} else {
				if (blockAddr < 100) {
					System.out.print("  "); // Pad with spaces
				} else {
					System.out.print(" "); // Pad with spaces
				}
			}
			System.out.print(blockAddr);
			System.out.print("  ");
			// Establish encrypted communications before reading the first block
			if (isSectorTrailer) {
				StatusCode status = authenticate(true, (byte) firstBlock, key, uid);
				if (status != StatusCode.OK) {
					System.out.println("PCD_Authenticate() failed: " + status);
					return;
				}
			}
			// Read block
			buffer = mifareRead(blockAddr);
			if (buffer == null) {
				System.out.print("MIFARE_Read() failed");
				continue;
			}
			// Dump data
			for (byte index = 0; index < 16; index++) {
				int val = buffer[index] & 0xff;
				if (val < 0x10) {
					System.out.print(" 0");
				} else {
					System.out.print(" ");
				}
				System.out.print(Integer.toHexString(val & 0xff));
				if ((index % 4) == 3) {
					System.out.print(" ");
				}
			}
			// Parse sector trailer data
			if (isSectorTrailer) {
				c1  = (buffer[7] & 0xff) >> 4;
				c2  = (buffer[8] & 0xF);
				c3  = ((buffer[8] & 0xff) >> 4);
				c1_ = (buffer[6] & 0xF);
				c2_ = ((buffer[6] & 0xff) >> 4);
				c3_ = (buffer[7] & 0xF);
				invertedError = (c1 != (~c1_ & 0xF)) || (c2 != (~c2_ & 0xF)) || (c3 != (~c3_ & 0xF));
				g[0] = ((c1 & 1) << 2) | ((c2 & 1) << 1) | ((c3 & 1) << 0);
				g[1] = ((c1 & 2) << 1) | ((c2 & 2) << 0) | ((c3 & 2) >> 1);
				g[2] = ((c1 & 4) << 0) | ((c2 & 4) >> 1) | ((c3 & 4) >> 2);
				g[3] = ((c1 & 8) >> 1) | ((c2 & 8) >> 2) | ((c3 & 8) >> 3);
				isSectorTrailer = false;
			}
			
			// Which access group is this block in?
			if (no_of_blocks == 4) {
				group = blockOffset;
				firstInGroup = true;
			} else {
				group = (byte) (blockOffset / 5);
				firstInGroup = (group == 3) || (group != (blockOffset + 1) / 5);
			}
			
			if (firstInGroup) {
				// Print access bits
				System.out.print(" [ ");
				System.out.print((g[group] >> 2) & 1); System.out.print(" ");
				System.out.print((g[group] >> 1) & 1); System.out.print(" ");
				System.out.print((g[group] >> 0) & 1);
				System.out.print(" ] ");
				if (invertedError) {
					System.out.print(" Inverted access bits did not match! ");
				}
			}
			
			if (group != 3 && (g[group] == 1 || g[group] == 6)) { // Not a sector trailer, a value block
				int value = ((buffer[3] & 0xff)<<24) | ((buffer[2] & 0xff)<<16) | ((buffer[1] & 0xff)<<8) | (buffer[0] & 0xff);
				System.out.print(String.format(" Value=0x%02x Adr=0x%02x", Integer.valueOf(value), Byte.valueOf(buffer[12])));
			}
			System.out.println();
		}
		
		return;
	} // End PICC_DumpMifareClassicSectorToSerial()

	/**
	 * Dumps memory contents of a MIFARE Ultralight PICC.
	 */
	public void dumpMifareUltralightToConsole() {
		int i;
		
		System.out.println("Page  0  1  2  3");
		// Try the mpages of the original Ultralight. Ultralight C has more pages.
		for (byte page=0; page<16; page+=4) { // Read returns data for 4 pages at a time.
			// Read pages
			byte[] buffer = mifareRead(page);
			if (buffer == null) {
				System.out.println("MIFARE_Read() failed: buffer was null");
				break;
			}
			// Dump data
			for (byte offset=0; offset<4; offset++) {
				i = page + offset;
				if (i < 10) {
					System.out.print("  "); // Pad with spaces
				} else {
					System.out.print(" "); // Pad with spaces
				}
				System.out.print(i);
				System.out.print("  ");
				for (byte index=0; index<4; index++) {
					i = 4 * offset + index;
					if (buffer[i] < 0x10) {
						System.out.print(" 0");
					} else {
						System.out.print(" ");
					}
					System.out.print(Integer.toHexString(buffer[i] & 0xff));
				}
				System.out.println();
			}
		}
	} // End PICC_DumpMifareUltralightToSerial()

	
	public static class Response {
		private StatusCode status;
		private byte[] backData;
		private int backLen;
		private byte validBits;
		
		public Response(StatusCode status) {
			this.status = status;
		}
		
		public Response(StatusCode status, byte[] backData) {
			this.status = status;
			this.backData = backData;
		}
		
		public Response(StatusCode status, byte[] backData, int backLen, byte validBits) {
			this.status = status;
			this.backData = backData;
			this.backLen = backLen;
			this.validBits = validBits;
		}

		public StatusCode getStatus() {
			return status;
		}

		public byte[] getBackData() {
			return backData;
		}
		
		public int getBackLen() {
			return backLen;
		}
		
		public byte getValidBits() {
			return validBits;
		}

		@Override
		public String toString() {
			return "Response [status=" + status + ", backData=" + Arrays.toString(backData) + ", backLen=" + backLen
					+ ", validBits=" + validBits + "]";
		}
	}
	
	public static enum StatusCode {
		OK(0)				,	// Success
		ERROR(1)			,	// Error in communication
		COLLISION(2)		,	// Collission detected
		TIMEOUT(3)			,	// Timeout in communication.
		NO_ROOM(4)			,	// A buffer is not big enough.
		INTERNAL_ERROR(5)	,	// Internal error in the code. Should not happen ;-)
		INVALID(6)			,	// Invalid argument.
		CRC_WRONG(7)		,	// The CRC_A does not match
		MIFARE_NACK(0xff)	;	// A MIFARE PICC responded with NAK.
		
		private byte code;
		
		private StatusCode(int code) {
			this((byte) code);
		}
		
		private StatusCode(byte code) {
			this.code = code;
		}
		
		public byte getCode() {
			return code;
		}
	}
	
	private static enum PcdRegister {
		// Registers
		//Reserved00(0x00),
		COMMAND_REG(0x01),				// starts and stops command execution
		COM_INT_EN_REG(0x02),			// enable and disable interrupt request control bits
		DIV_INT_EN_REG(0x03),			// enable and disable interrupt request control bits
		COM_IRQ_REG(0x04),				// interrupt request bits
		DIV_IRQ_REG(0x05),				// interrupt request bits
		ERROR_REG(0x06),				// error bits showing the error status of the last command executed
		STATUS1_REG(0x07),				// communication status bits
		STATUS2_REG(0x08),				// receiver and transmitter status bits
		FIFO_DATA_REG(0x09),			// input and output of 64 byte FIFO buffer
		FIFO_LEVEL_REG(0x0A),			// number of bytes stored in the FIFO buffer
		WATER_LEVEL_REG(0x0B),			// level for FIFO underflow and overflow warning
		CONTROL_REG(0x0C),				// miscellaneous control registers
		BIT_FRAMING_REG(0x0D),			// adjustments for bit-oriented frames
		COLL_REG(0x0E),					// bit position of the first bit-collision detected on the RF interface
		//RESERVED_01(0x0F),
		//RESERVED_10(0x10),
		MODE_REG(0x11),					// defines general modes for transmitting and receiving
		TX_MODE_REG(0x12),				// defines transmission data rate and framing
		RX_MODE_REG(0x13),				// defines reception data rate and framing
		TX_CONTROL_REG(0x14),			// controls the logical behavior of the antenna driver pins TX1 and TX2
		TX_ASK_REG(0x15),				// controls the setting of the transmission modulation
		TX_SEL_REG(0x16),				// selects the internal sources for the antenna driver
		RX_SEL_REG(0x17),				// selects internal receiver settings
		RX_THRESHOLD_REG(0x18),			// selects thresholds for the bit decoder
		DEMOD_REG(0x19),				// defines demodulator settings
		//Reserved11(0x1A),
		//Reserved12(0x1B),
		MIFARE_TX_REG(0x1C),			// controls some MIFARE communication transmit parameters
		MIFARE_RX_REG(0x1D),			// controls some MIFARE communication receive parameters
		//Reserved14(0x1E),
		SERIAL_SPEED_REG(0x1F),			// selects the speed of the serial UART interface
		//Reserved20(0x20),  
		CRC_RESULT_REG_MSB(0x21),		// shows the MSB values of the CRC calculation
		CRC_RESULT_REG_LSB(0x22),		// shows the LSB values of the CRC calculation
		//Reserved21(0x23),
		MOD_WIDTH_REG(0x24),			// controls the ModWidth setting
		//Reserved22(0x25),
		RF_CONFIG_REG(0x26),			// configures the receiver gain
		GS_N_REG(0x27),					// selects the conductance of the antenna driver pins TX1 and TX2 for modulation
		CWGsP_REG(0x28),				// defines the conductance of the p-driver output during periods of no modulation
		ModGsP_REG(0x29),				// defines the conductance of the p-driver output during periods of modulation
		T_MODE_REG(0x2A),				// defines settings for the internal timer
		T_PRESCALER_REG(0x2B),			// 
		T_RELOAD_REG_MSB(0x2C),			// defines the 16-bit timer reload value
		T_RELOAD_REG_LSB(0x2D),
		T_COUNTER_VALUE_REG_MSB(0x2E),	// shows the 16-bit timer value
		T_COUNTER_VALUE_REG_LSB(0x2F),
		//Reserved30(0x30),
		TEST_SEL1_REG(0x31),			// general test signal configuration
		TEST_SEL2_REG(0x32),			// general test signal configuration and PRBS control
		TEST_PIN_EN_REG(0x33),			// enables pin output driver on pins D1 to D7
		TEST_PIN_VALUE_REG(0x34),		// defines the values for D1 to D7 when it is used as an I/O bus
		TEST_BUS_REG(0x35),				// shows the status of the internal test bus
		AUTO_TEST_REG(0x36),			// controls the digital self test
		VERSION_REG(0x37),				// shows the software version
		ANALOG_TEST_REG(0x38),			// controls the pins AUX1 and AUX2
		TEST_DAC1_REG(0x39),			// defines the test value for TestDAC1
		TEST_DAC2_REG(0x3A),			// defines the test value for TestDAC2
		TEST_ADC_REG(0x3B);				// shows the value of ADC I and Q channels
		//Reserved31(0x3C),
		//Reserved32(0x3D),
		//Reserved33(0x3E),
		//Reserved34(0x3F);

		private byte value;
		private byte address;
		
		private PcdRegister(int value) {
			this.value = (byte) value;
			address = (byte) ((value << 1) & 0x7e);
		}

		public byte getAddress() {
			return address;
		}
		
		public byte getValue() {
			return value;
		}
	}
	
	private static enum PcdCommand {
		IDLE(0b0000),				// no action, cancels current command execution
		MEM(0b0001),				// stores 25 bytes into the internal buffer
		GENERATE_RANDOM_ID(0b0010),	// generates a 10-byte random ID number
		CALC_CRC(0b0011),			// activates the CRC coprocessor or performs a self test
		TRANSMIT(0b0100),			// transmits data from the FIFO buffer
		NO_CMD_CHANGE(0b0111),		// no command change, can be used to modify the
									// CommandReg register bits without affecting the command, 
									// for example, the PowerDown bit
		RECEIVE(0b1000),			// activates the receiver circuits
		TRANSCEIVE(0b1100),			// transmits data from FIFO buffer to antenna and automatically
									// activates the receiver after transmission
		MF_AUTHENT(0b1110),			// performs the MIFARE standard authentication as a reader
		SOFT_RESET(0b1111);			// resets the MFRC522
		
		private byte value;
		
		private PcdCommand(byte value) {
			this.value = value;
		}
		
		private PcdCommand(int value) {
			this.value = (byte) value;
		}
		
		public byte getValue() {
			return value;
		}
	}
	
	// AddicoreRFID Proximity Integrated Circuit Card (PICC) Commands
	private static enum PiccCommand {
		// The commands used by the PCD to manage communication with several PICCs (ISO 14443-3, Type A, section 6.4)
		REQUEST_A(0x26),		// REQuest command, Type A. Invites PICCs in state IDLE to go to READY and prepare for anticollision or selection. 7 bit frame.
		MF_READ(0x30),			// Reads one 16 byte block from the authenticated sector of the PICC. Also used for MIFARE Ultralight.
		HALT_A(0x50),			// HaLT command, Type A. Instructs an ACTIVE PICC to go to state HALT.
		WAKE_UP_A(0x52),		// Wake-UP command, Type A. Invites PICCs in state IDLE and HALT to go to READY(*) and prepare for anticollision or selection. 7 bit frame.
		MF_AUTH_KEY_A(0x60),	// Perform authentication with key A
		MF_AUTH_KEY_B(0x61),	// Perform authentication with key B
		CASCADE_TAG(0x88),		// Cascade Tag. Not really a command, but used during anti collision.
		SEL_CL1(0x93),			// Anti collision/Select, Cascade Level 1
		SEL_CL2(0x95),			// Anti collision/Select, Cascade Level 2
		SEL_CL3(0x97),			// Anti collision/Select, Cascade Level 3
		MF_WRITE(0xA0),			// Writes one 16 byte block to the authenticated sector of the PICC. Called "COMPATIBILITY WRITE" for MIFARE Ultralight.
		UL_WRITE(0xA2),			// Writes one 4 byte page to the PICC.
		MF_TRANSFER(0xB0),		// Writes the contents of the internal data register to a block.
		MF_DECREMENT(0xC0),		// Decrements the contents of a block and stores the result in the internal data register.
		MF_INCREMENT(0xC1),		// Increments the contents of a block and stores the result in the internal data register.
		MF_RESTORE(0xC2);		// Reads the contents of a block into the internal data register.
		
		private byte value;
		
		private PiccCommand(int value) {
			this((byte) value);
		}
		
		private PiccCommand(byte value) {
			this.value = value;
		}
		
		public byte getValue() {
			return value;
		}
	}
	
	public static class UID {
		/** Number of bytes in the UID. 4, 7 or 10. */
		private byte[] uidBytes;
		/** The SAK (Select acknowledge) byte returned from the PICC after successful selection. */
		private byte sak;
		
		public UID(List<Byte> bytes, byte sak) {
			uidBytes = new byte[bytes.size()];
			for (int i=0; i<bytes.size(); i++) {
				uidBytes[i] = bytes.get(i).byteValue();
			}
			this.sak = sak;
		}
		
		public int getSize() {
			return uidBytes.length;
		}
		
		public byte getUidByte(int index) {
			return uidBytes[index];
		}
		
		public byte[] getUidBytes() {
			return uidBytes;
		}
		
		public byte getSak() {
			return sak;
		}

		@Override
		public String toString() {
			return "UID [uidBytes=" + Hex.encodeHexString(uidBytes) + ", sak=" + sak + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(uidBytes);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			UID other = (UID) obj;
			if (!Arrays.equals(uidBytes, other.uidBytes))
				return false;
			return true;
		}

		public PiccType getType() {
			return PiccType.forId(sak);
		}
	}
	
	public static enum AntennaGain {
		DB_18A((byte) (0b000 << 4), 18),
		DB_23A((byte) (0b001 << 4), 23),
		DB_18B((byte) (0b010 << 4), 18),
		DB_23B((byte) (0b011 << 4), 23),
		DB_33((byte) (0b100 << 4), 33),
		DB_38((byte) (0b101 << 4), 38),
		DB_43((byte) (0b110 << 4), 43),
		DB_48((byte) (0b111 << 4), 48);
		
		private byte value;
		private int gainDb;
		
		private AntennaGain(byte value, int gainDb) {
			this.value = value;
			this.gainDb = gainDb;
		}

		public byte getValue() {
			return value;
		}

		public int getGainDb() {
			return gainDb;
		}

		public static AntennaGain forValue(byte value) {
			switch (value) {
			case 0b000:
				return DB_18A;
			case 0b001:
				return DB_23A;
			case 0b010:
				return DB_18B;
			case 0b011:
				return DB_23B;
			case 0b100:
				return DB_33;
			case 0b101:
				return DB_38;
			case 0b110:
				return DB_43;
			case 0b111:
				return DB_48;
			}
			return null;
		}
	}
	
	public static enum PiccType {
		UNKNOWN("Unknown type"),
		ISO_14443_4("PICC compliant with ISO/IEC 14443-4"),
		ISO_18092("PICC compliant with ISO/IEC 18092 (NFC)"),
		MIFARE_MINI("MIFARE Mini, 320 bytes"),
		MIFARE_1K("MIFARE 1KB"),
		MIFARE_4K("MIFARE 4KB"),
		MIFARE_UL("MIFARE Ultralight or Ultralight C"),
		MIFARE_PLUS("MIFARE Plus"),
		MIFARE_DESFIRE("MIFARE DESFire"),
		TNP3XXX("MIFARE TNP3XXX"),
		NOT_COMPLETE("SAK indicates UID is not complete.");
		
		private String name;
		
		private PiccType(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public static PiccType forId(byte id) {
			switch (id) {
			case 0x04:	return NOT_COMPLETE;	// UID not complete
			case 0x09:	return MIFARE_MINI;
			case 0x08:	return MIFARE_1K;
			case 0x18:	return MIFARE_4K;
			case 0x00:	return MIFARE_UL;
			case 0x10:
			case 0x11:	return MIFARE_PLUS;
			case 0x01:	return TNP3XXX;
			case 0x20:	return ISO_14443_4;
			case 0x40:	return ISO_18092;
			default:	return UNKNOWN;
			}
		}
	}
}
