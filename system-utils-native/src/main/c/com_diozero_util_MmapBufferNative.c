#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdarg.h>
#include <ctype.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/file.h>
#include <jni.h>
#include "com_diozero_util_MmapBufferNative.h"

static void* initMapMem(int fd, uint32_t offset, uint32_t length) {
	return mmap(0, length, PROT_READ|PROT_WRITE, MAP_SHARED, fd, offset);
	//return mmap(0, length, PROT_READ|PROT_WRITE, MAP_SHARED|MAP_LOCKED, fd, offset);
}

jobject createMmapByteBuffer(JNIEnv* env, int fd, void* map_ptr, long mapCapacity) {
	char* class_name = "com/diozero/util/MmapByteBuffer";
	jclass clz = (*env)->FindClass(env, class_name);
	if (clz == NULL) {
		printf("Error, could not find class '%s'\n", class_name);
		return NULL;
	}
	char* signature = "(IIILjava/nio/ByteBuffer;)V";
	jmethodID constructor = (*env)->GetMethodID(env, clz, "<init>", signature);
	if (constructor == NULL) {
		printf("Error, could not find constructor %s %s\n", class_name, signature);
		return NULL;
	}

	return (*env)->NewObject(env, clz, constructor, fd, map_ptr, mapCapacity,
			(*env)->NewDirectByteBuffer(env, map_ptr, mapCapacity));
}

JNIEXPORT jobject JNICALL Java_com_diozero_util_MmapBufferNative_createMmapBuffer(
		JNIEnv* env, jclass clz, jstring path, jint offset, jint length) {
	int str_len = (*env)->GetStringLength(env, path);
	char filename[str_len];
	(*env)->GetStringUTFRegion(env, path, 0, str_len, filename);
	int fd = open(filename, O_RDWR | O_SYNC);
	if (fd < 0) {
		// TODO Raise error
		return NULL;
	}
	void* map_ptr = initMapMem(fd, offset, length);
	if (map_ptr == MAP_FAILED) {
		// TODO Raise error
		return NULL;
	}

	return createMmapByteBuffer(env, fd, map_ptr, length);
}

JNIEXPORT void JNICALL Java_com_diozero_util_MmapBufferNative_closeMmapBuffer(JNIEnv *env, jclass clz, jint fd, jint mapPtr, jint length) {
	munmap((void*) mapPtr, length);
	close((int) fd);
}
