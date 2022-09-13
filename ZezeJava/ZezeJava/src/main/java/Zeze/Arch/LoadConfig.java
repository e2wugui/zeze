package Zeze.Arch;

public class LoadConfig {
	private int MaxOnlineNew = 30;
	private int ApproximatelyLinkdCount = 4; // 大致的Linkd数量。在Provider报告期间，用来估算负载均衡。

	private int ReportDelaySeconds = 10;
	private int ProposeMaxOnline = 15000;
	private int DigestionDelayExSeconds = 2;

	public final int getMaxOnlineNew() {
		return MaxOnlineNew;
	}

	public final void setMaxOnlineNew(int value) {
		MaxOnlineNew = value;
	}

	public final int getApproximatelyLinkdCount() {
		return ApproximatelyLinkdCount;
	}

	public final void setApproximatelyLinkdCount(int value) {
		ApproximatelyLinkdCount = value;
	}

	public final int getReportDelaySeconds() {
		return ReportDelaySeconds;
	}

	public final void setReportDelaySeconds(int value) {
		ReportDelaySeconds = value;
	}

	public final int getProposeMaxOnline() {
		return ProposeMaxOnline;
	}

	public final void setProposeMaxOnline(int value) {
		ProposeMaxOnline = value;
	}

	public final int getDigestionDelayExSeconds() {
		return DigestionDelayExSeconds;
	}

	public final void setDigestionDelayExSeconds(int value) {
		DigestionDelayExSeconds = value;
	}
}
