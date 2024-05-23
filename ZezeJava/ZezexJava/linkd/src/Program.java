import Zezex.App;

public class Program {
	public synchronized static void main(String[] args) throws Exception {
		App.getInstance().Start(args);
		try {
			Program.class.wait();
		} finally {
			App.getInstance().Stop();
		}
	}
}
