package Zeze.MQ;

import Zeze.Config;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

public class MQConfig implements Config.ICustomize {
	private int rpcTimeout = 20_000;

	// 【GB-D02】段物理回收：水位越过段尾的已确认段周期整段回收（文件+索引列族）。
	// 默认开，保守阈值：软删除窗口为段自"完全确认"首次被观察到起的保留时长
	//（实际粒度受 loadMonitorTimer 周期约束，默认 120s 检查一轮）。
	private boolean segmentRecycleEnabled = true;
	private long segmentRecycleDelayMs = 60_000;

	public int getRpcTimeout() {
		return rpcTimeout;
	}

	public void setRpcTimeout(int value) {
		rpcTimeout = value;
	}

	public boolean isSegmentRecycleEnabled() {
		return segmentRecycleEnabled;
	}

	public void setSegmentRecycleEnabled(boolean value) {
		segmentRecycleEnabled = value;
	}

	public long getSegmentRecycleDelayMs() {
		return segmentRecycleDelayMs;
	}

	public void setSegmentRecycleDelayMs(long value) {
		segmentRecycleDelayMs = value;
	}

	@Override
	public @NotNull String getName() {
		return "MQConfig";
	}

	@Override
	public void parse(@NotNull Element self) {
		var attr = self.getAttribute("RpcTimeout");
		if (!attr.isBlank())
			rpcTimeout = Integer.parseInt(attr);
		attr = self.getAttribute("SegmentRecycleEnabled");
		if (!attr.isBlank())
			segmentRecycleEnabled = Boolean.parseBoolean(attr);
		attr = self.getAttribute("SegmentRecycleDelayMs");
		if (!attr.isBlank())
			segmentRecycleDelayMs = Long.parseLong(attr);
	}
}
