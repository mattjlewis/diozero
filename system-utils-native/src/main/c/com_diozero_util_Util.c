#include "com_diozero_util_Util.h"

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <sys/time.h>

/* Java VM interface */
static JavaVM* globalJavaVM = NULL;

JavaVM* getGlobalJavaVM() {
	return globalJavaVM;
}

/* The VM calls this function upon loading the native library. */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm, void* reserved) {
	globalJavaVM = jvm;

	return JNI_VERSION_1_8;
}

unsigned long long getEpochTime() {
	struct timeval tp;
	gettimeofday(&tp, NULL);
	return ((long long) tp.tv_sec) * 1000 + tp.tv_usec / 1000;
}

unsigned long long getEpochTime2() {
	struct timespec ts;
	clock_gettime(CLOCK_REALTIME, &ts);
	return ((unsigned long long) ts.tv_sec) * 1000 + ts.tv_nsec / 1000 / 1000;
}

long long getNanoTime() {
	struct timespec ts;
	clock_gettime(CLOCK_MONOTONIC, &ts);
	return ((long long) ts.tv_sec) * 1000 * 1000 * 1000 + ts.tv_nsec;
}
