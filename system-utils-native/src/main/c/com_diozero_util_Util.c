/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Native System Utilities
 * Filename:     com_diozero_util_Util.c
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

#include "com_diozero_util_Util.h"

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <sys/time.h>

jclass epollEventClassRef = NULL;
jmethodID epollEventConstructor = NULL;
jclass mmapByteBufferClassRef = NULL;
jmethodID mmapByteBufferConstructor = NULL;

/* The VM calls this function upon loading the native library. */
jint JNI_OnLoad(JavaVM* jvm, void* reserved) {
	JNIEnv* env;
	if ((*jvm)->GetEnv(jvm, (void **) &env, JNI_VERSION_1_8) != JNI_OK) {
		fprintf(stderr, "Error, unable to get JNIEnv\n");
		return JNI_ERR;
	}

	// Cache the EpollEvent constructor on startup
	char* class_name = "com/diozero/util/EpollEvent";
	jclass epoll_event_class = (*env)->FindClass(env, class_name);
	if ((*env)->ExceptionCheck(env) || epoll_event_class == NULL) {
		fprintf(stderr, "Error looking up class %s\n", class_name);
		return JNI_ERR;
	}
	char* method_name = "<init>";
	char* signature = "(IIJJB)V";
	epollEventConstructor = (*env)->GetMethodID(env, epoll_event_class, method_name, signature);
	if ((*env)->ExceptionCheck(env) || epollEventConstructor == NULL) {
		fprintf(stderr, "Error looking up methodID for %s.%s%s\n", class_name, method_name, signature);
		return JNI_ERR;
	}

	// Cache the MmapByteBuffer constructor on startup
	class_name = "com/diozero/util/MmapByteBuffer";
	jclass mmap_byte_buffer_class = (*env)->FindClass(env, class_name);
	if ((*env)->ExceptionCheck(env) || mmap_byte_buffer_class == NULL) {
		printf("Error, could not find class '%s'\n", class_name);
		return JNI_ERR;
	}
	method_name = "<init>";
	signature = "(IIILjava/nio/ByteBuffer;)V";
	mmapByteBufferConstructor = (*env)->GetMethodID(env, mmap_byte_buffer_class, method_name, signature);
	if ((*env)->ExceptionCheck(env) || mmapByteBufferConstructor == NULL) {
		fprintf(stderr, "Error looking up methodID for %s.%s%s\n", class_name, method_name, signature);
		return JNI_ERR;
	}

	epollEventClassRef = (*env)->NewGlobalRef(env, epoll_event_class);
	mmapByteBufferClassRef = (*env)->NewGlobalRef(env, mmap_byte_buffer_class);

	return JNI_VERSION_1_8;
}

// Is automatically called once the Classloader is destroyed
void JNI_OnUnload(JavaVM *vm, void *reserved) {
	JNIEnv* env;

	if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
		// Nothing we can do about this
		return;
	}

	if (epollEventClassRef != NULL) {
		(*env)->DeleteGlobalRef(env, epollEventClassRef);
	}
	if (mmapByteBufferClassRef != NULL) {
		(*env)->DeleteGlobalRef(env, mmapByteBufferClassRef);
	}
}

jlong getEpochTime() {
	struct timeval tp;
	/*int rc = */gettimeofday(&tp, NULL);
	return ((jlong) tp.tv_sec) * 1000 + ((jlong) tp.tv_usec / 1000);
}

jlong getEpochTime2() {
	struct timespec ts;
	/*int rc = */clock_gettime(CLOCK_REALTIME, &ts);
	return ((jlong) ts.tv_sec) * 1000 + ((jlong) ts.tv_nsec / 1000 / 1000);
}

jlong getJavaNanoTime() {
	struct timespec ts;
	/*int rc = */clock_gettime(CLOCK_MONOTONIC, &ts);
	return ((jlong) ts.tv_sec) * (1000 * 1000 * 1000) + ((jlong) ts.tv_nsec);
}

// See: http://stas-blogspot.blogspot.co.uk/2012/02/what-is-behind-systemnanotime.html
// http://hg.openjdk.java.net/jdk7/jdk7/hotspot/file/9b0ca45cd756/src/os/linux/vm/os_linux.cpp
jlong javaTimeNanos() {
	int supports_monotonic_clock = 1;
	if (supports_monotonic_clock) {
		struct timespec tp;
		/*int status = */clock_gettime(CLOCK_MONOTONIC, &tp);
		//assert(status == 0, "gettime error");
		jlong result = ((jlong) tp.tv_sec) * (1000 * 1000 * 1000) + ((jlong) tp.tv_nsec);
		return result;
	} else {
		struct timeval time;
		/*int status = */gettimeofday(&time, NULL);
		//assert(status != -1, "linux error");
		jlong usecs = ((jlong) time.tv_sec) * (1000 * 1000) + ((jlong) time.tv_usec);
		return 1000 * usecs;
	}
}
