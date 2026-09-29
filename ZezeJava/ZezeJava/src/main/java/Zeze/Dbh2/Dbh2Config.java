package Zeze.Dbh2;

import Zeze.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Dbh2 配置项：从 Config 自定义节解析 RPC 超时、分桶与提交模式等参数。
 */
public class Dbh2Config implements Config.ICustomize {
	// 桶侧undo定时相对prepare时限的强制安全余量下界（FND29 dbh2-03止血）：2PC超时围栏的安全性
	// 建立在协调者与桶两侧墙钟读数一致上，余量必须覆盖最大允许钟差（NTP步进校正/VM暂停恢复的
	// 阶跃预算，60s量级）。低于此余量的配置会让桶侧在协调者仍合法的prepare窗口内undo已决定
	// 提交的事务（客户端确认成功而数据灭失），配置解析处fail-fast挡住。index fencing断根留池（独立裁定）。
	public static final long MinBucketUndoMarginMs = 60_000;
	private int rpcTimeout = 60_000;
	private long prepareMaxTime = 80_000; // 一般大于rpcTimeout
	// 必须 >= prepareMaxTime + MinBucketUndoMarginMs（默认值即满足），见MinBucketUndoMarginMs注释。
	private long bucketMaxTime = 140_000;
	private int serverFastErrorPeriod = 5000;
	private int splitPutCount = 100;
	private double splitLoad = 5000 * 0.8;
	private double splitMaxManagerLoad = splitLoad * 4;
	private int raftClusterCount = 3;
	private boolean serialize = true;
	private int splitCleanCount = 200;
	// splitting条目超龄告警阈值（默认10min量级）：master周期扫描，超龄error
	// 告警。只观测不动作——超大桶拷贝可超过任何阈值，超龄自动删除会人为重演永久读失败。
	private long splittingAgeWarnMs = 600_000;
	// 远程提交模式（Dbh2LocalCommit=false）的CommitServer直连地址，属性CommitServerAddress="host:port"，可空。
	// 单实例语义：commitPoint集中在该CommitServer的CommitRocks库，多实例时事务跨服务器无统一redo视角。
	private @Nullable String commitServerHost;
	private int commitServerPort;

	public @Nullable String getCommitServerHost() {
		return commitServerHost;
	}

	public int getCommitServerPort() {
		return commitServerPort;
	}

	public int getRpcTimeout() {
		return rpcTimeout;
	}

	public void setRpcTimeout(int value) {
		rpcTimeout = value;
	}

	public boolean isSerialize() {
		return serialize;
	}

	public void setSerialize(boolean value) {
		serialize = value;
	}

	public int getRaftClusterCount() {
		return raftClusterCount;
	}

	public double getSplitMaxManagerLoad() {
		return splitMaxManagerLoad;
	}

	public double getSplitLoad() {
		return splitLoad;
	}

	public int getSplitPutCount() {
		return splitPutCount;
	}

	public int getServerFastErrorPeriod() {
		return serverFastErrorPeriod;
	}

	public long getPrepareMaxTime() {
		return prepareMaxTime;
	}

	public long getBucketMaxTime() {
		return bucketMaxTime;
	}

	@Override
	public @NotNull String getName() {
		return "Dbh2Config";
	}

	public int getSplitCleanCount() {
		return splitCleanCount;
	}

	public void setSplitCleanCount(int value) {
		splitCleanCount = value;
	}

	public long getSplittingAgeWarnMs() {
		return splittingAgeWarnMs;
	}

	public void setSplittingAgeWarnMs(long value) {
		splittingAgeWarnMs = value;
	}

	@Override
	public void parse(@NotNull Element self) {

		var attr = self.getAttribute("RpcTimeout");
		if (!attr.isBlank())
			rpcTimeout = Integer.parseInt(attr);

		attr = self.getAttribute("PrepareMaxTime");
		if (!attr.isBlank())
			prepareMaxTime = Long.parseLong(attr);

		if (prepareMaxTime - rpcTimeout < 1000)
			prepareMaxTime = rpcTimeout + 1000;

		attr = self.getAttribute("BucketMaxTime");
		if (!attr.isBlank())
			bucketMaxTime = Long.parseLong(attr);

		// fail-fast（FND29 dbh2-03）：低余量误配直接报配置错误而非静默抬高——静默修正会掩盖
		// "安全围栏依赖跨机墙钟一致"这一部署前提，低余量下桶时钟前跳越余量即undo已决定提交
		// 的事务。错误信息带字段与要求值（对齐CommitServerAddress的校验惯例）。
		if (bucketMaxTime - prepareMaxTime < MinBucketUndoMarginMs)
			throw new RuntimeException("Dbh2Config BucketMaxTime must be >= PrepareMaxTime + "
					+ MinBucketUndoMarginMs + " (bucket-undo fence safety margin over cross-machine clock skew),"
					+ " got: RpcTimeout=" + rpcTimeout + " PrepareMaxTime=" + prepareMaxTime
					+ " (auto-raised to RpcTimeout+1000 if configured lower) BucketMaxTime=" + bucketMaxTime);

		attr = self.getAttribute("ServerFastErrorPeriod");
		if (!attr.isBlank())
			serverFastErrorPeriod = Integer.parseInt(attr);

		attr = self.getAttribute("SplitPutCount");
		if (!attr.isBlank())
			splitPutCount = Integer.parseInt(attr);

		attr = self.getAttribute("SplitLoad");
		if (!attr.isBlank())
			splitLoad = Double.parseDouble(attr);

		attr = self.getAttribute("SplitMaxManagerLoad");
		if (!attr.isBlank())
			splitMaxManagerLoad = Double.parseDouble(attr);

		attr = self.getAttribute("RaftClusterCount");
		if (!attr.isBlank())
			raftClusterCount = Integer.parseInt(attr);

		attr = self.getAttribute("Serialize");
		if (!attr.isBlank())
			serialize = Boolean.parseBoolean(attr);

		attr = self.getAttribute("SplitCleanCount");
		if (!attr.isBlank())
			splitCleanCount = Integer.parseInt(attr);

		attr = self.getAttribute("SplittingAgeWarnMs");
		if (!attr.isBlank())
			splittingAgeWarnMs = Long.parseLong(attr);

		attr = self.getAttribute("CommitServerAddress");
		if (!attr.isBlank()) {
			var hostPort = attr.split(":", 2);
			if (hostPort.length != 2 || hostPort[0].isBlank() || hostPort[1].isBlank())
				throw new RuntimeException("Dbh2Config CommitServerAddress must be host:port, got: " + attr);
			commitServerHost = hostPort[0];
			commitServerPort = Integer.parseInt(hostPort[1]);
		}
	}
}
