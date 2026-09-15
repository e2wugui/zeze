package UnitTest.Zeze.Services;

import java.lang.reflect.Method;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AllocateId128;
import Zeze.Services.ServiceManager.Id128UdpServer;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND6-28：Id128UdpServer唯一name数量无界——防御注释宣称「无界唯一name撑爆cache/RocksDB
 * 非法抛出」但实现只查count与name长度，攻击者以合法count+海量不同name可无界增长（DoS）。
 * 修复：新name超过MAX_UNIQUE_NAMES(1024)即告警（60秒限频）并拒绝整包；既有name不受影响。
 * 反射调process()（table=null构造，无网络交互）。
 */
@Fast
public class TestId128UniqueNamesLimit {

	@Test
	public void testUniqueNamesBounded() throws Exception {
		var server = new Id128UdpServer(); { // table=null, any ip & auto port
			var process = Id128UdpServer.class.getDeclaredMethod("process", AllocateId128.class, ByteBuffer.class);
			process.setAccessible(true);

			// 填满上限：MAX_UNIQUE_NAMES个不同name全部成功。
			for (int i = 0; i < Id128UdpServer.MAX_UNIQUE_NAMES; i++) {
				var rpc = new AllocateId128();
				rpc.Argument.setName("name-" + i);
				rpc.Argument.setCount(1);
				process.invoke(server, rpc, ByteBuffer.Allocate(32));
			}

			// 超限的新name被拒绝（run()内层catch记日志丢弃整包；直接调用表现为抛出）。
			var overflow = new AllocateId128();
			overflow.Argument.setName("overflow");
			overflow.Argument.setCount(1);
			var ex = Assertions.assertThrows(Exception.class,
					() -> process.invoke(server, overflow, ByteBuffer.Allocate(32)),
					"超限新name必须拒绝");
			Assertions.assertTrue(ex.getCause() instanceof IllegalArgumentException,
					"拒绝形态为入口校验IAE，实际: " + ex.getCause());

			// 既有name继续可用（不受限界影响）。
			var existing = new AllocateId128();
			existing.Argument.setName("name-0");
			existing.Argument.setCount(1);
			process.invoke(server, existing, ByteBuffer.Allocate(32));
			Assertions.assertEquals(1, existing.Result.getCount());
		}
	}
}
