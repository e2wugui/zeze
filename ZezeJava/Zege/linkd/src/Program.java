
public class Program {
	public synchronized static void main(String[] args) throws Throwable {
		Zege.App.Instance.Start();
		try {
			Program.class.wait();
		} finally {
			Zege.App.Instance.Stop();
		}
	}
}
