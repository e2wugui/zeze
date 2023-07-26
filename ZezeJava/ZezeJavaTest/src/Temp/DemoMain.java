package Temp;

import java.util.concurrent.Executors;
import Zeze.Util.Task;
import Zeze.Util.ThreadDiagnosable;

public class DemoMain {
	public static void main(String [] args) throws Exception {
		ThreadDiagnosable.startDiagnose(10);
		var executor = Executors.newSingleThreadExecutor(ThreadDiagnosable.newFactory("testExecutor"));
		executor.execute(() -> {
			System.out.println(1);
			try (var ignored = Task.createTimeout(1000)) {
				try {
					System.out.println(2);
					Thread.sleep(2000);
					System.out.println(3);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println(4);
		});

		System.out.println(5);
		Thread.sleep(3000);
		System.out.println(6);
		executor.shutdown();
		System.out.println(7);
	}
}
