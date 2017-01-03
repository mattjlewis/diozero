#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>
#include "com_diozero_util.h"

/* The VM calls this function upon loading the native library. */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm, void* reserved) {
	return JNI_VERSION_1_8;
}

unsigned long long getEpochTime() {
	struct timeval tp;
	gettimeofday(&tp, NULL);
	return (unsigned long long)(tp.tv_sec) * 1000 + (unsigned long long)(tp.tv_usec) / 1000;
}
