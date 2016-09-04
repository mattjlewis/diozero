package com.diozero.sandpit;

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


import java.io.Closeable;
import java.nio.ByteBuffer;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.SpiDevice;

/**
 * Datasheet: http://www.nxp.com/documents/data_sheet/MFRC522.pdf
 * Example Python code: https://github.com/mxgxw/MFRC522-python/blob/master/MFRC522.py
 * Work-in-progress!
 */
public class MFRC522 implements Closeable {
	
	// Commands
	private static final byte PCD_IDLE			= 0b0000;
	private static final byte PCD_CALCCRC		= 0b0011;
	private static final byte PCD_TRANSMIT		= 0b0100;
	private static final byte PCD_RECEIVE		= 0b1000;
	private static final byte PCD_TRANSCEIVE	= 0b1100;
	private static final byte PCD_MF_AUTHENT	= 0b1110;
	private static final byte PCD_SOFT_RESET	= 0b1111;
	
	// AddicoreRFID Proximity Integrated Circuit Card (PICC) Commands
	public static final byte PICC_REQIDL		= 0x26;  // search the antenna area. PCD does not enter hibernation
	public static final byte PICC_REQALL		= 0x52;  // find all the cards in antenna area
	public static final byte PICC_ANTICOLL		= (byte) 0x93;  // anti-collision
	public static final byte PICC_SElECTTAG	= (byte) 0x93;  // Select card
	public static final byte PICC_AUTHENT1A	= 0x60;  // authentication with key A
	public static final byte PICC_AUTHENT1B	= 0x61;  // authentication with key B
	public static final byte PICC_READ			= 0x30;  // reads one memory block
	public static final byte PICC_WRITE		= (byte) 0xA0;  // writes one memory block
	public static final byte PICC_DECREMENT	= (byte) 0xC0;  // decrements the contents of a block and stores the result in the internal data register
	public static final byte PICC_INCREMENT	= (byte) 0xC1;  // increments the contents of a block and stores the result in the internal data register
	public static final byte PICC_RESTORE		= (byte) 0xC2;  // reads the contents of a block into the internal data register
	public static final byte PICC_TRANSFER		= (byte) 0xB0;  // writes the contents of the internal data register to a block
	public static final byte PICC_HALT			= 0x50;  // Sleep the card
	
	// Registers
	//private static final int Reserved00 = 0x00;
	private static final int COMMAND_REG = 0x01;
	private static final int COMM_I_EN_REG = 0x02;
	private static final int DivlEn_REG = 0x03;
	private static final int COMM_IRQ_REG = 0x04;
	private static final int DIV_IRQ_REG = 0x05;
	private static final int ERROR_REG = 0x06;
	private static final int STATUS1_REG = 0x07;
	private static final int STATUS2_REG = 0x08;
	private static final int FIFO_DATA_REG = 0x09;
	private static final int FIFO_LEVEL_REG = 0x0A;
	private static final int WATER_LEVEL_REG = 0x0B;
	private static final int CONTROL_REG = 0x0C;
	private static final int BIT_FRAMING_REG = 0x0D;
	private static final int COLL_REG = 0x0E;
	//private static final int RESERVED_01 = 0x0F;

	//private static final int RESERVED_10 = 0x10;
	private static final int MODE_REG = 0x11;
	private static final int TX_MODE_REG = 0x12;
	private static final int RX_MODE_REG = 0x13;
	private static final int TX_CONTROL_REG = 0x14;
	private static final int TX_AUTO_REG = 0x15;
	private static final int TX_SEL_REG = 0x16;
	private static final int RX_SEL_REG = 0x17;
	private static final int RX_THRESHOLD_REG = 0x18;
	private static final int DEMOD_REG = 0x19;
	//private static final int Reserved11 = 0x1A;
	//private static final int Reserved12 = 0x1B;
	private static final int MIFARE_REG = 0x1C;
	//private static final int Reserved13 = 0x1D;
	//private static final int Reserved14 = 0x1E;
	private static final int SERIAL_SPEED_REG = 0x1F;

