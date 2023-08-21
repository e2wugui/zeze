import ClientGame.App;

public class Program {
	public synchronized static void main(String [] args) throws Exception {
		App.getInstance().Start("127.0.0.1", 10000);
		App.getInstance().ClientGame_Login.auth();
		try {
			Program.class.wait();
		} finally {
			App.getInstance().Stop();
		}
	}
}
