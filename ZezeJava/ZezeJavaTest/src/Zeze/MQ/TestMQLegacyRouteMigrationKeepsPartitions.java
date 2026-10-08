package Zeze.MQ.Master;

import harness.Extra;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.TreeSet;
import Zeze.Builtin.MQ.Master.BMQServer;
import Zeze.Builtin.MQ.Master.BMQServers;
import Zeze.Builtin.MQ.Master.BReportPartitions;
import Zeze.Builtin.MQ.Master.BTopicPartitions;
import Zeze.Config;
import Zeze.MQ.MqTestSupport;
import Zeze.Net.Service;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 存量 ManagerId=0 路由 + Manager 换地址迁移的对账回归（@Fast 直构 Master）：升级前
 * 版本落库的路由条目 id=0（decode 缺省，未知身份），承载 Manager 把 home 整体迁到新
 * 地址、以新版本（铸得新 id）重启后，周期上报的活分区不得判孤儿下发 DeletePartition
 * ——旧址已无任何存活注册（地址级死属主），上报本身即数据延续证据，判覆盖不删，并把
 * 条目地址改写为新址、id 升格为上报者（路由跟随数据真相）。
 * <p>
 * 修复前：覆盖判定三路（id 匹配/存量条目按注册地址匹配/属主非零 id 无存活连接）对
 * "id=0 + 地址已换"全不命中——整批分区连续判孤儿，超宽限（整 Manager 面积闸只翻倍
 * 一轮）后被物理删除，且路由仍指向已死旧址：唯一存活副本被摧毁、topic 永久不可用。
 * <p>
 * 防抢路由锚保留：旧址仍有存活注册（旧 Manager 在役，含仍跑无 id 旧版的形态）时，
 * 新地址的上报者是竞争者（克隆数据目录形态）而非延续证据，维持 fail-fast 孤儿裁决。
 * <p>
 * 注：文件放 src/MQ/ 但声明 package Zeze.MQ.Master——访问 reconcileOrphanReport/
 * putMqServers/putManager/orphanFirstSeen/deleteIssuer 包内缝（TestMasterReconcile
 * 先例）。
 */
@Fast
@Extra
public class TestMQLegacyRouteMigrationKeepsPartitions {

	private static void seed(Master master, String topic, String host, int port, long mid, int... partitionIndexes)
			throws Exception {
		var servers = new BMQServers();
		servers.getInfo().setTopic(topic);
		servers.getInfo().setPartition(partitionIndexes.length);
		for (var pi : partitionIndexes)
			servers.getServers().add(new BMQServer(host, port, pi, topic, mid));
		master.putMqServers(topic, servers);
	}

	private static BReportPartitions.Data reportOf(String topic, int... partitionIndexes) {
		var report = new BReportPartitions.Data();
		var tp = new BTopicPartitions.Data();
		tp.setTopic(topic);
		for (var pi : partitionIndexes)
			tp.getPartitionIndexes().add(pi);
		report.getTopics().add(tp);
		return report;
	}

	// 主形态：存量 id=0@旧址条目，Manager 迁址 + 升级铸新 id 后上报——不删 + 改写升格。
	@Test
	public void testLegacyIdZeroRouteMigratedAddressKeepsPartitions(@TempDir Path tempDir) throws Exception {
		var master = new Master(tempDir.resolve("master").toString(), new Config());
		try {
			master.getMqConfig().setOrphanGracePeriodMs(0); // 候选即满龄（判定与默认10分钟同构）
			var issued = new HashMap<String, TreeSet<Integer>>();
			master.deleteIssuer = (info, socket, topic, indexes) ->
					issued.computeIfAbsent(topic, __ -> new TreeSet<>()).addAll(indexes);

			// 存量：升级前版本落库的路由条目 id=0@旧址 10.0.0.1:20000。
			seed(master, "t", "10.0.0.1", 20000, 0L, 0, 1);
			// 迁移后：新版本 Manager 在新址 10.0.0.2:21000 重启，铸得新 id=42 并注册。
			var reporter = new Master.Manager(null, new BMQServer.Data("10.0.0.2", 21000, 0, "", 42L));

			master.reconcileOrphanReport(reporter, reportOf("t", 0, 1));

			Assertions.assertTrue(issued.isEmpty(),
					"旧址无存活注册=地址级死属主，上报即数据延续证据，不得下发删除");
			for (var server : master.getServers("t").getServers()) {
				Assertions.assertEquals(42L, server.getManagerId(), "存量 id=0 条目须证据化升格为上报者 id");
				Assertions.assertEquals("10.0.0.2", server.getHost(), "条目地址改写为迁移后新址");
				Assertions.assertEquals(21000, server.getPort());
			}
			Assertions.assertTrue(master.orphanFirstSeen.isEmpty(), "证据化覆盖不入孤儿候选");
		} finally {
			master.close();
		}
	}

