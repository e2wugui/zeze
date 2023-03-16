package Temp;

public class DemoMain {
	public static void main(String [] args) throws Exception {
		try {
			demo.App.getInstance().Start();
			synchronized (Thread.currentThread()) {
				Thread.currentThread().wait();
			}
		} finally {
			demo.App.getInstance().Stop();
		}
	}
}
