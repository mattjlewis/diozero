#include "com_diozero_util_PollNative.h"

#include <errno.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <fcntl.h>
#include <poll.h>
#include "com_diozero_util_Util.h"

JNIEXPORT void JNICALL Java_com_diozero_util_PollNative_poll(
		JNIEnv* env, jobject pollNative, jstring filename, jint timeout, jobject ref, jobject callback) {
	// Cache the System.nanoTime class/method references on start-up
	jclass system_class = (*env)->FindClass(env, "java/lang/System");
	if (system_class == NULL) {
		fprintf(stderr, "Error looking up class java/lang/System");
		return;
	}
	jmethodID nano_time_method_id = (*env)->GetStaticMethodID(env, system_class, "nanoTime", "()J");
	if (nano_time_method_id == NULL) {
		fprintf(stderr, "Error looking up methodID for java/lang/System.nanoTime()J");
		return;
	}

	jclass callback_class = (*env)->GetObjectClass(env, callback);
	if (callback_class == NULL) {
		printf("Error: poll() could not get callback class\n");
		return;
	}
	char* notify_method_name = "notify";
	char* notify_signature = "(Ljava/lang/String;JC)V";
	jmethodID notify_method_id = (*env)->GetMethodID(env, callback_class, notify_method_name, notify_signature);
	if (notify_method_id == NULL) {
		printf("Unable to find method '%s' with signature '%s' in callback object\n", notify_method_name, notify_signature);
		return;
	}

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
	read(fd, &c, BUF_LEN-1);

	int retval;

	struct pollfd pfd;
	pfd.fd = fd;
	pfd.events = POLLPRI | POLLERR | POLLHUP | POLLNVAL;
	//pfd.events = POLLPRI;
	unsigned long long epoch_time;

	while (1) {
		// TODO How to interrupt the blocking poll call?
		retval = poll(&pfd, 1, timeout);
		// Get the Java nano time as early as possible
		jlong nano_time = (*env)->CallStaticLongMethod(env, system_class, nano_time_method_id);
		epoch_time = getEpochTime();

		lseek(fd, 0, SEEK_SET); /* consume the interrupt */
		memset(c, 0, BUF_LEN);
		long r = read(fd, &c, BUF_LEN-1);

		if (retval < 0 || (pfd.revents & POLLNVAL) || r <= 0) {
			printf("Invalid response");
			break;
		} else if (retval > 0) {
			(*env)->CallVoidMethod(env, callback, notify_method_id, ref, epoch_time, nano_time, c[0]);
		}
	}

	close(fd);
}

JNIEXPORT void JNICALL Java_com_diozero_util_PollNative_stop
  (JNIEnv* env, jobject obj, jint fd) {
	close(fd);
}
