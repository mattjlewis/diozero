package com.diozero.internal.provider.builtin.i2c;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     I2cRetryTest.java
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class I2cRetryTest {
	private static final int OK = 0;
	private static final int ERROR = -1;
	private static final int EAGAIN = -11;
	private static final int ETIMEDOUT = -110;
	private static final int EREMOTEIO = -121;
	
	private int numRetries = 3;
	
	@Test
	public void testOk() {
		int rc = EAGAIN;
		int i;
		for (i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT || rc == EREMOTEIO); i++) {
			rc = OK;
		}
		
		Assertions.assertEquals(1, i);
		Assertions.assertEquals(OK, rc);
	}
	
	@Test
	public void testError() {
		int rc = EAGAIN;
		int i;
		for (i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT || rc == EREMOTEIO); i++) {
			rc = ERROR;
		}
		
		Assertions.assertEquals(1, i);
		Assertions.assertEquals(ERROR, rc);
	}
	
	@Test
	public void testRetryThenOk() {
		int rc = EAGAIN;
		int i;
		for (i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT || rc == EREMOTEIO); i++) {
			rc = nativeOperation(i, true);
			switch (i) {
			case 0:
				Assertions.assertEquals(EAGAIN, rc);
				break;
			case 1:
				Assertions.assertEquals(ETIMEDOUT, rc);
				break;
			default:
				Assertions.assertEquals(OK, rc);
			}
		}
		
		Assertions.assertEquals(numRetries, i);
		Assertions.assertEquals(OK, rc);
	}
	
	@Test
	public void testRetryExceeded() {
		int rc = EAGAIN;
		int i;
		for (i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT || rc == EREMOTEIO); i++) {
			rc = EAGAIN;
		}
		
		Assertions.assertEquals(numRetries, i);
		Assertions.assertEquals(EAGAIN, rc);
	}
	
	@Test
	public void testRetryThenError() {
		int rc = EAGAIN;
		int i;
		for (i = 0; i < numRetries && (rc == EAGAIN || rc == ETIMEDOUT || rc == EREMOTEIO); i++) {
			rc = nativeOperation(i, false);
			switch (i) {
			case 0:
				Assertions.assertEquals(EAGAIN, rc);
				break;
			case 1:
				Assertions.assertEquals(ETIMEDOUT, rc);
				break;
			default:
				Assertions.assertEquals(ERROR, rc);
			}
		}

		Assertions.assertEquals(numRetries, i);
		Assertions.assertEquals(ERROR, rc);
	}
	
	private static int nativeOperation(int retryNum, boolean result) {
		switch (retryNum) {
		case 0:
			return EAGAIN;
		case 1:
			return ETIMEDOUT;
		default:
			return result ? OK : ERROR;
		}
	}
}
