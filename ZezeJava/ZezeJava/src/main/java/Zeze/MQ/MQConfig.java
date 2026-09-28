package Zeze.MQ;

import Zeze.Config;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

/**
 * MQ 配置项（Zeze.Config 自定义段解析）：段回收、孤儿对账宽限期、毒消息重投退避与死信等参数。
 */
public class MQConfig implements Config.ICustomize {
	private int rpcTimeout = 20_000;

	// 段物理回收：水位越过段尾的已确认段周期整段回收（文件+索引列族）。
	// 默认开，保守阈值：软删除窗口为段自"完全确认"首次被观察到起的保留时长
	//（实际粒度受 loadMonitorTimer 周期约束，默认 120s 检查一轮）。
	private boolean segmentRecycleEnabled = true;
	private long segmentRecycleDelayMs = 60_000;

	// 孤儿分区对账宽限期（Master 侧解析，Manager 侧忽略）：Manager 周期上报的本地分区
	// 条目在 mqTable 无对应登记（或登记到别的 manager）判为孤儿候选，连续存活超过本宽限期才下发
	// DeletePartition——覆盖 CreateMQ 的部分成功窗口（"创建中"的正常中间态，防误删）。
	// "连续 N 个上报周期都在"由宽限期隐式表达：默认 10 分钟 / 上报周期 120s ≈ 连续 5 轮都在。
	private long orphanGracePeriodMs = 600_000;

	// 毒消息重投上限：单条消息连续投递失败达到本值（首推+重投共
	// PushRetryMax 次尝试）→ 位点照常推进 + 转死信（或按策略丢弃），不再阻塞队头。
	private int pushRetryMax = 16;
	// 指数退避基（delay = min(Cap, Base << retryCount)，见 MQSingle.retryBackoffMs）。
	// 退避期间该分区不推任何消息——保序的代价，故退避必须封顶（PushRetryBackoffCapMs）。
	private long pushRetryBackoffBaseMs = 500;
	private long pushRetryBackoffCapMs = 60_000;
	// 上限后处置动作两档："deadletter"（默认，转存 rocksdb dlq 表）| "discard"（仅 warn 日志兜底）。
	// 非 "discard"（大小写不敏感）一律按 deadletter 处置。
	private String pushDeadLetterPolicy = "deadletter";

	// dlq 保留上界（estimate 口径近似）：毒消息持续到达时防"只入不出"无界增长。
	// 写入后超限按键序淘汰至目标线并 warn（淘汰动作=可审计的告警面）。键序≠时间序，淘汰顺序
	// 跨 topic 任举但确定（不做时间序——需按 value 尾缀时间戳全量排序，代价不值）。字节上界
	// = DlqMaxEntries × 消息尺寸上界（协议 100MB 级）。
	private int dlqMaxEntries = 10_000;

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

	public long getOrphanGracePeriodMs() {
		return orphanGracePeriodMs;
	}

	public void setOrphanGracePeriodMs(long value) {
		orphanGracePeriodMs = value;
	}

	public int getPushRetryMax() {
		return pushRetryMax;
	}

	public void setPushRetryMax(int value) {
		pushRetryMax = value;
	}

	public long getPushRetryBackoffBaseMs() {
		return pushRetryBackoffBaseMs;
	}

	public void setPushRetryBackoffBaseMs(long value) {
		pushRetryBackoffBaseMs = value;
	}

	public long getPushRetryBackoffCapMs() {
		return pushRetryBackoffCapMs;
	}

	public void setPushRetryBackoffCapMs(long value) {
		pushRetryBackoffCapMs = value;
	}

	public String getPushDeadLetterPolicy() {
		return pushDeadLetterPolicy;
	}

	public void setPushDeadLetterPolicy(String value) {
		pushDeadLetterPolicy = value;
	}

	/** 死信表保留条目上界（estimate 口径近似）。 */
	public int getDlqMaxEntries() {
		return dlqMaxEntries;
	}

	public void setDlqMaxEntries(int value) {
		dlqMaxEntries = value;
	}

	/** 上限后动作是否为丢弃档（其余值一律按死信档处置）。 */
	public boolean isPushDiscardPolicy() {
		return "discard".equalsIgnoreCase(pushDeadLetterPolicy);
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
		attr = self.getAttribute("OrphanGracePeriodMs");
		if (!attr.isBlank())
			orphanGracePeriodMs = Long.parseLong(attr);
		attr = self.getAttribute("PushRetryMax");
		if (!attr.isBlank())
			pushRetryMax = Integer.parseInt(attr);
		attr = self.getAttribute("PushRetryBackoffBaseMs");
		if (!attr.isBlank())
			pushRetryBackoffBaseMs = Long.parseLong(attr);
		attr = self.getAttribute("PushRetryBackoffCapMs");
		if (!attr.isBlank())
			pushRetryBackoffCapMs = Long.parseLong(attr);
		attr = self.getAttribute("PushDeadLetterPolicy");
		if (!attr.isBlank())
			pushDeadLetterPolicy = attr;
		attr = self.getAttribute("DlqMaxEntries");
		if (!attr.isBlank())
			dlqMaxEntries = Integer.parseInt(attr);
	}
}
