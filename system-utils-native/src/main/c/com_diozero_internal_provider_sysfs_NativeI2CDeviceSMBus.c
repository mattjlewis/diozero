/*
 * #%L
 * Device I/O Zero - Java Sysfs provider
 * %%
 * Copyright (C) 2016 mattjlewis
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

#include <errno.h>
#include <string.h>

#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

//#include <linux/i2c.h>
#include <linux/i2c-dev.h>
#include <sys/ioctl.h>

#include "com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus.h"

#if __WORDSIZE == 32
#define long_t uint32_t
#else
#define long_t uint64_t
#endif

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus_smbusOpen(
		JNIEnv* env, jclass clz, jstring i2cAdapter, jint deviceAddress, jboolean force) {
	jboolean is_copy;
	const char* device = (*env)->GetStringUTFChars(env, i2cAdapter, &is_copy);

	int fd = open(device, O_RDWR | O_NONBLOCK);
	(*env)->ReleaseStringUTFChars(env, i2cAdapter, device);
	if (fd < 0) {
		fprintf(stderr, "Error opening I2C device: %s.\n", strerror(errno));
		return -errno;
	}

	uint32_t funcs;
	int rc = ioctl(fd, I2C_FUNCS, &funcs);
	if (rc < 0) {
		printf("Error reading I2C_FUNCS: %s\n", strerror(errno));
	} else {
		printf("funcs: %d\n", funcs);
		fflush(stdout);
	}

	if (ioctl(fd, force ? I2C_SLAVE_FORCE : I2C_SLAVE, (void*) deviceAddress) < 0) {
		close(fd);
		return -errno;
	}

	return fd;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus_getFuncs(
		JNIEnv* env, jclass clz, jint fd) {
	uint32_t funcs;
	int rc = ioctl(fd, I2C_FUNCS, &funcs);
	if (rc < 0) {
		printf("Error reading I2C_FUNCS: %s\n", strerror(errno));
		return -errno;
	}

	return funcs;
}

JNIEXPORT void JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus_smbusClose(
		JNIEnv* env, jclass clz, jint fd) {
	printf("smbusClose()\n");
	fflush(stdout);
	close(fd);
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus_writeQuick(
		JNIEnv* env, jclass clz, jint fd, jbyte value) {
	return i2c_smbus_write_quick(fd, value);
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus_readByte(
		JNIEnv* env, jclass clz, jint fd) {
	return i2c_smbus_read_byte(fd);
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus_writeByte(
		JNIEnv* env, jclass clz, jint fd, jbyte value) {
	return i2c_smbus_write_byte(fd, value);
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus_readByteData(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress) {
	return i2c_smbus_read_byte_data(fd, registerAddress);
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus_writeByteData(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress, jbyte value) {
	return i2c_smbus_write_byte_data(fd, registerAddress, value);
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus_readWordData(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress) {
	return i2c_smbus_read_word_data(fd, registerAddress);
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus_writeWordData(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress, jshort value) {
	return i2c_smbus_write_word_data(fd, registerAddress, value);
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus_processCall(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress, jshort value) {
	return i2c_smbus_process_call(fd, registerAddress, value);
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus_readBlockData(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress, jbyteArray rxData) {
	jboolean is_copy;
	jbyte* rx_buf = (*env)->GetByteArrayElements(env, rxData, &is_copy);

	int rc = i2c_smbus_read_block_data(fd, registerAddress, (uint8_t*) rx_buf);

	(*env)->ReleaseByteArrayElements(env, rxData, rx_buf, 0);

	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus_writeBlockData(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress, jint txLength, jbyteArray txData) {
	jboolean is_copy;
	jbyte* tx_buf = (*env)->GetByteArrayElements(env, txData, &is_copy);

	int rc = i2c_smbus_write_block_data(fd, registerAddress, txLength, (uint8_t*) tx_buf);

	(*env)->ReleaseByteArrayElements(env, txData, tx_buf, JNI_ABORT);

	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus_readI2CBlockData(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress, jint rxLength, jbyteArray rxData) {
	jboolean is_copy;
	jbyte* rx_buf = (*env)->GetByteArrayElements(env, rxData, &is_copy);

	int rc = i2c_smbus_read_i2c_block_data(fd, registerAddress, rxLength, (uint8_t*) rx_buf);

	(*env)->ReleaseByteArrayElements(env, rxData, rx_buf, 0);

	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus_writeI2CBlockData(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress, jint txLength, jbyteArray txData) {
	jboolean is_copy;
	jbyte* tx_buf = (*env)->GetByteArrayElements(env, txData, &is_copy);

	int rc = i2c_smbus_write_i2c_block_data(fd, registerAddress, txLength, (uint8_t*) tx_buf);

	(*env)->ReleaseByteArrayElements(env, txData, tx_buf, JNI_ABORT);

	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_sysfs_NativeI2CDeviceSMBus_blockProcessCall(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress, jint txLength, jbyteArray txData, jbyteArray rxData) {
	jboolean is_copy;
	jbyte* rx_buf = (*env)->GetByteArrayElements(env, rxData, &is_copy);
	jbyte* tx_buf = (*env)->GetByteArrayElements(env, txData, &is_copy);

	int rc = i2c_smbus_block_process_call(fd, registerAddress, txLength, (uint8_t*) tx_buf);

	(*env)->ReleaseByteArrayElements(env, txData, tx_buf, JNI_ABORT);

	if (rc < 0) {
		(*env)->ReleaseByteArrayElements(env, rxData, rx_buf, JNI_ABORT);

		return rc;
	}

	memcpy(rx_buf, tx_buf, rc);

	(*env)->ReleaseByteArrayElements(env, rxData, rx_buf, 0);

	return rc;
}
