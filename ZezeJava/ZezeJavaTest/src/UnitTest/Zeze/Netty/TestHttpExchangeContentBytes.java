package UnitTest.Zeze.Netty;

import harness.Fast;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND2-N2-5回归:contentBytes()快路径(readerIndex==0且hasArray时直接返回c.array())返回的
 * 是整个底层数组,长度=capacity;解码经addContent首块成为content的堆缓冲capacity常大于实际写入量,
 * 调用方拿到带尾随脏数据的超长数组(JSON/定长解析错乱,尾随字节可能是上一请求残留)。
 * 修复后快路径仅在数组与内容严格重合(readerIndex==0且arrayOffset==0且writerIndex==capacity)时
 * 生效,否则拷贝readableBytes字节。
 */
@Fast
public class TestHttpExchangeContentBytes {
	// context仅在派发时使用,contentBytes/addContent不触碰;无服务器直接构造
	private final HttpExchange x = new HttpExchange(new HttpServer(), null);

	@AfterEach
	public void tearDown() {
		x.releaseContent();
	}

	// readerIndex==0但capacity(16)>writerIndex(5):必须拷贝,长度==readableBytes。
	// 修复前返回16长度的数组(尾随11字节陈旧数据),本用例失败。
	@Test
	public void testSpareCapacityCopies() {
		var buf = Unpooled.buffer(16);
		buf.writeBytes(new byte[]{1, 2, 3, 4, 5});
		Assertions.assertSame(buf, x.addContent(buf)); // 所有权转移,首块直接成为content
		Assertions.assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, x.contentBytes());
	}

	// 数组与内容严格重合(writerIndex==capacity):仍走快路径,返回内容正确的数组
	@Test
	public void testExactFitFastPath() {
		Assertions.assertEquals(0, x.contentBytes().length); // 空content
		var buf = Unpooled.wrappedBuffer(new byte[]{6, 7, 8});
		Assertions.assertSame(buf, x.addContent(buf));
		Assertions.assertArrayEquals(new byte[]{6, 7, 8}, x.contentBytes());
	}

	// readerIndex>0:拷贝剩余readableBytes,不含已读部分
	@Test
	public void testNonZeroReaderIndex() {
		var buf = Unpooled.buffer(16);
		buf.writeBytes(new byte[]{1, 2, 3, 4, 5});
		buf.readBytes(2); // readerIndex=2, readable=3
		Assertions.assertSame(buf, x.addContent(buf));
		Assertions.assertArrayEquals(new byte[]{3, 4, 5}, x.contentBytes());
	}

	// 多块累积(CompositeByteBuf):无论内部形态,长度与内容必须等于readableBytes
	@Test
	public void testCompositeContent() {
		x.addContent(Unpooled.wrappedBuffer(new byte[]{1, 2, 3}));
		x.addContent(Unpooled.wrappedBuffer(new byte[]{4, 5}));
		Assertions.assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, x.contentBytes());
	}
}
