import java.nio.ByteBuffer;

public class MmapByteBuffer {
	private int fd;
	private int address;
	private int length;
	private ByteBuffer buffer;

	public MmapByteBuffer(int fd, int address, int length, ByteBuffer buffer) {
		this.fd = fd;
		this.address = address;
		this.length = length;
		this.buffer = buffer;
	}

	public int getFd() {
		return fd;
	}
	
	public int getAddress() {
		return address;
	}
	
	public int getLength() {
		return length;
	}
	
	public ByteBuffer getBuffer() {
		return buffer;
	}
}
