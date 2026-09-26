package Zeze.MQ.Master;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import Zeze.Builtin.MQ.Master.BMQServer;
import Zeze.Builtin.MQ.Master.BReportPartitions;
import Zeze.Builtin.MQ.Master.BTopicPartitions;
import Zeze.Builtin.MQ.Master.BMQServers;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND19 GB-D01 Master 侧对账判定回归（@Fast 直构 Master，拍板方案C：上报/Master 比对/宽限期/
 * DeletePartition 下发）。
 * <p>
 * 判定口径：上报条目 mqTable 无对应 topic、或该 topic 的 servers 不含此 manager 承载该分区
 * （含 ManagerId==0 存量条目的地址兜底匹配）= 孤儿候选；候选连续存活超宽限期（覆盖 CreateMQ
 * 部分成功窗口）才下发删除，动作可审计（issuer 钩子捕获）；候选在册化/从上报消失时除名
 *（部分成功窗口的自愈）；下发即除名——失败残留由下轮上报重新候选重新计时（宽限期间隔重试）。
 * <p>
 * 注：文件放 src/MQ/ 但声明 package Zeze.MQ.Master（访问 reconcileOrphanReport/putMqServers/
 * orphanFirstSeen/deleteIssuer 包内缝）。
 */
@Fast
public class TestFnd19GBD01MasterReconcile {

	private static final long managerId = 7L;
	private static final String managerHost = "127.0.0.1";
	private static final int managerPort = 40000;

	private static BReportPartitions.Data reportOf(Object... topicAndIndexes) {
		var report = new BReportPartitions.Data();
		for (int i = 0; i < topicAndIndexes.length; i += 2) {
			var tp = new BTopicPartitions.Data();
			tp.setTopic((String)topicAndIndexes[i]);
			@SuppressWarnings("unchecked")
			var indexes = (HashSet<Integer>)topicAndIndexes[i + 1];
			tp.getPartitionIndexes().addAll(indexes);
			report.getTopics().add(tp);
		}
		return report;
	}

	private static HashSet<Integer> setOf(int... indexes) {
		var set = new HashSet<Integer>();
		for (var i : indexes)
			set.add(i);
		return set;
	}

	private static void seed(Master master, String topic, String host, int port, long mid, int... partitionIndexes)
			throws Exception {
		var servers = new BMQServers();
		servers.getInfo().setTopic(topic);
		servers.getInfo().setPartition(partitionIndexes.length);
		for (var pi : partitionIndexes)
			servers.getServers().add(new BMQServer(host, port, pi, topic, mid));
		master.putMqServers(topic, servers);
	}

	@Test
	public void testOrphanCandidateGraceAndCoverage(@TempDir Path tempDir) throws Exception {
		var master = new Master(tempDir.resolve("master").toString(), new Config());
		try {
			master.getMqConfig().setOrphanGracePeriodMs(120); // 收缩宽限期（默认10分钟是同一逻辑）
			// 删除下发捕获（替代真实 rpc；socket 为 null——issuer 不依赖）
			var issued = new HashMap<String, TreeSet<Integer>>();
			master.deleteIssuer = (info, socket, topic, indexes) ->
					issued.computeIfAbsent(topic, __ -> new TreeSet<>()).addAll(indexes);

			var manager = new Master.Manager(null, new BMQServer.Data(managerHost, managerPort, 0, "", managerId));

			// 在册：live/0 按 ManagerId 登记；leg/0 存量条目（ManagerId=0）按地址登记。
			seed(master, "live", managerHost, managerPort, managerId, 0);
			seed(master, "leg", managerHost, managerPort, 0, 0);

			// 第一轮上报：孤儿候选登记，宽限期内不下发（防"创建中"误判）。
			master.reconcileOrphanReport(manager, reportOf(
					"live", setOf(0, 2),   // 0 在册；2 无登记=候选
					"leg", setOf(0),       // 存量地址匹配在册
					"ghost", setOf(0, 1))); // topic 整体不在册=候选
			Assertions.assertEquals(new TreeSet<>(java.util.List.of(
							"7|ghost|0", "7|ghost|1", "7|live|2")),
					new TreeSet<>(master.orphanFirstSeen.keySet()),
					"在册条目（id匹配+存量地址匹配）不入候选；未登记条目入候选");
			Assertions.assertTrue(issued.isEmpty(), "宽限期内不得下发删除");

			// 第二轮上报（超宽限期）：下发删除（按 topic 聚合），候选除名（失败重试走下轮重新计时）。
			Thread.sleep(150);
			master.reconcileOrphanReport(manager, reportOf(
					"live", setOf(0, 2), "leg", setOf(0), "ghost", setOf(0, 1)));
			Assertions.assertEquals(java.util.Set.of("ghost", "live"), issued.keySet(),
					"ghost 全量 + live 的未登记分区被裁决删除");
			Assertions.assertEquals(new TreeSet<>(java.util.List.of(0, 1)), issued.get("ghost"));
			Assertions.assertEquals(new TreeSet<>(java.util.List.of(2)), issued.get("live"));
			Assertions.assertTrue(master.orphanFirstSeen.isEmpty(), "已下发的候选除名（残留重试由重新候选起算）");

			// 自愈路径：CreateMQ 最终登记完成（ghost 在册化，分区 0/1 都登记给本 manager）→ 下轮上报不再候选。
			seed(master, "ghost", managerHost, managerPort, managerId, 0, 1);
			issued.clear();
			master.reconcileOrphanReport(manager, reportOf(
					"live", setOf(0, 2), "leg", setOf(0), "ghost", setOf(0, 1)));
			// live/2 重新候选（首见=本轮，未超宽限）；ghost 已覆盖不入候选。
			Assertions.assertEquals(new TreeSet<>(java.util.List.of("7|live|2")),
					new TreeSet<>(master.orphanFirstSeen.keySet()), "在册化后候选自愈");
			Assertions.assertTrue(issued.isEmpty(), "重新候选须重新过宽限期，不得立即下发");

			// 从上报消失的候选除名（分区被删/重建中）。
			master.reconcileOrphanReport(manager, reportOf(
					"live", setOf(0), "leg", setOf(0), "ghost", setOf(0, 1)));
			Assertions.assertTrue(master.orphanFirstSeen.isEmpty(), "候选从上报消失即除名");
		} finally {
			master.close();
		}
	}
}
