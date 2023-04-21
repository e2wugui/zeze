package Zeze.Dbh2;

public class CommitServer {
	public static void main(String[] args) throws Exception {
//		var config = "zeze.xml";
//		for (int i = 0; i < args.length; ++i) {
//			//noinspection SwitchStatementWithTooFewBranches
//			switch (args[i]) {
//			case "conf":
//				config = args[++i];
//				break;
//			}
//		}
		Dbh2AgentManager.getInstance().start();
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
