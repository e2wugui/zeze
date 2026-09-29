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

	// 单条消息字节上界（入口校验）：协议层 ProxyServer 放行 100MB，超过本值的消息在
	// SendMessage/replayDeadLetter 入口被响亮拒绝（eMessageTooLarge）——把"协议合法"收敛到
	// "部署可承受"。默认 16MB；解析钳制不超过协议上界 100MB。
	public static final int MaxMessageBytesCeiling = 100 * 1024 * 1024;
	private int maxMessageBytes = 16 * 1024 * 1024;

	// 单分区在飞字节预算：内存队列驻留（直入+装载）的按字节封顶（条数 4096 为正交维度）。
	// 判据统一：队列为空恒放行队头（保队头活性），否则分区+全局双预算均达标才装载/直入。
	private long maxInFlightBytesPerPartition = 64 * 1024 * 1024;

	// Manager 级全局在飞字节预算（全部分区共享）：重启装载（构造期 pullMessage）同受约束
	//——backlog 存在时"装载→OOM→重启→再装载"崩溃循环的根治点。全局满时各分区仍保队头
	// 单条推进（跨分区背压有界），不互相饿死。
	private long maxTotalInFlightBytes = 256 * 1024 * 1024;

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

	/** 单条消息字节上界（入口校验；默认 16MB，配置不超过协议上界 100MB）。 */
	public int getMaxMessageBytes() {
		return maxMessageBytes;
	}

	public void setMaxMessageBytes(int value) {
		maxMessageBytes = value;
	}

	/** 单分区在飞字节预算（默认 64MB）。 */
	public long getMaxInFlightBytesPerPartition() {
		return maxInFlightBytesPerPartition;
	}

	public void setMaxInFlightBytesPerPartition(long value) {
		maxInFlightBytesPerPartition = value;
	}

	/** Manager 级全局在飞字节预算（默认 256MB；重启装载同受约束）。 */
	public long getMaxTotalInFlightBytes() {
		return maxTotalInFlightBytes;
	}

	public void setMaxTotalInFlightBytes(long value) {
		maxTotalInFlightBytes = value;
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
		attr = self.getAttribute("MaxMessageBytes");
		if (!attr.isBlank())
			// 钳制不超过协议上界：超过 100MB 的配置无意义（ProxyServer 入口即拒），静默按上界收。
			maxMessageBytes = (int)Math.min((long)Integer.parseInt(attr), MaxMessageBytesCeiling);
		attr = self.getAttribute("MaxInFlightBytesPerPartition");
		if (!attr.isBlank())
			maxInFlightBytesPerPartition = Long.parseLong(attr);
		attr = self.getAttribute("MaxTotalInFlightBytes");
		if (!attr.isBlank())
			maxTotalInFlightBytes = Long.parseLong(attr);
	}
}
