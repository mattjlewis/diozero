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
import java.util.Arrays;

import org.pmw.tinylog.Logger;

import com.diozero.api.*;
import com.diozero.util.SleepUtil;

/**
 * <p><a href="http://www.nxp.com/documents/data_sheet/MFRC522.pdf">Datasheet</a><br>
 * <a href="https://github.com/mxgxw/MFRC522-python/blob/master/MFRC522.py">Example Python code</a></p>
 * <p>Work-in-progress!</p>
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
 */
@SuppressWarnings("unused")
public class MFRC522 implements Closeable {
	// Registers
	//private static final int Reserved00 = 0x00;
	private static final int COMMAND_REG = 0x01;		// starts and stops command execution
	private static final int COM_INT_EN_REG = 0x02;		// enable and disable interrupt request control bits
	private static final int DIV_INT_EN_REG = 0x03;		// enable and disable interrupt request control bits
	private static final int COM_IRQ_REG = 0x04;		// interrupt request bits
	private static final int DIV_IRQ_REG = 0x05;		// interrupt request bits
	private static final int ERROR_REG = 0x06;			// error bits showing the error status of the last command executed
	private static final int STATUS1_REG = 0x07;		// communication status bits
	private static final int STATUS2_REG = 0x08;		// receiver and transmitter status bits
	private static final int FIFO_DATA_REG = 0x09;		// input and output of 64 byte FIFO buffer
	private static final int FIFO_LEVEL_REG = 0x0A;		// number of bytes stored in the FIFO buffer
	private static final int WATER_LEVEL_REG = 0x0B;	// level for FIFO underflow and overflow warning
	private static final int CONTROL_REG = 0x0C;		// miscellaneous control registers
	private static final int BIT_FRAMING_REG = 0x0D;	// adjustments for bit-oriented frames
	private static final int COLL_REG = 0x0E;			// bit position of the first bit-collision detected on the RF interface
	//private static final int RESERVED_01 = 0x0F;
	//private static final int RESERVED_10 = 0x10;
	private static final int MODE_REG = 0x11;			// defines general modes for transmitting and receiving
	private static final int TX_MODE_REG = 0x12;		// defines transmission data rate and framing
	private static final int RX_MODE_REG = 0x13;		// defines reception data rate and framing
	private static final int TX_CONTROL_REG = 0x14;		// controls the logical behavior of the antenna driver pins TX1 and TX2
	private static final int TX_ASK_REG = 0x15;			// controls the setting of the transmission modulation
	private static final int TX_SEL_REG = 0x16;			// selects the internal sources for the antenna driver
	private static final int RX_SEL_REG = 0x17;			// selects internal receiver settings
	private static final int RX_THRESHOLD_REG = 0x18;	// selects thresholds for the bit decoder
	private static final int DEMOD_REG = 0x19;			// defines demodulator settings
	//private static final int Reserved11 = 0x1A;
	//private static final int Reserved12 = 0x1B;
	private static final int MIFARE_TX_REG = 0x1C;		// controls some MIFARE communication transmit parameters
	private static final int MIFARE_RX_REG = 0x1D;		// controls some MIFARE communication receive parameters
	//private static final int Reserved14 = 0x1E;
	private static final int SERIAL_SPEED_REG = 0x1F;	// selects the speed of the serial UART interface
	//private static final int Reserved20 = 0x20;  
	private static final int CRC_RESULT_REG_MSB = 0x21;	// shows the MSB values of the CRC calculation
	private static final int CRC_RESULT_REG_LSB = 0x22;	// shows the LSB values of the CRC calculation
	//private static final int Reserved21 = 0x23;
	private static final int MOD_WIDTH_REG = 0x24;		// controls the ModWidth setting
	//private static final int Reserved22 = 0x25;
	private static final int RF_CONFIG_REG = 0x26;		// configures the receiver gain
	private static final int GS_N_REG = 0x27;			// selects the conductance of the antenna driver pins TX1 and TX2 for modulation
	private static final int CWGsP_REG = 0x28;			// defines the conductance of the p-driver output during periods of no modulation
	private static final int ModGsP_REG = 0x29;			// defines the conductance of the p-driver output during periods of modulation
	private static final int T_MODE_REG = 0x2A;			// defines settings for the internal timer
	private static final int T_PRESCALER_REG = 0x2B;	// 
	private static final int T_RELOAD_REG_MSB = 0x2C;	// defines the 16-bit timer reload value
	private static final int T_RELOAD_REG_LSB = 0x2D;
	private static final int T_COUNTER_VALUE_REG_MSB = 0x2E;	// shows the 16-bit timer value
	private static final int T_COUNTER_VALUE_REG_LSB = 0x2F;
	//private static final int Reserved30 = 0x30;
	private static final int TEST_SEL1_REG = 0x31;		// general test signal configuration
	private static final int TEST_SEL2_REG = 0x32;		// general test signal configuration and PRBS control
	private static final int TEST_PIN_EN_REG = 0x33;	// enables pin output driver on pins D1 to D7
	private static final int TEST_PIN_VALUE_REG = 0x34;	// defines the values for D1 to D7 when it is used as an I/O bus
	private static final int TEST_BUS_REG = 0x35;		// shows the status of the internal test bus
	private static final int AUTO_TEST_REG = 0x36;		// controls the digital self test
	private static final int VERSION_REG = 0x37;		// shows the software version
	private static final int ANALOG_TEST_REG = 0x38;	// controls the pins AUX1 and AUX2
	private static final int TEST_DAC1_REG = 0x39;		// defines the test value for TestDAC1
	private static final int TEST_DAC2_REG = 0x3A;		// defines the test value for TestDAC2
	private static final int TEST_ADC_REG = 0x3B;		// shows the value of ADC I and Q channels
	//private static final int Reserved31 = 0x3C;
	//private static final int Reserved32 = 0x3D;
	//private static final int Reserved33 = 0x3E;
	//private static final int Reserved34 = 0x3F;
	
