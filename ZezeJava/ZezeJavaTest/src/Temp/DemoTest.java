package Temp;

import demo.App;

public class DemoTest {
	public synchronized static void main(String args[]) throws Exception {
		App.Instance.Start();
		try {
			DemoTest.class.wait();
		} finally {
			App.Instance.Stop();
		}
	}
}
