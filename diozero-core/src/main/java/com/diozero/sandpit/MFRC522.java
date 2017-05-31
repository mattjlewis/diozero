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
	private static final byte PCD_AUTHENTICATE	= 0b1110;	// performs the MIFARE standard authentication as a reader
	private static final byte PCD_SOFT_RESET	= 0b1111;	// resets the MFRC522
	
	
	// AddicoreRFID error codes
	public static final byte MI_OK = 0;
	public static final byte MI_NOTAGERR = 1;
	public static final byte MI_ERR = 2;
	
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
		
		init();
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	// Basic interface functions for communicating with the MFRC522
	/////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * TODO Warning - NOT TESTED
	 * @param address
	 * @param length
	 * @return
	 */
	private byte[] readRegister(int address, int length) {
		ByteBuffer tx = ByteBuffer.allocateDirect(length+1);
		tx.put((byte) ((address << 1) & 0x7e | 0x80));
		for (int i=0; i<length; i++) {
			tx.put((byte) 0);
		}
		tx.flip();
		
		ByteBuffer rx = device.writeAndRead(tx);
		byte[] buffer = new byte[length];
		rx.get(buffer, 1, length);
		
		return buffer;
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
	
	private void writeRegister(int address, byte[] values) {
		ByteBuffer tx = ByteBuffer.allocateDirect(values.length+1);
		// Address Format: 0XXXXXX0, the left most "0" indicates a write
		tx.put((byte) ((address << 1) & 0x7e));
		tx.put(values);
		tx.flip();
		
		device.write(tx);
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
	
	private byte[] calculateCRC(byte[] data) {
		writeRegister(COMMAND_REG, PCD_IDLE);		// Stop any active command.
		writeRegister(DIV_IRQ_REG, (byte) 0x04);	// Clear the CRCIRq interrupt request bit
		setBitMask(FIFO_LEVEL_REG, (byte) 0x80);	// FlushBuffer = 1, FIFO initialization
		writeRegister(FIFO_DATA_REG, data);			// Write data to the FIFO
		writeRegister(COMMAND_REG, PCD_CALC_CRC);	// Start the calculation
		/*
		for (byte b : data) {
			writeRegister(FIFO_DATA_REG, b);
		}
		writeRegister(COMMAND_REG, PCD_CALC_CRC);
		*/
		
		// Wait for the CRC calculation to complete. Each iteration of the while-loop takes 17.73us.
		int i;
		int MAX_ITERATIONS = 5000;
		for (i=0; i<MAX_ITERATIONS; i++) {
			// DivIrqReg[7..0] bits are: Set2 reserved reserved MfinActIRq reserved CRCIRq reserved reserved
			byte n = readRegister(DIV_IRQ_REG);
			if ((n & 0x04) != 0) {						// CRCIRq bit set - calculation done
				break;
			}
		}
		// The emergency break. We will eventually terminate on this one after 89ms. Communication with the MFRC522 might be down.
		if (i == MAX_ITERATIONS) {
			return null;
		}
		writeRegister(COMMAND_REG, PCD_IDLE);		// Stop calculating CRC for new content in the FIFO.
		
		// Transfer the result from the registers to the result buffer
		byte[] result = new byte[2];
		result[0] = readRegister(CRC_RESULT_REG_LSB);
		result[1] = readRegister(CRC_RESULT_REG_MSB);
		
		return result;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	// Functions for manipulating the MFRC522
	/////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Initializes the MFRC522 chip.
	 */
	private void init() {
		if (! resetPin.isOn()) {
			// Exit power down mode. This triggers a hard reset.
			resetPin.on();
			// Section 8.8.2 in the datasheet says the oscillator start-up time is the start up time of the crystal + 37,74us. Let us be generous: 50ms.
			SleepUtil.sleepMillis(50);
		} else {
			reset();
		}
		
		// The following formula is used to calculate the timer frequency if the 
		// DEMOD_REG register’s TPrescalEven bit is set to logic 0:
		// fTimer = 13.56 MHz / (2*TPreScaler+1).
		// The following formula is used to calculate the timer frequency if the 
		// DEMOD_REG register’s TPrescalEven bit inDemoReg is set to logic 1:
		// fTimer = 13.56 MHz / (2*TPreScaler+2).
		
		/*
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
		// joan:
		//self._PCDWrite(self._TModeReg,	  0x80)
		//self._PCDWrite(self._TPrescalerReg, 0xA9)
		//self._PCDWrite(self._TReloadRegH,   0x03)
		//self._PCDWrite(self._TReloadRegL,   0xe8)

		//self._PCDWrite(self._TxASKReg,	  0x40)
		//#self._PCDWrite(self._ModeReg,	   0x3D)
		//self._PCDWrite(self._ModeReg,	   0x29)

		setAntennaOn(true);
		*/


		// When communicating with a PICC we need a timeout if something goes wrong.
		// f_timer = 13.56 MHz / (2*TPreScaler+1) where TPreScaler = [TPrescaler_Hi:TPrescaler_Lo].
		// TPrescaler_Hi are the four low bits in TModeReg. TPrescaler_Lo is TPrescalerReg.
		writeRegister(T_MODE_REG, (byte) 0x80);			// TAuto=1; timer starts automatically at the end of the transmission in all communication modes at all speeds
		writeRegister(T_PRESCALER_REG, (byte) 0xA9);	// TPreScaler = TModeReg[3..0]:TPrescalerReg, ie 0x0A9 = 169 => f_timer=40kHz, ie a timer period of 25us.
		writeRegister(T_RELOAD_REG_MSB, (byte) 0x03);	// Reload timer with 0x03E8 = 1000, ie 25ms before timeout.
		writeRegister(T_RELOAD_REG_LSB, (byte) 0xE8);
		
		writeRegister(TX_ASK_REG, (byte) 0x40);			// Default 0x00. Force a 100 % ASK modulation independent of the ModGsPReg register setting
		writeRegister(MODE_REG, (byte) 0x3D);			// Default 0x3F. Set the preset value for the CRC coprocessor for the CalcCRC command to 0x6363 (ISO 14443-3 part 6.2.4)
		setAntennaOn(true);								// Enable the antenna driver pins TX1 and TX2 (they were disabled by the reset)
	}

	/**
	 * Perform soft reset of AddicoreRFID Module
	 */
	private void reset() {
		writeRegister(COMMAND_REG, PCD_SOFT_RESET);
		// The datasheet does not mention how long the SoftRest command takes to complete.
		// But the MFRC522 might have been in soft power-down mode (triggered by bit 4 of CommandReg) 
		// Section 8.8.2 in the datasheet says the oscillator start-up time is the start up time of the crystal + 37,74us. Let us be generous: 50ms.
		SleepUtil.sleepMillis(50);
		while ((readRegister(COMMAND_REG) & (1<<4)) != 0) {
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
			byte value = readRegister(TX_CONTROL_REG);
			if ((value & 0x03) != 0x03) {
				setBitMask(TX_CONTROL_REG, (byte) (value | 0x03));
			}
		} else {
			clearBitMask(TX_CONTROL_REG, (byte) 0x03);
		}
	}
	
	/**
	 * Get the current MFRC522 Receiver Gain (RxGain[2:0]) value.
	 * See 9.3.3.6 / table 98 in http://www.nxp.com/documents/data_sheet/MFRC522.pdf
	 * NOTE: Return value scrubbed with (0x07<<4)=01110000b as RCFfgReg may use reserved bits.
	 * 
	 * @return Value of the RxGain, scrubbed to the 3 bits used.
	 */
	public int getAntennaGain() {
		return readRegister(RF_CONFIG_REG) & (0x07<<4);
	}
	
	/**
	 * Set the MFRC522 Receiver Gain (RxGain) to value specified by given mask.
	 * See 9.3.3.6 / table 98 in http://www.nxp.com/documents/data_sheet/MFRC522.pdf
	 * NOTE: Given mask is scrubbed with (0x07<<4)=01110000b as RCFfgReg may use reserved bits.
	 * @param mask New antenna gain value
	 */
	public void setAntennaGain(byte mask) {
		if (getAntennaGain() != mask) {								// only bother if there is a change
			clearBitMask(RF_CONFIG_REG, (byte) (0x07<<4));			// clear needed to allow 000 pattern
			setBitMask(RF_CONFIG_REG, (byte) (mask & (0x07<<4)));	// only set RxGain[2:0] bits
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	// Functions for communicating with PICCs
	/////////////////////////////////////////////////////////////////////////////////////
	
	private Response communicateWithPICC(PcdCommand command, byte[] sendData) {
		return communicateWithPICC(command, sendData, (byte) 0, (byte) 0, false);
	}
	
	private Response communicateWithPICC(PcdCommand command, byte[] sendData, byte validBits) {
		return communicateWithPICC(command, sendData, validBits, (byte) 0, false);
	}
	
	private Response communicateWithPICC(PcdCommand command, byte[] sendData, byte validBits, byte rxAlign) {
		return communicateWithPICC(command, sendData, validBits, rxAlign, false);
	}

	/**
	 * 
	 * @param command The command to execute. One of the PCD_Command enums.
	 * @param sendData Data to transfer to the FIFO.
	 * @param validBits The number of valid bits in the last byte. 0 for 8 valid bits.
	 * @param rxAlign Defines the bit position in backData[0] for the first bit received. Default 0.
	 * @param checkCRC True => The last two bytes of the response is assumed to be a CRC_A that must be validated.
	 * @return
	 */
	private Response communicateWithPICC(PcdCommand command, byte[] sendData, byte validBits, byte rxAlign,
			boolean checkCRC) {
		byte wait_irq;
		
		switch (command) {
		case AUTHENTICATE:	// Certification cards close
			wait_irq = 0x10;	// IdleIRq
			break;
		case TRANSCEIVE:	// Transmit FIFO data
			wait_irq = 0x30;	// RxIRq and IdleIRq
			break;
		default:
			throw new IllegalArgumentException("Unsupported command " + command);
		}
		
		// Prepare values for BitFramingReg
		byte tx_last_bits = validBits;
		byte bit_framing = (byte) ((rxAlign << 4) + tx_last_bits);	// RxAlign = BitFramingReg[6..4]. TxLastBits = BitFramingReg[2..0]
		
		writeRegister(COMMAND_REG, PCD_IDLE);			// Stop any active command.
		writeRegister(COM_IRQ_REG, (byte) 0x7f);		// Clear all seven interrupt request bits
		setBitMask(FIFO_LEVEL_REG, (byte) 0x80);		// FlushBuffer=1, FIFO Initialisation
		writeRegister(FIFO_DATA_REG, sendData);			// Write sendData to the FIFO
		writeRegister(BIT_FRAMING_REG, bit_framing);	// Bit adjustments
		writeRegister(COMMAND_REG, command.getValue());	// Execute the command
		if (command == PcdCommand.TRANSCEIVE) {
			setBitMask(BIT_FRAMING_REG, (byte) 0x80);	// StartSend=1, transmission of data starts
		}
		
		// Wait for the command to complete.
		// In PCD_Init() we set the TAuto flag in TModeReg. This means the timer automatically starts when the PCD stops transmitting.
		// Each iteration of the do-while-loop takes 17.86us.
		// FIXME Review the above timings
		boolean save_log = log;
		int MAX_ITERATIONS = 2000;
		int i;
		for (i=0; i<MAX_ITERATIONS; i++) {
			byte n = readRegister(COM_IRQ_REG);
			log = false;
			//Logger.debug("i={}, n=0x{}, wait_irq=0x{}", Integer.valueOf(i), Integer.toHexString(n&0xff), Integer.toHexString(wait_irq&0xff));
			// URGH - What is this trying to achieve?!
			//if ~((i != 0) and ~(n & 0x01) and ~(n & waitIRq)):
			if ((n & wait_irq) != 0) {
				// One of the interrupts that signal success has been set.
				break;
			}
			if ((n & 0x01) != 0) {
				// Timer interrupt - nothing received in 25ms
				return new Response(StatusCode.TIMEOUT);
			}
		}
		if (i == MAX_ITERATIONS) {
			// The emergency break. If all other conditions fail we will eventually terminate on this one after 35.7ms. Communication with the MFRC522 might be down.
			return new Response(StatusCode.TIMEOUT);
		}
		log = save_log;
		
		// StartSend=0
		//clearBitMask(BIT_FRAMING_REG, (byte) 0x80);
	
		// Stop now if any errors except collisions were detected.
		// ErrorReg[7..0] bits are: WrErr TempErr reserved BufferOvfl CollErr CRCErr ParityErr ProtocolErr
		byte error_reg_val = readRegister(ERROR_REG);
		// BufferOvfl ParityErr ProtocolErr
		if ((error_reg_val & 0x13) != 0) {
			return new Response(StatusCode.ERROR);
		}
		
		int back_len = 0;
		int valid_bits = 0;
		
		// If the caller wants data back, get it from the MFRC522.
		byte[] back_data = new byte[0];
		if (command == PcdCommand.TRANSCEIVE) {
			// Number of bytes in the FIFO
			back_len = readRegister(FIFO_LEVEL_REG) & 0xff;
			// Get received data from FIFO
			back_data = new byte[back_len];
			for (int index=0; index<back_len; index++) {
				back_data[index] = readRegister(FIFO_DATA_REG);
			}
			
			// RxLastBits[2:0] indicates the number of valid bits in the last received byte.
			// If this value is 000b, the whole byte is valid.
			valid_bits = readRegister(CONTROL_REG) & 0x07;
		}
		
		// Tell about collisions
		if ((error_reg_val & 0x08) != 0) {		// CollErr
			return new Response(StatusCode.COLLISION);
		}
		
		if (back_len > 0 && checkCRC) {
			// In this case a MIFARE Classic NAK is not OK.
			if (back_len == 1 && valid_bits == 4) {
				return new Response(StatusCode.MIFARE_NACK);
			}
			// We need at least the CRC_A value and all 8 bits of the last byte must be received.
			if (back_len < 2 || valid_bits != 0) {
				return new Response(StatusCode.CRC_WRONG);
			}
			// Verify CRC_A - do our own calculation and store the control in controlBuffer.
			byte[] data = new byte[2];
			System.arraycopy(back_data, back_data.length-2, data, 0, 2);
			byte[] control_buffer = calculateCRC(data);
			if (control_buffer == null) {
				return new Response(StatusCode.TIMEOUT);
			}
			
			if ((data[0] != control_buffer[0]) || (data[1] != control_buffer[1])) {
				return new Response(StatusCode.CRC_WRONG);
			}
		}
	
		return new Response(StatusCode.OK, back_data, back_len, valid_bits);
	}
	
	/**
	 * Transmits a REQuest command, Type A. Invites PICCs in state IDLE to go to READY and prepare for anticollision or selection. 7 bit frame.
	 * Beware: When two PICCs are in the field at the same time I often get STATUS_TIMEOUT - probably due do bad antenna design.
	 * 
	 * @param bufferATQA The buffer to store the ATQA (Answer to request) in
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	public StatusCode PICC_requestA(byte[] bufferATQA) {
		return PICC_REQA_or_WUPA(PiccCommand.REQUEST_A, bufferATQA);
	}
	
	/**
	 * Transmits a Wake-UP command, Type A. Invites PICCs in state IDLE and HALT to go to READY(*) and prepare for anticollision or selection. 7 bit frame.
	 * Beware: When two PICCs are in the field at the same time I often get STATUS_TIMEOUT - probably due do bad antenna design.
	 * 
	 * @param bufferATQA The buffer to store the ATQA (Answer to request) in
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	public StatusCode PICC_WakeupA(byte[] bufferATQA) {
		return PICC_REQA_or_WUPA(PiccCommand.WAKE_UP_A, bufferATQA);
	}
	
	/**
	 * Transmits REQA or WUPA commands.
	 * Beware: When two PICCs are in the field at the same time I often get STATUS_TIMEOUT - probably due do bad antenna design.
	 * 
	 * @param command The command to send - PICC_CMD_REQA or PICC_CMD_WUPA
	 * @param bufferATQA The buffer to store the ATQA (Answer to request) in
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */ 
	public StatusCode PICC_REQA_or_WUPA(PiccCommand command, byte[] bufferATQA) {
		if (bufferATQA == null || bufferATQA.length < 2) {	// The ATQA response is 2 bytes long.
			return StatusCode.NO_ROOM;
		}
		clearBitMask(COLL_REG, (byte) 0x80);		// ValuesAfterColl=1 => Bits received after collision are cleared.
		byte valid_bits = 7;						// For REQA and WUPA we need the short frame format - transmit only 7 bits of the last (and only) byte. TxLastBits = BitFramingReg[2..0]
		Response response = communicateWithPICC(PcdCommand.TRANSCEIVE,
				new byte[] { command.getValue() }, valid_bits, (byte) 0, false);
		if (response.getStatus() != StatusCode.OK) {
			return response.getStatus();
		}
		
		if (response.getBackData().length != 2 || response.getValidBits() != 0) {		// ATQA must be exactly 16 bits.
			return StatusCode.ERROR;
		}
		
		return StatusCode.OK;
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
	 * @param uid Pointer to Uid struct. Normally output, but can also be used to supply a known UID.
	 * @param validBits The number of known UID bits supplied in *uid. Normally 0. If set you must also supply uid->size.
	 * @return STATUS_OK on success, STATUS_??? otherwise.
	 */
	public StatusCode PICC_Select(UID uid, byte validBits) {
		byte[] buffer = new byte[7];	// The SELECT/ANTICOLLISION commands uses a 7 byte standard frame + 2 bytes CRC_A
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
			return StatusCode.INVALID;
		}
		
		// Prepare MFRC522
		clearBitMask(COLL_REG, (byte) 0x80);		// ValuesAfterColl=1 => Bits received after collision are cleared.
		
		// Repeat Cascade Level loop until we have a complete UID.
		boolean uid_complete = false;
		boolean use_cascade_tag;
		byte cascade_level = 1;
		byte uid_index;					// The first index in uid->uidByte[] that is used in the current Cascade Level.
		int current_level_known_bits;	// The number of known UID bits in the current Cascade Level.
		byte[] response_buffer = {};
		while (!uid_complete) {
			// Set the Cascade Level in the SEL byte, find out if we need to use the Cascade Tag in byte 2.
			switch (cascade_level) {
			case 1:
				buffer[0] = PiccCommand.SEL_CL1.getValue();
				uid_index = 0;
				use_cascade_tag = validBits != 0 && uid.getSize() > 4;	// When we know that the UID has more than 4 bytes
				break;
			
			case 2:
				buffer[0] = PiccCommand.SEL_CL2.getValue();
				uid_index = 3;
				use_cascade_tag = validBits != 0 && uid.getSize() > 7;	// When we know that the UID has more than 7 bytes
				break;
			
			case 3:
				buffer[0] = PiccCommand.SEL_CL3.getValue();
				uid_index = 6;
				use_cascade_tag = false;						// Never used in CL3.
				break;
			
			default:
				return StatusCode.INTERNAL_ERROR;
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
			int bytesToCopy = current_level_known_bits / 8 + ((current_level_known_bits % 8) != 0 ? 1 : 0); // The number of bytes needed to represent the known bits for this level.
			if (bytesToCopy != 0) {
				int maxBytes = use_cascade_tag ? 3 : 4; // Max 4 bytes in each Cascade Level. Only 3 left if we use the Cascade Tag
				if (bytesToCopy > maxBytes) {
					bytesToCopy = maxBytes;
				}
				for (int count = 0; count < bytesToCopy; count++) {
					buffer[index++] = uid.getUidByte(uid_index + count);
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
			while (!select_done) {
				// Find out how many bits and bytes to send and receive.
				if (current_level_known_bits >= 32) { // All UID bits in this Cascade Level are known. This is a SELECT.
					//Serial.print(F("SELECT: currentLevelKnownBits=")); Serial.println(currentLevelKnownBits, DEC);
					buffer[1] = 0x70; // NVB - Number of Valid Bits: Seven whole bytes
					// Calculate BCC - Block Check Character
					buffer[6] = (byte) (buffer[2] ^ buffer[3] ^ buffer[4] ^ buffer[5]);
					// Calculate CRC_A
					byte[] crc = calculateCRC(buffer);
					if (crc == null) {
						return StatusCode.CRC_WRONG;
					}
					tx_last_bits		= 0; // 0 => All 8 bits are valid.
					buffer_used		= 9;
					// Store response in the last 3 bytes of buffer (BCC and CRC_A - not needed after tx)
					//responseBuffer	= &buffer[6];
					response_length	= 3;
					response_buffer = new byte[response_length];
					for (int i=0; i<response_length; i++) {
						response_buffer[i] = buffer[6+i];
					}
				} else { // This is an ANTICOLLISION.
					//Logger.info("ANTICOLLISION: currentLevelKnownBits={}", currentLevelKnownBits);
					tx_last_bits	= current_level_known_bits % 8;
					int count		= current_level_known_bits / 8;			// Number of whole bytes in the UID part.
					index			= 2 + count;							// Number of whole bytes: SEL + NVB + UIDs
					buffer[1]		= (byte) ((index << 4) + tx_last_bits);	// NVB - Number of Valid Bits
					buffer_used		= index + (tx_last_bits != 0 ? 1 : 0);
					// Store response in the unused part of buffer
					//responseBuffer	= &buffer[index];
					response_length	= buffer.length - index;
					response_buffer = new byte[response_length];
					for (int i=0; i<response_length; i++) {
						response_buffer[i] = buffer[index+i];
					}
				}
				
				// Set bit adjustments
				int rx_align = tx_last_bits;													// Having a separate variable is overkill. But it makes the next line easier to read.
				writeRegister(BIT_FRAMING_REG, (byte) ((rx_align << 4) + tx_last_bits));	// RxAlign = BitFramingReg[6..4]. TxLastBits = BitFramingReg[2..0]
				
				// Transmit the buffer and receive the response.
				// TODO Size buffer appropriately
				Response response = communicateWithPICC(PcdCommand.TRANSCEIVE, buffer,
						(byte) tx_last_bits, (byte) rx_align);
				//result = PCD_TransceiveData(buffer, bufferUsed, responseBuffer, &responseLength, &txLastBits, rxAlign);
				//PCD_TransceiveData(sendData, sendLen, backData, backLen, validBits, rxAlign, checkCRC);
				// TODO Map the response values back to the variables
				response_buffer = response.getBackData();
				response_length = response.getBackLen();
				tx_last_bits = response.getValidBits();
				if (response.getStatus() == StatusCode.COLLISION) { // More than one PICC in the field => collision.
					byte valueOfCollReg = readRegister(COLL_REG); // CollReg[7..0] bits are: ValuesAfterColl reserved CollPosNotValid CollPos[4:0]
					if ((valueOfCollReg & 0x20) != 0) { // CollPosNotValid
						return StatusCode.COLLISION; // Without a valid collision position we cannot continue
					}
					int collisionPos = valueOfCollReg & 0x1F; // Values 0-31, 0 means bit 32.
					if (collisionPos == 0) {
						collisionPos = 32;
					}
					if (collisionPos <= current_level_known_bits) { // No progress - should not happen 
						return StatusCode.INTERNAL_ERROR;
					}
					// Choose the PICC with the bit set.
					current_level_known_bits = collisionPos;
					int count		= (current_level_known_bits - 1) % 8; // The bit to modify
					index			= 1 + (current_level_known_bits / 8) + (count != 0 ? 1 : 0); // First byte is index 0.
					buffer[index]	|= (1 << count);
				} else if (response.getStatus() != StatusCode.OK) {
					return response.getStatus();
				} else { // STATUS_OK
					if (current_level_known_bits >= 32) { // This was a SELECT.
						select_done = true; // No more anticollision 
						// We continue below outside the while.
					}
					else { // This was an ANTICOLLISION.
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
			for (int count = 0; count < bytesToCopy; count++) {
				uid.setUidByte(uid_index + count, buffer[index++]);
			}
			
			// Check response SAK (Select Acknowledge)
			if (response_length != 3 || tx_last_bits != 0) { // SAK must be exactly 24 bits (1 byte + CRC_A).
				return StatusCode.ERROR;
			}
			// Verify CRC_A - do our own calculation and store the control in buffer[2..3] - those bytes are not needed anymore.
			byte[] crc = calculateCRC(new byte[] { response_buffer[0] });
			if (crc == null) {
				return StatusCode.CRC_WRONG;
			}
			buffer[2] = crc[0];
			buffer[3] = crc[1];
			if ((buffer[2] != response_buffer[1]) || (buffer[3] != response_buffer[2])) {
				return StatusCode.CRC_WRONG;
			}
			if ((response_buffer[0] & 0x04) != 0) { // Cascade bit set - UID not complete yes
				cascade_level++;
			} else {
				uid_complete = true;
				uid.setSak(response_buffer[0]);
			}
		} // End of while (!uidComplete)
		
		// Set correct uid->size
		uid.setSize(3 * cascade_level + 1);
		
		return StatusCode.OK;
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
		Response response = communicateWithPICC(PcdCommand.TRANSCEIVE, tag_type);
		
		StatusCode status = response.getStatus();
		if ((status != StatusCode.OK) || (response.getBackLen() != 0x10)) {
			status = StatusCode.ERROR;
		}
		
		return new Response(status, response.getBackData());
	}

	public Response anticoll() {
		writeRegister(BIT_FRAMING_REG, (byte) 0x00);
		
		byte[] ser_num = new byte[2];
		ser_num[0] = PiccCommand.SEL_CL1.getValue();
		ser_num[1] = 0x20;
		
		Response response = communicateWithPICC(PcdCommand.TRANSCEIVE, ser_num);
		ser_num = response.getBackData();
		byte ser_num_check=0;
		StatusCode status = response.getStatus();
		if (status == StatusCode.OK) {
			// Check card serial number
			int i;
			for (i=0; i<4; i++) {
				ser_num_check ^= ser_num[i];
			}
			if (ser_num_check != ser_num[i]) {
				status = StatusCode.ERROR;
			}
		}

		return new Response(status, response.getBackData());
	}
	
	public int selectTag(byte[] serNum) {
		byte[] buffer = new byte[7];
		int pos = 0;
		buffer[pos++] = PiccCommand.SEL_CL1.getValue();
		buffer[pos++] = 0x70;
		for (int i=0; i<5; i++) {
			buffer[pos++] = serNum[i];
		}

		byte[] p_out = calculateCRC(buffer);
		byte[] buffer2 = new byte[9];
		System.arraycopy(buffer, 0, buffer2, 0, buffer.length);
		buffer = buffer2;
		buffer[pos++] = p_out[0];
		buffer[pos++] = p_out[1];
		Logger.info(Integer.valueOf(pos));
		Response response = communicateWithPICC(PcdCommand.TRANSCEIVE, buffer);
		
		byte size;
		Logger.info(response);
		if ((response.getStatus() == StatusCode.OK) && (response.getBackLen() == 0x18)) {
			size = response.getBackData()[0];
			Logger.debug("Size: {}", Byte.valueOf(size));
		} else {
			Logger.debug("Setting size to 0, error?");
			size = 0;
		}
	
		return size;
	}
	
	public StatusCode authenticate(byte authMode, byte blockAddr, byte[] sectorKey, byte[] serNum) {
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
		Response response = communicateWithPICC(PcdCommand.AUTHENTICATE, buff);

		StatusCode status = response.getStatus();
		if (status != StatusCode.OK) {
			Logger.error("AUTH ERROR!!");
		}
		
		byte status2_reg = readRegister(STATUS2_REG);
		//if not (self.Read_MFRC522(self.Status2Reg) & 0x08) != 0:
		if ((status2_reg & 0x08) == 0) {
			Logger.error("AUTH ERROR(status2reg & 0x08) != 0, status2_reg=0x{}", Integer.toHexString(status2_reg));
		}
		
		return status;
	}
	
	public void stopCrypto1() {
		clearBitMask(STATUS2_REG, (byte) 0x08);
	}

	public void read(byte blockAddr) {
		byte[] recv_data = { PiccCommand.MF_READ.getValue(), blockAddr };
		byte[] p_out = calculateCRC(recv_data);
		recv_data = new byte[] { recv_data[0], recv_data[1], p_out[0], p_out[1] };
		
		Response response = communicateWithPICC(PcdCommand.TRANSCEIVE, recv_data);
		Logger.info(response);
		if (response.getStatus() != StatusCode.OK) {
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
		private StatusCode status;
		private byte[] backData;
		private int backLen;
		private int validBits;
		
		public Response(StatusCode status) {
			this.status = status;
		}
		
		public Response(StatusCode status, byte[] backData) {
			this.status = status;
			this.backData = backData;
		}
		
		public Response(StatusCode status, byte[] backData, int backLen, int validBits) {
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
		
		public int getValidBits() {
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
	
	public static enum PcdCommand {
		TRANSCEIVE(MFRC522.PCD_TRANSCEIVE), AUTHENTICATE(MFRC522.PCD_AUTHENTICATE);
		
		private byte value;
		
		private PcdCommand(byte value) {
			this.value = value;
		}
		
		public byte getValue() {
			return value;
		}
	}
	
	// AddicoreRFID Proximity Integrated Circuit Card (PICC) Commands
	public static enum PiccCommand {
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
		private int size;
		private byte[] uidBytes;
		// The SAK (Select acknowledge) byte returned from the PICC after successful selection.
		private byte sak;
		
		public UID() {
			uidBytes = new byte[10];
		}
		
		public int getSize() {
			return size;
		}
		
		public void setSize(int size) {
			this.size = size;
		}
		
		public byte getUidByte(int index) {
			return uidBytes[index];
		}
		
		public void setUidByte(int index, byte value) {
			uidBytes[index] = value;
		}
		
		public byte getSak() {
			return sak;
		}
		
		public void setSak(byte sak) {
			this.sak = sak;
		}
	}
}
