package UnitTest.Zeze.Net;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import Zeze.Net.BufferCodec;
import Zeze.Net.Compress;
import Zeze.Net.CompressMppcZstd;
import Zeze.Net.CompressZstd;
import Zeze.Net.Decompress;
import Zeze.Net.DecompressMppcZstd;
import Zeze.Net.DecompressZstd;
import Zeze.Util.Task;
import Zeze.Util.ZstdFactory;
import java.util.Arrays;
import java.util.Random;

/**
 * L5-P1 候选【压缩流全链路单发字节错位】归因回归：zstd 解压流尾部丢失。
 *
 * 根因（独立复现定位，见 review-2026-09/l5/G5-SUMMARY.md 压缩流归因报告）：
 * ZstdFactory.ZstdDecompressStream.decompress 的主循环在输入耗尽（srcPos==srcEnd）时退出，
 * 但 native ZSTD_decompressStream 上下文内还滞留最多一个 dstBuf 的已解压输出（每次调用
 * dstBuf 满载时写不下的部分留在上下文里，仅当下一次调用时才吐出）。DecompressZstd.flush()
 * 只透传 sink.flush()，从不 drain —— 整个压缩流的最后一批（≤dstBufSize）静默丢失。
 * 触发条件：单条流解压总量跨过 dstBuf 批次边界（默认 128KB）且流在内部滞留非零时结束。
 * 表现：尾部协议帧丢失，或后续帧整体前移导致解码 "too large"/错位（与 InputLimitCodec 无关）。
 * 修复：decompress 两个重载主循环后空输入 drain 到无产出（官方 ZstdInputStreamNoFinalizer
 * .readInternal 的 while 循环同样以“无进展”为退出条件覆盖此场景）。
 *
 * MPPC（Decompress）无内部输出缓冲、flush 处理完所有完整符号，无此缺陷——61KB 0x42 与
 * 4MB 全零均逐字节正确（本类 testMppcBigPayloadRoundTrip 固化该结论，证伪 L4 观察中
 * “MPPC/zstd 均错位”的 MPPC 半句；zstd 半句由本类其余用例钉死）。
 */
@Fast
public class TestZstdStreamTail {
	static {
		Task.tryInitThreadPool();
	}

	private static byte[] compressZstdOnce(byte[] payload) {
		var sink = new BufferCodec();
		var cp = new CompressZstd(sink);
		cp.update(payload, 0, payload.length);
		cp.flush();
		return Arrays.copyOfRange(sink.Bytes, sink.ReadIndex, sink.WriteIndex);
	}

	private static byte[] decompressZstdChunked(byte[] wire, int chunkSize) {
		var sink = new BufferCodec();
		// 与 TcpSocket.setInputSecurityCodec 的 eCompressTypeZstd 相同参数
		var dp = new DecompressZstd(sink, 128 * 1024, 128 * 1024);
		for (int off = 0; off < wire.length; off += chunkSize) {
			int n = Math.min(chunkSize, wire.length - off);
			dp.update(wire, off, n);
			dp.flush();
		}
		return Arrays.copyOfRange(sink.Bytes, sink.ReadIndex, sink.WriteIndex);
	}

	// 修复前红：4MB 全零解压只得 4063232（恰少一个 128KB dstBuf 批次）。
	@Test
	public final void testZstdSingleSendBigZeros() {
		var payload = new byte[4 * 1024 * 1024];
		var out = decompressZstdChunked(compressZstdOnce(payload), 64 * 1024);
		Assertions.assertArrayEquals(payload, out, "zstd 单发大载荷尾部批次丢失");
	}

	@Test
	public final void testZstdSingleSendBigRandom() {
		var payload = new byte[4 * 1024 * 1024];
		new Random(42).nextBytes(payload);
		var out = decompressZstdChunked(compressZstdOnce(payload), 64 * 1024);
		Assertions.assertArrayEquals(payload, out, "zstd 单发随机大载荷往返");
	}

