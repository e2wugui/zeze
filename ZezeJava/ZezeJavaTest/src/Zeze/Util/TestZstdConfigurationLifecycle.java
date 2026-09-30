package Zeze.Util;

import java.util.List;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestZstdConfigurationLifecycle {
	@Test
	public void compressedStreamRejectsNativeConfigurationAfterClose() throws Exception {
		var stream = new ZstdFactory.ZstdCompressStream();
		stream.setLevel(1);
		stream.close();
		List<Executable> setters = List.of(
				() -> stream.setChecksum(true), () -> stream.setLevel(1), () -> stream.setLong(0),
				() -> stream.setWorkers(0), () -> stream.setOverlapLog(0), () -> stream.setJobSize(0),
				() -> stream.setTargetLength(0), () -> stream.setMinMatch(0), () -> stream.setSearchLog(0),
				() -> stream.setChainLog(0), () -> stream.setHashLog(0), () -> stream.setWindowLog(0),
				() -> stream.setStrategy(0), () -> stream.setDict(new byte[0]),
				() -> stream.setDict((com.github.luben.zstd.ZstdDictCompress)null));
		for (var setter : setters)
			assertThrows(IllegalStateException.class, setter);
		stream.close();
	}

	@Test
	public void decompressedStreamRejectsNativeConfigurationAfterClose() throws Exception {
		var stream = new ZstdFactory.ZstdDecompressStream();
		stream.setLongMax(0);
		stream.close();
		assertThrows(IllegalStateException.class, () -> stream.setDict(new byte[0]));
		assertThrows(IllegalStateException.class,
				() -> stream.setDict((com.github.luben.zstd.ZstdDictDecompress)null));
		assertThrows(IllegalStateException.class, () -> stream.setLongMax(0));
		assertThrows(IllegalStateException.class, () -> stream.setRefMultipleDDicts(true));
		stream.close();
	}
}
