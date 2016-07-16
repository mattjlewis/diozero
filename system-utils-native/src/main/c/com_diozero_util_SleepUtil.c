#include <time.h>
#include "com_diozero_util_SleepUtil.h"

JNIEXPORT jlong JNICALL Java_com_diozero_util_SleepUtil_sleepNanos(JNIEnv* env, jclass clz, jint secs, jlong nanos) {
	struct timespec req, rem;
	req.tv_sec = secs;
	req.tv_nsec = nanos;

	if (nanosleep(&req, &rem) == -1) {
		return rem.tv_sec * 1000000000L + rem.tv_nsec;
	}

	return 0;
}
