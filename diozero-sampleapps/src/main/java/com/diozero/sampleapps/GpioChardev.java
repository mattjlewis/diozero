package com.diozero.sampleapps;

import java.io.IOException;
import java.util.Collection;

import org.tinylog.Logger;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.PinInfo;
import com.diozero.internal.provider.builtin.gpio.GpioChip;
import com.diozero.internal.provider.builtin.gpio.GpioLine;
import com.diozero.sbc.BoardPinInfo;
import com.diozero.sbc.DeviceFactoryHelper;

public class GpioChardev {
	public static void main(String[] args) throws IOException {
		final Collection<GpioChip> chips = GpioChip.openAllChips().values();
		try {
			final BoardPinInfo bpi = DeviceFactoryHelper.getNativeDeviceFactory().getBoardPinInfo();
			chips.forEach(chip -> testLines(bpi, chip));
		} finally {
			chips.forEach(GpioChip::close);
		}
	}

	private static void testLines(BoardPinInfo bpi, GpioChip gpioChip) {
		for (GpioLine line : gpioChip.getLines()) {
			try {
				final PinInfo pin_info = bpi.getByChipAndLineOffsetOrThrow(gpioChip.getChipId(), line.getOffset());
				final DigitalInputDevice did = DigitalInputDevice.Builder.builder(pin_info).build();
				Logger.info("Provisioned digital input device gpiochip #{} line #{}, value: {}",
						Integer.valueOf(gpioChip.getChipId()), Integer.valueOf(line.getOffset()),
						Boolean.valueOf(did.getValue()));
			} catch (Throwable t) {
				Logger.info("Unable to provision digital input device gpiochip #{} line #{}: {}",
						Integer.valueOf(gpioChip.getChipId()), Integer.valueOf(line.getOffset()), t);
			}
		}
	}
}
