package com.diozero.remote.message;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Server
 * Filename:     GetBoardInfoResponse.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 mattjlewis
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

import java.util.List;

public class GetBoardInfoResponse extends Response {
	private static final long serialVersionUID = 1060208133208783116L;

	private String make;
	private String model;
	private int memory;
	private List<GpioInfo> gpios;
	
	public GetBoardInfoResponse(String make, String model, int memory, List<GpioInfo> gpios, String correlationId) {
		this(Status.OK, null, make, model, memory, gpios, correlationId);
	}
	
	public GetBoardInfoResponse(Status status, String detail, String make, String model, int memory,
			List<GpioInfo> gpios, String correlationId) {
		super(status, detail, correlationId);
		
		this.make = make;
		this.model = model;
		this.memory = memory;
		this.gpios = gpios;
	}

	public String getMake() {
		return make;
	}

	public String getModel() {
		return model;
	}
	
	public int getMemory() {
		return memory;
	}

	public List<GpioInfo> getGpios() {
		return gpios;
	}
}
