package com.diozero.sampleapps;

import org.tinylog.Logger;

import com.diozero.devices.SGP30;
import com.diozero.devices.SGP30.FeatureSetVersion;
import com.diozero.util.SleepUtil;

public class SGP30Test {
	public static void main(String[] args) {
		int controller = 1;
		if (args.length > 0) {
			controller = Integer.parseInt(args[0]);
		}
		try (SGP30 sgp30 = new SGP30(controller)) {
			// Product Type should be SGP30_PRODUCT_TYPE
			// Product Feature Set should be 0x22
			FeatureSetVersion fsv = sgp30.getFeatureSetVersion();
			System.out.println(fsv);
			if (fsv.getProductType() != SGP30.PRODUCT_TYPE) {
				Logger.error("Incorrect product type - got {}, expected {}", Integer.valueOf(fsv.getProductType()),
						Integer.valueOf(SGP30.PRODUCT_TYPE));
			}
			System.out.format("Serial Id: 0x%X%n", Long.valueOf(sgp30.getSerialId()));
			System.out.println("Baseline: " + sgp30.getIaqBaseline());
			System.out.println("Total VOC Inceptive Baseline: " + sgp30.getTvocInceptiveBaseline());
			sgp30.measureTest();

			System.out.println("Raw: " + sgp30.rawMeasurement());

			sgp30.start(measurement -> System.out.println(measurement));

			SleepUtil.sleepSeconds(17);

			System.out.println("Raw: " + sgp30.rawMeasurement());

			SleepUtil.sleepSeconds(5);
		}

		// System.exit(1);
	}
}
