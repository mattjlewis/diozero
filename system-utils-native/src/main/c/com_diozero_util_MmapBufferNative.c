/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Native System Utilities
 * Filename:     com_diozero_util_MmapBufferNative.c
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

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>

#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/file.h>

#include <jni.h>

#include "com_diozero_util_MmapBufferNative.h"

extern jclass mmapByteBufferClassRef;
extern jmethodID mmapByteBufferConstructor;

#if __WORDSIZE == 32
#define long_t uint32_t
#else
#define long_t uint64_t
#endif

jobject createMmapByteBuffer(JNIEnv* env, void* mapPtr, jlong length) {
	return (*env)->NewObject(env, mmapByteBufferClassRef, mmapByteBufferConstructor, (uint64_t) (long_t) mapPtr, length,
			(*env)->NewDirectByteBuffer(env, mapPtr, length));
}

JNIEXPORT jobject JNICALL Java_com_diozero_util_MmapBufferNative_createMmapBuffer(
		JNIEnv* env, jclass clz, jstring path, jlong offset, jlong length) {
	int str_len = (*env)->GetStringLength(env, path);
	char filename[str_len];
	(*env)->GetStringUTFRegion(env, path, 0, str_len, filename);
	int fd = open(filename, O_RDWR | O_SYNC);
	if (fd < 0) {
		// TODO Raise error
		perror("Couldn't open file");
		return NULL;
	}

	void* map_ptr = mmap(0, (size_t) length, PROT_READ|PROT_WRITE, MAP_SHARED, fd, (off_t) (long_t) offset);
	//void* map_ptr = mmap(0, length, PROT_READ|PROT_WRITE, MAP_SHARED|MAP_LOCKED, fd, offset);
	/*
	 * From: https://man7.org/linux/man-pages/man2/mmap.2.html
	 * After the mmap() call has returned, the file descriptor, fd, can
     * be closed immediately without invalidating the mapping.
	 */
	close(fd);

	if (map_ptr == MAP_FAILED) {
		// TODO Raise error
		perror("Error in mmap");
		return NULL;
	}

	return createMmapByteBuffer(env, map_ptr, length);
}

JNIEXPORT void JNICALL Java_com_diozero_util_MmapBufferNative_closeMmapBuffer(
		JNIEnv *env, jclass clz, jlong mapPtr, jlong length) {
	long_t map_ptr = (long_t) mapPtr;
	munmap((void*) map_ptr, length);
}
