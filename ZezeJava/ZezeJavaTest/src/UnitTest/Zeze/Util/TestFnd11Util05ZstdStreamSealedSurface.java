package UnitTest.Zeze.Util;

import Zeze.Util.ZstdFactory;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND11 util-05回归：ZstdCompressStream未覆写继承的write()/flush()/closeWithoutClosingParentStream()，
 * 继承实现走DummyBufferPool的0长缓冲配静态dstSize的native写——按OutputStream多态调用即越过
 * 数组末端的堆越界写（进程级内存破坏）。ZstdDecompressStream的read()族同因误用读到DummyInputStream
 * 的EOF（静默空数据）。主树仅CompressZstd/CompressMppcZstd调用自有compress/flush(sink)/close，
 * 当前不可达——本测试把两类的Stream多态面封死为响亮失败（UOE）钉住。
 */
@Fast
public class TestFnd11Util05ZstdStreamSealedSurface {

	@Test
	public void testCompressStreamSealed() {
		var c = ZstdFactory.newCompressStream();
		Assertions.assertThrows(UnsupportedOperationException.class, () -> c.write(1));
		Assertions.assertThrows(UnsupportedOperationException.class, () -> c.write(new byte[1], 0, 1));
		Assertions.assertThrows(UnsupportedOperationException.class, () -> c.write(new byte[1]));
		Assertions.assertThrows(UnsupportedOperationException.class, c::flush);
		Assertions.assertThrows(UnsupportedOperationException.class, c::closeWithoutClosingParentStream);
		c.close(); // 幂等安全，不受封印影响
		c.close();
	}

	@Test
	public void testDecompressStreamSealed() {
		var d = ZstdFactory.newDecompressStream();
		Assertions.assertThrows(UnsupportedOperationException.class, d::read);
		Assertions.assertThrows(UnsupportedOperationException.class, () -> d.read(new byte[1], 0, 1));
		Assertions.assertThrows(UnsupportedOperationException.class, () -> d.read(new byte[1]));
		Assertions.assertThrows(UnsupportedOperationException.class, () -> d.skip(1));
		Assertions.assertThrows(UnsupportedOperationException.class, d::available);
		d.close();
		d.close();
	}
}
