package Zeze.Dbh2;

import Zeze.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class Dbh2Config implements Config.ICustomize {
	private int rpcTimeout = 60_000;
	private long prepareMaxTime = 80_000; // 一般大于rpcTimeout
	private long bucketMaxTime = 100_000; // 必须大于prepareMaxTime
	private int serverFastErrorPeriod = 5000;
	private int splitPutCount = 100;
	private double splitLoad = 5000 * 0.8;
	private double splitMaxManagerLoad = splitLoad * 4;
	private int raftClusterCount = 3;
	private boolean serialize = true;
	private int splitCleanCount = 200;
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

		if (bucketMaxTime - prepareMaxTime < 1000)
			bucketMaxTime = prepareMaxTime + 1000;

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
