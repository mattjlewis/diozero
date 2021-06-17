/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Native System Utilities
 * Filename:     com_diozero_util_FileNative.c
 *
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

#include "com_diozero_util_FileNative.h"

#include <unistd.h>
#include <fcntl.h>

#include <jni.h>

/*
 * Class:     com_diozero_util_FileNative
 * Method:    open
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_diozero_util_FileNative_open(
		JNIEnv * env, jclass clz, jstring filename, jint flags) {
	jboolean is_copy;
	const char* buf = (*env)->GetStringUTFChars(env, filename, &is_copy);

	int fd = open(buf, flags);
	(*env)->ReleaseStringUTFChars(env, filename, buf);
	return fd;
}

/*
 * Class:     com_diozero_util_FileNative
 * Method:    close
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_diozero_util_FileNative_close(
		JNIEnv * env, jclass clz, jint fd) {
	return close(fd);
}
