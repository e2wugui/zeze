package Zeze.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import Zeze.Serialize.ByteBuffer;
import com.github.luben.zstd.BufferPool;
import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdInputStreamNoFinalizer;
import com.github.luben.zstd.ZstdOutputStreamNoFinalizer;

public final class ZstdFactory {
	private static final Field fCStream, fCSrcPos, fCDstPos;
	private static final Field fDStream, fDSrcPos, fDDstPos;
	private static final MethodHandle mhResetCStream, mhCompressStream, mhFlushStream;
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
		public static final java.nio.ByteBuffer empty = java.nio.ByteBuffer.allocate(0);
		public static final DummyBufferPool instance = new DummyBufferPool();

		@Override
		public java.nio.ByteBuffer get(int capacity) {
			return empty;
		}

		@Override
		public void release(java.nio.ByteBuffer buffer) {
		}
	}

	public static class CompressStream extends ZstdOutputStreamNoFinalizer {
		public static final int DEFAULT_DST_BUF_SIZE = (int)ZstdOutputStreamNoFinalizer.recommendedCOutSize();
		public static final int DEFAULT_COMPRESS_LEVEL = Zstd.defaultCompressionLevel();
		public static final int DEFAULT_WINDOW_LOG = -1;

		private long ctxPtr;
		private final byte[] dstBuf;

		public CompressStream() throws IOException {
			this(DEFAULT_DST_BUF_SIZE, DEFAULT_COMPRESS_LEVEL, DEFAULT_WINDOW_LOG);
		}

		public CompressStream(int dstBufSize, int compressLevel, int windowLog) throws IOException {
			super(DummyOutputStream.instance, DummyBufferPool.instance);
			try {
				ctxPtr = fCStream.getLong(this);
				if (ctxPtr == 0)
					throw new IllegalStateException("ctxPtr = 0");
				dstBuf = new byte[dstBufSize];
				setLevel(compressLevel);
				if (windowLog >= 0)
					setLong(windowLog);
				int r = (int)mhResetCStream.invoke(this, ctxPtr);
				if (r != 0)
					throw new IllegalStateException("mhResetCStream = " + r);
			} catch (Throwable e) {
				throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
			}
		}

		public void compress(byte[] src, int srcPos, int srcEnd, ByteBuffer dst) {
			if (ctxPtr == 0)
				throw new IllegalStateException("ctxPtr = 0");
			try {
				fCSrcPos.set(this, srcPos);
				while (srcPos < srcEnd) {
					int r = (int)mhCompressStream.invoke(this, ctxPtr, dstBuf, dstBuf.length, src, srcEnd);
					if (r < 0)
						throw new IllegalStateException("mhCompressStream = " + r);
					dst.Append(dstBuf, 0, (int)fCDstPos.getLong(this));
					srcPos = (int)fCSrcPos.getLong(this);
				}
			} catch (Throwable e) {
				throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
			}
		}

		public void flush(ByteBuffer dst) {
			if (ctxPtr == 0)
				throw new IllegalStateException("ctxPtr = 0");
			try {
				int r;
				do {
					r = (int)mhFlushStream.invoke(this, ctxPtr, dstBuf, dstBuf.length);
					if (r < 0)
						throw new IllegalStateException("mhFlushStream = " + r);
					dst.Append(dstBuf, 0, (int)fCDstPos.getLong(this));
				} while (r > 0);
			} catch (Throwable e) {
				throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
			}
		}

		@Override
		public void close() {
			ctxPtr = 0;
			try {
				super.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class DecompressStream extends ZstdInputStreamNoFinalizer {
		private long ctxPtr;

		public DecompressStream() throws IOException {
			super(DummyInputStream.instance, DummyBufferPool.instance);
			try {
				ctxPtr = fDStream.getLong(this);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			if (ctxPtr == 0)
				throw new IllegalStateException("ctxPtr = 0");
		}

		public void decompress(byte[] src, int srcPos, int srcEnd, ByteBuffer dst) {
			if (ctxPtr == 0)
				throw new IllegalStateException("ctxPtr = 0");
			try {
				fDSrcPos.set(this, srcPos);
				fDDstPos.set(this, dst.WriteIndex);
				byte[] dstBytes = dst.Bytes;
				int dstPos = dst.WriteIndex;
				int dstEnd = dstBytes.length;
				int r = 0;
				while (srcPos < srcEnd) {
					r = Math.max(r, 16);
					if (dstEnd - dstPos < r) {
						dst.WriteIndex = dstPos;
						dst.EnsureWrite(srcEnd - srcPos);
						dstBytes = dst.Bytes;
						dstEnd = dstBytes.length;
					}
					r = (int)mhDecompressStream.invoke(this, ctxPtr, dstBytes, dstEnd, src, srcEnd);
					if (r < 0)
						throw new IllegalStateException("mhDecompressStream = " + r);
					srcPos = (int)fDSrcPos.getLong(this);
					dstPos = (int)fDDstPos.getLong(this);
				}
				dst.WriteIndex = dstPos;
			} catch (Throwable e) {
				throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
			}
		}

		@Override
		public void close() {
			ctxPtr = 0;
			try {
				super.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static CompressStream newCompressStream() {
		try {
			return new CompressStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static CompressStream newCompressStream(int dstBufSize, int compressLevel, int windowLog) {
		try {
			return new CompressStream(dstBufSize, compressLevel, windowLog);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static DecompressStream newDecompressStream() {
		try {
			return new DecompressStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
