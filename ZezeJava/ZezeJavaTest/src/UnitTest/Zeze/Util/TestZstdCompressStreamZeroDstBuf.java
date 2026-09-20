package UnitTest.Zeze.Util;

import Zeze.Net.BufferCodec;
import Zeze.Net.Codec;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.ZstdFactory;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Random;

/**
 * U6-F1：ZstdCompressStream 对 dstBufSize==0 无防御，压缩主循环/flush 循环
 * 0 输出 0 消耗永久自旋挂死。修复：构造中 ==0 归一为 DEFAULT_DST_BUF_SIZE
 * （对齐解压侧 "&gt;0 才生效、否则默认" 的语义）；负值保持既有 fail-fast 契约
 * （NegativeArraySizeException，FND7-46 钉死的构造失败→ctx 释放路径，
 * 由 TestFnd746ZstdCompressStreamCtorFree 覆盖）。
 */
@Fast
public class TestZstdCompressStreamZeroDstBuf {

	private static void roundTrip(int dstBufSize, byte[] data) throws Exception {
		var bufcp = new BufferCodec();
		var cp = new ZstdFactory.ZstdCompressStream(dstBufSize,
				ZstdFactory.ZstdCompressStream.DEFAULT_COMPRESS_LEVEL,
				ZstdFactory.ZstdCompressStream.DEFAULT_WINDOW_LOG);
		cp.compress(data, 0, data.length, (Codec)bufcp);
		cp.flush((Codec)bufcp);
		cp.close();

		var bufdp = new BufferCodec();
		var dp = ZstdFactory.newDecompressStream();
		dp.decompress(bufcp.getBuffer().Bytes, bufcp.getBuffer().ReadIndex, bufcp.getBuffer().size(), (Codec)bufdp);
		dp.close();
		Assertions.assertEquals(ByteBuffer.Wrap(data), bufdp.getBuffer());
	}

	/** 修复前：构造静默接受 new byte[0]，compress 永久自旋（由 @Timeout 兜底）。 */
	@Test
	@Timeout(10)
	public void testZeroDstBufNormalized() throws Exception {
		var data = new byte[1000];
		new Random(1234).nextBytes(data);
		roundTrip(0, data);
	}

	/** 显式传入默认尺寸（正路径）行为不变。 */
	@Test
	@Timeout(10)
	public void testExplicitDstBufUnchanged() throws Exception {
		var data = new byte[300];
		new Random(5678).nextBytes(data);
		roundTrip(ZstdFactory.ZstdCompressStream.DEFAULT_DST_BUF_SIZE, data);
	}
}
