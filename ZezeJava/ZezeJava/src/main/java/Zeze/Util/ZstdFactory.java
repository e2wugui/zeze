package Zeze.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import Zeze.Net.Codec;
import Zeze.Serialize.ByteBuffer;
import com.github.luben.zstd.BufferPool;
import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdInputStreamNoFinalizer;
import com.github.luben.zstd.ZstdOutputStreamNoFinalizer;
import org.jspecify.annotations.Nullable;

/** zstd-jni 无 finalizer 流式压缩/解压封装：直接驱动 native stream 上下文，禁用 InputStream/OutputStream 多态入口。 */
public final class ZstdFactory {
	private static final Field fCStream, fCSrcPos, fCDstPos;
	private static final Field fDStream, fDSrcPos, fDDstPos;
	private static final MethodHandle mhResetCStream, mhCompressStream, mhFlushStream;
	private static final MethodHandle mhFreeCStream;
	private static final MethodHandle mhDecompressStream;
	private static final MethodHandle mhFreeDStream;

	private static Field getField(Class<?> cls,
										   String fieldName) throws ReflectiveOperationException {
		var field = cls.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field;
	}

	private static MethodHandle getMethodHandle(MethodHandles.Lookup lookup, Class<?> cls,
														 String methodName,
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
			mhFreeCStream = getMethodHandle(lookup, cls, "freeCStream", long.class);
			cls = ZstdInputStreamNoFinalizer.class; // stream, dst, dstSize (write dstPos)
			fDStream = getField(cls, "stream"); // long
			fDSrcPos = getField(cls, "srcPos"); // long
			fDDstPos = getField(cls, "dstPos"); // long
			mhDecompressStream = getMethodHandle(lookup, cls, "decompressStream", long.class, byte[].class, int.class,
					byte[].class, int.class); // stream, dst, dstSize, src, srcEnd (read srcPos & dstPos, write srcPos & dstPos)
			mhFreeDStream = getMethodHandle(lookup, cls, "freeDStream", long.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
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

	public static class ZstdCompressStream extends ZstdOutputStreamNoFinalizer {
		public static final int DEFAULT_DST_BUF_SIZE = (int)ZstdOutputStreamNoFinalizer.recommendedCOutSize();
		public static final int DEFAULT_COMPRESS_LEVEL = Zstd.defaultCompressionLevel();
		public static final int DEFAULT_WINDOW_LOG = -1;

		private long ctxPtr;
		private final byte [] dstBuf;

		public ZstdCompressStream() throws IOException {
			this(DEFAULT_DST_BUF_SIZE, DEFAULT_COMPRESS_LEVEL, DEFAULT_WINDOW_LOG);
		}

		public ZstdCompressStream(int dstBufSize, int compressLevel, int windowLog) throws IOException {
			super(DummyOutputStream.instance, DummyBufferPool.instance);
			try {
				ctxPtr = fCStream.getLong(this);
				if (ctxPtr == 0)
					throw new IllegalStateException("ctxPtr = 0");
				if (dstBufSize == 0) // 0 长度缓冲会导致压缩循环空转，归一为默认尺寸；负值保持抛 NegativeArraySizeException
					dstBufSize = DEFAULT_DST_BUF_SIZE;
				dstBuf = new byte[dstBufSize];
				//noinspection resource
				setLevel(compressLevel);
				if (windowLog >= 0) //noinspection resource
					setLong(windowLog);
				int r = (int)mhResetCStream.invokeExact((ZstdOutputStreamNoFinalizer)this, ctxPtr);
				if (r != 0)
					throw new IllegalStateException("mhResetCStream = " + r);
			} catch (Throwable e) {
				// super 构造已创建 native cstream，构造失败须显式释放防泄漏
				long ptr = ctxPtr;
				ctxPtr = 0;
				if (ptr != 0) {
					try {
						int r = (int)mhFreeCStream.invokeExact(ptr);
					} catch (Throwable ignored) {
						// 释放失败无法补救，优先重抛原始构造异常
					}
				}
				throw Task.forceThrow(e);
			}
		}

		// 基类保存的 stream 字段不会随包装 close 清零，所有 native 配置必须先检查包装生命周期。
		private void ensureOpen() {
			if (ctxPtr == 0)
				throw new IllegalStateException("stream is closed");
		}

		@Override
		public synchronized ZstdCompressStream setChecksum(boolean value) throws IOException {
			ensureOpen();
			super.setChecksum(value);
			return this;
		}

		@Override
		public synchronized ZstdCompressStream setLevel(int value) throws IOException {
			ensureOpen();
			super.setLevel(value);
			return this;
		}

		@Override
		public synchronized ZstdCompressStream setLong(int value) throws IOException {
			ensureOpen();
			super.setLong(value);
			return this;
		}

		@Override
		public synchronized ZstdCompressStream setWorkers(int value) throws IOException {
			ensureOpen();
			super.setWorkers(value);
			return this;
		}

		@Override
		public synchronized ZstdCompressStream setOverlapLog(int value) throws IOException {
			ensureOpen();
			super.setOverlapLog(value);
			return this;
		}

		@Override
		public synchronized ZstdCompressStream setJobSize(int value) throws IOException {
			ensureOpen();
			super.setJobSize(value);
			return this;
		}

		@Override
		public synchronized ZstdCompressStream setTargetLength(int value) throws IOException {
			ensureOpen();
			super.setTargetLength(value);
			return this;
		}

		@Override
		public synchronized ZstdCompressStream setMinMatch(int value) throws IOException {
			ensureOpen();
			super.setMinMatch(value);
			return this;
		}

		@Override
		public synchronized ZstdCompressStream setSearchLog(int value) throws IOException {
			ensureOpen();
			super.setSearchLog(value);
			return this;
		}

		@Override
		public synchronized ZstdCompressStream setChainLog(int value) throws IOException {
			ensureOpen();
			super.setChainLog(value);
			return this;
		}

		@Override
		public synchronized ZstdCompressStream setHashLog(int value) throws IOException {
			ensureOpen();
			super.setHashLog(value);
			return this;
		}

		@Override
		public synchronized ZstdCompressStream setWindowLog(int value) throws IOException {
			ensureOpen();
			super.setWindowLog(value);
			return this;
		}

		@Override
		public synchronized ZstdCompressStream setStrategy(int value) throws IOException {
			ensureOpen();
			super.setStrategy(value);
			return this;
		}

		@Override
		public synchronized ZstdCompressStream setDict(byte [] value) throws IOException {
			ensureOpen();
			super.setDict(value);
			return this;
		}

		@Override
		public synchronized ZstdCompressStream setDict(com.github.luben.zstd.ZstdDictCompress value) throws IOException {
			ensureOpen();
			super.setDict(value);
			return this;
		}

		public synchronized void compress(byte [] src, int srcPos, int srcEnd, ByteBuffer dst) {
			if (ctxPtr == 0)
				throw new IllegalStateException("ctxPtr = 0");
			try {
				fCSrcPos.set(this, srcPos);
				while (srcPos < srcEnd) {
					int r = (int)mhCompressStream.invokeExact((ZstdOutputStreamNoFinalizer)this,
							ctxPtr, dstBuf, dstBuf.length, src, srcEnd);
					if (r < 0)
						throw new IllegalStateException("mhCompressStream = " + r);
					dst.Append(dstBuf, 0, (int)fCDstPos.getLong(this));
					srcPos = (int)fCSrcPos.getLong(this);
				}
			} catch (Throwable e) { // MethodHandle.invoke
				throw Task.forceThrow(e);
			}
		}

		public synchronized void compress(byte [] src, int srcPos, int srcEnd, Codec dst) {
			if (ctxPtr == 0)
				throw new IllegalStateException("ctxPtr = 0");
			try {
				fCSrcPos.set(this, srcPos);
				while (srcPos < srcEnd) {
					int r = (int)mhCompressStream.invokeExact((ZstdOutputStreamNoFinalizer)this,
							ctxPtr, dstBuf, dstBuf.length, src, srcEnd);
					if (r < 0)
						throw new IllegalStateException("mhCompressStream = " + r);
					dst.update(dstBuf, 0, (int)fCDstPos.getLong(this));
					srcPos = (int)fCSrcPos.getLong(this);
				}
			} catch (Throwable e) { // MethodHandle.invoke
				throw Task.forceThrow(e);
			}
		}

		public synchronized void flush(ByteBuffer dst) {
			if (ctxPtr == 0)
				throw new IllegalStateException("ctxPtr = 0");
			try {
				int r;
				do {
					r = (int)mhFlushStream.invokeExact((ZstdOutputStreamNoFinalizer)this,
							ctxPtr, dstBuf, dstBuf.length);
					if (r < 0)
						throw new IllegalStateException("mhFlushStream = " + r);
					dst.Append(dstBuf, 0, (int)fCDstPos.getLong(this));
				} while (r > 0);
			} catch (Throwable e) { // MethodHandle.invoke
				throw Task.forceThrow(e);
			}
		}

		public synchronized void flush(Codec dst) {
			if (ctxPtr == 0)
				throw new IllegalStateException("ctxPtr = 0");
			try {
				int r;
				do {
					r = (int)mhFlushStream.invokeExact((ZstdOutputStreamNoFinalizer)this,
							ctxPtr, dstBuf, dstBuf.length);
					if (r < 0)
						throw new IllegalStateException("mhFlushStream = " + r);
					dst.update(dstBuf, 0, (int)fCDstPos.getLong(this));
				} while (r > 0);
				dst.flush();
			} catch (Throwable e) { // MethodHandle.invoke
				throw Task.forceThrow(e);
			}
		}

		// 禁用 OutputStream 多态入口（避免经 DummyBufferPool 触发越界写），仅通过 compress/flush/close 使用
		@Override
		public void write(int b) {
			throw new UnsupportedOperationException("use compress(...) instead");
		}

		@Override
		public void write(byte [] b, int off, int len) {
			throw new UnsupportedOperationException("use compress(...) instead");
		}

		@Override
		public void flush() {
			throw new UnsupportedOperationException("use flush(ByteBuffer/Codec) instead");
		}

		@Override
		public void closeWithoutClosingParentStream() {
			throw new UnsupportedOperationException("close() only");
		}

		@Override
		public synchronized void close() {
			// 不调基类 close()（避免往 DummyBufferPool 写帧尾越界），仅释放 native 上下文
			long ptr = ctxPtr;
			if (ptr == 0)
				return; // 幂等
			ctxPtr = 0;
			try {
				int r = (int)mhFreeCStream.invokeExact(ptr);
				if (r != 0)
					throw new IllegalStateException("mhFreeCStream = " + r);
			} catch (Throwable e) { // MethodHandle.invoke
				throw Task.forceThrow(e);
			}
		}
	}

	public static class ZstdDecompressStream extends ZstdInputStreamNoFinalizer {
		public static final int DEFAULT_DST_BUF_SIZE = (int)ZstdInputStreamNoFinalizer.recommendedDOutSize();

		private long ctxPtr;
		private byte @Nullable [] dstBuf;

		public ZstdDecompressStream() throws IOException {
			this(0);
		}

		public ZstdDecompressStream(int dstBufSize) throws IOException {
			super(DummyInputStream.instance, DummyBufferPool.instance);
			try {
				ctxPtr = fDStream.getLong(this);
				if (ctxPtr == 0)
					throw new IllegalStateException("ctxPtr = 0");
				if (dstBufSize > 0)
					dstBuf = new byte[dstBufSize];
			} catch (Throwable e) {
				// super 构造已创建 native dstream，构造失败须显式释放防泄漏
				long ptr = ctxPtr;
				ctxPtr = 0;
				if (ptr != 0) {
					try {
						int r = (int)mhFreeDStream.invokeExact(ptr);
					} catch (Throwable ignored) {
						// 释放失败无法补救，优先重抛原始构造异常
					}
				}
				throw Task.forceThrow(e);
			}
		}

		// 基类保存的 stream 字段不会随包装 close 清零，所有 native 配置必须先检查包装生命周期。
		private void ensureOpen() {
			if (ctxPtr == 0)
				throw new IllegalStateException("stream is closed");
		}

		@Override
		public synchronized ZstdDecompressStream setDict(byte [] value) throws IOException {
			ensureOpen();
			super.setDict(value);
			return this;
		}

		@Override
		public synchronized ZstdDecompressStream setDict(com.github.luben.zstd.ZstdDictDecompress value) throws IOException {
			ensureOpen();
			super.setDict(value);
			return this;
		}

		@Override
		public synchronized ZstdDecompressStream setLongMax(int value) throws IOException {
			ensureOpen();
			super.setLongMax(value);
			return this;
		}

		@Override
		public synchronized ZstdDecompressStream setRefMultipleDDicts(boolean value) throws IOException {
			ensureOpen();
			super.setRefMultipleDDicts(value);
			return this;
		}

		public synchronized void decompress(byte [] src, int srcPos, int srcEnd, ByteBuffer dst) {
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
						dst.ensureWriteNoCompact(srcEnd - srcPos);
						dstBytes = dst.Bytes;
						dstEnd = dstBytes.length;
					}
					r = (int)mhDecompressStream.invokeExact((ZstdInputStreamNoFinalizer)this,
							ctxPtr, dstBytes, dstEnd, src, srcEnd);
					if (r < 0)
						throw new IllegalStateException("mhDecompressStream = " + r);
					srcPos = (int)fDSrcPos.getLong(this);
					dstPos = (int)fDDstPos.getLong(this);
				}
				dst.WriteIndex = dstPos;
				// drain：同 Codec 版本，输入耗尽时排空上下文内滞留的输出（最多一个批次，无进展即止）
				fDSrcPos.set(this, srcEnd);
				for (int lastPos = -1; dstPos != lastPos; ) {
					lastPos = dstPos;
					r = Math.max(r, 16);
					if (dstEnd - dstPos < r) {
						dst.WriteIndex = dstPos;
						dst.ensureWriteNoCompact(r);
						dstBytes = dst.Bytes;
						dstEnd = dstBytes.length;
					}
					fDDstPos.set(this, dstPos);
					r = (int)mhDecompressStream.invokeExact((ZstdInputStreamNoFinalizer)this,
							ctxPtr, dstBytes, dstEnd, src, srcEnd);
					if (r < 0)
						throw new IllegalStateException("mhDecompressStream = " + r);
					dstPos = (int)fDDstPos.getLong(this);
				}
				dst.WriteIndex = dstPos;
			} catch (Throwable e) { // MethodHandle.invoke
				throw Task.forceThrow(e);
			}
		}

