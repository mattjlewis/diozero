/*
 * #%L
 * Device I/O Zero - Java Sysfs provider
 * %%
 * Copyright (C) 2016 mattjlewis
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

#include <stdint.h>
#include <sys/ioctl.h>
#include <linux/i2c-dev.h>
#include "com_diozero_internal_provider_sysfs_NativeI2CDevice.h"

#if __WORDSIZE == 32
#define long_t uint32_t
#else
#define long_t uint64_t
#endif

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDevice_selectSlave(
		JNIEnv* env, jclass clz, jint fd, jint address) {
	long_t addr = address;
	return ioctl(fd, I2C_SLAVE, (void*)addr);
}
