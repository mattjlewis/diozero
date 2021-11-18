package com.diozero.sampleapps.sandpit;

import com.diozero.devices.sandpit.VL6180;
import com.diozero.util.SleepUtil;

public class VL6180Test {
	public static void main(String[] args) {
		try (VL6180 vl6180 = new VL6180()) {
			System.out.format("Model id: 0x%x%n", vl6180.getModelId());
			System.out.format("Model revision: v%d.%d%n", vl6180.getModelMajor(), vl6180.getModelMinor());
			System.out.format("Module revision: v%d.%d%n", vl6180.getModuleMajor(), vl6180.getModuleMinor());
			System.out.format("Manufactured on: %s, phase %d%n", vl6180.getManufactureDateTime(),
					vl6180.getManufacturePhase());

			for (int i = 0; i < 10; i++) {
				System.out.println("Distance: " + vl6180.getDistanceCm() + " cm");
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
