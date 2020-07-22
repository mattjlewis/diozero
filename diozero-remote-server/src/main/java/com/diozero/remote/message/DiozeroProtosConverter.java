package com.diozero.remote.message;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Server
 * Filename:     DiozeroProtosConverter.java  
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

import java.util.ArrayList;
import java.util.List;

import org.pmw.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.SpiClockMode;
import com.diozero.remote.message.protobuf.DiozeroProtos;
import com.google.protobuf.ByteString;

public class DiozeroProtosConverter {
	public static Response convert(DiozeroProtos.Response obj) {
		return new Response(convert(obj.getStatus()), obj.getDetail(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Response convert(Response obj) {
		return DiozeroProtos.Response.newBuilder().setStatus(convert(obj.getStatus())).setDetail(obj.getDetail())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static GetBoardInfo convert(DiozeroProtos.Gpio.GetBoardInfo obj) {
		return new GetBoardInfo(obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.GetBoardInfo convert(GetBoardInfo obj) {
		return DiozeroProtos.Gpio.GetBoardInfo.newBuilder().setCorrelationId(obj.getCorrelationId()).build();
	}

	public static GetBoardInfoResponse convert(DiozeroProtos.Gpio.GetBoardInfoResponse obj) {
		List<GpioInfo> gpios = new ArrayList<>();
		for (DiozeroProtos.Gpio.GpioInfo gpio_info : obj.getGpioInfoList()) {
			gpios.add(convert(gpio_info));
		}
		return new GetBoardInfoResponse(convert(obj.getStatus()), obj.getDetail(), obj.getMake(), obj.getModel(),
				obj.getMemory(), gpios, obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.GetBoardInfoResponse convert(GetBoardInfoResponse obj) {
		DiozeroProtos.Gpio.GetBoardInfoResponse.Builder builder = DiozeroProtos.Gpio.GetBoardInfoResponse.newBuilder()
				.setStatus(convert(obj.getStatus())).setDetail(obj.getDetail()).setMake(obj.getMake())
				.setModel(obj.getModel()).setCorrelationId(obj.getCorrelationId());
		for (GpioInfo gpio_info : obj.getGpios()) {
			builder.addGpioInfo(convert(gpio_info));
		}

		return builder.build();
	}

	public static ProvisionDigitalInputDevice convert(DiozeroProtos.Gpio.ProvisionDigitalInput obj) {
		return new ProvisionDigitalInputDevice(obj.getGpio(), convert(obj.getPud()), convert(obj.getTrigger()),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.ProvisionDigitalInput convert(ProvisionDigitalInputDevice obj) {
		return DiozeroProtos.Gpio.ProvisionDigitalInput.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).setPud(convert(obj.getPud())).setTrigger(convert(obj.getTrigger())).build();
	}

	public static ProvisionDigitalOutputDevice convert(DiozeroProtos.Gpio.ProvisionDigitalOutput obj) {
		return new ProvisionDigitalOutputDevice(obj.getGpio(), obj.getInitialValue(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.ProvisionDigitalOutput convert(ProvisionDigitalOutputDevice obj) {
		return DiozeroProtos.Gpio.ProvisionDigitalOutput.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).setInitialValue(obj.getInitialValue()).build();
	}

	public static ProvisionDigitalInputOutputDevice convert(DiozeroProtos.Gpio.ProvisionDigitalInputOutput obj) {
		return new ProvisionDigitalInputOutputDevice(obj.getGpio(), obj.getOutput(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.ProvisionDigitalInputOutput convert(ProvisionDigitalInputOutputDevice obj) {
		return DiozeroProtos.Gpio.ProvisionDigitalInputOutput.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).setOutput(obj.getOutput()).build();
	}

	public static ProvisionPwmOutputDevice convert(DiozeroProtos.Gpio.ProvisionPwmOutput obj) {
		return new ProvisionPwmOutputDevice(obj.getGpio(), obj.getFrequency(), obj.getInitialValue(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.ProvisionPwmOutput convert(ProvisionPwmOutputDevice obj) {
		return DiozeroProtos.Gpio.ProvisionPwmOutput.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).setFrequency(obj.getFrequency()).setInitialValue(obj.getInitialValue()).build();
	}

	public static ProvisionAnalogInputDevice convert(DiozeroProtos.Gpio.ProvisionAnalogInput obj) {
		return new ProvisionAnalogInputDevice(obj.getGpio(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.ProvisionAnalogInput convert(ProvisionAnalogInputDevice obj) {
		return DiozeroProtos.Gpio.ProvisionAnalogInput.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).build();
	}

	public static ProvisionAnalogOutputDevice convert(DiozeroProtos.Gpio.ProvisionAnalogOutput obj) {
		return new ProvisionAnalogOutputDevice(obj.getGpio(), obj.getInitialValue(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.ProvisionAnalogOutput convert(ProvisionAnalogOutputDevice obj) {
		return DiozeroProtos.Gpio.ProvisionAnalogOutput.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).setInitialValue(obj.getInitialValue()).build();
	}

	public static GpioDigitalRead convert(DiozeroProtos.Gpio.DigitalRead obj) {
		return new GpioDigitalRead(obj.getGpio(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.DigitalRead convert(GpioDigitalRead obj) {
		return DiozeroProtos.Gpio.DigitalRead.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).build();
	}

	public static GpioDigitalReadResponse convert(DiozeroProtos.Gpio.DigitalReadResponse obj) {
		return new GpioDigitalReadResponse(convert(obj.getStatus()), obj.getDetail(), obj.getValue(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.DigitalReadResponse convert(GpioDigitalReadResponse obj) {
		return DiozeroProtos.Gpio.DigitalReadResponse.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setStatus(convert(obj.getStatus())).setDetail(obj.getDetail()).setValue(obj.getValue()).build();
	}

	public static GpioDigitalWrite convert(DiozeroProtos.Gpio.DigitalWrite obj) {
		return new GpioDigitalWrite(obj.getGpio(), obj.getValue(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.DigitalWrite convert(GpioDigitalWrite obj) {
		return DiozeroProtos.Gpio.DigitalWrite.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).setValue(obj.getValue()).build();
	}

	public static GpioPwmRead convert(DiozeroProtos.Gpio.PwmRead obj) {
		return new GpioPwmRead(obj.getGpio(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.PwmRead convert(GpioPwmRead obj) {
		return DiozeroProtos.Gpio.PwmRead.newBuilder().setCorrelationId(obj.getCorrelationId()).setGpio(obj.getGpio())
				.build();
	}

	public static GpioPwmReadResponse convert(DiozeroProtos.Gpio.PwmReadResponse obj) {
		return new GpioPwmReadResponse(convert(obj.getStatus()), obj.getDetail(), obj.getValue(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.PwmReadResponse convert(GpioPwmReadResponse obj) {
		return DiozeroProtos.Gpio.PwmReadResponse.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setStatus(convert(obj.getStatus())).setDetail(obj.getDetail()).setValue(obj.getValue()).build();
	}

	public static GpioPwmWrite convert(DiozeroProtos.Gpio.PwmWrite obj) {
		return new GpioPwmWrite(obj.getGpio(), obj.getValue(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.PwmWrite convert(GpioPwmWrite obj) {
		return DiozeroProtos.Gpio.PwmWrite.newBuilder().setCorrelationId(obj.getCorrelationId()).setGpio(obj.getGpio())
				.setValue(obj.getValue()).build();
	}

	public static GpioAnalogRead convert(DiozeroProtos.Gpio.AnalogRead obj) {
		return new GpioAnalogRead(obj.getGpio(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.AnalogRead convert(GpioAnalogRead obj) {
		return DiozeroProtos.Gpio.AnalogRead.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).build();
	}

	public static GpioAnalogReadResponse convert(DiozeroProtos.Gpio.AnalogReadResponse obj) {
		return new GpioAnalogReadResponse(convert(obj.getStatus()), obj.getDetail(), obj.getValue(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.AnalogReadResponse convert(GpioAnalogReadResponse obj) {
		return DiozeroProtos.Gpio.AnalogReadResponse.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setStatus(convert(obj.getStatus())).setDetail(obj.getDetail()).setValue(obj.getValue()).build();
	}

	public static GpioAnalogWrite convert(DiozeroProtos.Gpio.AnalogWrite obj) {
		return new GpioAnalogWrite(obj.getGpio(), obj.getValue(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.AnalogWrite convert(GpioAnalogWrite obj) {
		return DiozeroProtos.Gpio.AnalogWrite.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).setValue(obj.getValue()).build();
	}

	public static GpioEvents convert(DiozeroProtos.Gpio.Events obj) {
		return new GpioEvents(obj.getGpio(), obj.getEnabled(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.Events convert(GpioEvents obj) {
		return DiozeroProtos.Gpio.Events.newBuilder().setCorrelationId(obj.getCorrelationId()).setGpio(obj.getGpio())
				.setEnabled(obj.getEnabled()).build();
	}

	public static GpioClose convert(DiozeroProtos.Gpio.Close obj) {
		return new GpioClose(obj.getGpio(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.Close convert(GpioClose obj) {
		return DiozeroProtos.Gpio.Close.newBuilder().setCorrelationId(obj.getCorrelationId()).setGpio(obj.getGpio())
				.build();
	}

	public static I2COpen convert(DiozeroProtos.I2C.Open obj) {
		return new I2COpen(obj.getController(), obj.getAddress(), obj.getAddressSize(), obj.getClockFrequency(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.Open convert(I2COpen obj) {
		return DiozeroProtos.I2C.Open.newBuilder().setController(obj.getController()).setAddress(obj.getAddress())
				.setAddressSize(obj.getAddressSize()).setClockFrequency(obj.getClockFrequency())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CReadByte convert(DiozeroProtos.I2C.ReadByte obj) {
		return new I2CReadByte(obj.getController(), obj.getAddress(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.ReadByte convert(I2CReadByte obj) {
		return DiozeroProtos.I2C.ReadByte.newBuilder().setController(obj.getController()).setAddress(obj.getAddress())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CWriteByte convert(DiozeroProtos.I2C.WriteByte obj) {
		return new I2CWriteByte(obj.getController(), obj.getAddress(), (byte) obj.getData(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.WriteByte convert(I2CWriteByte obj) {
		return DiozeroProtos.I2C.WriteByte.newBuilder().setController(obj.getController()).setAddress(obj.getAddress())
				.setData(obj.getData()).setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CRead convert(DiozeroProtos.I2C.Read obj) {
		return new I2CRead(obj.getController(), obj.getAddress(), obj.getLength(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.Read convert(I2CRead obj) {
		return DiozeroProtos.I2C.Read.newBuilder().setController(obj.getController()).setAddress(obj.getAddress())
				.setLength(obj.getLength()).setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CWrite convert(DiozeroProtos.I2C.Write obj) {
		return new I2CWrite(obj.getController(), obj.getAddress(), obj.getData().toByteArray(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.Write convert(I2CWrite obj) {
		return DiozeroProtos.I2C.Write.newBuilder().setController(obj.getController()).setAddress(obj.getAddress())
				.setData(ByteString.copyFrom(obj.getData())).setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CReadByteData convert(DiozeroProtos.I2C.ReadByteData obj) {
		return new I2CReadByteData(obj.getController(), obj.getAddress(), obj.getRegister(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.ReadByteData convert(I2CReadByteData obj) {
		return DiozeroProtos.I2C.ReadByteData.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setRegister(obj.getRegister()).setCorrelationId(obj.getCorrelationId())
				.build();
	}

	public static I2CWriteByteData convert(DiozeroProtos.I2C.WriteByteData obj) {
		return new I2CWriteByteData(obj.getController(), obj.getAddress(), obj.getRegister(), (byte) obj.getData(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.WriteByteData convert(I2CWriteByteData obj) {
		return DiozeroProtos.I2C.WriteByteData.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setRegister(obj.getRegister()).setData(obj.getData())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CReadI2CBlockData convert(DiozeroProtos.I2C.ReadI2CBlockData obj) {
		return new I2CReadI2CBlockData(obj.getController(), obj.getAddress(), obj.getRegister(), obj.getLength(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.ReadI2CBlockData convert(I2CReadI2CBlockData obj) {
		return DiozeroProtos.I2C.ReadI2CBlockData.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setRegister(obj.getRegister()).setLength(obj.getLength())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CWriteI2CBlockData convert(DiozeroProtos.I2C.WriteI2CBlockData obj) {
		return new I2CWriteI2CBlockData(obj.getController(), obj.getAddress(), obj.getRegister(),
				obj.getData().toByteArray(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.WriteI2CBlockData convert(I2CWriteI2CBlockData obj) {
		return DiozeroProtos.I2C.WriteI2CBlockData.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setRegister(obj.getRegister()).setData(ByteString.copyFrom(obj.getData()))
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CClose convert(DiozeroProtos.I2C.Close obj) {
		return new I2CClose(obj.getController(), obj.getAddress(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.Close convert(I2CClose obj) {
		return DiozeroProtos.I2C.Close.newBuilder().setController(obj.getController()).setAddress(obj.getAddress())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CReadByteResponse convert(DiozeroProtos.I2C.ReadByteResponse obj) {
		return new I2CReadByteResponse(convert(obj.getStatus()), obj.getDetail(), (byte) obj.getData(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.ReadByteResponse convert(I2CReadByteResponse obj) {
		return DiozeroProtos.I2C.ReadByteResponse.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setStatus(convert(obj.getStatus())).setDetail(obj.getDetail()).setData(obj.getData()).build();
	}

	public static I2CReadResponse convert(DiozeroProtos.I2C.ReadResponse obj) {
		return new I2CReadResponse(convert(obj.getStatus()), obj.getDetail(), obj.getData().toByteArray(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.ReadResponse convert(I2CReadResponse obj) {
		return DiozeroProtos.I2C.ReadResponse.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setStatus(convert(obj.getStatus())).setDetail(obj.getDetail())
				.setData(ByteString.copyFrom(obj.getData())).build();
	}

	public static SpiOpen convert(DiozeroProtos.Spi.Open obj) {
		return new SpiOpen(obj.getController(), obj.getChipSelect(), obj.getFrequency(), convert(obj.getClockMode()),
				obj.getLsbFirst(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Spi.Open convert(SpiOpen obj) {
		return DiozeroProtos.Spi.Open.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setController(obj.getController()).setChipSelect(obj.getChipSelect()).setFrequency(obj.getFrequency())
				.setClockMode(convert(obj.getClockMode())).setLsbFirst(obj.getLsbFirst()).build();
	}

	public static SpiWrite convert(DiozeroProtos.Spi.Write obj) {
		return new SpiWrite(obj.getController(), obj.getChipSelect(), obj.getTxData().toByteArray(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.Spi.Write convert(SpiWrite obj) {
		return DiozeroProtos.Spi.Write.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setController(obj.getController()).setChipSelect(obj.getChipSelect())
				.setTxData(ByteString.copyFrom(obj.getTxData())).build();
	}

	public static SpiWriteAndRead convert(DiozeroProtos.Spi.WriteAndRead obj) {
		return new SpiWriteAndRead(obj.getController(), obj.getChipSelect(), obj.getTxData().toByteArray(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.Spi.WriteAndRead convert(SpiWriteAndRead obj) {
		return DiozeroProtos.Spi.WriteAndRead.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setController(obj.getController()).setChipSelect(obj.getChipSelect())
				.setTxData(ByteString.copyFrom(obj.getTxData())).build();
	}

	public static SpiClose convert(DiozeroProtos.Spi.Close obj) {
		return new SpiClose(obj.getController(), obj.getChipSelect(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Spi.Close convert(SpiClose obj) {
		return DiozeroProtos.Spi.Close.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setController(obj.getController()).setChipSelect(obj.getChipSelect()).build();
	}

	public static SpiResponse convert(DiozeroProtos.Spi.SpiResponse obj) {
		return new SpiResponse(convert(obj.getStatus()), obj.getDetail(), obj.getRxData().toByteArray(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.Spi.SpiResponse convert(SpiResponse obj) {
		return DiozeroProtos.Spi.SpiResponse.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setStatus(convert(obj.getStatus())).setDetail(obj.getDetail())
				.setRxData(ByteString.copyFrom(obj.getRxData())).build();
	}

	public static DigitalInputEvent convert(DiozeroProtos.Gpio.Notification notification) {
		return new DigitalInputEvent(notification.getGpio(), notification.getEpochTime(), -1, notification.getValue());
	}

	public static DiozeroProtos.Gpio.Notification convert(DigitalInputEvent event) {
		return DiozeroProtos.Gpio.Notification.newBuilder().setGpio(event.getGpio()).setEpochTime(event.getEpochTime())
				.setValue(event.getValue()).build();
	}

	public static GpioPullUpDown convert(DiozeroProtos.Gpio.PullUpDown pud) {
		switch (pud) {
		case PUD_NONE:
			return GpioPullUpDown.NONE;
		case PUD_PULL_DOWN:
			return GpioPullUpDown.PULL_DOWN;
		case PUD_PULL_UP:
			return GpioPullUpDown.PULL_UP;
		default:
			Logger.error("Invalid Gpio.PullUpDown value: {}", pud);
			return GpioPullUpDown.NONE;
		}
	}

	public static DiozeroProtos.Gpio.PullUpDown convert(GpioPullUpDown pud) {
		switch (pud) {
		case NONE:
			return DiozeroProtos.Gpio.PullUpDown.PUD_NONE;
		case PULL_DOWN:
			return DiozeroProtos.Gpio.PullUpDown.PUD_PULL_DOWN;
		case PULL_UP:
			return DiozeroProtos.Gpio.PullUpDown.PUD_PULL_UP;
		default:
			Logger.error("Invalid GpioPullUpDown value: {}", pud);
			return DiozeroProtos.Gpio.PullUpDown.PUD_NONE;
		}
	}

	public static GpioEventTrigger convert(DiozeroProtos.Gpio.Trigger trigger) {
		switch (trigger) {
		case TRIGGER_NONE:
			return GpioEventTrigger.NONE;
		case TRIGGER_RISING:
			return GpioEventTrigger.RISING;
		case TRIGGER_FALLING:
			return GpioEventTrigger.FALLING;
		case TRIGGER_BOTH:
			return GpioEventTrigger.BOTH;
		default:
			Logger.error("Invalid Gpio.Trigger value: {}", trigger);
			return GpioEventTrigger.BOTH;
		}
	}

	public static DiozeroProtos.Gpio.Trigger convert(GpioEventTrigger trigger) {
		switch (trigger) {
		case NONE:
			return DiozeroProtos.Gpio.Trigger.TRIGGER_NONE;
		case RISING:
			return DiozeroProtos.Gpio.Trigger.TRIGGER_RISING;
		case FALLING:
			return DiozeroProtos.Gpio.Trigger.TRIGGER_FALLING;
		case BOTH:
			return DiozeroProtos.Gpio.Trigger.TRIGGER_BOTH;
		default:
			Logger.error("Invalid GpioEventTrigger value: {}", trigger);
			return DiozeroProtos.Gpio.Trigger.TRIGGER_BOTH;
		}
	}

	public static SpiClockMode convert(DiozeroProtos.Spi.ClockMode clockMode) {
		switch (clockMode) {
		case MODE_0:
			return SpiClockMode.MODE_0;
		case MODE_1:
			return SpiClockMode.MODE_1;
		case MODE_2:
			return SpiClockMode.MODE_2;
		case MODE_3:
			return SpiClockMode.MODE_3;
		default:
			Logger.error("Invalid Spi.ClockMode value: {}", clockMode);
			return SpiClockMode.MODE_0;
		}
	}

	public static DiozeroProtos.Spi.ClockMode convert(SpiClockMode clockMode) {
		switch (clockMode) {
		case MODE_0:
			return DiozeroProtos.Spi.ClockMode.MODE_0;
		case MODE_1:
			return DiozeroProtos.Spi.ClockMode.MODE_1;
		case MODE_2:
			return DiozeroProtos.Spi.ClockMode.MODE_2;
		case MODE_3:
			return DiozeroProtos.Spi.ClockMode.MODE_3;
		default:
			Logger.error("Invalid Spi.ClockMode value: {}", clockMode);
			return DiozeroProtos.Spi.ClockMode.MODE_0;
		}
	}

	private static Response.Status convert(DiozeroProtos.Status status) {
		switch (status) {
		case OK:
			return Response.Status.OK;
		case ERROR:
			return Response.Status.ERROR;
		default:
			Logger.error("Invalid Status: {}", status);
			return Response.Status.ERROR;
		}
	}

	private static DiozeroProtos.Status convert(Response.Status status) {
		switch (status) {
		case OK:
			return DiozeroProtos.Status.OK;
		case ERROR:
			return DiozeroProtos.Status.ERROR;
		default:
			Logger.error("Invalid Status: {}", status);
			return DiozeroProtos.Status.ERROR;
		}
	}

	private static GpioInfo convert(DiozeroProtos.Gpio.GpioInfo obj) {
		List<DeviceMode> modes = new ArrayList<>();
		for (DiozeroProtos.Gpio.GpioMode mode : obj.getModeList()) {
			modes.add(convert(mode));
		}

		return new GpioInfo(obj.getGpio(), modes);
	}

	private static DiozeroProtos.Gpio.GpioInfo convert(GpioInfo obj) {
		DiozeroProtos.Gpio.GpioInfo.Builder builder = DiozeroProtos.Gpio.GpioInfo.newBuilder().setGpio(obj.getGpio());
		for (DeviceMode mode : obj.getModes()) {
			builder.addMode(convert(mode));
		}

		return builder.build();
	}

	private static DeviceMode convert(DiozeroProtos.Gpio.GpioMode mode) {
		switch (mode) {
		case DIGITAL_INPUT:
			return DeviceMode.DIGITAL_INPUT;
		case DIGITAL_OUTPUT:
			return DeviceMode.DIGITAL_OUTPUT;
		case PWM_OUTPUT:
			return DeviceMode.PWM_OUTPUT;
		case ANALOG_INPUT:
			return DeviceMode.ANALOG_INPUT;
		case ANALOG_OUTPUT:
			return DeviceMode.ANALOG_OUTPUT;
		default:
			return DeviceMode.UNKNOWN;
		}
	}

	private static DiozeroProtos.Gpio.GpioMode convert(DeviceMode mode) {
		switch (mode) {
		case DIGITAL_INPUT:
			return DiozeroProtos.Gpio.GpioMode.DIGITAL_INPUT;
		case DIGITAL_OUTPUT:
			return DiozeroProtos.Gpio.GpioMode.DIGITAL_OUTPUT;
		case PWM_OUTPUT:
			return DiozeroProtos.Gpio.GpioMode.PWM_OUTPUT;
		case ANALOG_INPUT:
			return DiozeroProtos.Gpio.GpioMode.ANALOG_INPUT;
		case ANALOG_OUTPUT:
			return DiozeroProtos.Gpio.GpioMode.ANALOG_OUTPUT;
		default:
			return DiozeroProtos.Gpio.GpioMode.UNKNOWN;
		}
	}
}
