package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     ButtonPudTriggerTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.function.DeviceEventConsumer;
import com.diozero.devices.Button;
import com.diozero.util.SleepUtil;

public class ButtonPudTriggerTest implements DeviceEventConsumer<DigitalInputEvent> {
	public static void main(String[] args) {
		ButtonPudTriggerTest listener = new ButtonPudTriggerTest();
		int delay_s = 10;
		if (args.length > 0) {
			delay_s = Integer.parseInt(args[0]);
		}
		
		try (Button pud_d_both = new Button(17, GpioPullUpDown.PULL_DOWN, GpioEventTrigger.BOTH); //
				Button pud_d_rising = new Button(19, GpioPullUpDown.PULL_DOWN, GpioEventTrigger.RISING); //
				Button pud_d_falling = new Button(26, GpioPullUpDown.PULL_DOWN, GpioEventTrigger.FALLING); //
				Button pud_x_both = new Button(18, GpioPullUpDown.NONE, GpioEventTrigger.BOTH); //
				Button pud_u_both = new Button(5, GpioPullUpDown.PULL_UP, GpioEventTrigger.BOTH); //
				Button pud_u_rising = new Button(6, GpioPullUpDown.PULL_UP, GpioEventTrigger.RISING);//
				Button pud_u_falling = new Button(4, GpioPullUpDown.PULL_UP, GpioEventTrigger.FALLING)) {
			pud_d_both.addListener(listener);
			pud_d_both.whenActivated(t -> System.out.println("pud_d_both activated " + t));
			pud_d_both.whenDeactivated(t -> System.out.println("pud_d_both deactivated " + t));

			pud_d_rising.addListener(listener);
			pud_d_rising.whenActivated(t -> System.out.println("pud_d_rising activated " + t));
			pud_d_rising.whenDeactivated(t -> System.out.println("pud_d_rising deactivated " + t));

			pud_d_falling.addListener(listener);
			pud_d_falling.whenActivated(t -> System.out.println("pud_d_falling activated " + t));
			pud_d_falling.whenDeactivated(t -> System.out.println("pud_d_falling deactivated " + t));

			pud_x_both.addListener(listener);
			pud_x_both.whenActivated(t -> System.out.println("pud_x_both activated " + t));
			pud_x_both.whenDeactivated(t -> System.out.println("pud_x_both deactivated " + t));

			pud_u_both.addListener(listener);
			pud_u_both.whenActivated(t -> System.out.println("pud_u_both activated " + t));
			pud_u_both.whenDeactivated(t -> System.out.println("pud_u_both deactivated " + t));

			pud_u_rising.addListener(listener);
			pud_u_rising.whenActivated(t -> System.out.println("pud_u_rising activated " + t));
			pud_u_rising.whenDeactivated(t -> System.out.println("pud_u_rising deactivated " + t));

			pud_u_falling.addListener(listener);
			pud_u_falling.whenActivated(t -> System.out.println("pud_u_falling activated " + t));
			pud_u_falling.whenDeactivated(t -> System.out.println("pud_u_falling deactivated " + t));
			
			System.out.println("Sleeping for " + delay_s + " seconds");
			SleepUtil.sleepSeconds(delay_s);
		}
	}

	@Override
	public void accept(DigitalInputEvent event) {
		System.out.println(event);
	}
}
