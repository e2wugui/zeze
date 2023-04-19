package Zeze.Dbh2;

import Zeze.Config;
import org.w3c.dom.Element;

public class Dbh2Config implements Config.ICustomize {
	private long prepareMaxTime = 12_000; // 12s
	private long bucketMaxTime = 14_000; // 14s 必须大于prepareMaxTime
	private int serverFastErrorPeriod = 5000;

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
	}
}
