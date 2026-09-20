package UnitTest.Zeze.Services;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AllocateId128;
import Zeze.Services.ServiceManager.Id128UdpServer;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND6-28：Id128UdpServer唯一name数量无界——防御注释宣称「无界唯一name撑爆cache/RocksDB
 * 非法抛出」但实现只查count与name长度，攻击者以合法count+海量不同name可无界增长（DoS）。
 * 修复：新name超过MAX_UNIQUE_NAMES(1024)即拒绝整包（table=null纯内存）；既有name不受影响。
 * 反射调process()（table=null构造，无网络交互）。
 * FND6-28补：拒绝路径的run()全栈日志改限频不打栈（日志刷屏DoS）；持久化部署（table!=null）
 * 满员改逐出闲置条目自愈——重启冷却后cache为空，一律拒绝会把合法name锁死在rocks外
 * （rpc超时直击finalCommit主路径），逐出语义等同重启加载。
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

			// 超限的新name被拒绝（纯内存部署无恢复手段；run()内层catch限频记日志丢弃整包；
			// 直接调用表现为抛出）。
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

	@Test
	public void testPersistentTableEvictsIdleAndRecovers() throws Exception {
		var dir = Files.createTempDirectory("id128-evict");
		try (var db = new RocksDatabase(dir.toString())) {
			// 2026-09-20审核：原只关库不删目录（%TEMP%残留rocksdb目录），对齐同族deleteDirectory收尾

			var table = db.getOrAddTable("id128");
			var server = new Id128UdpServer(table); // 持久化部署
			var process = Id128UdpServer.class.getDeclaredMethod("process", AllocateId128.class, ByteBuffer.class);
			process.setAccessible(true);
			var cacheField = field(Id128UdpServer.class, "cache");

			// 填满上限，全部推进过max（count=1使current>max=0）并持久化到rocks。
			for (int i = 0; i < Id128UdpServer.MAX_UNIQUE_NAMES; i++) {
				var rpc = new AllocateId128();
				rpc.Argument.setName("name-" + i);
				rpc.Argument.setCount(1);
				process.invoke(server, rpc, ByteBuffer.Allocate(32));
			}

			// 满员后的新name不再被拒：逐出一个闲置条目腾位（FND6-28补自愈）。
			var newcomer = new AllocateId128();
			newcomer.Argument.setName("newcomer");
			newcomer.Argument.setCount(1);
			process.invoke(server, newcomer, ByteBuffer.Allocate(32));
			Assertions.assertEquals(1, newcomer.Result.getCount(), "持久化部署满员新name必须成功（逐出自愈）");

			// 被逐出的name再请求：从rocks恢复，startId不回退（≥已持久化的max），
			// 不会重发已分配的号段。
			var old = new AllocateId128();
			old.Argument.setName("name-0");
			old.Argument.setCount(1);
			process.invoke(server, old, ByteBuffer.Allocate(32));
			Assertions.assertEquals(1, old.Result.getCount());
			Assertions.assertTrue(old.Result.getStartId().compareTo(new Zeze.Util.Id128()) > 0,
					"恢复后startId必须从已持久化max继续（不回退重发）");

			// 容量仍有界（单线程下恰为上限）。
			@SuppressWarnings("unchecked")
			var cache = (java.util.concurrent.ConcurrentHashMap<?, ?>)cacheField.get(server);
			Assertions.assertEquals(Id128UdpServer.MAX_UNIQUE_NAMES, cache.size());
		} finally {
			Zeze.Raft.LogSequence.deleteDirectory(dir.toFile()); // 2026-09-20审核：只关库不删目录
		}
	}

	@SuppressWarnings("SameParameterValue")
	private static Field field(Class<?> cls, String name) throws Exception {
		var f = cls.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}
}
