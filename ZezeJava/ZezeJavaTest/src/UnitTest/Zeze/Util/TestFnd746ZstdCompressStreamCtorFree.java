package UnitTest.Zeze.Util;

import java.io.IOException;
import java.util.Random;
import Zeze.Net.BufferCodec;
import Zeze.Util.ZstdFactory;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-46回归：ZstdCompressStream构造链中super构造已建立native cstream
 * （ZstdOutputStreamNoFinalizer构造调createCStream），后续任一步失败（实测：
 * 负dstBufSize的NegativeArraySizeException；ctxPtr==0；resetCStream非0；
 * 注：越界level/windowLog被native钳制，实测不抛）使构造失败——对象不可达而
 * close()（freeCStream）永不会被调用，native压缩上下文永久泄漏。
 * 修复：构造catch块先释放ctxPtr再重抛原始异常。
 * 说明：释放动作发生在native堆，zstd-jni无公开分配计数器，"已释放"不可确定性
 * 断言；此测试钉住可观察契约：构造失败必须抛错（触发释放路径），且反复失败
 * 不破坏native库状态——后续合法构造/压缩/关闭往返正常。
 */
@Fast
public class TestFnd746ZstdCompressStreamCtorFree {

	@Test
	public void testCtorFailureThrows() {
		// 负dstBufSize：dstBuf数组分配失败，cstream已建立——构造失败的可达路径
		Assertions.assertThrows(NegativeArraySizeException.class, () -> new ZstdFactory.ZstdCompressStream(
				-1, ZstdFactory.ZstdCompressStream.DEFAULT_COMPRESS_LEVEL,
				ZstdFactory.ZstdCompressStream.DEFAULT_WINDOW_LOG));
	}

	@Test
	public void testRepeatedFailedCtorKeepsNativeHealthy() throws IOException {
		// 反复失败的构造（修复前每次泄漏一个cctx）不得影响后续native调用
		for (var i = 0; i < 100; i++) {
			try {
				new ZstdFactory.ZstdCompressStream(-1,
						ZstdFactory.ZstdCompressStream.DEFAULT_COMPRESS_LEVEL,
						ZstdFactory.ZstdCompressStream.DEFAULT_WINDOW_LOG);
				Assertions.fail("unreachable");
			} catch (NegativeArraySizeException expected) {
			}
		}
		// 失败后合法流仍可完整压缩往返
		var data = new byte[700];
		new Random(42).nextBytes(data);
		var bufcp = new BufferCodec();
		var cp = ZstdFactory.newCompressStream();
		cp.compress(data, 0, data.length, (Zeze.Net.Codec)bufcp);
		cp.flush((Zeze.Net.Codec)bufcp);
		cp.close();
		var bufdp = new BufferCodec();
		var dp = ZstdFactory.newDecompressStream();
		dp.decompress(bufcp.getBuffer().Bytes, bufcp.getBuffer().ReadIndex, bufcp.getBuffer().size(),
				(Zeze.Net.Codec)bufdp);
		dp.close();
		Assertions.assertArrayEquals(data, java.util.Arrays.copyOfRange(
				bufdp.getBuffer().Bytes, bufdp.getBuffer().ReadIndex, bufdp.getBuffer().WriteIndex));
	}
}
