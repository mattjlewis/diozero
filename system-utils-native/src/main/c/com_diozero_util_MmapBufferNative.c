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

extern jclass mmapByteBufferClassRef;
extern jmethodID mmapByteBufferConstructor;

static void* initMapMem(int fd, uint32_t offset, uint32_t length) {
	return mmap(0, length, PROT_READ|PROT_WRITE, MAP_SHARED, fd, offset);
	//return mmap(0, length, PROT_READ|PROT_WRITE, MAP_SHARED|MAP_LOCKED, fd, offset);
}

jobject createMmapByteBuffer(JNIEnv* env, int fd, void* map_ptr, long mapCapacity) {
	return (*env)->NewObject(env, mmapByteBufferClassRef, mmapByteBufferConstructor, fd, map_ptr, mapCapacity,
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
