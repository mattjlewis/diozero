package com.diozero.util;

import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class MmapIntBuffer implements AutoCloseable {
	private MmapByteBuffer mmapByteBuffer;
	private IntBuffer intBuffer;

	public MmapIntBuffer(String file, int offset, int pageSize, ByteOrder byteOrder) {
		mmapByteBuffer = MmapBufferNative.createMmapBuffer(file, offset, pageSize);
		intBuffer = mmapByteBuffer.getBuffer().order(byteOrder).asIntBuffer();
	}

	@Override
	public void close() {
		MmapBufferNative.closeMmapBuffer(mmapByteBuffer.getFd(), mmapByteBuffer.getAddress(),
				mmapByteBuffer.getLength());
		mmapByteBuffer = null;
		intBuffer = null;
	}

	@Deprecated()
	public IntBuffer getIntBuffer() {
		return intBuffer;
	}
	
	public int get(int index) {
		return intBuffer.get(index);
	}
	
	public int get(int index, int mask) {
		return intBuffer.get(index) & mask;
	}
	
	public int getShiftRight(int index, int shift, int mask) {
		return (intBuffer.get(index) >> shift) & mask;
	}
	
	public void put(int index, int i) {
		intBuffer.put(index, i);
	}
}