	//private static final int Reserved20 = 0x20;  
	private static final int CRC_RESULT_REG_M = 0x21;
	private static final int CRC_RESULT_REG_L = 0x22;
	//private static final int Reserved21 = 0x23;
	private static final int MOD_WIDTH_REG = 0x24;
	//private static final int Reserved22 = 0x25;
	private static final int RFCfg_REG = 0x26;
	private static final int GsN_REG = 0x27;
	private static final int CWGsP_REG = 0x28;
	private static final int ModGsP_REG = 0x29;
	private static final int T_MODE_REG = 0x2A;
	private static final int T_PRESCALER_REG = 0x2B;
	private static final int T_RELOAD_REG_H = 0x2C;
	private static final int T_RELOAD_REG_L = 0x2D;
	private static final int T_COUNTER_VALUE_REG_H = 0x2E;
	private static final int T_COUNTER_VALUE_REG_L = 0x2F;
			  
	//private static final int Reserved30 = 0x30;
	private static final int TEST_SEL1_REG = 0x31;
	private static final int TEST_SEL2_REG = 0x32;
	private static final int TEST_PIN_EN_REG = 0x33;
	private static final int TEST_PIN_VALUE_REG = 0x34;
	private static final int TEST_BUS_REG = 0x35;
	private static final int AUTO_TEST_REG = 0x36;
	private static final int VERSION_REG = 0x37;
	private static final int ANALOG_TEST_REG = 0x38;
	private static final int TEST_DAC1_REG = 0x39;
	private static final int TEST_DAC2_REG = 0x3A;
	private static final int TEST_ADC_REG = 0x3B;
	//private static final int Reserved31 = 0x3C;
	//private static final int Reserved32 = 0x3D;
	//private static final int Reserved33 = 0x3E;
	//private static final int Reserved34 = 0x3F;
	
	// AddicoreRFID error codes
	public static final byte MI_OK = 0;
	public static final byte MI_NOTAGERR = 1;
	public static final byte MI_ERR = 2;

	// Maximum length of the array
	private static final int MAX_LEN = 16;
	
	private SpiDevice device;
	private DigitalOutputDevice resetPin;
	
	public MFRC522(int chipSelect, int resetPinNumber) {
		device = new SpiDevice(chipSelect);
		resetPin = new DigitalOutputDevice(resetPinNumber, true, false);
		
		resetPin.on();
		reset();
		
		// Timer: TPrescaler*TreloadVal/6.78MHz = 24ms
	    writeRegister(T_MODE_REG, (byte) 0x8D);			// Tauto=1; f(Timer) = 6.78MHz/TPreScaler
	    writeRegister(T_PRESCALER_REG, (byte) 0x3E);	// TModeReg[3..0] + TPrescalerReg
	    writeRegister(T_RELOAD_REG_L, (byte) 30);           
	    writeRegister(T_RELOAD_REG_H, (byte) 0);
		
	    writeRegister(TX_AUTO_REG, (byte) 0x40);		// 100%ASK
	    writeRegister(MODE_REG, (byte) 0x3D);			// CRC Initial value 0x6363	???
	    
	    setAntennaOn(true);
	}
	
	private byte readRegister(int address) {
		ByteBuffer tx = ByteBuffer.allocateDirect(2);
		// Address Format: 1XXXXXX0, the first "1" indicates a read
		tx.put((byte) ((address << 1) & 0x7e | 0x80));
		tx.put((byte) 0);
		tx.flip();
		
		ByteBuffer rx = device.writeAndRead(tx);
		
		return rx.get(1);
	}
	
	private void writeRegister(int address, byte value) {
		ByteBuffer tx = ByteBuffer.allocateDirect(2);
		// Address Format: 0XXXXXX0, the left most "0" indicates a write
		tx.put((byte) ((address << 1) & 0x7e));
		tx.put(value);
		tx.flip();
		
		device.writeAndRead(tx);
	}
	
	private byte writeAndReadRegister(int address, byte value) {
		ByteBuffer tx = ByteBuffer.allocateDirect(2);
		tx.put((byte) ((address << 1) & 0x7e | 0x80));
		tx.put(value);
		tx.flip();
		
		return device.writeAndRead(tx).get(1);
	}
	
	private void setBitMask(int address, byte mask) {
		byte temp = readRegister(address);
		// Already set?
		if ((temp & mask) != mask) {
			writeRegister(address, (byte) (temp | mask));
		}
	}
	
	private void clearBitMask(int address, byte mask) {
		byte temp = readRegister(address);
		// Already clear?
		if ((temp & mask) != 0) {
			writeRegister(address, (byte) (temp & ~mask));
		}
	}
	
