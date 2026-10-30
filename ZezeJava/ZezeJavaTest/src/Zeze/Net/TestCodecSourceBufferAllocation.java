package Zeze.Net;

import harness.Fast;
import harness.ForkedJvm;
import org.junit.jupiter.api.Test;
import Zeze.Util.Json;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
@ForkedJvm
public class TestCodecSourceBufferAllocation {
	private static void allocationFailsBeforeNativeFactory(Class<? extends Throwable> failure, Runnable create)
			throws Exception {
		var factory = Class.forName("Zeze.Util.ZstdFactory", false,
				TestCodecSourceBufferAllocation.class.getClassLoader());
		var unsafe = Json.getUnsafe();
		assertTrue(unsafe.shouldBeInitialized(factory), "独立子JVM尚未使用原生流工厂");
		assertThrows(failure, create::run);
		assertTrue(unsafe.shouldBeInitialized(factory), "源缓冲分配失败不得先取得原生流");
	}

	@Test
	void compressorRejectsUnallocatableSourceBeforeCreatingNativeStream() throws Exception {
		allocationFailsBeforeNativeFactory(OutOfMemoryError.class,
				() -> new CompressZstd(new BufferCodec(), Integer.MAX_VALUE, 1024, 1, -1));
	}

	@Test
	void decompressorRejectsUnallocatableSourceBeforeCreatingNativeStream() throws Exception {
		allocationFailsBeforeNativeFactory(OutOfMemoryError.class,
				() -> new DecompressZstd(new BufferCodec(), Integer.MAX_VALUE, 1024));
	}

	@Test
	void mixedDecompressorRejectsUnallocatableSourceBeforeCreatingNativeStream() throws Exception {
		allocationFailsBeforeNativeFactory(OutOfMemoryError.class,
				() -> new DecompressMppcZstd(new BufferCodec(), Integer.MAX_VALUE, 1024));
	}

	@Test
	void mixedDecompressorRejectsNegativeSourceBeforeCreatingNativeStream() throws Exception {
		allocationFailsBeforeNativeFactory(NegativeArraySizeException.class,
				() -> new DecompressMppcZstd(new BufferCodec(), -1, 1024));
	}
}
