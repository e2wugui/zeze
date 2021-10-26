package Zezex;

public class LinkConfig {
	private int MaxOnlineNew = 30;
	public final int getMaxOnlineNew() {
		return MaxOnlineNew;
	}
	public final void setMaxOnlineNew(int value) {
		MaxOnlineNew = value;
	}
	// 大致的Linkd数量。在Provider报告期间，用来估算负载均衡。
	private int ApproximatelyLinkdCount = 4;
	public final int getApproximatelyLinkdCount() {
		return ApproximatelyLinkdCount;
	}
	public final void setApproximatelyLinkdCount(int value) {
		ApproximatelyLinkdCount = value;
	}
}