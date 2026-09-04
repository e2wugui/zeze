package UnitTest.Zeze.Services;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.BAllocateId128Argument;
import harness.Fast;

/**
 * FND2-S2-1：BAllocateId128Argument.decode 不再把对端可控的 name intern 进静态无界 BinaryPool
 * （向Id128端口发包即可令每个唯一name永久驻留，无界撑堆）。
 * <p>
 * 两个可观察契约：
 * 1. 同name重复decode不再返回池中同一实例（去intern），但内容等值——消费方
 *    （Id128UdpServer.cache的computeIfAbsent、getName）按Binary内容语义工作不受影响；
 * 2. decode必须拷贝bytes（接收缓冲跨报文复用），decode后破坏缓冲不影响已解码的name。
 */
@Fast
public class TestBAllocateId128Decode {
	@Test
	public void testDecodeNoIntern() {
		var name = "UnitTest.FND2_S2_1.NoIntern.Name";
		var src = new BAllocateId128Argument(name, 100);
		var bb = ByteBuffer.Allocate();
		src.encode(bb);

		bb.ReadIndex = 0;
		var d1 = new BAllocateId128Argument();
		d1.decode(bb);
		bb.ReadIndex = 0;
		var d2 = new BAllocateId128Argument();
		d2.decode(bb);

		// 去intern：同name重复decode各自持有独立实例，不再驻留共享池。
		Assertions.assertNotSame(d1.getBinaryName(), d2.getBinaryName());
		// 但内容等值：cache键查找（hashCode/equals为内容语义）不受影响。
		Assertions.assertEquals(d1.getBinaryName(), d2.getBinaryName());
		Assertions.assertEquals(name, d1.getName());
		Assertions.assertEquals(name, d2.getName());
		Assertions.assertEquals(100, d1.getCount());
		Assertions.assertEquals(100, d2.getCount());
	}

	@Test
	public void testDecodeCopiesBytes() {
		var name = "UnitTest.FND2_S2_1.Copy";
		var src = new BAllocateId128Argument(name, 16);
		var bb = ByteBuffer.Allocate();
		src.encode(bb);
		var d = new BAllocateId128Argument();
		bb.ReadIndex = 0;
		d.decode(bb);
		// decode后破坏接收缓冲，已解码的name不受影响（拷贝语义，缓冲复用安全）。
		Arrays.fill(bb.Bytes, 0, bb.WriteIndex, (byte)'x');
		Assertions.assertEquals(name, d.getName());
		Assertions.assertEquals(16, d.getCount());
	}
}
