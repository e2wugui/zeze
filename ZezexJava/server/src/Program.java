
public class Program {
	public static void main(String[] args) throws Throwable {
		Game.App.getInstance().Start(args);
		try {
			while (true) {
				Thread.sleep(1000);
			}
		}
		finally {
			Game.App.getInstance().Stop();
		}
	}
}
