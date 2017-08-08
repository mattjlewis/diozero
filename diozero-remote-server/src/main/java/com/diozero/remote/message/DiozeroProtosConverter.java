package com.diozero.remote.message;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Server
 * Filename:     DiozeroProtosConverter.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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

import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.SpiClockMode;
import com.google.protobuf.ByteString;

public class DiozeroProtosConverter {
	public static Response convert(DiozeroProtos.Response response) {
		return new Response(convert(response.getStatus()), response.getDetail());
	}

	public static DiozeroProtos.Response convert(Response response, String correlationId) {
		return DiozeroProtos.Response.newBuilder().setCorrelationId(correlationId)
				.setStatus(convert(response.getStatus())).setDetail(response.getDetail()).build();
	}

	public static ProvisionDigitalInputDevice convert(DiozeroProtos.Gpio.ProvisionInput gpioInput) {
		return new ProvisionDigitalInputDevice(gpioInput.getGpio(), convert(gpioInput.getPud()),
				convert(gpioInput.getTrigger()));
	}

	public static ProvisionDigitalOutputDevice convert(DiozeroProtos.Gpio.ProvisionOutput gpioOutput) {
		return new ProvisionDigitalOutputDevice(gpioOutput.getGpio(), gpioOutput.getInitialValue());
	}

	public static ProvisionDigitalInputOutputDevice convert(DiozeroProtos.Gpio.ProvisionInputOutput gpioInputOutput) {
		return new ProvisionDigitalInputOutputDevice(gpioInputOutput.getGpio(), gpioInputOutput.getOutput());
	}

	public static GpioDigitalRead convert(DiozeroProtos.Gpio.DigitalRead gpioRead) {
		return new GpioDigitalRead(gpioRead.getGpio());
	}

	public static GpioDigitalWrite convert(DiozeroProtos.Gpio.DigitalWrite gpioWrite) {
		return new GpioDigitalWrite(gpioWrite.getGpio(), gpioWrite.getValue());
	}

	public static GpioEvents convert(DiozeroProtos.Gpio.Events gpioEvents) {
		return new GpioEvents(gpioEvents.getGpio(), gpioEvents.getEnabled());
	}

	public static GpioClose convert(DiozeroProtos.Gpio.Close gpioClose) {
		return new GpioClose(gpioClose.getGpio());
	}

	public static GpioDigitalReadResponse convert(DiozeroProtos.Gpio.DigitalReadResponse response) {
		return new GpioDigitalReadResponse(convert(response.getStatus()), response.getDetail(), response.getDigitalValue());
	}

	public static ProvisionSpiDevice convert(DiozeroProtos.Spi.Provision spiProvision) {
		return new ProvisionSpiDevice(spiProvision.getController(), spiProvision.getChipSelect(),
				spiProvision.getFrequency(), convert(spiProvision.getClockMode()), spiProvision.getLsbFirst());
	}

	public static SpiWrite convert(DiozeroProtos.Spi.Write spiWrite) {
		return new SpiWrite(spiWrite.getController(), spiWrite.getChipSelect(), spiWrite.getTxData().toByteArray());
	}

	public static SpiWriteAndRead convert(DiozeroProtos.Spi.WriteAndRead spiWriteAndRead) {
		return new SpiWriteAndRead(spiWriteAndRead.getController(), spiWriteAndRead.getChipSelect(),
				spiWriteAndRead.getTxData().toByteArray());
	}

	public static SpiClose convert(DiozeroProtos.Spi.Close spiClose) {
		return new SpiClose(spiClose.getController(), spiClose.getChipSelect());
	}

	public static SpiResponse convert(DiozeroProtos.Spi.SpiResponse response) {
		return new SpiResponse(convert(response.getStatus()), response.getDetail(), response.getRxData().toByteArray());
	}

	public static DiozeroProtos.Gpio.ProvisionInput convert(ProvisionDigitalInputDevice request, String correlationId) {
		return DiozeroProtos.Gpio.ProvisionInput.newBuilder().setCorrelationId(correlationId).setGpio(request.getGpio())
				.setPud(convert(request.getPud())).setTrigger(convert(request.getTrigger())).build();
	}

