package com.diozero.remote;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Remote Common
 * Filename:     DiozeroProtosConverter.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.I2CDevice.ProbeMode;
import com.diozero.api.PinInfo;
import com.diozero.api.SpiClockMode;
import com.diozero.remote.message.protobuf.Board;
import com.diozero.remote.message.protobuf.Gpio;
import com.diozero.remote.message.protobuf.I2C;
import com.diozero.remote.message.protobuf.SPI;

public class DiozeroProtosConverter {
	//
	// GPIO
	//

	// FIXME Not used
	public static DigitalInputEvent convert(Gpio.Event event) {
		return new DigitalInputEvent(event.getGpio(), event.getEpochTime(), event.getNanoTime(), event.getValue());
	}

	public static GpioPullUpDown convert(Gpio.PullUpDown pud) {
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

	public static Gpio.PullUpDown convert(GpioPullUpDown pud) {
		switch (pud) {
		case NONE:
			return Gpio.PullUpDown.PUD_NONE;
		case PULL_DOWN:
			return Gpio.PullUpDown.PUD_PULL_DOWN;
		case PULL_UP:
			return Gpio.PullUpDown.PUD_PULL_UP;
		default:
			Logger.error("Invalid GpioPullUpDown value: {}", pud);
			return Gpio.PullUpDown.PUD_NONE;
		}
	}

	public static GpioEventTrigger convert(Gpio.Trigger trigger) {
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

	public static Gpio.Trigger convert(GpioEventTrigger trigger) {
		switch (trigger) {
		case NONE:
			return Gpio.Trigger.TRIGGER_NONE;
		case RISING:
			return Gpio.Trigger.TRIGGER_RISING;
		case FALLING:
			return Gpio.Trigger.TRIGGER_FALLING;
		case BOTH:
			return Gpio.Trigger.TRIGGER_BOTH;
		default:
			Logger.error("Invalid GpioEventTrigger value: {}", trigger);
			return Gpio.Trigger.TRIGGER_BOTH;
		}
	}

	public static DeviceMode convert(Board.GpioMode mode) {
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

	public static Board.GpioMode convert(DeviceMode mode) {
		switch (mode) {
		case DIGITAL_INPUT:
			return Board.GpioMode.DIGITAL_INPUT;
		case DIGITAL_OUTPUT:
			return Board.GpioMode.DIGITAL_OUTPUT;
		case PWM_OUTPUT:
			return Board.GpioMode.PWM_OUTPUT;
		case ANALOG_INPUT:
			return Board.GpioMode.ANALOG_INPUT;
		case ANALOG_OUTPUT:
			return Board.GpioMode.ANALOG_OUTPUT;
		default:
			return Board.GpioMode.UNKNOWN;
		}
	}

	public static Board.GpioInfo convert(PinInfo pinInfo) {
		Board.GpioInfo.Builder gpio_info_builder = Board.GpioInfo.newBuilder();

		gpio_info_builder.setHeader(pinInfo.getHeader());
		gpio_info_builder.setPhysicalPin(pinInfo.getPhysicalPin());
		gpio_info_builder.setGpioNumber(pinInfo.getDeviceNumber());
		gpio_info_builder.setSysFsNumber(pinInfo.getSysFsNumber());
		gpio_info_builder.setChip(pinInfo.getChip());
		gpio_info_builder.setLineOffset(pinInfo.getLineOffset());
		gpio_info_builder.setPwmChip(pinInfo.getPwmChip());
		gpio_info_builder.setPwmNum(pinInfo.getPwmNum());
		gpio_info_builder.setName(pinInfo.getName());
		for (DeviceMode mode : pinInfo.getModes()) {
			gpio_info_builder.addMode(DiozeroProtosConverter.convert(mode));
		}

		return gpio_info_builder.build();
	}

	public static Collection<DeviceMode> convert(List<Board.GpioMode> gpioModes) {
		return gpioModes.stream().map(DiozeroProtosConverter::convert).collect(Collectors.toList());
	}

	//
	// I2C
	//

	public static ProbeMode convert(I2C.ProbeMode probeMode) {
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

	public static I2C.ProbeMode convert(ProbeMode probeMode) {
		switch (probeMode) {
		case QUICK:
			return I2C.ProbeMode.QUICK;
		case READ:
			return I2C.ProbeMode.READ;
		case AUTO:
		default:
			return I2C.ProbeMode.AUTO;
		}
	}

	//
	// SPI
	//

	public static SpiClockMode convert(SPI.ClockMode clockMode) {
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
			Logger.error("Invalid SPI.ClockMode value: {}", clockMode);
			return SpiClockMode.MODE_0;
		}
	}

	public static SPI.ClockMode convert(SpiClockMode clockMode) {
		switch (clockMode) {
		case MODE_0:
			return SPI.ClockMode.MODE_0;
		case MODE_1:
			return SPI.ClockMode.MODE_1;
		case MODE_2:
			return SPI.ClockMode.MODE_2;
		case MODE_3:
			return SPI.ClockMode.MODE_3;
		default:
			Logger.error("Invalid SpiClockMode value: {}", clockMode);
			return SPI.ClockMode.MODE_0;
		}
	}

	//
	// Serial
	//
}