	// 极端分块（drain 在每个分块边界反复触发），验证顺序与无重复。
	@Test
	public final void testZstdChunkedBoundaries() {
		var payload = new byte[4 * 1024 * 1024];
		new Random(7).nextBytes(payload);
		var wire = compressZstdOnce(payload);
		for (int chunk : new int[] {4096, 997, 1})
			Assertions.assertArrayEquals(payload, decompressZstdChunked(wire, chunk), "chunk=" + chunk);
	}

	// 61KB 0x42 + 协议头（TestTcpSocketInputLimit 负控载荷形状）：单批内完成，修复前后均绿（回归钉）。
	@Test
	public final void testZstdUnderOneBatch() {
		var body = new byte[60 * 1024];
		Arrays.fill(body, (byte)0x42);
		var payload = Zeze.Serialize.ByteBuffer.Allocate(12 + body.length);
		payload.WriteInt(0x1234);
		payload.WriteInt(0x5678);
		payload.WriteInt(64 * 1024);
		payload.Append(body, 0, body.length);
		var bytes = Arrays.copyOfRange(payload.Bytes, payload.ReadIndex, payload.WriteIndex);
		Assertions.assertArrayEquals(bytes, decompressZstdChunked(compressZstdOnce(bytes), 64 * 1024));
	}

	// MPPC 证伪钉：61KB 0x42 与 4MB 全零（跨 8192 窗口多次重置、match_len 顶格）逐字节正确。
	@Test
	public final void testMppcBigPayloadRoundTrip() {
		var body = new byte[60 * 1024];
		Arrays.fill(body, (byte)0x42);
		var payload = Zeze.Serialize.ByteBuffer.Allocate(12 + body.length);
		payload.WriteInt(0x1234);
		payload.WriteInt(0x5678);
		payload.WriteInt(64 * 1024);
		payload.Append(body, 0, body.length);
		var cases = new byte[][] {
				Arrays.copyOfRange(payload.Bytes, payload.ReadIndex, payload.WriteIndex),
				new byte[4 * 1024 * 1024],
		};
		for (var payloadBytes : cases) {
			var csink = new BufferCodec();
			var cp = new Compress(csink);
			cp.update(payloadBytes, 0, payloadBytes.length);
			cp.flush();
			var wire = Arrays.copyOfRange(csink.Bytes, csink.ReadIndex, csink.WriteIndex);
			for (int chunk : new int[] {64 * 1024, 100, 7}) {
				var dsink = new BufferCodec();
				var dp = new Decompress(dsink);
				for (int off = 0; off < wire.length; off += chunk) {
					int n = Math.min(chunk, wire.length - off);
					dp.update(wire, off, n);
					dp.flush();
				}
				Assertions.assertArrayEquals(payloadBytes,
						Arrays.copyOfRange(dsink.Bytes, dsink.ReadIndex, dsink.WriteIndex), "chunk=" + chunk);
			}
		}
	}

	// 组合编解码 MppcZstd（zstd 块路径共用 ZstdDecompressStream）：大载荷尾部不丢（修复受益面）。
	@Test
	public final void testMppcZstdBigBlockRoundTrip() {
		var data = new byte[4 * 1024 * 1024];
		new Random(1234).nextBytes(data);
		var csink = new BufferCodec();
		var cp = new CompressMppcZstd(csink,
				ZstdFactory.ZstdCompressStream.DEFAULT_DST_BUF_SIZE,
				ZstdFactory.ZstdCompressStream.DEFAULT_COMPRESS_LEVEL,
				ZstdFactory.ZstdCompressStream.DEFAULT_WINDOW_LOG);
		cp.updateBlock(data, 0, data.length);
		cp.flushBlock();
		var wire = Arrays.copyOfRange(csink.Bytes, csink.ReadIndex, csink.WriteIndex);
		var dsink = new BufferCodec();
		var dp = new DecompressMppcZstd(dsink,
				ZstdFactory.ZstdDecompressStream.DEFAULT_DST_BUF_SIZE,
				ZstdFactory.ZstdDecompressStream.DEFAULT_DST_BUF_SIZE);
		dp.update(wire, 0, wire.length);
		dp.flush();
		Assertions.assertArrayEquals(data, Arrays.copyOfRange(dsink.Bytes, dsink.ReadIndex, dsink.WriteIndex));
	}
}
