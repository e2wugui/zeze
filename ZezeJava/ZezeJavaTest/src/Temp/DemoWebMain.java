package Temp;

import demo.App;

public class DemoWebMain {
	public static void main(String [] args) throws Exception {
		App.Instance.Start();
		try {
			synchronized (Thread.currentThread()) {
				Thread.currentThread().wait();
			}
		} finally {
			App.Instance.Stop();
		}
	}
}
