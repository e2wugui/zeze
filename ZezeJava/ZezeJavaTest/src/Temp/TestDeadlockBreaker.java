package Temp;

import Zeze.Transaction.TableKey;

public class TestDeadlockBreaker {
	public static void main(String [] args) throws Exception {
		var tKey1 = new TableKey(1, "");
		var tKey2 = new TableKey(2, "");
		var deadObj = new Object();
		try {
			demo.App.getInstance().Start();
			var lockey1 = demo.App.getInstance().getZeze().getLocks().get(tKey1);
			var lockey2 = demo.App.getInstance().getZeze().getLocks().get(tKey2);
			lockey1.enterWriteLock();
			var deadThread = new Thread(() -> {
				try {
					lockey2.enterWriteLock();
					synchronized (deadObj) {
						deadObj.notify();
					}
					System.out.println("enter dead lock1");
					lockey1.enterWriteLock();
					System.out.println("exit dead lock1");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
			deadThread.start();
			synchronized (deadObj) {
				deadObj.wait();
			}
			System.out.println("enter dead lock2");
			lockey2.enterWriteLock();
			System.out.println("exit dead lock2");
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			demo.App.getInstance().Stop();
		}
	}
}