	/**
	 * Open antennas, each time you start or shut down the natural barrier between the transmitter should be at least 1ms interval
	 * @param on on/off value
	 */
	public void setAntennaOn(boolean on) {
		if (on) {
			setBitMask(TX_CONTROL_REG, (byte) 0x03);
		} else {
			clearBitMask(TX_CONTROL_REG, (byte) 0x03);
		}
	}
	
	/**
	 * Perform soft reset of AddicoreRFID Module
	 */
	public void reset() {
		writeRegister(COMMAND_REG, PCD_SOFT_RESET);
	}
	
	/**
	 * Find cards, read the card type number
	 * Input parameters: reqMode - find cards way
	 *  TagType - Return Card Type
	 *   0x4400 = Mifare_UltraLight
	 *   0x0400 = Mifare_One(S50)
	 *   0x0200 = Mifare_One(S70)
	 *   0x0800 = Mifare_Pro(X)
	 *   0x4403 = Mifare_DESFire
	 * @param reqMode The request mode
	 * @return response?
	 */
	public Response request(byte reqMode) {
		// TxLastBists = BitFramingReg[2..0] ???
		writeRegister(BIT_FRAMING_REG, (byte) 0x07);
		
		byte[] tag_type = new byte[] {reqMode};
		Response response = toCard(PCD_TRANSCEIVE, tag_type);
		
		byte status = response.getStatus();
		if ((status != MI_OK) || (response.getBackData().length != 0x10)) {    
			status = MI_ERR;
		}
		
		return new Response(status, response.getBackData());
	}

	public Response toCard(byte command, byte[] data) {
		byte irq_en = 0x00;
		byte wait_irq = 0x00;
		
		switch (command) {
		case PCD_MF_AUTHENT:	// Certification cards close
			irq_en = 0x12;
			wait_irq = 0x10;
			break;
		case PCD_TRANSCEIVE:	// Transmit FIFO data
			irq_en = 0x77;
			wait_irq = 0x30;
			break;
		}
		
		// Interrupt request
		writeRegister(COMM_I_EN_REG, (byte) (irq_en | 0x80));
		// Clear all interrupt request bit
		clearBitMask(COMM_IRQ_REG, (byte) 0x80);
		//FlushBuffer=1, FIFO Initialisation
		setBitMask(FIFO_LEVEL_REG, (byte) 0x80);
		
		// NO action; Cancel the current command???
		writeRegister(COMMAND_REG, PCD_IDLE);
		
		// Writing data to the FIFO
		for (byte b : data) {
			writeRegister(FIFO_DATA_REG, b);
		}
		
		// Execute the command
		writeRegister(COMMAND_REG, command);
		
		if (command == PCD_TRANSCEIVE) {
			// StartSend=1,transmission of data starts
			setBitMask(BIT_FRAMING_REG, (byte) 0x80);
		}
		
		// Waiting to receive data to complete
		// i according to the clock frequency adjustment, the operator M1 card maximum waiting time 25ms???
		int i = 2000;	
		int n;
		do {
			n = readRegister(COMM_IRQ_REG);
			i--;
		} while ((i != 0) && ((n & 0x01) == 0) && ((n & wait_irq) == 0));
		/*while (true) {
			n = read(COMM_IRQ_REG);
			i = i - 1;
			//if ( ~((i != 0) && ~(n & 0x01) && ~(n & waitIRq)) )
			if (! ((i != 0) && ((n & 0x01) == 0) && ((n & wait_irq) == 0))) {
				break;
			}
		}*/
		
		// StartSend=0
		clearBitMask(BIT_FRAMING_REG, (byte) 0x80);
		
		byte[] back_data = new byte[0];
		
		byte status = MI_ERR;
		int back_bits = 0;
		if (i != 0) {
			// BufferOvfl Collerr CRCErr ProtecolErr
			if ((readRegister(ERROR_REG) & 0x1B) == 0x00) {
				status = MI_OK;

				if ((n & irq_en & 0x01) != 0) {
					status = MI_NOTAGERR;
				}
				
				if (command == PCD_TRANSCEIVE) {
					n = readRegister(FIFO_LEVEL_REG);
					// Indicates the number of valid bits in the last received byte
					// If this value is 000b, the whole byte is valid
					byte rx_last_bits = (byte) (readRegister(CONTROL_REG) & 0b0111);
					if (rx_last_bits != 0) {
						back_bits = (n-1)*8 + rx_last_bits;
					} else {
						back_bits = n*8;
					}
					
					if (n == 0) {
						n = 1;
					}
					if (n > MAX_LEN) {
						n = MAX_LEN;
					}
					
					back_data = new byte[n];
					for (i=0; i<n; i++) {
						back_data[i] = readRegister(FIFO_DATA_REG);
					}
				}
			} else {
				status = MI_ERR;
			}
		}

		return new Response(status, back_data, back_bits);
	}
	
