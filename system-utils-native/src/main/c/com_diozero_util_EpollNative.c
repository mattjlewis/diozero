/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Native System Utilities
 * Filename:     com_diozero_util_EpollNative.c
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

#include "com_diozero_util_EpollNative.h"

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>

#include <fcntl.h>
#include <sys/time.h>

#include <sys/epoll.h>

#include <jni.h>

#include "com_diozero_util_Util.h"

extern jmethodID epollNativeCallbackMethod;
extern jclass epollEventClassRef;
extern jmethodID epollEventConstructor;

const char INTERRUPT = 'I';

/*
 * Class:     com_diozero_util_EpollNative
 * Method:    epollCreate
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_diozero_util_EpollNative_epollCreate(
		JNIEnv * env, jclass clz) {
	// Note since Linux 2.6.8, the size argument is ignored, but must be greater than zero
	return epoll_create(1);
}

/*
 * Class:     com_diozero_util_EpollNative
 * Method:    addFile
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_diozero_util_EpollNative_addFile(
		JNIEnv * env, jclass clz, jint epollFd, jstring filename) {
	jsize len = (*env)->GetStringLength(env, filename);
	char c_filename[len];
	(*env)->GetStringUTFRegion(env, filename, 0, len, c_filename);

	int fd = open(c_filename, O_RDONLY | O_NONBLOCK);
	if (fd < 0) {
		fprintf(stderr, "open: file %s could not be opened, %s\n", c_filename, strerror(errno));
		return -1;
	}

	/* Consume any prior interrupts */
	char buf;
	lseek(fd, 0, SEEK_SET);
	read(fd, &buf, 1);

	struct epoll_event ev;
	ev.events = EPOLLIN | EPOLLPRI | EPOLLET;
	ev.data.fd = fd;

	if (epoll_ctl(epollFd, EPOLL_CTL_ADD, fd, &ev) == -1) {
		fprintf(stderr, "epoll_ctl: error in add, %s\n", strerror(errno));
		return -1;
	}

	return fd;
}

/*
 * Class:     com_diozero_util_EpollNative
 * Method:    removeFile
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_com_diozero_util_EpollNative_removeFile(
		JNIEnv * env, jclass clz, jint epollFd, jint fileFd)
{
	int rc = epoll_ctl(epollFd, EPOLL_CTL_DEL, fileFd, NULL);
	if (rc != 0) {
		fprintf(stderr, "close: error in epoll_ctl(%d, EPOLL_CTL_DEL, %d), %s\n", epollFd, fileFd, strerror(errno));
	}

	rc = close(fileFd);
	if (rc != 0) {
		fprintf(stderr, "close: error closing fd %d, %s\n", fileFd, strerror(errno));
	}
	return rc;
}

/*
 * Class:     com_diozero_util_EpollNative
 * Method:    waitForEvents
 * Signature: (I)[Lcom/diozero/util/NativePollEvent;
 */
JNIEXPORT jobjectArray JNICALL Java_com_diozero_util_EpollNative_waitForEvents(
		JNIEnv * env, jclass clz, jint epollFd) {
	int max_events = 10;
	struct epoll_event epoll_events[max_events];
	int num_fds = epoll_wait(epollFd, epoll_events, max_events, -1);
	// Get the Java nano time and epoch time as early as possible
	jlong nano_time = getJavaTimeNanos();
	jlong epoch_time = getEpochTimeMillis();
	if (num_fds < 0) {
		fprintf(stderr, "epoll_wait failed! %s\n", strerror(errno));
		return NULL;
	}

	jobjectArray poll_events = (*env)->NewObjectArray(env, num_fds, epollEventClassRef, NULL);
	char val;
	int i;
	for (i=0; i<num_fds; i++) {
		// Consume the interrupt
		lseek(epoll_events[i].data.fd, 0, SEEK_SET);
		read(epoll_events[i].data.fd, &val, 1);

		jobject poll_event = (*env)->NewObject(env, epollEventClassRef, epollEventConstructor,
				epoll_events[i].data.fd, epoll_events[i].events, epoch_time, nano_time, val);
		(*env)->SetObjectArrayElement(env, poll_events, i, poll_event);
	}

	return poll_events;
}

/*
 * Class:     com_diozero_util_EpollNative
 * Method:    eventLoop
 * Signature: (ILcom/diozero/util/PollEventListener;)V
 */
JNIEXPORT jint JNICALL Java_com_diozero_util_EpollNative_eventLoop(
		JNIEnv * env, jclass clz, jint epollFd, jobject callback) {
	int max_events = 40;
	struct epoll_event epoll_events[max_events];

	int running = 1;
	while (running) {
		int num_fds = epoll_wait(epollFd, epoll_events, max_events, -1);
		// Get the Java nano time and epoch time as early as possible
		jlong nano_time = getJavaTimeNanos();
		jlong epoch_time = getEpochTimeMillis();
		if (num_fds < 0) {
			fprintf(stderr, "epoll_wait failed! %s\n", strerror(errno));
			return -1;
		}

		char val;
		int i;
		for (i=0; i<num_fds; i++) {
			// Consume the interrupt
			lseek(epoll_events[i].data.fd, 0, SEEK_SET);
			// Note this only reads one byte - not very generic, developed for sysfs GPIO events
			read(epoll_events[i].data.fd, &val, 1);

			if (val == INTERRUPT) {
				running = 0;
			} else {
				(*env)->CallVoidMethod(env, callback, epollNativeCallbackMethod,
						epoll_events[i].data.fd, epoll_events[i].events, epoch_time, nano_time, val);
			}
		}
	}

	return 0;
}

/*
 * Class:     com_diozero_util_EpollNative
 * Method:    stopWait
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_diozero_util_EpollNative_stopWait(
		JNIEnv * env, jclass clz, jint epollFd) {
	int pipefds[2] = {};
	struct epoll_event ev = {};
	int rc = pipe(pipefds);
	if (rc < 0) {
		fprintf(stderr, "Error creating pipe: %s\n", strerror(errno));
		return;
	}
	int read_pipe = pipefds[0];
	int write_pipe = pipefds[1];

	/*
	// Make the read-end non-blocking
	int flags = fcntl(read_pipe, F_GETFL, 0);
	rc = fcntl(read_pipe, F_SETFL, flags | O_NONBLOCK);
	if (rc < 0) {
		fprintf(stderr, "Error calling fcntl on read pipe: %s\n", strerror(errno));
		close(read_pipe);
		close(write_pipe);
		return;
	}
	*/

	// Add the read end to the epoll
	ev.events = EPOLLIN;
	ev.data.fd = read_pipe;
	rc = epoll_ctl(epollFd, EPOLL_CTL_ADD, read_pipe, &ev);
	if (rc < 0) {
		fprintf(stderr, "Error adding read pipe to epoll: %s\n", strerror(errno));
		close(read_pipe);
		close(write_pipe);
		return;
	}

	write(write_pipe, &INTERRUPT, 1);

	rc = epoll_ctl(epollFd, EPOLL_CTL_DEL, read_pipe, NULL);
	rc = close(write_pipe);
}

/*
 * Class:     com_diozero_util_EpollNative
 * Method:    shutdown
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_diozero_util_EpollNative_shutdown(
		JNIEnv * env, jclass clz, jint epollFd) {
	close(epollFd);
}
