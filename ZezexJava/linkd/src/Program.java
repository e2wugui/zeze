
public class Program {
	public static void main(String[] args) throws InterruptedException {
		Zeze.Serialize.ByteBuffer.setBinaryNoCopy(true);
		Zezex.App.getInstance().Start();
		try {
			while (true) {
				Thread.sleep(1000);
			}
		}
		finally {
			Zezex.App.getInstance().Stop();
		}
	}
}