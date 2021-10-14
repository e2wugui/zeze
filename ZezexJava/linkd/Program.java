package linkd;

public class Program {
	public static void main(String[] args) {
		Zeze.Serialize.ByteBuffer.BinaryNoCopy = true;
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