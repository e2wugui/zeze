public class Program {
	public static void main(String[] args) throws Throwable {
		Game.App.getInstance().Start(args);
		try {
			//noinspection InfiniteLoopStatement
			while (true) {
				//noinspection BusyWait
				Thread.sleep(1000);
			}
		} finally {
			Game.App.getInstance().Stop();
		}
	}
}
