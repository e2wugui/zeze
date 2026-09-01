package UnitTest.Zeze.Util;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import Zeze.Net.BufferCodec;
import Zeze.Net.Codec;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.ZstdFactory;
import java.util.Random;

@Fast
public class TestZstdFactory {

	// N-?缺陷: ZstdCompressStream.close() 委托基类close()，基类用 DummyBufferPool 的0长缓冲写帧尾，
	// native越过数组末端写完后又被 OutputStream.write 边界检查抛 IndexOutOfBoundsException，
	// TcpSocket.realClose 里的 codec.close() 每次都会中招。修复: close() 直接 freeCStream 只释放
	// native上下文；帧不收尾是既有线格式，流式解码端兼容（见 flush 后的往返断言）。
	@Test
	public final void testCompressStreamClose() {
		var data = new byte[1000];
		new Random(1234).nextBytes(data);

		// 用过的流: flush后close不得抛异常，且幂等，关闭后禁止再压缩
		var bufcp = new BufferCodec();
		var cp = ZstdFactory.newCompressStream();
		cp.compress(data, 0, data.length, (Zeze.Net.Codec)bufcp);
		cp.flush((Zeze.Net.Codec)bufcp);
		cp.close();
		cp.close();
		Assertions.assertThrows(IllegalStateException.class, () -> cp.compress(data, 0, data.length, (Zeze.Net.Codec)bufcp));

		// close不破坏已写数据：未收尾的流仍可完整解压
		var bufdp = new BufferCodec();
		var dp = ZstdFactory.newDecompressStream();
		dp.decompress(bufcp.getBuffer().Bytes, bufcp.getBuffer().ReadIndex, bufcp.getBuffer().size(), (Zeze.Net.Codec)bufdp);
		dp.close();
		dp.close();
		Assertions.assertEquals(ByteBuffer.Wrap(data), bufdp.getBuffer());

		// 未使用过的流直接close也必须无害（旧代码此时也会写空帧而抛）
		ZstdFactory.newCompressStream().close();
		ZstdFactory.newDecompressStream().close();
	}

	// ZstdDecompressStream.close() 原实现委托基类close()（池release + 共享DummyInputStream.close + freeDStream），
	// 不出错纯靠Dummy组件的偶然实现；与压缩侧对齐改为直调freeDStream。本用例固化关闭契约。
	@Test
	public final void testDecompressStreamClose() {
		var data = new byte[500];
		new Random(4321).nextBytes(data);

		var bufcp = new BufferCodec();
		var cp = ZstdFactory.newCompressStream();
		cp.compress(data, 0, data.length, (Codec)bufcp);
		cp.flush((Codec)bufcp);
		cp.close();

		var bufdp = new BufferCodec();
		var dp = ZstdFactory.newDecompressStream();
		dp.decompress(bufcp.getBuffer().Bytes, bufcp.getBuffer().ReadIndex, bufcp.getBuffer().size(), (Codec)bufdp);
		dp.close();
		dp.close(); // 幂等
		Assertions.assertEquals(ByteBuffer.Wrap(data), bufdp.getBuffer());
		Assertions.assertThrows(IllegalStateException.class,
				() -> dp.decompress(bufcp.getBuffer().Bytes, bufcp.getBuffer().ReadIndex, bufcp.getBuffer().size(), (Codec)bufdp));

		// 未使用过的流直接close也必须无害
		ZstdFactory.newDecompressStream().close();
	}
}
