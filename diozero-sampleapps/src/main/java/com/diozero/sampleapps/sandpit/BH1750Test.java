package com.diozero.sampleapps.sandpit;

/*-
 * #%L
 * Device I/O Zero - Sample applications
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

import com.diozero.devices.TSL2561;
import com.diozero.devices.TSL2561.TSL2561Package;
import com.diozero.sandpit.BH1750;
import com.diozero.util.SleepUtil;

public class BH1750Test {
	public static void main(String[] args) {
		int controller = 1;
		try (BH1750 bh1750 = new BH1750(controller); TSL2561 tsl2561 = new TSL2561(TSL2561Package.T_FN_CL)) {
			Logger.info("Mode: {}", bh1750.getMode());
			for (int i=0; i<5; i++) {
				Logger.info("Luminosity: BH1750: {0.##}, TSL2561: {0.##}", Float.valueOf(bh1750.getLuminosity()),
						Float.valueOf(tsl2561.getLuminosity()));
				SleepUtil.sleepSeconds(1);
			}
			Logger.info("Mode: {}", bh1750.getMode());
			bh1750.setMode(BH1750.Mode.CONTINUOUS_HIGH_RES_MODE_2);
			for (int i=0; i<5; i++) {
				Logger.info("Luminosity: BH1750: {0.##}, TSL2561: {0.##}", Float.valueOf(bh1750.getLuminosity()),
						Float.valueOf(tsl2561.getLuminosity()));
				SleepUtil.sleepSeconds(1);
			}
			Logger.info("Mode: {}", bh1750.getMode());
			bh1750.setMode(BH1750.Mode.CONTINUOUS_LOW_RES_MODE);
			for (int i=0; i<5; i++) {
				Logger.info("Luminosity: BH1750: {0.##}, TSL2561: {0.##}", Float.valueOf(bh1750.getLuminosity()),
						Float.valueOf(tsl2561.getLuminosity()));
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