		public synchronized void decompress(byte [] src, int srcPos, int srcEnd, Codec dst) {
			if (ctxPtr == 0)
				throw new IllegalStateException("ctxPtr = 0");
			if (dstBuf == null)
				dstBuf = new byte[DEFAULT_DST_BUF_SIZE];
			try {
				fDSrcPos.set(this, srcPos);
				while (srcPos < srcEnd) {
					fDDstPos.set(this, 0);
					int r = (int)mhDecompressStream.invokeExact((ZstdInputStreamNoFinalizer)this,
							ctxPtr, dstBuf, dstBuf.length, src, srcEnd);
					if (r < 0)
						throw new IllegalStateException("mhDecompressStream = " + r);
					dst.update(dstBuf, 0, (int)fDDstPos.getLong(this));
					srcPos = (int)fDSrcPos.getLong(this);
				}
				// 排空 native 上下文中滞留的尾部解压输出，直到无产出为止
				fDSrcPos.set(this, srcEnd);
				while (true) {
					fDDstPos.set(this, 0);
					int r = (int)mhDecompressStream.invokeExact((ZstdInputStreamNoFinalizer)this,
							ctxPtr, dstBuf, dstBuf.length, src, srcEnd);
					if (r < 0)
						throw new IllegalStateException("mhDecompressStream = " + r);
					long dstPos = fDDstPos.getLong(this);
					if (dstPos <= 0)
						break;
					dst.update(dstBuf, 0, (int)dstPos);
				}
			} catch (Throwable e) { // MethodHandle.invoke
				throw Task.forceThrow(e);
			}
		}

