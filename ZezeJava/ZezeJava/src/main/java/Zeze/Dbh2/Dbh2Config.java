package Zeze.Dbh2;

import Zeze.Config;
import org.w3c.dom.Element;

public class Dbh2Config implements Config.ICustomize {
	private long prepareMaxTime = 12_000; // 12s
	private long bucketMaxTime = 14_000; // 14s 必须大于prepareMaxTime
	private int serverFastErrorPeriod = 5000;
	private int splitPutCount = 100;
	private double splitLoad = 5000 * 0.8;
	private double splitMaxManagerLoad = splitLoad * 4;
	private int raftClusterCount = 3;

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
	public String getName() {
		return "Dbh2Config";
	}

	@Override
	public void parse(Element self) {
		var attr = self.getAttribute("PrepareMaxTime");
		if (!attr.isBlank())
			prepareMaxTime = Long.parseLong(attr);

		attr = self.getAttribute("BucketMaxTime");
		if (!attr.isBlank())
			bucketMaxTime = Long.parseLong(attr);

		if (bucketMaxTime - prepareMaxTime < 2000)
			bucketMaxTime = prepareMaxTime + 2000;

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
	}
}