	public Response anticoll() {
		writeRegister(BIT_FRAMING_REG, (byte) 0x00);
		
		byte[] ser_num = new byte[2];
		ser_num[0] = PICC_ANTICOLL;
		ser_num[1] = 0x20;
		
		Response response = toCard(PCD_TRANSCEIVE, ser_num);
		ser_num = response.getBackData();
		byte ser_num_check=0;
		byte status = response.getStatus();
	    if (status == MI_OK) {
			// Check card serial number
	    	int i;
			for (i=0; i<4; i++) {
				ser_num_check ^= ser_num[i];
			}
			if (ser_num_check != ser_num[i]) {
				status = MI_ERR;
			}
	    }
	    
	    return new Response(status, response.getBackData());
	}
	
	public byte[] calculateCrc(byte[] data) {
		clearBitMask(DIV_IRQ_REG, (byte) 0x04);		// CRCIrq = 0
		setBitMask(FIFO_LEVEL_REG, (byte) 0x80);	// Clear the FIFO pointer
		for (byte b : data) {
			writeRegister(FIFO_DATA_REG, b);
		}
		writeRegister(COMMAND_REG, PCD_CALCCRC);
		
		// Wait CRC calculation is complete
		int i = 0xFF;
		byte n;
		do {
			n = readRegister(DIV_IRQ_REG);
			i--;
		} while ((i != 0) && (n & 0x04) == 0);	// CRCIrq = 1
		
		byte[] resp = new byte[2];
		resp[0] = readRegister(CRC_RESULT_REG_L);
		resp[1] = readRegister(CRC_RESULT_REG_M);
		
		return resp;
	}
	
	public int selectTag(byte[] serNum) {
		byte[] buffer = new byte[9];
	    buffer[0] = PICC_SElECTTAG;
	    buffer[1] = 0x70;
	    for (int i=0; i<5; i++) {
	    	buffer[i+2] = serNum[i];
	    }
	    
	    byte[] crc = calculateCrc(buffer);
	    buffer[7] = crc[0];
	    buffer[8] = crc[1];
	    Response response = toCard(PCD_TRANSCEIVE, buffer);
	    
	    byte size;
	    if ((response.getStatus() == MI_OK) && (response.getBackBits() == 0x18)) {
			size = response.getBackData()[0];
		} else {
			size = 0;
		}

	    return size;
	}
	
	public byte auth(byte authMode, byte blockAddr, byte[] sectorKey, byte[] serNum) {
		byte[] buff = new byte[12];

		// Verify the command block address + sector + password + card serial number
		// First byte should be the authMode (A or B)
	    buff[0] = authMode;
	    // Second byte is the trailerBlock (usually 7)
	    buff[1] = blockAddr;
	    // Now we need to append the authKey which usually is 6 bytes of 0xFF
	    int i;
	    for (i=0; i<sectorKey.length; i++) {
			buff[i+2] = sectorKey[i];
		}
	    // Next we append the first 4 bytes of the UID
	    for (i=0; i<4; i++) {    
			buff[i+2+sectorKey.length] = serNum[i];
		}
	    Response response = toCard(PCD_MF_AUTHENT, buff);

	    byte status = response.getStatus();
	    if ((status != MI_OK) || ((readRegister(STATUS2_REG) & 0x08) == 0)) {
			status = MI_ERR;
		}
	    
	    return status;
	}


	@Override
	public void close() {
		if (device != null) {
			device.close();
		}
	}
}

class Response {
	private byte status;
	private byte[] backData;
	private int backBits;
	
	public Response(byte status, byte[] backData) {
		this.status = status;
		this.backData = backData;
	}
	
	public Response(byte status, byte[] backData, int backBits) {
		this.status = status;
		this.backData = backData;
		this.backBits = backBits;
	}

	public byte getStatus() {
		return status;
	}

	public byte[] getBackData() {
		return backData;
	}
	
	public int getBackBits() {
		return backBits;
	}
}
