package Zeze.Component;

import java.nio.file.Path;
import Zeze.Builtin.RedoQueue.BTaskId;
import Zeze.Builtin.RedoQueue.RunTask;
import Zeze.Config;
import Zeze.Net.Rpc;
import Zeze.Transaction.Procedure;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Fast
public class TestRedoQueueRestartPending {
	@TempDir
	Path directory;

	@Test
	void ignoresThePreviousRunResponseAfterRestart() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		var queue = new RedoQueue(directory.resolve("queue").toString(), config);
		var pending = RedoQueue.class.getDeclaredField("pending");
		pending.setAccessible(true);
		var onResponse = RedoQueue.class.getDeclaredMethod("processRunTaskResult", Rpc.class);
		onResponse.setAccessible(true);
		var previousRun = new RunTask();
		previousRun.Argument.setTaskId(1);
		previousRun.Result.setTaskId(1);

		try {
			queue.start();
			queue.add(1, new BTaskId());
			pending.set(queue, previousRun); // Simulate Send succeeding before the reply is dispatched.
			queue.stop();
			queue.start();

			assertNull(pending.get(queue));
			assertEquals(Procedure.LogicError, onResponse.invoke(queue, previousRun));
		} finally {
			queue.stop();
		}
	}
}
