#include "com_diozero_util_EpollNative.h"

#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <sys/time.h>
#include <unistd.h>
#include <sys/epoll.h>

#include "com_diozero_util_Util.h"

static jclass systemClassRef = NULL;
static jmethodID nanoTimeMethodId = NULL;
static jclass epollEventClassRef = NULL;
static jmethodID epollEventConstructor = NULL;

/*
 * Class:     com_diozero_util_EpollNative
 * Method:    epollCreate
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_diozero_util_EpollNative_epollCreate(
		JNIEnv * env, jclass clz) {
	// Cache the System.nanoTime class/method references on start-up
	jclass system_class = (*env)->FindClass(env, "java/lang/System");
	if (system_class == NULL) {
		fprintf(stderr, "Error looking up class java/lang/System");
		return -1;
	}
	nanoTimeMethodId = (*env)->GetStaticMethodID(env, system_class, "nanoTime", "()J");
	if (nanoTimeMethodId == NULL) {
		fprintf(stderr, "Error looking up methodID for java/lang/System.nanoTime()J");
		return -1;
	}

	char* class_name = "com/diozero/util/EpollEvent";
	jclass epoll_event_class = (*env)->FindClass(env, class_name);
	if (epoll_event_class == NULL) {
		fprintf(stderr, "Could not find class '%s'\n", class_name);
		return -1;
	}

	char* signature = "(IIJJB)V";
	epollEventConstructor = (*env)->GetMethodID(env, epoll_event_class, "<init>", signature);
	if (epollEventConstructor == NULL) {
		fprintf(stderr, "Could not find constructor with signature '%s' in class '%s'\n", signature, class_name);
		return -1;
	}

	systemClassRef = (*env)->NewGlobalRef(env, system_class);
	epollEventClassRef = (*env)->NewGlobalRef(env, epoll_event_class);

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

	/* consume any prior interrupts */
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
	// Get the Java nano time as early as possible
	jlong nano_time = (*env)->CallStaticLongMethod(env, systemClassRef, nanoTimeMethodId);
	unsigned long long epoch_time = getEpochTime();
	if (num_fds < 0) {
		fprintf(stderr, "epoll_wait failed! %s\n", strerror(errno));
		return NULL;
	}

	jobjectArray poll_events = (*env)->NewObjectArray(env, num_fds, epollEventClassRef, NULL);
	char val;
	int i;
	for (i=0; i<num_fds; i++) {
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
 * Method:    stopWait
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_diozero_util_EpollNative_stopWait(
		JNIEnv * env, jclass clz, jint epollFd) {
	 int pipefds[2] = {};
	 struct epoll_event ev = {};
	 pipe(pipefds);
	 int read_pipe = pipefds[0];
	 int write_pipe = pipefds[1];

	 // make read-end non-blocking
	 int flags = fcntl(read_pipe, F_GETFL, 0);
	 fcntl(write_pipe, F_SETFL, flags|O_NONBLOCK);

	 // add the read end to the epoll
	 ev.events = EPOLLIN;
	 ev.data.fd = read_pipe;
	 epoll_ctl(epollFd, EPOLL_CTL_ADD, read_pipe, &ev);

	 const char* terminate = "terminate";
	 write(write_pipe, terminate, 1);
}

/*
 * Class:     com_diozero_util_EpollNative
 * Method:    shutdown
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_diozero_util_EpollNative_shutdown(
		JNIEnv * env, jclass clz, jint epollFd) {
	close(epollFd);

	(*env)->DeleteGlobalRef(env, systemClassRef);
	(*env)->DeleteGlobalRef(env, epollEventClassRef);
}
