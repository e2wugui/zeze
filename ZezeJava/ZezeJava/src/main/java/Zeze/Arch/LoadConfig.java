package Zeze.Arch;

public class LoadConfig {
	private int maxOnlineNew = 100;
	private int approximatelyLinkdCount = 100; // 大致的Linkd数量。在Provider报告期间，用来估算负载均衡。

	private int reportDelaySeconds = 2;
	private int proposeMaxOnline = 30000;
	private int digestionDelayExSeconds = 1;

	public final int getMaxOnlineNew() {
		return maxOnlineNew;
	}

	public final void setMaxOnlineNew(int value) {
		// FND8-90（加固二）：0会让LoadBase.onTimerTask的消化延迟除法除零断链（纵深防护
		// 已用Math.max兜底，此处再拒绝非法值）。"禁新增"语义由消费端onlineNew>maxOnlineNew
		// 门承担，应用以1表达近似语义。主源码零调用方，无兼容性破坏。
		if (value <= 0)
			throw new IllegalArgumentException("maxOnlineNew must be > 0");
		maxOnlineNew = value;
	}

	public final int getApproximatelyLinkdCount() {
		return approximatelyLinkdCount;
	}

	public final void setApproximatelyLinkdCount(int value) {
		approximatelyLinkdCount = value;
	}

	public final int getReportDelaySeconds() {
		return reportDelaySeconds;
	}

	public final void setReportDelaySeconds(int value) {
		reportDelaySeconds = value;
	}

	public final int getProposeMaxOnline() {
		return proposeMaxOnline;
	}

	public final void setProposeMaxOnline(int value) {
		proposeMaxOnline = value;
	}

	public final int getDigestionDelayExSeconds() {
		return digestionDelayExSeconds;
	}

	public final void setDigestionDelayExSeconds(int value) {
		digestionDelayExSeconds = value;
	}
}
