package com.diozero.sampleapps;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
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


import java.io.IOException;
import java.util.function.Consumer;

import com.diozero.Button;
import com.diozero.api.DigitalPinEvent;
import com.diozero.api.GpioPullUpDown;
import com.diozero.util.SleepUtil;

public class ButtonTest implements Consumer<DigitalPinEvent> {
	public static void main(String[] args) {
		new ButtonTest().test();
	}
	
	public void test() {
		try (Button button = new Button(12, GpioPullUpDown.PULL_UP)) {
			button.setConsumer(this);
			System.out.println("Sleeping for 10s, thread name=" + Thread.currentThread().getName());
			SleepUtil.sleepSeconds(10);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	@Override
	public void accept(DigitalPinEvent event) {
		System.out.println("accept(" + event + "), thread name=" + Thread.currentThread().getName());
	}
}
