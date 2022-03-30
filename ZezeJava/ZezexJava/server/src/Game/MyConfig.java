package Game;

public class MyConfig {
	private int MaxOnlineNew = 30;
	public final int getMaxOnlineNew() {
		return MaxOnlineNew;
	}
	public final void setMaxOnlineNew(int value) {
		MaxOnlineNew = value;
	}
	private int ReportDelaySeconds = 10;
	public final int getReportDelaySeconds() {
		return ReportDelaySeconds;
	}
	public final void setReportDelaySeconds(int value) {
		ReportDelaySeconds = value;
	}
	private int ProposeMaxOnline = 15000;
	public final int getProposeMaxOnline() {
		return ProposeMaxOnline;
	}
	public final void setProposeMaxOnline(int value) {
		ProposeMaxOnline = value;
	}
	private int DigestionDelayExSeconds = 2;
	public final int getDigestionDelayExSeconds() {
		return DigestionDelayExSeconds;
	}
	public final void setDigestionDelayExSeconds(int value) {
		DigestionDelayExSeconds = value;
	}
}