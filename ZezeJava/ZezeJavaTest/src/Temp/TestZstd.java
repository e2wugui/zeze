package Temp;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import com.github.luben.zstd.BufferPool;
import com.github.luben.zstd.ZstdInputStreamNoFinalizer;
import com.github.luben.zstd.ZstdOutputStreamNoFinalizer;

public final class TestZstd {
	public static final class DummyOutputStream extends OutputStream {
		public static final DummyOutputStream instance = new DummyOutputStream();

		@Override
		public void write(int b) {
		}
	}

	public static final class DummyInputStream extends InputStream {
		public static final DummyInputStream instance = new DummyInputStream();

		@Override
		public int read() {
			return -1;
		}
	}

	public static final class DummyBufferPool implements BufferPool {
		public static final ByteBuffer empty = ByteBuffer.allocate(0);
		public static final DummyBufferPool instance = new DummyBufferPool();

		@Override
		public ByteBuffer get(int capacity) {
			return empty;
		}

		@Override
		public void release(ByteBuffer buffer) {
		}
	}

	private static final Field fCStream;
	private static final Field fCSrcPos;
	private static final Field fCDstPos;
	private static final Field fDStream;
	private static final Field fDSrcPos;
	private static final Field fDDstPos;
	private static final MethodHandle mhResetCStream;
	private static final MethodHandle mhCompressStream;
	private static final MethodHandle mhFlushStream;
	private static final MethodHandle mhDecompressStream;

	private static Field getField(Class<?> cls, String fieldName) throws ReflectiveOperationException {
		var field = cls.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field;
	}

	private static MethodHandle getMethodHandle(MethodHandles.Lookup lookup, Class<?> cls, String methodName,
												Class<?>... paramTypes) throws ReflectiveOperationException {
		var method = cls.getDeclaredMethod(methodName, paramTypes);
		method.setAccessible(true);
		return lookup.unreflect(method);
	}

	static {
		try {
			var lookup = MethodHandles.lookup();
			Class<?> cls = ZstdOutputStreamNoFinalizer.class;
			fCStream = getField(cls, "stream"); // long
			fCSrcPos = getField(cls, "srcPos"); // long
			fCDstPos = getField(cls, "dstPos"); // long
			mhResetCStream = getMethodHandle(lookup, cls, "resetCStream", long.class);
			mhCompressStream = getMethodHandle(lookup, cls, "compressStream", long.class, byte[].class, int.class,
					byte[].class, int.class); // stream, dst, dstSize, src, srcEnd (read srcPos, write srcPos & dstPos)
			mhFlushStream = getMethodHandle(lookup, cls, "flushStream", long.class, byte[].class, int.class);
			cls = ZstdInputStreamNoFinalizer.class; // stream, dst, dstSize (write dstPos)
			fDStream = getField(cls, "stream"); // long
			fDSrcPos = getField(cls, "srcPos"); // long
			fDDstPos = getField(cls, "dstPos"); // long
			mhDecompressStream = getMethodHandle(lookup, cls, "decompressStream", long.class, byte[].class, int.class,
					byte[].class, int.class); // stream, dst, dstSize, src, srcEnd (read srcPos & dstPos, write srcPos & dstPos)
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws Throwable {
		var bb = Zeze.Serialize.ByteBuffer.Allocate();
		try (var zstdC = new ZstdOutputStreamNoFinalizer(DummyOutputStream.instance, DummyBufferPool.instance)) {
			var cCtx = fCStream.getLong(zstdC);
			System.out.format("fCStream: %X\n", cCtx);

			int r = (int)mhResetCStream.invoke(zstdC, cCtx);
			System.out.format("mhResetCStream: %d\n", r);

			var dst = new byte[(int)ZstdOutputStreamNoFinalizer.recommendedCOutSize()];
			System.out.format("dst.length: %d\n", dst.length);

			var src = new byte[256];
			for (int i = 0; i < 256; i++)
				src[i] = (byte)i;

			r = (int)mhCompressStream.invoke(zstdC, cCtx, dst, dst.length, src, src.length);
			System.out.format("mhCompressStream: %d\n", r);
			System.out.format("srcPos/dstPos: %d/%d\n", fCSrcPos.getLong(zstdC), fCDstPos.getLong(zstdC));
			bb.Append(dst, 0, (int)fCDstPos.getLong(zstdC));

			r = (int)mhFlushStream.invoke(zstdC, cCtx, dst, dst.length);
			System.out.format("mhFlushStream: %d\n", r);
			System.out.format("dstPos: %d\n", fCDstPos.getLong(zstdC));
			bb.Append(dst, 0, (int)fCDstPos.getLong(zstdC));

			fCSrcPos.setLong(zstdC, 0);
			r = (int)mhCompressStream.invoke(zstdC, cCtx, dst, dst.length, src, src.length);
			System.out.format("mhCompressStream: %d\n", r);
			System.out.format("srcPos/dstPos: %d/%d\n", fCSrcPos.getLong(zstdC), fCDstPos.getLong(zstdC));
			bb.Append(dst, 0, (int)fCDstPos.getLong(zstdC));

			r = (int)mhFlushStream.invoke(zstdC, cCtx, dst, dst.length);
			System.out.format("mhFlushStream: %d\n", r);
			System.out.format("dstPos: %d\n", fCDstPos.getLong(zstdC));
			bb.Append(dst, 0, (int)fCDstPos.getLong(zstdC));
		}

		try (var zstdD = new ZstdInputStreamNoFinalizer(DummyInputStream.instance, DummyBufferPool.instance)) {
			var cCtx = fDStream.getLong(zstdD);
			System.out.format("fDStream: %X\n", cCtx);

			var dst = new byte[(int)ZstdInputStreamNoFinalizer.recommendedDInSize()];
			System.out.format("dst.length: %d\n", dst.length);
			System.out.format("bb.length: %d\n", bb.WriteIndex);

			int r = (int)mhDecompressStream.invoke(zstdD, cCtx, dst, dst.length, bb.Bytes, bb.WriteIndex / 2);
			System.out.format("mhDecompressStream: %d\n", r);
			System.out.format("srcPos/dstPos: %d/%d\n", fDSrcPos.getLong(zstdD), fDDstPos.getLong(zstdD));

			r = (int)mhDecompressStream.invoke(zstdD, cCtx, dst, dst.length, bb.Bytes, bb.WriteIndex);
			System.out.format("mhDecompressStream: %d\n", r);
			System.out.format("srcPos/dstPos: %d/%d\n", fDSrcPos.getLong(zstdD), fDDstPos.getLong(zstdD));
		}
	}
}
