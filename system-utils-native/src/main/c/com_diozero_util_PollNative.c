/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Native System Utilities
 * Filename:     com_diozero_util_PollNative.c
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

#include "com_diozero_util_PollNative.h"

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>

#include <fcntl.h>
#include <poll.h>

#include <jni.h>

#include "com_diozero_util_Util.h"

extern jmethodID pollEventListenerNotifyMethod;

JNIEXPORT void JNICALL Java_com_diozero_util_PollNative_poll(
		JNIEnv* env, jobject pollNative, jstring filename, jint timeout, jint ref, jobject callback) {
	jsize len = (*env)->GetStringLength(env, filename);
	char c_filename[len];
	(*env)->GetStringUTFRegion(env, filename, 0, len, c_filename);

	int fd = open(c_filename, O_RDONLY | O_NONBLOCK);
	if (fd < 0) {
		printf("open: file %s could not be opened, %s\n", c_filename, strerror(errno));
		return;
	}

	jclass poll_native_class = (*env)->GetObjectClass(env, pollNative);
	if (poll_native_class == NULL) {
		printf("Error: poll() could not get PollNative class\n");
		return;
	}
	char* set_fd_method_name = "setFd";
	char* set_fd_signature = "(I)V";
	jmethodID set_fd_method_id = (*env)->GetMethodID(env, poll_native_class, set_fd_method_name, set_fd_signature);
	if (set_fd_method_id == NULL) {
		printf("Unable to find method '%s' with signature '%s' in PollNative object\n", set_fd_method_name, set_fd_signature);
		return;
	}
	(*env)->CallVoidMethod(env, pollNative, set_fd_method_id, fd);

	const int BUF_LEN = 2;
	uint8_t c[BUF_LEN];
	memset(c, 0, BUF_LEN);

	lseek(fd, 0, SEEK_SET); /* consume any prior interrupts */
	int rc = read(fd, &c, BUF_LEN-1);
	if (rc < 0) {
		perror("read error");
	}

	int retval;

	struct pollfd pfd;
	pfd.fd = fd;
	pfd.events = POLLPRI | POLLERR | POLLHUP | POLLNVAL;
	//pfd.events = POLLPRI;
	jlong nano_time;
	jlong epoch_time;

	while (1) {
		// TODO How to interrupt the blocking poll call?
		retval = poll(&pfd, 1, timeout);
		// Get the Java nano time as early as possible
		nano_time = getJavaTimeNanos();
		epoch_time = getEpochTimeMillis();

		lseek(fd, 0, SEEK_SET); /* consume the interrupt */
		memset(c, 0, BUF_LEN);
		long r = read(fd, &c, BUF_LEN-1);

		if (retval < 0 || (pfd.revents & POLLNVAL) || r <= 0) {
			printf("Invalid response");
			break;
		} else if (retval > 0) {
			(*env)->CallVoidMethod(env, callback, pollEventListenerNotifyMethod, ref, epoch_time, nano_time, c[0]);
		}
	}

	close(fd);
}

JNIEXPORT void JNICALL Java_com_diozero_util_PollNative_stop
  (JNIEnv* env, jobject obj, jint fd) {
	close(fd);
}
