#define _GNU_SOURCE

#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>
#include <string.h>
#include <errno.h>
#include <time.h>
#include <stdio.h>
#include <stdint.h>
#include <inttypes.h>
#include <dirent.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/epoll.h>

#include <linux/gpio.h>

#define CONSUMER "diozero"

#include "com_diozero_internal_provider_sysfs_NativeGpioChip.h"
#include <jni.h>

extern jclass arrayListClassRef;
extern jmethodID arrayListConstructor;
extern jmethodID arrayListAddMethod;

extern jclass gpioChipInfoClassRef;
extern jmethodID gpioChipInfoConstructor;

extern jclass nativeGpioChipClassRef;
extern jmethodID nativeGpioChipConstructor;

extern jclass gpioLineClassRef;
extern jmethodID gpioLineConstructor;

extern jmethodID gpioLineEventListenerMethod;

static int dir_filter(const struct dirent *dir) {
	return !strncmp(dir->d_name, "gpiochip", 8);
}

JNIEXPORT jobject JNICALL Java_com_diozero_internal_provider_sysfs_NativeGpioChip_getChips(
		JNIEnv* env, jclass clz) {
	struct dirent **dirs;
	int num_chips = scandir("/dev", &dirs, dir_filter, alphasort);
	if (num_chips <= 0) {
			return NULL;
	}

	jobject chip_array = (*env)->NewObject(env, arrayListClassRef, arrayListConstructor);
	int i;
	for (i=0; i<num_chips; i++) {
		char* chrdev_name;
		if (asprintf(&chrdev_name, "/dev/%s", dirs[i]->d_name) < 0) {
			perror("Error defining chip char dev name");
		} else {
			int chip_fd = open(chrdev_name, O_RDWR | O_CLOEXEC);
			if (chip_fd < 0) {
				perror("Error opening chip char dev");
			} else {
				struct gpiochip_info cinfo;
				if (ioctl(chip_fd, GPIO_GET_CHIPINFO_IOCTL, &cinfo) < 0) {
					perror("Error getting chip info");
				} else {
					jstring name = (*env)->NewStringUTF(env, cinfo.name);
					jstring label = (*env)->NewStringUTF(env, cinfo.label);
					jobject chip_info_obj = (*env)->NewObject(env, gpioChipInfoClassRef, gpioChipInfoConstructor,
							name, label, cinfo.lines);
					(*env)->CallObjectMethod(env, chip_array, arrayListAddMethod, chip_info_obj);
				}
			}

			free(chrdev_name);
			close(chip_fd);
		}

		free(dirs[i]);
	}

	free(dirs);

	return chip_array;
}

