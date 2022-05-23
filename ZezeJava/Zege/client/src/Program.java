import java.net.InetAddress;
import java.net.UnknownHostException;
import Zege.App;

public class Program {
	private static String getComputerName() throws UnknownHostException {
		InetAddress addr;
		addr = InetAddress.getLocalHost();
		return addr.getHostName();
	}

	public synchronized static void main(String[] args) throws Throwable {
		App.Instance.Start(null, 0);
		try {
			App.Instance.Connector.WaitReady();
			var account = getComputerName();
			App.Instance.Zege_Linkd.auth(account).await();
			App.Instance.Zeze_Builtin_Online.login("PC").await();
			Program.class.wait();
		} finally {
			App.Instance.Stop();
		}
	}
}
