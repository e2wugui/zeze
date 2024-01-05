package Benchmark;

import Zeze.Serialize.ByteBuffer;

// java -Xms256m -Xmx256m -Xlog:gc=info,gc+heap=info:gc.log:time -cp target/classes;../ZezeJava/target/classes Benchmark.PerfByteBufferCapacity
public class PerfByteBufferCapacity {
	private static void cleanup() throws InterruptedException {
		System.gc();
		// System.runFinalization();
		Thread.sleep(1000);
	}

	private static void perfWriteLong(int cap, int writeSize) throws InterruptedException {
		cleanup();
		int n = 10_0000_0000 / writeSize;
		int r = 0;
		long timeBegin = System.nanoTime();
		for (int i = 0; i < n; i++) {
			ByteBuffer bb = ByteBuffer.Allocate(cap);
			for (int j = 0; j < writeSize; j++)
				bb.WriteByte(j);
			r += bb.Bytes[writeSize - 1];
		}
		System.out.println("perfWriteLong(" + cap + ", " + writeSize + ") = "
				+ r + " " + (System.nanoTime() - timeBegin) / 1_000_000 + " ms");
	}

	public static void main(String[] args) throws InterruptedException {
		int[] BYTE_BUFFER_CAPS = {16, 1024};
		int[] WRITE_SIZES = {16, 1024};

		for (int writeSize : WRITE_SIZES)
			for (int cap : BYTE_BUFFER_CAPS)
				for (int i = 0; i < 5; i++)
					perfWriteLong(cap, writeSize);
	}
}