	// 变体：Manager 不升级（仍无 id）只迁址——同为数据延续证据，地址改写、id 保持未知身份。
	@Test
	public void testLegacyReporterMigratedAddressWithoutId(@TempDir Path tempDir) throws Exception {
		var master = new Master(tempDir.resolve("master").toString(), new Config());
		try {
			master.getMqConfig().setOrphanGracePeriodMs(0);
			var issued = new HashMap<String, TreeSet<Integer>>();
			master.deleteIssuer = (info, socket, topic, indexes) ->
					issued.computeIfAbsent(topic, __ -> new TreeSet<>()).addAll(indexes);

			seed(master, "t", "10.0.0.1", 20000, 0L, 0);
			var reporter = new Master.Manager(null, new BMQServer.Data("10.0.0.2", 21000, 0, "", 0L));

			master.reconcileOrphanReport(reporter, reportOf("t", 0));

			Assertions.assertTrue(issued.isEmpty(), "未升级 Manager 的上报同样是数据延续证据，不得下发删除");
			var rewritten = master.getServers("t").getServers().get(0);
			Assertions.assertEquals("10.0.0.2", rewritten.getHost(), "条目地址改写为新址");
			Assertions.assertEquals(21000, rewritten.getPort());
			Assertions.assertEquals(0L, rewritten.getManagerId(), "id 保持未知身份（与 legacy 改写口径一致）");
			Assertions.assertTrue(master.orphanFirstSeen.isEmpty());
		} finally {
			master.close();
		}
	}

	// 防抢路由锚：旧址在役（存活注册在旧址，含 legacy id=0 形态）时，新地址上报者是
	// 竞争者而非延续证据——维持 fail-fast 孤儿裁决，条目不得被改写。
	@Test
	public void testLegacyAddressStillLiveBlocksTransfer(@TempDir Path tempDir) throws Exception {
		var master = new Master(tempDir.resolve("master").toString(), new Config());
		try {
			master.getMqConfig().setOrphanGracePeriodMs(0);
			var issued = new HashMap<String, TreeSet<Integer>>();
			master.deleteIssuer = (info, socket, topic, indexes) ->
					issued.computeIfAbsent(topic, __ -> new TreeSet<>()).addAll(indexes);

			seed(master, "t", "10.0.0.1", 20000, 0L, 0);
			// 旧址在役：旧 Manager（无 id 旧版形态）在旧址持有存活连接。
			var ownerSocket = new MqTestSupport.FakeSocket(
					new Service("TestMQLegacyRouteMigrationKeepsPartitions.liveLegacyOwner"));
			master.putManager(new Master.Manager(ownerSocket, new BMQServer.Data("10.0.0.1", 20000, 0, "", 0L)));
			// 新地址的竞争者（克隆数据目录形态）。
			var stranger = new Master.Manager(null, new BMQServer.Data("10.0.0.2", 21000, 0, "", 42L));

			master.reconcileOrphanReport(stranger, reportOf("t", 0));

			Assertions.assertEquals(new TreeSet<>(java.util.List.of(0)), issued.get("t"),
					"旧址仍有存活注册时维持 fail-fast 孤儿裁决（防克隆数据目录抢路由）");
			var untouched = master.getServers("t").getServers().get(0);
			Assertions.assertEquals(0L, untouched.getManagerId(), "在役旧址的条目不得被竞争者改写");
			Assertions.assertEquals("10.0.0.1", untouched.getHost());
			Assertions.assertEquals(20000, untouched.getPort());
		} finally {
			master.close();
		}
	}
}
