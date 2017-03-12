#include "com_diozero_internal_provider_bbbiolib_BbbIoLibNative.h"

#include <BBBiolib.h>
#include <jni_md.h>

/* The VM calls this function upon loading the native library. */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm, void* reserved) {
	return JNI_VERSION_1_8;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_bbbiolib_BbbIoLibNative_init
  (JNIEnv* env, jclass clz) {
	return iolib_init();
}

JNIEXPORT void JNICALL Java_com_diozero_internal_provider_bbbiolib_BbbIoLibNative_shutdown
  (JNIEnv* env, jclass clz) {
	iolib_free();
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_bbbiolib_BbbIoLibNative_setDir
  (JNIEnv* env, jclass clz, jbyte port, jbyte pin, jbyte dir) {
	return iolib_setdir(port, pin, dir);
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_bbbiolib_BbbIoLibNative_getValue
  (JNIEnv* env, jclass clz, jbyte port, jbyte pin) {
	return is_high(port, pin);
}

JNIEXPORT void JNICALL Java_com_diozero_internal_provider_bbbiolib_BbbIoLibNative_setValue
  (JNIEnv* env, jclass clz, jbyte port, jbyte pin, jboolean value) {
	if (value == JNI_TRUE) {
		pin_high(port, pin);
	} else {
		pin_low(port, pin);
	}
}
