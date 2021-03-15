package com.diozero.sampleapps;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioPullUpDown;

public class DigitalInputTest {
	public static void main(String[] args) {
		// NO switch with internal pull-up using GPIO #18
		try (DigitalInputDevice did1 = DigitalInputDevice.builder(18).setPullUpDown(GpioPullUpDown.PULL_UP).build()) {
			// Do stuff, noting that active-high will have been defaulted to
			// false as diozero assumes the switch is wired NO
		}

		// NC switch with internal pull-up using GPIO #18
		try (DigitalInputDevice did1 = DigitalInputDevice.builder(18).setPullUpDown(GpioPullUpDown.PULL_UP)
				.setActiveHigh(true).build()) {
			// Do stuff, noting that active-high has been overridden to true
			// as the application developer knows that the switch is wired NC
		}

		// NO switch with internal pull-down using GPIO #18
		try (DigitalInputDevice did1 = DigitalInputDevice.builder(18).setPullUpDown(GpioPullUpDown.PULL_DOWN).build()) {
			// Do stuff, noting that active-high will have been defaulted to
			// true as diozero assumes the switch is wired NO
		}

		// NC switch with internal pull-down using GPIO #18
		try (DigitalInputDevice did1 = DigitalInputDevice.builder(18).setPullUpDown(GpioPullUpDown.PULL_DOWN)
				.setActiveHigh(false).build()) {
			// Do stuff, noting that active-high has been overridden to false
			// as the application developer knows that the switch is wired NC
		}

		// NO switch with external pull-up using GPIO #18
		try (DigitalInputDevice did1 = DigitalInputDevice.builder(18).setPullUpDown(GpioPullUpDown.NONE)
				.setActiveHigh(false).build()) {
			// Do stuff, noting that active-high has been overridden to false as the
			// application developer knows that the switch is wired NO with an external pull-up
		}
		
		// NC switch with external pull-up using GPIO #18
		try (DigitalInputDevice did1 = DigitalInputDevice.builder(18).setPullUpDown(GpioPullUpDown.NONE)
				.setActiveHigh(true).build()) {
			// Do stuff, noting that active-high has been overridden to true as the
			// application developer knows that the switch is wired NC with an external pull-up
		}
	}
}
