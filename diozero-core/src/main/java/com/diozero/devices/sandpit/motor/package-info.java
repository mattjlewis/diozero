/**
 * <p>Currently, these classes comprise the objects for
 * <a href="https://learn.adafruit.com/all-about-stepper-motors">stepper motors</a>.
 * Additional support for the "chopper" style is also included.</p>
 * <p><strong>Direction of Rotation</strong></p>
 * <p>The rotational direction is depenendent on the actual hardware hookup of the motor.
 * The direction can be changed either through application code (use it "backwards"
 * transparently) or by re-attaching the stepper in a different configuration.</p>
 * <p><strong>Speed of Rotation</strong></p>
 * <p>This is highly variable and dependent on many factors: operating system(s), hardware, etc. Each implementation
 * will need to determine how to best utilize the motor.</p>
 */

package com.diozero.devices.sandpit.motor;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     package-info.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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
