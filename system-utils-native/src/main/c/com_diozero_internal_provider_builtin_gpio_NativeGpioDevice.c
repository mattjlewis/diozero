
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
#include <sys/eventfd.h>

#include <linux/gpio.h>

#define CONSUMER "diozero"

#include "com_diozero_internal_provider_builtin_gpio_NativeGpioDevice.h"
#include <jni.h>

#include "com_diozero_util_Util.h"

extern jclass arrayListClassRef;
extern jmethodID arrayListConstructor;
extern jmethodID arrayListAddMethod;

extern jclass gpioChipInfoClassRef;
extern jmethodID gpioChipInfoConstructor;

extern jclass gpioChipClassRef;
extern jmethodID gpioChipConstructor;

extern jclass gpioLineClassRef;
extern jmethodID gpioLineConstructor;

extern jmethodID gpioLineEventListenerMethod;

static int dir_filter(const struct dirent *dir) {
	return !strncmp(dir->d_name, "gpiochip", 8);
}

volatile int exitLoopFd = -1;
volatile bool running = false;

JNIEXPORT jobject JNICALL Java_com_diozero_internal_provider_builtin_gpio_NativeGpioDevice_getChips(
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

JNIEXPORT jobject JNICALL Java_com_diozero_internal_provider_builtin_gpio_NativeGpioDevice_openChip(
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
	jobject chip_obj = (*env)->NewObject(env, gpioChipClassRef, gpioChipConstructor,
			name, label, chip_fd, lines);

	return chip_obj;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_gpio_NativeGpioDevice_provisionGpioInputDevice(
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

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_gpio_NativeGpioDevice_provisionGpioOutputDevice(
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

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_gpio_NativeGpioDevice_getValue(
		JNIEnv* env, jclass clz, jint lineFd) {
	struct gpiohandle_data data;
	if (ioctl(lineFd, GPIOHANDLE_GET_LINE_VALUES_IOCTL, &data) < 0) {
		perror("Error setting GPIO values");
		return -errno;
	}

	return data.values[0];
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_gpio_NativeGpioDevice_setValue(
		JNIEnv* env, jclass clz, jint lineFd, jint value) {
	struct gpiohandle_data data;
	memset(&data, 0, sizeof(data));

	data.values[0] = value;
	if (ioctl(lineFd, GPIOHANDLE_SET_LINE_VALUES_IOCTL, &data) < 0) {
		perror("Error setting GPIO value");
		return -errno;
	}

	return 0;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_gpio_NativeGpioDevice_epollCreate(
		JNIEnv* env, jclass clz) {
	// Note since Linux 2.6.8, the size argument is ignored, but must be greater than zero
	return epoll_create(1);
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_gpio_NativeGpioDevice_epollAddFileDescriptor(
		JNIEnv* env, jclass clz, jint epollFd, jint lineFd) {
	struct epoll_event ev;
	ev.events = EPOLLIN | EPOLLPRI;
	ev.data.fd = lineFd;

	int rc = epoll_ctl(epollFd, EPOLL_CTL_ADD, lineFd, &ev);
	if (rc < 0) {
		perror("Error in epoll_ctl EPOLL_CTL_ADD");
		return -errno;
	}

	return 0;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_gpio_NativeGpioDevice_epollRemoveFileDescriptor(
		JNIEnv* env, jclass clz, jint epollFd, jint lineFd) {
	int rc = epoll_ctl(epollFd, EPOLL_CTL_DEL, lineFd, NULL);
	if (rc < 0) {
		perror("Error in epoll_ctl EPOLL_CTL_DEL in epollRemoveFileDescriptor");
		return -errno;
	}

	return 0;
}

JNIEXPORT void JNICALL Java_com_diozero_internal_provider_builtin_gpio_NativeGpioDevice_eventLoop(
		JNIEnv* env, jclass clz, jint epollFd, jint timeout, jobject callback) {
	int max_events = 40;
	struct epoll_event epoll_events[max_events];
	memset(&epoll_events, 0, sizeof(epoll_events));

	// https://github.com/torvalds/linux/blob/v5.4/include/uapi/linux/gpio.h#L148
	struct gpioevent_data evdata;
	memset(&evdata, 0, sizeof(evdata));

	int num_fds;
	running = true;
	while (running) {
		// TODO Use epoll_pwait for signal?
		// https://man7.org/linux/man-pages/man2/epoll_wait.2.html
		num_fds = epoll_pwait(epollFd, epoll_events, max_events, timeout, NULL);
		jlong epoch_time_ms = getEpochTimeMillis();

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
				fprintf(stderr, "TODO: epoll events indicates that fd %d should be removed\n",
						epoll_events[i].data.fd);
				running = false;
				continue;
			}

			if (epoll_events[i].data.fd == exitLoopFd) {
				running = false;
				break;
			}

			if (epoll_events[i].events & EPOLLIN) {
				// Read the event data
				if (read(epoll_events[i].data.fd, &evdata, sizeof(evdata)) < 0) {
					perror("Error reading event data from line fd");
					running = false;
					break;
				}

				/*
				struct timespec tp;
				clock_gettime(CLOCK_REALTIME, &tp);
				uint64_t nanotime = ((uint64_t) tp.tv_sec) * 1000*1000*1000 + tp.tv_nsec;
				printf("Calling event method, delta: %lld\n", (nanotime - evdata.timestamp));
				*/

				// https://github.com/torvalds/linux/blob/v5.4/include/uapi/linux/gpio.h#L140
				// evdata.id: either GPIOEVENT_EVENT_RISING_EDGE or GPIOEVENT_EVENT_FALLING_EDGE
				// timestamp: best estimate of time of event occurrence, in nanoseconds
				// Note uses CLOCK_MONOTONIC, not CLOCK_REALTIME
				(*env)->CallVoidMethod(env, callback, gpioLineEventListenerMethod,
						epoll_events[i].data.fd, evdata.id, epoch_time_ms, evdata.timestamp);
			}
		}
	}
}

int stopEventLoopPipe(int epollFd) {
	int pipefds[2] = {};
	int rc = pipe(pipefds);
	if (rc < 0) {
		perror("Error creating pipe");
		return rc;
	}
	int read_pipe = pipefds[0];
	exitLoopFd = read_pipe;
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
	struct epoll_event ev = {};
	ev.events = EPOLLIN;
	ev.data.fd = read_pipe;
	rc = epoll_ctl(epollFd, EPOLL_CTL_ADD, read_pipe, &ev);
	if (rc < 0) {
		perror("epoll_ctl EPOLL_CTL_ADD error");
		close(read_pipe);
		close(write_pipe);
		return rc;
	}

	uint8_t value = 1;
	rc = write(write_pipe, &value, sizeof(uint8_t));
	if (rc < 0) {
		perror("write error");
	}
	// Wait for the epoll_wait thread to wake up
	while (running) {
		rc = usleep(10);
	}

	rc = epoll_ctl(epollFd, EPOLL_CTL_DEL, read_pipe, NULL);
	if (rc < 0) {
		perror("epoll_ctl EPOLL_CTL_DEL error");
	}

	rc = close(write_pipe);
	if (rc < 0) {
		perror("close error write_pipe");
	}

	rc = close(read_pipe);
	if (rc < 0) {
		perror("close error on read_pipe");
	}

	return 0;
}

int stopEventLoopEventFd(int epollFd) {
	//exitLoopFd = eventfd(0, EFD_SEMAPHORE| EFD_NONBLOCK);
	exitLoopFd = eventfd(0, 0);
	if (exitLoopFd < 0) {
		perror("Error creating eventfd");
		return -1;
	}

	struct epoll_event ev;
	ev.events = EPOLLIN | EPOLLPRI | EPOLLET;
	ev.data.fd = exitLoopFd;

	int rc = epoll_ctl(epollFd, EPOLL_CTL_ADD, exitLoopFd, &ev);
	if (rc < 0) {
		perror("Error in epoll_ctl EPOLL_CTL_ADD");
		return -errno;
	}

	// value cannot be 0
	eventfd_t value = 1;
	rc = eventfd_write(exitLoopFd, value);
	if (rc < 0) {
		perror("Error in write to exitLoopFd");
		return -errno;
	}

	// Wait for the epoll_wait thread to wake up
	while (running) {
		// Very small sleep
		rc = usleep(10);
	}

	// Now cleanup the eventfd

	rc = epoll_ctl(epollFd, EPOLL_CTL_DEL, exitLoopFd, NULL);
	if (rc < 0) {
		perror("Error in epoll_ctl EPOLL_CTL_DEL in stopEventLoopEventFd");
		return -errno;
	}

	rc = close(exitLoopFd);
	if (rc < 0) {
		perror("Error in close");
		return -errno;
	}

	return 0;
}

JNIEXPORT int JNICALL Java_com_diozero_internal_provider_builtin_gpio_NativeGpioDevice_stopEventLoop(
		JNIEnv* env, jclass clz, jint epollFd) {
	int rc = stopEventLoopEventFd(epollFd);
	if (rc < 0) {
		fprintf(stderr, "Failed to stop event loop: %d\n", rc);
	}
	rc = close(epollFd);
	if (rc < 0) {
		perror("Failed to stop close epollFd");
	}
	return rc;
}

JNIEXPORT void JNICALL Java_com_diozero_internal_provider_builtin_gpio_NativeGpioDevice_close(
		JNIEnv* env, jclass clz, jint chipFd) {
	close(chipFd);
}
