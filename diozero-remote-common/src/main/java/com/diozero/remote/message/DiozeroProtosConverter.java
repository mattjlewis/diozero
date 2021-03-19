package com.diozero.remote.message;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Common
 * Filename:     DiozeroProtosConverter.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.I2CDevice.ProbeMode;
import com.diozero.api.SerialDevice;
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

	public static GetBoardInfoRequest convert(DiozeroProtos.Gpio.GetBoardInfoRequest obj) {
		return new GetBoardInfoRequest(obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.GetBoardInfoRequest convert(GetBoardInfoRequest obj) {
		return DiozeroProtos.Gpio.GetBoardInfoRequest.newBuilder().setCorrelationId(obj.getCorrelationId()).build();
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

	public static ProvisionDigitalInputDevice convert(DiozeroProtos.Gpio.ProvisionDigitalInputRequest obj) {
		return new ProvisionDigitalInputDevice(obj.getGpio(), convert(obj.getPud()), convert(obj.getTrigger()),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.ProvisionDigitalInputRequest convert(ProvisionDigitalInputDevice obj) {
		return DiozeroProtos.Gpio.ProvisionDigitalInputRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).setPud(convert(obj.getPud())).setTrigger(convert(obj.getTrigger())).build();
	}

	public static ProvisionDigitalOutputDevice convert(DiozeroProtos.Gpio.ProvisionDigitalOutputRequest obj) {
		return new ProvisionDigitalOutputDevice(obj.getGpio(), obj.getInitialValue(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.ProvisionDigitalOutputRequest convert(ProvisionDigitalOutputDevice obj) {
		return DiozeroProtos.Gpio.ProvisionDigitalOutputRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).setInitialValue(obj.getInitialValue()).build();
	}

	public static ProvisionDigitalInputOutputDevice convert(DiozeroProtos.Gpio.ProvisionDigitalInputOutputRequest obj) {
		return new ProvisionDigitalInputOutputDevice(obj.getGpio(), obj.getOutput(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.ProvisionDigitalInputOutputRequest convert(ProvisionDigitalInputOutputDevice obj) {
		return DiozeroProtos.Gpio.ProvisionDigitalInputOutputRequest.newBuilder()
				.setCorrelationId(obj.getCorrelationId()).setGpio(obj.getGpio()).setOutput(obj.getOutput()).build();
	}

	public static ProvisionPwmOutputDevice convert(DiozeroProtos.Gpio.ProvisionPwmOutputRequest obj) {
		return new ProvisionPwmOutputDevice(obj.getGpio(), obj.getFrequency(), obj.getInitialValue(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.ProvisionPwmOutputRequest convert(ProvisionPwmOutputDevice obj) {
		return DiozeroProtos.Gpio.ProvisionPwmOutputRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).setFrequency(obj.getFrequency()).setInitialValue(obj.getInitialValue()).build();
	}

	public static ProvisionAnalogInputDevice convert(DiozeroProtos.Gpio.ProvisionAnalogInputRequest obj) {
		return new ProvisionAnalogInputDevice(obj.getGpio(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.ProvisionAnalogInputRequest convert(ProvisionAnalogInputDevice obj) {
		return DiozeroProtos.Gpio.ProvisionAnalogInputRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).build();
	}

	public static ProvisionAnalogOutputDevice convert(DiozeroProtos.Gpio.ProvisionAnalogOutputRequest obj) {
		return new ProvisionAnalogOutputDevice(obj.getGpio(), obj.getInitialValue(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.ProvisionAnalogOutputRequest convert(ProvisionAnalogOutputDevice obj) {
		return DiozeroProtos.Gpio.ProvisionAnalogOutputRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).setInitialValue(obj.getInitialValue()).build();
	}

	public static GpioDigitalRead convert(DiozeroProtos.Gpio.DigitalReadRequest obj) {
		return new GpioDigitalRead(obj.getGpio(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.DigitalReadRequest convert(GpioDigitalRead obj) {
		return DiozeroProtos.Gpio.DigitalReadRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
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

	public static GpioDigitalWrite convert(DiozeroProtos.Gpio.DigitalWriteRequest obj) {
		return new GpioDigitalWrite(obj.getGpio(), obj.getValue(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.DigitalWriteRequest convert(GpioDigitalWrite obj) {
		return DiozeroProtos.Gpio.DigitalWriteRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).setValue(obj.getValue()).build();
	}

	public static GpioPwmRead convert(DiozeroProtos.Gpio.PwmReadRequest obj) {
		return new GpioPwmRead(obj.getGpio(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.PwmReadRequest convert(GpioPwmRead obj) {
		return DiozeroProtos.Gpio.PwmReadRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).build();
	}

	public static GpioPwmReadResponse convert(DiozeroProtos.Gpio.PwmReadResponse obj) {
		return new GpioPwmReadResponse(convert(obj.getStatus()), obj.getDetail(), obj.getValue(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.PwmReadResponse convert(GpioPwmReadResponse obj) {
		return DiozeroProtos.Gpio.PwmReadResponse.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setStatus(convert(obj.getStatus())).setDetail(obj.getDetail()).setValue(obj.getValue()).build();
	}

	public static GpioPwmWrite convert(DiozeroProtos.Gpio.PwmWriteRequest obj) {
		return new GpioPwmWrite(obj.getGpio(), obj.getValue(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.PwmWriteRequest convert(GpioPwmWrite obj) {
		return DiozeroProtos.Gpio.PwmWriteRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).setValue(obj.getValue()).build();
	}

	public static GpioGetPwmFrequency convert(DiozeroProtos.Gpio.GetPwmFrequencyRequest obj) {
		return new GpioGetPwmFrequency(obj.getGpio(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.GetPwmFrequencyRequest convert(GpioGetPwmFrequency obj) {
		return DiozeroProtos.Gpio.GetPwmFrequencyRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).build();
	}

	public static GpioGetPwmFrequencyResponse convert(DiozeroProtos.Gpio.GetPwmFrequencyResponse obj) {
		return new GpioGetPwmFrequencyResponse(convert(obj.getStatus()), obj.getDetail(), obj.getFrequency(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.GetPwmFrequencyResponse convert(GpioGetPwmFrequencyResponse obj) {
		return DiozeroProtos.Gpio.GetPwmFrequencyResponse.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setStatus(convert(obj.getStatus())).setDetail(obj.getDetail()).setFrequency(obj.getFrequency()).build();
	}

	public static GpioSetPwmFrequency convert(DiozeroProtos.Gpio.SetPwmFrequencyRequest obj) {
		return new GpioSetPwmFrequency(obj.getGpio(), obj.getFrequency(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.SetPwmFrequencyRequest convert(GpioSetPwmFrequency obj) {
		return DiozeroProtos.Gpio.SetPwmFrequencyRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).setFrequency(obj.getFrequency()).build();
	}
	
	public static GpioAnalogRead convert(DiozeroProtos.Gpio.AnalogReadRequest obj) {
		return new GpioAnalogRead(obj.getGpio(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.AnalogReadRequest convert(GpioAnalogRead obj) {
		return DiozeroProtos.Gpio.AnalogReadRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
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

	public static GpioAnalogWrite convert(DiozeroProtos.Gpio.AnalogWriteRequest obj) {
		return new GpioAnalogWrite(obj.getGpio(), obj.getValue(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.AnalogWriteRequest convert(GpioAnalogWrite obj) {
		return DiozeroProtos.Gpio.AnalogWriteRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).setValue(obj.getValue()).build();
	}

	public static GpioEvents convert(DiozeroProtos.Gpio.EventsRequest obj) {
		return new GpioEvents(obj.getGpio(), obj.getEnabled(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.EventsRequest convert(GpioEvents obj) {
		return DiozeroProtos.Gpio.EventsRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).setEnabled(obj.getEnabled()).build();
	}

	public static GpioClose convert(DiozeroProtos.Gpio.CloseRequest obj) {
		return new GpioClose(obj.getGpio(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Gpio.CloseRequest convert(GpioClose obj) {
		return DiozeroProtos.Gpio.CloseRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setGpio(obj.getGpio()).build();
	}

	//
	// I2C
	//

	public static I2COpen convert(DiozeroProtos.I2C.OpenRequest obj) {
		return new I2COpen(obj.getController(), obj.getAddress(), obj.getAddressSize(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.OpenRequest convert(I2COpen obj) {
		return DiozeroProtos.I2C.OpenRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setAddressSize(obj.getAddressSize())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CProbe convert(DiozeroProtos.I2C.ProbeRequest obj) {
		return new I2CProbe(obj.getController(), obj.getAddress(), convert(obj.getProbeMode()), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.ProbeRequest convert(I2CProbe obj) {
		return DiozeroProtos.I2C.ProbeRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setProbeMode(convert(obj.getProbeMode()))
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	private static ProbeMode convert(DiozeroProtos.I2C.ProbeMode probeMode) {
		switch (probeMode) {
		case QUICK:
			return ProbeMode.QUICK;
		case READ:
			return ProbeMode.READ;
		case AUTO:
		default:
			return ProbeMode.AUTO;
		}
	}

	private static DiozeroProtos.I2C.ProbeMode convert(ProbeMode probeMode) {
		switch (probeMode) {
		case QUICK:
			return DiozeroProtos.I2C.ProbeMode.QUICK;
		case READ:
			return DiozeroProtos.I2C.ProbeMode.READ;
		case AUTO:
		default:
			return DiozeroProtos.I2C.ProbeMode.AUTO;
		}
	}

	public static I2CWriteQuick convert(DiozeroProtos.I2C.WriteQuickRequest obj) {
		return new I2CWriteQuick(obj.getController(), obj.getAddress(), obj.getBit(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.WriteQuickRequest convert(I2CWriteQuick obj) {
		return DiozeroProtos.I2C.WriteQuickRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setBit(obj.getBit()).setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CReadByte convert(DiozeroProtos.I2C.ReadByteRequest obj) {
		return new I2CReadByte(obj.getController(), obj.getAddress(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.ReadByteRequest convert(I2CReadByte obj) {
		return DiozeroProtos.I2C.ReadByteRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CWriteByte convert(DiozeroProtos.I2C.WriteByteRequest obj) {
		return new I2CWriteByte(obj.getController(), obj.getAddress(), (byte) obj.getData(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.WriteByteRequest convert(I2CWriteByte obj) {
		return DiozeroProtos.I2C.WriteByteRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setData(obj.getData()).setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CReadBytes convert(DiozeroProtos.I2C.ReadBytesRequest obj) {
		return new I2CReadBytes(obj.getController(), obj.getAddress(), obj.getLength(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.ReadBytesRequest convert(I2CReadBytes obj) {
		return DiozeroProtos.I2C.ReadBytesRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setLength(obj.getLength()).setCorrelationId(obj.getCorrelationId())
				.build();
	}

	public static I2CWriteBytes convert(DiozeroProtos.I2C.WriteBytesRequest obj) {
		return new I2CWriteBytes(obj.getController(), obj.getAddress(), obj.getData().toByteArray(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.WriteBytesRequest convert(I2CWriteBytes obj) {
		return DiozeroProtos.I2C.WriteBytesRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setData(ByteString.copyFrom(obj.getData()))
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CReadByteData convert(DiozeroProtos.I2C.ReadByteDataRequest obj) {
		return new I2CReadByteData(obj.getController(), obj.getAddress(), obj.getRegister(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.ReadByteDataRequest convert(I2CReadByteData obj) {
		return DiozeroProtos.I2C.ReadByteDataRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setRegister(obj.getRegister()).setCorrelationId(obj.getCorrelationId())
				.build();
	}

	public static I2CWriteByteData convert(DiozeroProtos.I2C.WriteByteDataRequest obj) {
		return new I2CWriteByteData(obj.getController(), obj.getAddress(), obj.getRegister(), (byte) obj.getData(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.WriteByteDataRequest convert(I2CWriteByteData obj) {
		return DiozeroProtos.I2C.WriteByteDataRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setRegister(obj.getRegister()).setData(obj.getData())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CReadWordData convert(DiozeroProtos.I2C.ReadWordDataRequest obj) {
		return new I2CReadWordData(obj.getController(), obj.getAddress(), obj.getRegister(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.ReadWordDataRequest convert(I2CReadWordData obj) {
		return DiozeroProtos.I2C.ReadWordDataRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setRegister(obj.getRegister()).setCorrelationId(obj.getCorrelationId())
				.build();
	}

	public static I2CWriteWordData convert(DiozeroProtos.I2C.WriteWordDataRequest obj) {
		return new I2CWriteWordData(obj.getController(), obj.getAddress(), obj.getRegister(), obj.getData(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.WriteWordDataRequest convert(I2CWriteWordData obj) {
		return DiozeroProtos.I2C.WriteWordDataRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setRegister(obj.getRegister()).setData(obj.getData())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CProcessCall convert(DiozeroProtos.I2C.ProcessCallRequest obj) {
		return new I2CProcessCall(obj.getController(), obj.getAddress(), obj.getRegister(), obj.getData(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.ProcessCallRequest convert(I2CProcessCall obj) {
		return DiozeroProtos.I2C.ProcessCallRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setRegister(obj.getRegister()).setData(obj.getData())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CReadBlockData convert(DiozeroProtos.I2C.ReadBlockDataRequest obj) {
		return new I2CReadBlockData(obj.getController(), obj.getAddress(), obj.getRegister(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.ReadBlockDataRequest convert(I2CReadBlockData obj) {
		return DiozeroProtos.I2C.ReadBlockDataRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setRegister(obj.getRegister()).setCorrelationId(obj.getCorrelationId())
				.build();
	}

	public static I2CWriteBlockData convert(DiozeroProtos.I2C.WriteBlockDataRequest obj) {
		return new I2CWriteBlockData(obj.getController(), obj.getAddress(), obj.getRegister(),
				obj.getData().toByteArray(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.WriteBlockDataRequest convert(I2CWriteBlockData obj) {
		return DiozeroProtos.I2C.WriteBlockDataRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setRegister(obj.getRegister()).setData(ByteString.copyFrom(obj.getData()))
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CBlockProcessCall convert(DiozeroProtos.I2C.BlockProcessCallRequest obj) {
		return new I2CBlockProcessCall(obj.getController(), obj.getAddress(), obj.getRegister(),
				obj.getData().toByteArray(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.BlockProcessCallRequest convert(I2CBlockProcessCall obj) {
		return DiozeroProtos.I2C.BlockProcessCallRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setRegister(obj.getRegister()).setData(ByteString.copyFrom(obj.getData()))
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CReadI2CBlockData convert(DiozeroProtos.I2C.ReadI2CBlockDataRequest obj) {
		return new I2CReadI2CBlockData(obj.getController(), obj.getAddress(), obj.getRegister(), obj.getLength(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.ReadI2CBlockDataRequest convert(I2CReadI2CBlockData obj) {
		return DiozeroProtos.I2C.ReadI2CBlockDataRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setRegister(obj.getRegister()).setLength(obj.getLength())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CWriteI2CBlockData convert(DiozeroProtos.I2C.WriteI2CBlockDataRequest obj) {
		return new I2CWriteI2CBlockData(obj.getController(), obj.getAddress(), obj.getRegister(),
				obj.getData().toByteArray(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.WriteI2CBlockDataRequest convert(I2CWriteI2CBlockData obj) {
		return DiozeroProtos.I2C.WriteI2CBlockDataRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setRegister(obj.getRegister()).setData(ByteString.copyFrom(obj.getData()))
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CClose convert(DiozeroProtos.I2C.CloseRequest obj) {
		return new I2CClose(obj.getController(), obj.getAddress(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.CloseRequest convert(I2CClose obj) {
		return DiozeroProtos.I2C.CloseRequest.newBuilder().setController(obj.getController())
				.setAddress(obj.getAddress()).setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CBooleanResponse convert(DiozeroProtos.I2C.BooleanResponse obj) {
		return new I2CBooleanResponse(convert(obj.getStatus()), obj.getDetail(), obj.getResult(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.BooleanResponse convert(I2CBooleanResponse obj) {
		return DiozeroProtos.I2C.BooleanResponse.newBuilder().setStatus(convert(obj.getStatus()))
				.setDetail(obj.getDetail()).setResult(obj.getResult()).setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CByteResponse convert(DiozeroProtos.I2C.ByteResponse obj) {
		return new I2CByteResponse(convert(obj.getStatus()), obj.getDetail(), (byte) obj.getData(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.ByteResponse convert(I2CByteResponse obj) {
		return DiozeroProtos.I2C.ByteResponse.newBuilder().setStatus(convert(obj.getStatus()))
				.setDetail(obj.getDetail()).setData(obj.getData()).setCorrelationId(obj.getCorrelationId()).build();
	}

	public static I2CBytesResponse convert(DiozeroProtos.I2C.BytesResponse obj) {
		return new I2CBytesResponse(convert(obj.getStatus()), obj.getDetail(), obj.getData().toByteArray(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.BytesResponse convert(I2CBytesResponse obj) {
		return DiozeroProtos.I2C.BytesResponse.newBuilder().setStatus(convert(obj.getStatus()))
				.setDetail(obj.getDetail()).setData(ByteString.copyFrom(obj.getData()))
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static DiozeroProtos.I2C.WordResponse convert(I2CWordResponse obj) {
		return DiozeroProtos.I2C.WordResponse.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setStatus(convert(obj.getStatus())).setDetail(obj.getDetail()).setData(obj.getData()).build();
	}

	public static I2CWordResponse convert(DiozeroProtos.I2C.WordResponse obj) {
		return new I2CWordResponse(convert(obj.getStatus()), obj.getDetail(), obj.getData(), obj.getCorrelationId());
	}

	public static I2CReadBlockDataResponse convert(DiozeroProtos.I2C.ReadBlockDataResponse obj) {
		return new I2CReadBlockDataResponse(convert(obj.getStatus()), obj.getDetail(), obj.getBytesRead(),
				obj.getData().toByteArray(), obj.getCorrelationId());
	}

	public static DiozeroProtos.I2C.ReadBlockDataResponse convert(I2CReadBlockDataResponse obj) {
		return DiozeroProtos.I2C.ReadBlockDataResponse.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setStatus(convert(obj.getStatus())).setDetail(obj.getDetail()).setBytesRead(obj.getBytesRead())
				.setData(ByteString.copyFrom(obj.getData())).build();
	}

	//
	// SPI
	//

	public static SpiOpen convert(DiozeroProtos.Spi.OpenRequest obj) {
		return new SpiOpen(obj.getController(), obj.getChipSelect(), obj.getFrequency(), convert(obj.getClockMode()),
				obj.getLsbFirst(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Spi.OpenRequest convert(SpiOpen obj) {
		return DiozeroProtos.Spi.OpenRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setController(obj.getController()).setChipSelect(obj.getChipSelect()).setFrequency(obj.getFrequency())
				.setClockMode(convert(obj.getClockMode())).setLsbFirst(obj.getLsbFirst()).build();
	}

	public static SpiWrite convert(DiozeroProtos.Spi.WriteRequest obj) {
		return new SpiWrite(obj.getController(), obj.getChipSelect(), obj.getTxData().toByteArray(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.Spi.WriteRequest convert(SpiWrite obj) {
		return DiozeroProtos.Spi.WriteRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setController(obj.getController()).setChipSelect(obj.getChipSelect())
				.setTxData(ByteString.copyFrom(obj.getTxData())).build();
	}

	public static SpiWriteAndRead convert(DiozeroProtos.Spi.WriteAndReadRequest obj) {
		return new SpiWriteAndRead(obj.getController(), obj.getChipSelect(), obj.getTxData().toByteArray(),
				obj.getCorrelationId());
	}

	public static DiozeroProtos.Spi.WriteAndReadRequest convert(SpiWriteAndRead obj) {
		return DiozeroProtos.Spi.WriteAndReadRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
				.setController(obj.getController()).setChipSelect(obj.getChipSelect())
				.setTxData(ByteString.copyFrom(obj.getTxData())).build();
	}

	public static SpiClose convert(DiozeroProtos.Spi.CloseRequest obj) {
		return new SpiClose(obj.getController(), obj.getChipSelect(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Spi.CloseRequest convert(SpiClose obj) {
		return DiozeroProtos.Spi.CloseRequest.newBuilder().setCorrelationId(obj.getCorrelationId())
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
		return new DigitalInputEvent(notification.getGpio(), notification.getEpochTime(), notification.getNanoTime(),
				notification.getValue());
	}

	public static DiozeroProtos.Gpio.Notification convert(DigitalInputEvent event) {
		return DiozeroProtos.Gpio.Notification.newBuilder().setGpio(event.getGpio()).setEpochTime(event.getEpochTime())
				.setNanoTime(event.getNanoTime()).setValue(event.getValue()).build();
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

	public static SerialOpen convert(DiozeroProtos.Serial.OpenRequest obj) {
		return new SerialOpen(obj.getDeviceFile(), obj.getBaud(), SerialDevice.DataBits.values()[obj.getDataBits()],
				SerialDevice.StopBits.values()[obj.getStopBits()], SerialDevice.Parity.values()[obj.getParity()],
				obj.getReadBlocking(), obj.getMinReadChars(), obj.getReadTimeoutMillis(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Serial.OpenRequest convert(SerialOpen obj) {
		return DiozeroProtos.Serial.OpenRequest.newBuilder().setDeviceFile(obj.getDeviceFile()).setBaud(obj.getBaud())
				.setDataBits(obj.getDataBits().ordinal()).setStopBits(obj.getStopBits().ordinal())
				.setParity(obj.getParity().ordinal()).setReadBlocking(obj.isReadBlocking())
				.setMinReadChars(obj.getMinReadChars()).setReadTimeoutMillis(obj.getReadTimeoutMillis())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static SerialRead convert(DiozeroProtos.Serial.ReadRequest obj) {
		return new SerialRead(obj.getDeviceFile(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Serial.ReadRequest convert(SerialRead obj) {
		return DiozeroProtos.Serial.ReadRequest.newBuilder().setDeviceFile(obj.getDeviceFile())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static SerialReadByte convert(DiozeroProtos.Serial.ReadByteRequest obj) {
		return new SerialReadByte(obj.getDeviceFile(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Serial.ReadByteRequest convert(SerialReadByte obj) {
		return DiozeroProtos.Serial.ReadByteRequest.newBuilder().setDeviceFile(obj.getDeviceFile())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static SerialWriteByte convert(DiozeroProtos.Serial.WriteByteRequest obj) {
		return new SerialWriteByte(obj.getDeviceFile(), (byte) obj.getData(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Serial.WriteByteRequest convert(SerialWriteByte obj) {
		return DiozeroProtos.Serial.WriteByteRequest.newBuilder().setDeviceFile(obj.getDeviceFile())
				.setData(obj.getData()).setCorrelationId(obj.getCorrelationId()).build();
	}

	public static SerialReadBytes convert(DiozeroProtos.Serial.ReadBytesRequest obj) {
		return new SerialReadBytes(obj.getDeviceFile(), obj.getLength(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Serial.ReadBytesRequest convert(SerialReadBytes obj) {
		return DiozeroProtos.Serial.ReadBytesRequest.newBuilder().setDeviceFile(obj.getDeviceFile())
				.setLength(obj.getLength()).setCorrelationId(obj.getCorrelationId()).build();
	}

	public static SerialWriteBytes convert(DiozeroProtos.Serial.WriteBytesRequest obj) {
		return new SerialWriteBytes(obj.getDeviceFile(), obj.getData().toByteArray(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Serial.WriteBytesRequest convert(SerialWriteBytes obj) {
		return DiozeroProtos.Serial.WriteBytesRequest.newBuilder().setDeviceFile(obj.getDeviceFile())
				.setData(ByteString.copyFrom(obj.getData())).setCorrelationId(obj.getCorrelationId()).build();
	}

	public static SerialBytesAvailable convert(DiozeroProtos.Serial.BytesAvailableRequest obj) {
		return new SerialBytesAvailable(obj.getDeviceFile(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Serial.BytesAvailableRequest convert(SerialBytesAvailable obj) {
		return DiozeroProtos.Serial.BytesAvailableRequest.newBuilder().setDeviceFile(obj.getDeviceFile())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static SerialClose convert(DiozeroProtos.Serial.CloseRequest obj) {
		return new SerialClose(obj.getDeviceFile(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Serial.CloseRequest convert(SerialClose obj) {
		return DiozeroProtos.Serial.CloseRequest.newBuilder().setDeviceFile(obj.getDeviceFile())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static SerialReadResponse convert(DiozeroProtos.Serial.ReadResponse obj) {
		return new SerialReadResponse((byte) obj.getData(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Serial.ReadResponse convert(SerialReadResponse obj) {
		return DiozeroProtos.Serial.ReadResponse.newBuilder().setData(obj.getData())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static SerialReadByteResponse convert(DiozeroProtos.Serial.ReadByteResponse obj) {
		return new SerialReadByteResponse((byte) obj.getData(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Serial.ReadByteResponse convert(SerialReadByteResponse obj) {
		return DiozeroProtos.Serial.ReadByteResponse.newBuilder().setData(obj.getData())
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static SerialReadBytesResponse convert(DiozeroProtos.Serial.ReadBytesResponse obj) {
		return new SerialReadBytesResponse(obj.getData().toByteArray(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Serial.ReadBytesResponse convert(SerialReadBytesResponse obj) {
		return DiozeroProtos.Serial.ReadBytesResponse.newBuilder().setData(ByteString.copyFrom(obj.getData()))
				.setCorrelationId(obj.getCorrelationId()).build();
	}

	public static SerialBytesAvailableResponse convert(DiozeroProtos.Serial.BytesAvailableResponse obj) {
		return new SerialBytesAvailableResponse(obj.getBytesAvailable(), obj.getCorrelationId());
	}

	public static DiozeroProtos.Serial.BytesAvailableResponse convert(SerialBytesAvailableResponse obj) {
		return DiozeroProtos.Serial.BytesAvailableResponse.newBuilder().setBytesAvailable(obj.getBytesAvailable())
				.setCorrelationId(obj.getCorrelationId()).build();
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