		// 禁用 InputStream 多态入口，仅通过 decompress/close 使用
		@Override
		public int read() {
			throw new UnsupportedOperationException("use decompress(...) instead");
		}

		@Override
		public int read(byte [] b, int off, int len) {
			throw new UnsupportedOperationException("use decompress(...) instead");
		}

		@Override
		public long skip(long n) {
			throw new UnsupportedOperationException("use decompress(...) instead");
		}

		@Override
		public int available() {
			throw new UnsupportedOperationException("use decompress(...) instead");
		}

		@Override
		public synchronized void close() {
			// 不调基类 close()，仅释放 native 上下文
			long ptr = ctxPtr;
			if (ptr == 0)
				return; // 幂等
			ctxPtr = 0;
			try {
				int r = (int)mhFreeDStream.invokeExact(ptr);
				if (r != 0)
					throw new IllegalStateException("mhFreeDStream = " + r);
			} catch (Throwable e) { // MethodHandle.invoke
				throw Task.forceThrow(e);
			}
		}
	}

	public static ZstdCompressStream newCompressStream() {
		try {
			return new ZstdCompressStream();
		} catch (IOException e) {
			throw Task.forceThrow(e);
		}
	}

	public static ZstdCompressStream newCompressStream(int dstBufSize, int compressLevel, int windowLog) {
		try {
			return new ZstdCompressStream(dstBufSize, compressLevel, windowLog);
		} catch (IOException e) {
			throw Task.forceThrow(e);
		}
	}

	public static ZstdDecompressStream newDecompressStream() {
		try {
			return new ZstdDecompressStream();
		} catch (IOException e) {
			throw Task.forceThrow(e);
		}
	}

	public static ZstdDecompressStream newDecompressStream(int dstBufSize) {
		try {
			return new ZstdDecompressStream(dstBufSize);
		} catch (IOException e) {
			throw Task.forceThrow(e);
		}
	}
}
