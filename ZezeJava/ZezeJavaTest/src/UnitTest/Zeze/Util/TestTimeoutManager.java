package UnitTest.Zeze.Util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Util.Task;
import Zeze.Util.TimeoutExecutor;
import Zeze.Util.TimeoutManager;
import org.junit.Assert;
import org.junit.Test;

public class TestTimeoutManager {
	@Test
	public void test() throws InterruptedException, ExecutionException, TimeoutException {
		Task.tryInitThreadPool(null, null, null);
		TimeoutManager.instance.start(1000);

		var e = new TimeoutExecutor();
		e.setDefaultTimeout(1000);
		var r = new AtomicBoolean();
		var f = e.submit(() -> {
			try {
				Thread.sleep(5_000);
			} catch (InterruptedException ex) {
				System.out.println("Interrupted!");
				r.set(true);
			}
		});
		f.get(5_000, TimeUnit.MILLISECONDS);
		Assert.assertTrue(r.get());
	}
}
