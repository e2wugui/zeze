package UnitTest.Zeze.Net;

import harness.Fast;
import org.junit.jupiter.api.Test;
import Zeze.Serialize.*;
import Zeze.Net.BufferCodec;
import Zeze.Net.Compress;
import Zeze.Net.CompressMppcZstd;
import Zeze.Net.Decompress;
import Zeze.Net.DecompressMppcZstd;
import Zeze.Net.Decrypt;
import Zeze.Net.Encrypt;
import Zeze.Util.ZstdFactory;
import org.junit.jupiter.api.Assertions;
import java.util.*;

@Fast
public class TestCodec{

	@Test
	public final void testEncrypt() {
		BufferCodec b2flush = new BufferCodec();
		byte[] key = {1};
		{
			Encrypt en = new Encrypt(b2flush, key);
			en.update((byte)1);
			en.flush();
			en.update((byte)2);
			en.flush();
		}
		BufferCodec b1flush = new BufferCodec();
		{
			Encrypt en = new Encrypt(b1flush, key);
			en.update((byte)1);
			en.update((byte)2);
			en.flush();
		}
		Assertions.assertEquals(b2flush.getBuffer(), b1flush.getBuffer());

		BufferCodec bdecrypt = new BufferCodec(); {
			Decrypt de = new Decrypt(bdecrypt, key);
			de.update(b2flush.getBuffer().Bytes, b2flush.getBuffer().ReadIndex, b2flush.getBuffer().size());
			de.flush();
		}
		Assertions.assertEquals(2, bdecrypt.getBuffer().size());
		Assertions.assertEquals(1, bdecrypt.getBuffer().Bytes[0]);
		Assertions.assertEquals(2, bdecrypt.getBuffer().Bytes[1]);
	}

	@Test
	public final void testEncrypt2() {
		Random rand = new Random();

		byte[] key = {1, 2, 3, 4, 5};

		int[] sizes = new int[1000];
		for (int i = 0; i < sizes.length; ++i) {
			sizes[i] = rand.nextInt(10 * 1024);
		}
		for (int size : sizes) {
			byte[] buffer = new byte[size];
			rand.nextBytes(buffer);

			BufferCodec encrypt = new BufferCodec();
			Encrypt en = new Encrypt(encrypt, key);
			en.update(buffer, 0, buffer.length);
			en.flush();

			BufferCodec decrypt = new BufferCodec();
			Decrypt de = new Decrypt(decrypt, key);
			de.update(encrypt.getBuffer().Bytes, encrypt.getBuffer().ReadIndex, encrypt.getBuffer().size());
			de.flush();

			Assertions.assertEquals(ByteBuffer.Wrap(buffer), decrypt.getBuffer());
		}
	}

	@Test
	public final void testCompress() {
		Random rand = new Random();
		int[] sizes = new int[1000];
		for (int i = 0; i < sizes.length; ++i) {
			sizes[i] = rand.nextInt(10 * 1024);
		}
		for (int size : sizes) {
			BufferCodec bufcp = new BufferCodec();
			Compress cp = new Compress(bufcp);
			byte[] buffer = new byte[size];
			rand.nextBytes(buffer);
			cp.update(buffer, 0, buffer.length);
			cp.flush();

			BufferCodec bufdp = new BufferCodec();
			Decompress dp = new Decompress(bufdp);
			dp.update(bufcp.getBuffer().Bytes, bufcp.getBuffer().ReadIndex, bufcp.getBuffer().size());
			dp.flush();
			Assertions.assertEquals(ByteBuffer.Wrap(buffer), bufdp.getBuffer());
		}
	}

	// N-2缺陷: SinkWrapper.update的varint编码用len本体移位,最后写数据用的是移位后的残余长度,
	// len>=0x80时向解码方声明了完整长度却只写残余字节,流错位。帧格式: [varint len][len字节数据]。
	@Test
	public final void testMppcZstdSinkWrapperVarint() {
		var data = new byte[200];
		new Random(1234).nextBytes(data);

		var sink = new BufferCodec();
		var wrapper = new CompressMppcZstd.SinkWrapper(sink);
		wrapper.update((byte)0xAB); // 单字节帧: [0x01][0xAB]
		wrapper.update(data, 0, data.length); // 200=0b11001000 -> varint [0xC8, 0x01] + 全部200字节

		var expected = ByteBuffer.Allocate(3 + 200);
		expected.WriteByte(0x01);
		expected.WriteByte((byte)0xAB);
		expected.WriteByte((byte)(200 | 0x80));
		expected.WriteByte((byte)(200 >> 7));
		expected.Append(data, 0, data.length);
		Assertions.assertEquals(expected, sink.getBuffer());

		// len==0: 不能写出任何字节(0是解码侧的flush标记)
		int sizeBefore = sink.getBuffer().size();
		wrapper.update(data, 0, 0);
		Assertions.assertEquals(sizeBefore, sink.getBuffer().size());
	}

	// N-?缺陷: DecompressMppcZstd.update(byte[],int pos,int len) 的参数 pos 遮蔽基类位计数字段 pos，
	// 进块模式分支 int p = pos 取到数组下标、pos = 0 把循环下标清零。
	// 旧 testMppcZstdRoundTrip 未命中：块标记在流头部时，重扫恰好把 [DF FF 长度] 吃成废 varint，
	// 其后 zstd 载荷字节序碰巧不错位。必须在块之前先有常规 MPPC 数据，让进块事件发生在数组中段。
	@Test
	public final void testMppcZstdEnterBlockAfterMppc() {
		var rand = new Random(1234);
		var raw1 = new byte[1000]; // 常规MPPC段（块模式之前）
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
			cp.flush();
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
		Assertions.assertEquals(expected, bufdp.getBuffer());
	}

	@Test
	public final void testMppcZstdRoundTrip() {
		var rand = new Random();
		int[] sizes = {1, 100, 127, 128, 300, 64 * 1024};
		for (int size : sizes) {
			var data = new byte[size];
			rand.nextBytes(data);

			var bufcp = new BufferCodec();
			var cp = new CompressMppcZstd(bufcp,
					ZstdFactory.ZstdCompressStream.DEFAULT_DST_BUF_SIZE,
					ZstdFactory.ZstdCompressStream.DEFAULT_COMPRESS_LEVEL,
					ZstdFactory.ZstdCompressStream.DEFAULT_WINDOW_LOG);
			cp.updateBlock(data, 0, size);
			cp.flushBlock();

			var bufdp = new BufferCodec();
			var dp = new DecompressMppcZstd(bufdp,
					ZstdFactory.ZstdDecompressStream.DEFAULT_DST_BUF_SIZE,
					ZstdFactory.ZstdDecompressStream.DEFAULT_DST_BUF_SIZE);
			dp.update(bufcp.getBuffer().Bytes, bufcp.getBuffer().ReadIndex, bufcp.getBuffer().size());
			dp.flush();
			Assertions.assertEquals(ByteBuffer.Wrap(data), bufdp.getBuffer());
		}
	}
}
