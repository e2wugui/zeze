package World;

public class Main {
	public static void main(String[] args) throws Exception {
		var link = new Linkd.App();
		var server = new Demo.App();

		link.Start(-1, 12000, 15000);
		server.Start(0, 20000);

		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