	private static final byte PCD_RCV_OFF = 1 << 5; // analog part of the receiver is switched off
	private static final byte PCD_POWER_DOWN = 1 << 4; // Soft power-down mode entered
	
	// Commands
	private static final byte PCD_IDLE			= 0b0000;	// Places the MFRC522 in Idle mode
	private static final byte PCD_MEM			= 0b0001;	// stores 25 bytes into the internal buffer
	private static final byte PCD_GEN_RANDOM_ID	= 0b0010;	// generates a 10-byte random ID number
	private static final byte PCD_CALC_CRC		= 0b0011;	// activates the CRC coprocessor or performs a self test
	private static final byte PCD_TRANSMIT		= 0b0100;	// transmits data from the FIFO buffer
	private static final byte PCD_NO_CMD_CHANGE	= 0b0111;	// no command change, can be used to modify the
															// CommandReg register bits without affecting the command, 
															// for example, the PowerDown bit
	private static final byte PCD_RECEIVE		= 0b1000;	// activates the receiver circuits
	private static final byte PCD_TRANSCEIVE	= 0b1100;	// transmits data from FIFO buffer to antenna and automatically
															// activates the receiver after transmission
	private static final byte PCD_AUTHENT		= 0b1110;	// performs the MIFARE standard authentication as a reader
	private static final byte PCD_SOFT_RESET	= 0b1111;	// resets the MFRC522
	
