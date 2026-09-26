package Onz;

import java.nio.file.Path;
import Zeze.Net.AsyncSocket;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static Onz.Fnd21GcOnzFastSupport.*;

/**
 * FND21 GC-C01 回归：共享SM构造器（OnzServer三参）原先在共享agent上只订阅固定名"Onz"，
 * 而getZezeInstance在shared模式下按集群名查subscribeStates（Agent以订阅请求里的服务名
 * 为键）——别名键永不存在，共享模式下每次地址解析恒抛"subscribe not found"（fd7b9eea6
 * 初版即坏，零存量调用方的"文档承诺可用、实际一用即死"）。修复两头对齐：协调者对
 * specialZezeNames逐名订阅（订阅键=查询键）；参与方侧Onz.setRegisterServiceName开注册名
 * 通道（缺省"Onz"，非共享零变化）。
 * 形态：@Fast自包含（进程内SM+两桩参与方按集群唯一名注册——参与方注册侧与Onz.start()
 * 修复后同型），serverId 890段。修复前红：getZezeInstance("zeze890")抛
 * "serviceManager subscribe not found. zeze890"。
 */
@Fast
public class TestFnd21GcC01SharedSmNameResolution {
	// 890段：serverId=890（协调者RocksDB目录CommitOnzServer890），共享配置xml ServerId=893，
	// 桩端口51891/51892、SM端口51890——@Fast并行与其他类（含FND20的85x/5185x段）零冲突。
	private static final int ServerId = 890;
	private static final int SmPort = 51890;
	private static final int SharedConfigServerId = 893;
	private static final String NameA = "zeze890";
	private static final String NameB = "zeze891";
	private static final int PortA = 51891;
	private static final int PortB = 51892;

	/**
	 * 核心红测：共享SM模式下按集群名逐名解析——zeze890→桩A、zeze891→桩B（各名订阅到
	 * 各自注册的参与方地址，不再恒抛"subscribe not found"）；名单外名字仍走既有
	 * "unknown zeze"防御（与订阅错位无关的路径不动）。
	 */
	@Test
	@Timeout(90)
	public void testSharedModeResolvesPerClusterName(@TempDir Path tempDir) throws Exception {
		try (var fixture = startSharedOnzServer(ServerId, SmPort, tempDir, SharedConfigServerId,
				NameA + ";" + NameB,
				new StubSpec(NameA, "891", PortA, null),
				new StubSpec(NameB, "892", PortB, null))) {

			assertRemotePort(fixture.onzServer.getZezeInstance(NameA), PortA,
					"zeze890必须解析到按zeze890注册的桩A（订阅键=查询键，修复前恒抛subscribe not found）");
			assertRemotePort(fixture.onzServer.getZezeInstance(NameB), PortB,
					"zeze891必须解析到按zeze891注册的桩B（两集群互不串址）");

			var ex = Assertions.assertThrows(RuntimeException.class,
					() -> fixture.onzServer.getZezeInstance("zeze899"), "名单外名字的既有防御路径");
			Assertions.assertTrue(ex.getMessage().contains("unknown zeze"), ex.getMessage());
		}
	}

	private static void assertRemotePort(AsyncSocket socket, int expectedPort, String message) {
		Assertions.assertNotNull(socket, message);
		var inet = socket.getRemoteInet();
		Assertions.assertNotNull(inet, "已就绪socket必带远端地址");
		Assertions.assertEquals(expectedPort, inet.getPort(), message);
	}
}
