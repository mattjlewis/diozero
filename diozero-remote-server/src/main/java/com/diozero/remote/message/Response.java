package com.diozero.remote.message;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - MQTT Server Host Process
 * Filename:     Response.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

import java.io.Serializable;

public class Response implements Serializable {
	public static enum Status {
		OK, ERROR;
	}
	
	public static final Response OK = new Response(Status.OK);

	private static final long serialVersionUID = -6333455741476066202L;
	
	private Status status;
	private String detail;
	
	public Response(Status status) {
		this.status = status;
	}
	
	public Response(Status status, String detail) {
		this.status = status;
		this.detail = detail;
	}

	public Status getStatus() {
		return status;
	}

	public String getDetail() {
		return detail;
	}

	@Override
	public String toString() {
		return "Response [status=" + status + ", detail=" + detail + "]";
	}
}
