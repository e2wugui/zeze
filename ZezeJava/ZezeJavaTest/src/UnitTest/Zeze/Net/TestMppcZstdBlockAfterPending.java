package UnitTest.Zeze.Net;

import java.util.Random;

import Zeze.Net.BufferCodec;
import Zeze.Net.CompressMppcZstd;
import Zeze.Net.DecompressMppcZstd;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.ZstdFactory;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * N1-F2回归：CompressMppcZstd.updateBlock进块模式前若MPPC还有挂起输出（literal/match），
 * 必须先落盘——否则逃逸码+块数据先于挂起输出到达下游（纯流重排），解码端必然错位失败。
 * 既有测试（testMppcZstdEnterBlockAfterMppc等）都在update与updateBlock之间显式flush()，
 * 从未覆盖"不flush直接切块模式"的API契约缺口；本测试去掉中间flush构造确定性红。
 */
@Fast
public class TestMppcZstdBlockAfterPending {

	@Test
	public void testUpdateBlockWithoutExplicitFlushRoundTrip() {
		var rand = new Random(4321);
		// 多个raw1长度：MPPC挂起状态覆盖literal与match两种尾态
		for (int raw1Len : new int[] {1, 17, 100, 1000}) {
			var raw1 = new byte[raw1Len]; // 常规MPPC段（update后必留挂起literal/match，不flush）
			rand.nextBytes(raw1);
			var block = new byte[4096]; // 块模式段（zstd）
			rand.nextBytes(block);
			var raw2 = new byte[777]; // 块结束后回到常规MPPC段
			rand.nextBytes(raw2);

			var bufcp = new BufferCodec();
			var expected = ByteBuffer.Allocate(raw1.length + block.length + raw2.length);
			expected.Append(raw1, 0, raw1.length);
			expected.Append(block, 0, block.length);
			expected.Append(raw2, 0, raw2.length);
			{
				var cp = new CompressMppcZstd(bufcp,
						ZstdFactory.ZstdCompressStream.DEFAULT_DST_BUF_SIZE,
						ZstdFactory.ZstdCompressStream.DEFAULT_COMPRESS_LEVEL,
						ZstdFactory.ZstdCompressStream.DEFAULT_WINDOW_LOG);
				cp.update(raw1, 0, raw1.length);
				// 关键差异：不调cp.flush()，带着挂起输出直接进块模式（修复前为流重排）
				cp.updateBlock(block, 0, block.length);
				cp.flushBlock();
				cp.update(raw2, 0, raw2.length);
				cp.flush();
				cp.close();
			}

			var bufdp = new BufferCodec();
			var dp = new DecompressMppcZstd(bufdp,
					ZstdFactory.ZstdDecompressStream.DEFAULT_DST_BUF_SIZE,
					ZstdFactory.ZstdDecompressStream.DEFAULT_DST_BUF_SIZE);
			dp.update(bufcp.getBuffer().Bytes, bufcp.getBuffer().ReadIndex, bufcp.getBuffer().size());
			dp.flush();
			dp.close();
			Assertions.assertEquals(expected, bufdp.getBuffer(), "raw1Len=" + raw1Len);
		}
	}

	// 注：flushPending与显式flush()的线格式刻意不同——Compress.flush()在_flush()后补
	// 10bit填充token（0x3c0）终结位流，而updateBlock以块模式逃逸码+字节对齐替代终结符，
	// 两者都以解码端可识别的分隔落地。等价性由解码往返保证（上方测试），不做逐字节对比。
}