	public static DiozeroProtos.Gpio.ProvisionOutput convert(ProvisionDigitalOutputDevice request,
			String correlationId) {
		return DiozeroProtos.Gpio.ProvisionOutput.newBuilder().setCorrelationId(correlationId)
				.setGpio(request.getGpio()).setInitialValue(request.getInitialValue()).build();
	}

	public static DiozeroProtos.Gpio.ProvisionInputOutput convert(ProvisionDigitalInputOutputDevice request,
			String correlationId) {
		return DiozeroProtos.Gpio.ProvisionInputOutput.newBuilder().setCorrelationId(correlationId)
				.setGpio(request.getGpio()).setOutput(request.getOutput()).build();
	}

	public static DiozeroProtos.Gpio.DigitalRead convert(GpioDigitalRead request, String correlationId) {
		return DiozeroProtos.Gpio.DigitalRead.newBuilder().setCorrelationId(correlationId).setGpio(request.getGpio())
				.build();
	}

	public static DiozeroProtos.Gpio.DigitalWrite convert(GpioDigitalWrite request, String correlationId) {
		return DiozeroProtos.Gpio.DigitalWrite.newBuilder().setCorrelationId(correlationId).setGpio(request.getGpio())
				.setValue(request.getValue()).build();
	}

	public static DiozeroProtos.Gpio.Events convert(GpioEvents request, String correlationId) {
		return DiozeroProtos.Gpio.Events.newBuilder().setCorrelationId(correlationId).setGpio(request.getGpio())
				.setEnabled(request.getEnabled()).build();
	}

	public static DiozeroProtos.Gpio.Close convert(GpioClose request, String correlationId) {
		return DiozeroProtos.Gpio.Close.newBuilder().setCorrelationId(correlationId).setGpio(request.getGpio()).build();
	}

	public static DiozeroProtos.Gpio.DigitalReadResponse convert(GpioDigitalReadResponse response,
			String correlationId) {
		return DiozeroProtos.Gpio.DigitalReadResponse.newBuilder().setCorrelationId(correlationId)
				.setStatus(convert(response.getStatus())).setDetail(response.getDetail())
				.setDigitalValue(response.getValue()).build();
	}

	public static DiozeroProtos.Spi.Provision convert(ProvisionSpiDevice request, String correlationId) {
		return DiozeroProtos.Spi.Provision.newBuilder().setCorrelationId(correlationId)
				.setController(request.getController()).setChipSelect(request.getChipSelect())
				.setFrequency(request.getFrequency()).setClockMode(convert(request.getClockMode()))
				.setLsbFirst(request.getLsbFirst()).build();
	}

	public static DiozeroProtos.Spi.Write convert(SpiWrite request, String correlationId) {
		return DiozeroProtos.Spi.Write.newBuilder().setCorrelationId(correlationId)
				.setController(request.getController()).setChipSelect(request.getChipSelect())
				.setTxData(ByteString.copyFrom(request.getTxData())).build();
	}

	public static DiozeroProtos.Spi.Close convert(SpiClose request, String correlationId) {
		return DiozeroProtos.Spi.Close.newBuilder().setCorrelationId(correlationId)
				.setController(request.getController()).setChipSelect(request.getChipSelect()).build();
	}

	public static DiozeroProtos.Spi.SpiResponse convert(SpiResponse response, String correlationId) {
		return DiozeroProtos.Spi.SpiResponse.newBuilder().setCorrelationId(correlationId)
				.setStatus(convert(response.getStatus())).setDetail(response.getDetail())
				.setRxData(ByteString.copyFrom(response.getRxData())).build();
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

	public static DigitalInputEvent convert(DiozeroProtos.Gpio.Notification notification) {
		return new DigitalInputEvent(notification.getGpio(), notification.getEpochTime(), -1, notification.getValue());
	}

	public static DiozeroProtos.Gpio.Notification convert(DigitalInputEvent event) {
		return DiozeroProtos.Gpio.Notification.newBuilder().setGpio(event.getGpio()).setEpochTime(event.getEpochTime())
				.setValue(event.getValue()).build();
	}
}
