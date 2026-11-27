package Zeze.Dbh2;

import Zeze.Config;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Dbh2 配置项：从 Config 自定义节解析 RPC 超时、分桶与提交模式等参数。
 */
public class Dbh2Config implements Config.ICustomize {
	// 桶侧 undo 超时相对 prepareMaxTime 的最小安全余量，避免桶侧抢先回滚仍在合法 prepare 窗口内的事务
	public static final long MinBucketUndoMarginMs = 60_000;
	private int rpcTimeout = 60_000;
	private long prepareMaxTime = 80_000; // 一般大于 rpcTimeout
	// 必须 >= prepareMaxTime + MinBucketUndoMarginMs
	private long bucketMaxTime = 140_000;
	private int serverFastErrorPeriod = 5000;
	private int splitPutCount = 100;
	private double splitLoad = 5000 * 0.8;
	private double splitMaxManagerLoad = splitLoad * 4;
	private int raftClusterCount = 3;
	private boolean serialize = true;
	private int splitCleanCount = 200;
	// splitting 条目超龄告警阈值，仅观测与结构判死回收门槛（超大桶拷贝可超过此阈值）
	private long splittingAgeWarnMs = 600_000;
	// 远程提交模式的单实例 CommitServer 地址（CommitServerAddress="host:port"），可空
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

	// 远程提交 Commit RPC 等待上界，需同时覆盖服务端同步 decide 与 deliver 阶段及往返余量
	public long getCommitRpcTimeout() {
		return prepareMaxTime + 2L * rpcTimeout + Dbh2Agent.SendForWaitGraceMs + 5_000L;
	}

	public long getBucketMaxTime() {
		return bucketMaxTime;
	}

	@Override
	public String getName() {
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
	public void parse(Element self) {

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
