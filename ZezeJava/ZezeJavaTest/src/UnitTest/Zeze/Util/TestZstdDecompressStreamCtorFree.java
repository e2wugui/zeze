package UnitTest.Zeze.Util;

import java.io.IOException;
import java.util.Random;
import Zeze.Net.BufferCodec;
import Zeze.Util.ZstdFactory;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * R2-U2回归（FND7-46姊妹点）：ZstdDecompressStream构造链中super构造已建立native
 * dstream（ZstdInputStreamNoFinalizer构造调createDStream），后续失败使构造失败——
 * 对象不可达而close()（freeDStream）永不会被调用，native解压上下文永久泄漏。
 * 可达失败路径：超大dstBufSize的OutOfMemoryError（实测负值被{@code dstBufSize>0}
 * 守卫跳过不抛；Integer.MAX_VALUE超VM数组长度上限，确定性抛OOM且不实际消耗堆）。
 * 压缩侧同类问题已按FND7-46收口，解压侧漏改（原只catch IllegalAccessException，
 * OOM直接逃逸）。
 * 修复：构造catch(Throwable)块先释放ctxPtr再重抛原始异常。
 * 说明：释放动作发生在native堆，zstd-jni无公开分配计数器，"已释放"不可确定性
 * 断言；此测试钉住可观察契约：构造失败必须抛错（触发释放路径），且反复失败
 * 不破坏native库状态——后续合法构造/解压/关闭往返正常。
 */
@Fast
public class TestZstdDecompressStreamCtorFree {

	@Test
	public void testCtorFailureThrows() {
		// 超VM数组长度上限：dstBuf分配抛OutOfMemoryError（不实际消耗堆），dstream已建立
		Assertions.assertThrows(OutOfMemoryError.class,
				() -> new ZstdFactory.ZstdDecompressStream(Integer.MAX_VALUE));
	}

	@Test
	public void testRepeatedFailedCtorKeepsNativeHealthy() throws IOException {
		// 反复失败的构造（修复前每次泄漏一个dctx）不得影响后续native调用
		for (var i = 0; i < 100; i++) {
			try {
				new ZstdFactory.ZstdDecompressStream(Integer.MAX_VALUE);
				Assertions.fail("unreachable");
			} catch (OutOfMemoryError expected) {
			}
		}
		// 失败后合法流仍可完整解压往返
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