	// AddicoreRFID Proximity Integrated Circuit Card (PICC) Commands
	public static final byte PICC_REQIDL		= 0x26;  // search the antenna area. PCD does not enter hibernation
	public static final byte PICC_READ			= 0x30;  // reads one memory block
	public static final byte PICC_HALT			= 0x50;  // Sleep the card
	public static final byte PICC_REQALL		= 0x52;  // find all the cards in antenna area
	public static final byte PICC_AUTH_KEY_A	= 0x60;  // authentication with key A
	public static final byte PICC_AUTH_KEY_B	= 0x61;  // authentication with key B
	public static final byte PICC_ANTICOLL		= (byte) 0x93;  // anti-collision
	public static final byte PICC_SEL_CL1		= (byte) 0x93;  // Select card
	public static final byte PICC_SEL_CL2		= (byte) 0x95;  // Select card
	public static final byte PICC_SEL_CL3		= (byte) 0x97;  // Select card
	public static final byte PICC_MF_WRITE		= (byte) 0xA0;  // writes one memory block
	public static final byte PICC_MF_TRANSFER	= (byte) 0xB0;  // writes the contents of the internal data register to a block
	public static final byte PICC_MF_DECREMENT	= (byte) 0xC0;  // decrements the contents of a block and stores the result in the internal data register
	public static final byte PICC_MF_INCREMENT	= (byte) 0xC1;  // increments the contents of a block and stores the result in the internal data register
	public static final byte PICC_MF_RESTORE	= (byte) 0xC2;  // reads the contents of a block into the internal data register
	
	// AddicoreRFID error codes
	public static final byte MI_OK = 0;
	public static final byte MI_NOTAGERR = 1;
	public static final byte MI_ERR = 2;

	// Maximum length of the array
	private static final int MAX_LEN = 16;
	
	private static final int SPI_CLOCK_FREQUENCY = 1_000_000;
	
	private SpiDevice device;
	private DigitalOutputDevice resetPin;

	private boolean log;
	
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
		
		reset();
		
		// The following formula is used to calculate the timer frequency if the 
		// DEMOD_REG register’s TPrescalEven bit is set to logic 0:
		// fTimer = 13.56 MHz / (2*TPreScaler+1).
		// The following formula is used to calculate the timer frequency if the 
		// DEMOD_REG register’s TPrescalEven bit inDemoReg is set to logic 1:
		// fTimer = 13.56 MHz / (2*TPreScaler+2).

		int t_prescaler = 0;
		// 110100111110 = 3390; 13_560_000 / 6781 -> fTimer = 1999
		// total time delay = ((TPrescaler * 2 + 1) * (TReloadVal + 1)) / 13.56 MHz
		// 203430 / 13560000 = 0.015 = 0.015
		
		// Timer: TPrescaler*TReloadVal/6.78MHz = 24ms
		writeRegister(T_MODE_REG, (byte) 0x8D);			// Tauto=1; f(Timer) = 6.78MHz/TPreScaler
		writeRegister(T_PRESCALER_REG, (byte) 0x3E);	// TModeReg[3..0] + TPrescalerReg

		// 30
		writeRegister(T_RELOAD_REG_MSB, (byte) 0);
		writeRegister(T_RELOAD_REG_LSB, (byte) 0x1E);
		
		writeRegister(TX_ASK_REG, (byte) 0x40);			// 100%ASK
		writeRegister(MODE_REG, (byte) 0x3D);			// CRC Initial value 0x6363	???
		
		// TPrescaler: 000010101001 = 169;  13_560_000 / 169  -> fTimer = 80236
		// TReload: 11111101000 = 2024
		// ((169 * 2 + 1) * (2024 + 1)) / 13.56 MHz = 0.050
		/* joan:
		self._PCDWrite(self._TModeReg,	  0x80)
		self._PCDWrite(self._TPrescalerReg, 0xA9)
		self._PCDWrite(self._TReloadRegH,   0x03)
		self._PCDWrite(self._TReloadRegL,   0xe8)

		self._PCDWrite(self._TxASKReg,	  0x40)
		#self._PCDWrite(self._ModeReg,	   0x3D)
		self._PCDWrite(self._ModeReg,	   0x29)
		*/

