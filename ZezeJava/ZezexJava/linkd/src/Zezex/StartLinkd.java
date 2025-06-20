package Zezex;

public class StartLinkd {
	public static synchronized void main(String [] args) throws Exception {
		var linkd = new Zezex.App();
		try {
			linkd.Start(-1, 12000, 15000);
			StartLinkd.class.wait();
		} finally {
			linkd.Stop();
		}
	}
}
