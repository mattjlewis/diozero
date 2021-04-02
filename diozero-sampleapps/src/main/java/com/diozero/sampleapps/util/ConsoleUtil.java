package com.diozero.sampleapps.util;

import java.util.Optional;

import org.fusesource.jansi.Ansi.Color;

import com.diozero.api.DeviceMode;
import com.diozero.api.PinInfo;
import com.diozero.internal.spi.MmapGpioInterface;

public class ConsoleUtil {
	public static Color getPinColour(PinInfo pinInfo) {
		if (pinInfo.getDeviceNumber() != PinInfo.NOT_DEFINED) {
			return Color.GREEN;
		}

		Color colour;
		switch (pinInfo.getName()) {
		case PinInfo.VCC_3V3:
			colour = Color.MAGENTA;
			break;
		case PinInfo.VCC_5V:
			colour = Color.RED;
			break;
		case PinInfo.GROUND:
			colour = Color.BLUE;
			break;
		default:
			colour = Color.DEFAULT;
		}
		return colour;
	}

	public static Optional<Boolean> gpioRead(MmapGpioInterface mmapGpio, PinInfo pinInfo) {
		int gpio = pinInfo.getDeviceNumber();
		if (gpio == PinInfo.NOT_DEFINED) {
			return Optional.empty();
		}

		if (pinInfo.getModes().contains(DeviceMode.DIGITAL_INPUT)
				|| pinInfo.getModes().contains(DeviceMode.DIGITAL_OUTPUT)) {
			return Optional.of(Boolean.valueOf(mmapGpio.gpioRead(gpio)));
		}

		return Optional.empty();
	}

	public static String getValueString(Optional<Boolean> value) {
		return value.map(v -> v.booleanValue() ? "1" : "0").orElse(" ");
	}
	
	public static Color getValueColour(Optional<Boolean> value) {
		return value.map(v -> v.booleanValue() ? Color.MAGENTA : Color.BLUE).orElse(Color.DEFAULT);
	}

	public static String getGpiodName(int chip, int lineOffset) {
		if (chip == PinInfo.NOT_DEFINED || lineOffset == PinInfo.NOT_DEFINED) {
			return "";
		}
		return String.format("%2s:%-3s", Integer.valueOf(chip), Integer.valueOf(lineOffset));
	}

	public static String getModeString(MmapGpioInterface mmapGpio, PinInfo pinInfo) {
		int gpio = pinInfo.getDeviceNumber();
		if (gpio == PinInfo.NOT_DEFINED) {
			return "";
		}

		switch (mmapGpio.getMode(gpio)) {
		case DIGITAL_OUTPUT:
			return "Out";
		case DIGITAL_INPUT:
			return "In";
		default:
			return "Unkn";
		}
	}

	public static Color getModeColour(MmapGpioInterface mmapGpio, PinInfo pinInfo) {
		int gpio = pinInfo.getDeviceNumber();

		if (gpio != PinInfo.NOT_DEFINED && pinInfo.getModes().contains(DeviceMode.DIGITAL_INPUT)
				|| pinInfo.getModes().contains(DeviceMode.DIGITAL_OUTPUT)) {
			switch (mmapGpio.getMode(gpio)) {
			case DIGITAL_OUTPUT:
				return Color.YELLOW;
			case DIGITAL_INPUT:
				return Color.CYAN;
			default:
			}
		}

		return Color.WHITE;
	}

	public static String getNotDefined(int value) {
		return value == PinInfo.NOT_DEFINED ? "" : Integer.toString(value);
	}
}