		setAntennaOn(true);
	}
	
	private byte readRegister(int address) {
		ByteBuffer tx = ByteBuffer.allocateDirect(2);
		// Address Format: 1XXXXXX0, the first "1" indicates a read
		tx.put((byte) ((address << 1) & 0x7e | 0x80));
		tx.put((byte) 0);
		tx.flip();
		
		ByteBuffer rx = device.writeAndRead(tx);
		
		if (log) {
			System.out.format("read(0x%02x): 0x%02x, 0x%02x%n", Integer.valueOf(address), Byte.valueOf(rx.get(0)), Byte.valueOf(rx.get(1)));
		}
		
		return rx.get(1);
	}
	
	private void writeRegister(int address, byte value) {
		if (log) {
			System.out.format("write(0x%02x, 0x%02x)%n", Integer.valueOf(address), Byte.valueOf(value));
		}
		
		ByteBuffer tx = ByteBuffer.allocateDirect(2);
		// Address Format: 0XXXXXX0, the left most "0" indicates a write
		tx.put((byte) ((address << 1) & 0x7e));
		tx.put(value);
		tx.flip();
		
		device.write(tx);
	}
	
	private byte writeAndReadRegister(int address, byte value) {
		ByteBuffer tx = ByteBuffer.allocateDirect(2);
		tx.put((byte) ((address << 1) & 0x7e | 0x80));
		tx.put(value);
		tx.flip();
		
		ByteBuffer rx = device.writeAndRead(tx);
		
		return rx.get(1);
	}
	
	private void setBitMask(int address, byte mask) {
		byte current = readRegister(address);
		//Logger.debug("Current: 0x" + Integer.toHexString(current&0xff) + ", mask: 0x" + Integer.toHexString(mask&0xff));
		// Already set?
		//if ((current & mask) != mask) {
			//Logger.debug("Setting bit mask 0x" + Integer.toHexString(mask&0xff));
			writeRegister(address, (byte) (current | mask));
		//}
	}
	
	private void clearBitMask(int address, byte mask) {
		byte current = readRegister(address);
		//Logger.debug("Current: 0x" + Integer.toHexString(current&0xff) + ", mask: 0x" + Integer.toHexString(mask&0xff));
		// Already clear?
		//if ((current & mask) != 0) {
			//Logger.debug("Clearing bit mask 0x" + Integer.toHexString(mask&0xff));
			writeRegister(address, (byte) (current & ~mask));
		//}
	}
	
	/**
	 * Open antennas, each time you start or shut down the natural barrier between the transmitter should be at least 1ms interval
	 * @param on on/off value
	 */
	public void setAntennaOn(boolean on) {
		if (on) {
			byte temp = readRegister(TX_CONTROL_REG);
			if ((temp & 0x03) == 0) {
				setBitMask(TX_CONTROL_REG, (byte) 0x03);
			}
		} else {
			clearBitMask(TX_CONTROL_REG, (byte) 0x03);
		}
	}
	
	/**
	 * Perform soft reset of AddicoreRFID Module
	 */
	public void reset() {
		// Power on
		resetPin.on();
		SleepUtil.sleepMillis(100);
		
		// Soft reset
		//Logger.debug("COMMAND_REG=" + readRegister(COMMAND_REG));
		writeRegister(COMMAND_REG, PCD_SOFT_RESET);
		for (int i=0; i<5; i++) {
			//Logger.debug("COMMAND_REG=" + readRegister(COMMAND_REG));
			SleepUtil.sleepMillis(10);
		}
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
		// TxLastBits = BitFramingReg[2..0] ???
		writeRegister(BIT_FRAMING_REG, (byte) 0x07);
		
		byte[] tag_type = new byte[] {reqMode};
		Response response = toCard(PCD_TRANSCEIVE, tag_type);
		
		byte status = response.getStatus();
		if ((status != MI_OK) || (response.getBackLen() != 0x10)) {
			status = MI_ERR;
		}
		
		return new Response(status, response.getBackData());
	}

	public Response toCard(byte command, byte[] data) {
		byte irq_en = 0x00;
		byte wait_irq = 0x00;
		
		switch (command) {
		case PCD_AUTHENT:	// Certification cards close
			irq_en = 0x12;
			wait_irq = 0x10;
			break;
		case PCD_TRANSCEIVE:	// Transmit FIFO data
			irq_en = 0x77;
			wait_irq = 0x30;
			break;
		}
		
		// Interrupt request
		writeRegister(COM_INT_EN_REG, (byte) (irq_en | 0x80));
		// Clear all interrupt request bit
		clearBitMask(COM_IRQ_REG, (byte) 0x80);
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
		boolean save_log = log;
		while (true) {
			n = readRegister(COM_IRQ_REG);
			log = false;
			//Logger.debug("i={}, n=0x{}, wait_irq=0x{}", Integer.valueOf(i), Integer.toHexString(n&0xff), Integer.toHexString(wait_irq&0xff));
			i--;
			// URGH - What is this trying to achieve?!
			//if ~((i != 0) and ~(n & 0x01) and ~(n & waitIRq)):
			if ((i == 0)/* || ((n & 0x01) != 0)*/ || ((n & wait_irq) != 0)) {
				break;
			}
		}
		log = save_log;
		if (log) {
			System.out.format("i=%d, n=0x%02x%n", Integer.valueOf(i), Integer.valueOf(n & 0xff));
		}
		
		// StartSend=0
		clearBitMask(BIT_FRAMING_REG, (byte) 0x80);
		
		byte[] back_data = new byte[0];
		
		byte status = MI_ERR;
		int back_len = 0;
		if (i != 0) {
			// BufferOvfl Collerr CRCErr ProtecolErr
			byte error_reg_val = readRegister(ERROR_REG);
			if (log) {
				Logger.debug("error_reg_val=0x{}", Integer.toHexString(error_reg_val&0xff));
			}
			if ((error_reg_val & 0x1B) == 0x00) {
				if (log) {
					Logger.debug("error reg status ok");
				}
				status = MI_OK;

				if (log) {
					Logger.debug("n=0x{}, irq_en=0x{}", Integer.toHexString(n&0xff), Integer.toHexString(irq_en&0xff));
				}
				if ((n & irq_en & 0x01) != 0) {
					status = MI_NOTAGERR;
				}
				
				if (command == PCD_TRANSCEIVE) {
					n = readRegister(FIFO_LEVEL_REG);
					if (log) {
						Logger.debug("FIFO_LEVEL_REG=0x{}", Integer.toHexString(n&0xff));
					}
					// Indicates the number of valid bits in the last received byte
					// If this value is 000b, the whole byte is valid
					int rx_last_bits = readRegister(CONTROL_REG) & 0b0111;
					if (log) {
						Logger.debug("rx_last_bits=" + rx_last_bits);
					}
					if (rx_last_bits != 0) {
						back_len = (n-1)*8 + rx_last_bits;
					} else {
						back_len = n*8;
					}
					if (log) {
						Logger.debug("back_bits=" + back_len);
					}
					
					if (n == 0) {
						n = 1;
					}
					if (n > MAX_LEN) {
						n = MAX_LEN;
					}
					
					back_data = new byte[n];
					for (int x=0; x<n; x++) {
						back_data[x] = readRegister(FIFO_DATA_REG);
						if (log) {
							Logger.debug("back_data[{}]=0x{}", Integer.valueOf(x), Integer.toHexString(back_data[x]&0xff));
						}
					}
				}
			} else {
				if (log) {
					Logger.debug("error reg status NOT ok");
				}
				status = MI_ERR;
			}
		}

		return new Response(status, back_data, back_len);
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
		writeRegister(COMMAND_REG, PCD_CALC_CRC);
		
		// Wait CRC calculation is complete
		int i = 0xFF;
		byte n;
		do {
			n = readRegister(DIV_IRQ_REG);
			i--;
		} while ((i != 0) && (n & 0x04) == 0);	// CRCIrq = 1
		//if not ((i != 0) and not (n&0x04)):
		//	break
		//if (i == 0) || (n&0x04) != 0:
		//	break
			
		byte[] p_out_data = new byte[2];
		p_out_data[0] = readRegister(CRC_RESULT_REG_LSB);
		p_out_data[1] = readRegister(CRC_RESULT_REG_MSB);
		
		return p_out_data;
	}
	
	public int selectTag(byte[] serNum) {
		byte[] buffer = new byte[7];
		int pos = 0;
		buffer[pos++] = PICC_SEL_CL1;
		buffer[pos++] = 0x70;
		for (int i=0; i<5; i++) {
			buffer[pos++] = serNum[i];
		}

		byte[] p_out = calculateCrc(buffer);
		byte[] buffer2 = new byte[9];
		System.arraycopy(buffer, 0, buffer2, 0, buffer.length);
		buffer = buffer2;
		buffer[pos++] = p_out[0];
		buffer[pos++] = p_out[1];
		Logger.info(Integer.valueOf(pos));
		Response response = toCard(PCD_TRANSCEIVE, buffer);
		
		byte size;
		Logger.info(response);
		if ((response.getStatus() == MI_OK) && (response.getBackLen() == 0x18)) {
			size = response.getBackData()[0];
			Logger.debug("Size: {}", Byte.valueOf(size));
		} else {
			Logger.debug("Setting size to 0, error?");
			size = 0;
		}
	
		return size;
	}
	
	public byte authenticate(byte authMode, byte blockAddr, byte[] sectorKey, byte[] serNum) {
		byte[] buff = new byte[12];
	
		int pos = 0;
		// Verify the command block address + sector + password + card serial number
		// First byte should be the authMode (A or B)
		buff[pos++] = authMode;
		// Second byte is the trailerBlock (usually 7)
		buff[pos++] = blockAddr;
		// Now we need to append the authKey which usually is 6 bytes of 0xFF
		int i;
		for (i=0; i<sectorKey.length; i++) {
			buff[pos++] = sectorKey[i];
		}
		// Next we append the first 4 bytes of the UID
		for (i=0; i<4; i++) {
			buff[pos++] = serNum[i];
		}
		Logger.info("pos=" + pos);
		Response response = toCard(PCD_AUTHENT, buff);

		byte status = response.getStatus();
		if (status != MI_OK) {
			Logger.error("AUTH ERROR!!");
		}
		
		byte status2_reg = readRegister(STATUS2_REG);
		//if not (self.Read_MFRC522(self.Status2Reg) & 0x08) != 0:
		if ((status2_reg & 0x08) == 0) {
			Logger.error("AUTH ERROR(status2reg & 0x08) != 0, status2_reg=0x{}", Integer.toString(status2_reg));
		}
		
		return status;
	}
	
	public void stopCrypto1() {
		clearBitMask(STATUS2_REG, (byte) 0x08);
	}

	public void read(byte blockAddr) {
		byte[] recv_data = { PICC_READ, blockAddr };
		byte[] p_out = calculateCrc(recv_data);
		recv_data = new byte[] { recv_data[0], recv_data[1], p_out[0], p_out[1] };
		
		Response response = toCard(PCD_TRANSCEIVE, recv_data);
		Logger.info(response);
		if (response.getStatus() != MI_OK) {
			Logger.error("Error in read");
		}
		if (response.getBackData().length == 16) {
			Logger.info("Sector 0x{}: {}", Integer.toHexString(blockAddr), Arrays.toString(response.getBackData()));
		}
	}

	@Override
	public void close() {
		if (device != null) {
			device.close();
		}
	}
	
	public void setLog(boolean log) {
		this.log = log;
	}
	
	public static class Response {
		private byte status;
		private byte[] backData;
		private int backLen;
		
		public Response(byte status, byte[] backData) {
			this.status = status;
			this.backData = backData;
		}
		
		public Response(byte status, byte[] backData, int backLen) {
			this.status = status;
			this.backData = backData;
			this.backLen = backLen;
		}

		public byte getStatus() {
			return status;
		}

		public byte[] getBackData() {
			return backData;
		}
		
		public int getBackLen() {
			return backLen;
		}
		
		@Override
		public String toString() {
			return "Response [status=" + status + ", backData=" + Arrays.toString(backData) + ", backLen=" + backLen
					+ "]";
		}
	}
}