JNIEXPORT jobject JNICALL Java_com_diozero_internal_provider_sysfs_NativeGpioChip_openChip(
		JNIEnv* env, jclass clz, jstring filename) {
	const char* chrdev_name = (*env)->GetStringUTFChars(env, filename, NULL);
	int chip_fd = open(chrdev_name, O_RDWR | O_CLOEXEC);
	(*env)->ReleaseStringUTFChars(env, filename, chrdev_name);
	if (chip_fd < 0) {
		perror("Error opening gpiochip file");
		return NULL;
	}

	struct gpiochip_info cinfo;
	if (ioctl(chip_fd, GPIO_GET_CHIPINFO_IOCTL, &cinfo) < 0) {
		perror("Error getting chip info");
		return NULL;
	}
	fprintf(stdout, "GPIO chip: %s, \"%s\", %u GPIO lines\n", cinfo.name, cinfo.label, cinfo.lines);

	// Loop over the lines
	jobjectArray lines = (*env)->NewObjectArray(env, cinfo.lines, gpioLineClassRef, NULL);
	int i;
	for (i=0; i<cinfo.lines; i++) {
		struct gpioline_info linfo;
		memset(&linfo, 0, sizeof(linfo));
		linfo.line_offset = i;
		if (ioctl(chip_fd, GPIO_GET_LINEINFO_IOCTL, &linfo) < 0) {
			perror("Failed to issue LINEINFO IOCTL\n");
		} else {
			jstring line_name = (*env)->NewStringUTF(env, linfo.name);
			jstring line_consumer = NULL;
			if (linfo.consumer != NULL && strlen(linfo.consumer) > 0) {
				line_consumer = (*env)->NewStringUTF(env, linfo.consumer);
			}
			jobject line_obj = (*env)->NewObject(env, gpioLineClassRef, gpioLineConstructor,
					linfo.line_offset, linfo.flags, line_name, line_consumer);
			(*env)->SetObjectArrayElement(env, lines,i, line_obj);
		}
	}

	jstring name = (*env)->NewStringUTF(env, cinfo.name);
	jstring label = (*env)->NewStringUTF(env, cinfo.label);
	jobject chip_obj = (*env)->NewObject(env, nativeGpioChipClassRef, nativeGpioChipConstructor,
			name, label, chip_fd, lines);

	return chip_obj;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeGpioChip_provisionGpioInputDevice(
		JNIEnv* env, jclass clz, jint chipFd, jint gpioOffset, jint handleFlags, jint eventFlags) {
	/*
	struct gpiohandle_request handle_req;

	strcpy(handle_req.consumer_label, CONSUMER);
	handle_req.flags = GPIOHANDLE_REQUEST_INPUT | handleFlags;
	handle_req.lines = 1;
	handle_req.lineoffsets[0] = gpioOffset;

	if (ioctl(chipFd, GPIO_GET_LINEHANDLE_IOCTL, &handle_req) < 0) {
		perror("Error getting line handle");
		return -1;
	}

	int line_fd = handle_req.fd;
	*/

	// Enable events
	struct gpioevent_request event_req;
	memset(&event_req, 0, sizeof(event_req));
	strcpy(event_req.consumer_label, CONSUMER);
	event_req.handleflags |= GPIOHANDLE_REQUEST_INPUT | handleFlags;
	event_req.lineoffset = gpioOffset;
	event_req.eventflags = eventFlags;

	if (ioctl(chipFd, GPIO_GET_LINEEVENT_IOCTL, &event_req)) {
		perror("Error setting line event");
		return -errno;
	}

	return event_req.fd;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeGpioChip_provisionGpioOutputDevice(
		JNIEnv* env, jclass clz, jint chipFd, jint gpioOffset, jint initialValue) {
	struct gpiohandle_request req;

	req.flags = GPIOHANDLE_REQUEST_OUTPUT;
	strcpy(req.consumer_label, CONSUMER);
	req.lines = 1;
	req.lineoffsets[0] = gpioOffset;
	req.default_values[0] = initialValue;

	if (ioctl(chipFd, GPIO_GET_LINEHANDLE_IOCTL, &req) < 0) {
		perror("Error getting line handle");
		return -errno;
	}

	return req.fd;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeGpioChip_getValue(
		JNIEnv* env, jclass clz, jint chipFd, jint gpioOffset) {
	struct gpiohandle_data data;
	if (ioctl(chipFd, GPIOHANDLE_GET_LINE_VALUES_IOCTL, &data) < 0) {
		perror("Error setting GPIO values");
		return -errno;
	}

	return data.values[0];
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeGpioChip_setValue(
		JNIEnv* env, jclass clz, jint chipFd, jint gpioOffset, jint value) {
	struct gpiohandle_data data;
	memset(&data, 0, sizeof(data));

	data.values[0] = value;
	if (ioctl(chipFd, GPIOHANDLE_SET_LINE_VALUES_IOCTL, &data) < 0) {
		perror("Error setting GPIO value");
		return -errno;
	}

	return 0;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeGpioChip_epollCreate(
		JNIEnv* env, jclass clz) {
	// Note since Linux 2.6.8, the size argument is ignored, but must be greater than zero
	return epoll_create(1);
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeGpioChip_epollAddFileDescriptor(
		JNIEnv* env, jclass clz, jint epollFd, jint fd) {
	struct epoll_event ev;
	ev.events = EPOLLIN | EPOLLPRI | EPOLLET;
	ev.data.fd = fd;

	int rc = epoll_ctl(epollFd, EPOLL_CTL_ADD, fd, &ev);
	if (rc < 0) {
		perror("Error in epoll_ctl EPOLL_CTL_ADD");
		return -errno;
	}

	return 0;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeGpioChip_epollRemoveFileDescriptor(
		JNIEnv* env, jclass clz, jint epollFd, jint fd) {
	int rc = epoll_ctl(epollFd, EPOLL_CTL_DEL, fd, NULL);
	if (rc < 0) {
		perror("Error in epoll_ctl EPOLL_CTL_DEL");
		return -errno;
	}

	return 0;
}

JNIEXPORT void JNICALL Java_com_diozero_internal_provider_sysfs_NativeGpioChip_eventLoop(
		JNIEnv* env, jclass clz, jint epollFd, jint timeout, jobject callback) {
	int max_events = 40;
	struct epoll_event epoll_events[max_events];
	memset(&epoll_events, 0, sizeof(epoll_events));

	// https://github.com/torvalds/linux/blob/v5.4/include/uapi/linux/gpio.h#L148
	struct gpioevent_data evdata;
	memset(&evdata, 0, sizeof(evdata));

	int num_fds;
	while (true) {
		// TODO Use epoll_pwait for signal?
		// https://man7.org/linux/man-pages/man2/epoll_wait.2.html
		num_fds = epoll_wait(epollFd, epoll_events, max_events, timeout);
		if (num_fds < 0) {
			// On error, -1 is returned, and errno is set to indicate the cause of the error
			perror("Error polling");
			break;
		}
		if (num_fds == 0) {
			// A return value of zero indicates that the system call timed out before any file
		    // descriptors became read.
			continue;
		}

		int i;
		for (i=0; i<num_fds; i++) {
			if (epoll_events[i].events & (EPOLLRDHUP | EPOLLERR | EPOLLHUP)) {
				fprintf(stderr, "epoll events indicates that fd %d should be removed\n",
						epoll_events[i].data.fd	);
				continue;
			}
			if (epoll_events[i].events & EPOLLIN) {
				// Read the event data
				if (read(epoll_events[i].data.fd, &evdata, sizeof(evdata)) < 0) {
					perror("Error reading event data from line fd");
					continue;
				}

				// https://github.com/torvalds/linux/blob/v5.4/include/uapi/linux/gpio.h#L140
				// id: either GPIOEVENT_EVENT_RISING_EDGE or GPIOEVENT_EVENT_FALLING_EDGE
				// timestamp: best estimate of time of event occurrence, in nanoseconds
				(*env)->CallVoidMethod(env, callback, gpioLineEventListenerMethod,
						epoll_events[i].data.fd, evdata.id, evdata.timestamp);
			}
		}
	}
}

JNIEXPORT void JNICALL Java_com_diozero_internal_provider_sysfs_NativeGpioChip_close(
		JNIEnv* env, jclass clz, jint chipFd) {
	close(chipFd);
}
