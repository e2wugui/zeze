package Zeze.Component;

import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Builtin.RedoQueue.RunTask;
import Zeze.Transaction.Procedure;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRedoQueueProgress {
	@Test
	void aTaskCannotLeaveTheInitialProgressUnchanged() throws Exception {
		checkProgress(false);
	}

	@Test
	void aTaskCannotMoveEstablishedProgressBackwards() throws Exception {
		checkProgress(true);
	}

	private static void checkProgress(boolean established) throws Exception {
		var app = new Application("TestRedoQueueProgress", TakeoverTestEnv.newConf("off", 60_000, 60_000));
		app.initialize(new TakeoverTestEnv.TestAppBase(app));
		var server = new RedoQueueServer(app);
		var applied = new AtomicInteger();
		server.register("progress", 1, ignored -> {
			applied.incrementAndGet();
			return true;
		});
		try {
			app.start();
			if (established) {
				assertTask(app, server, 0, 1, Procedure.Success, 1);
				assertTask(app, server, 1, 2, Procedure.Success, 2);
			}
			long previous = established ? 2 : 0;
			assertTask(app, server, previous, established ? 1 : 0, Procedure.ErrorRequestId, previous);
			assertTask(app, server, previous, previous, Procedure.ErrorRequestId, previous);
			assertTask(app, server, previous, Long.MIN_VALUE, Procedure.ErrorRequestId, previous);
			assertEquals(established ? 2 : 0, applied.get(), "拒绝非法进度必须先于任务处理回调");
			assertTask(app, server, previous, previous + 1, Procedure.Success, previous + 1);
			assertEquals(established ? 3 : 1, applied.get());
		} finally {
			server.stop();
			app.stop();
		}
	}

	private static void assertTask(Application app, RedoQueueServer server, long previous, long next,
			long expectedCode, long expectedProgress) {
		var rpc = new RunTask();
		rpc.Argument.setQueueName("progress");
		rpc.Argument.setTaskType(1);
		rpc.Argument.setPrevTaskId(previous);
		rpc.Argument.setTaskId(next);
		var result = new long[1];
		assertEquals(Procedure.Success, app.newProcedure(() -> {
			result[0] = server.ProcessRunTaskRequest(rpc);
			return Procedure.Success;
		}, "TestRedoQueueProgress.task").call());
		assertEquals(expectedCode, result[0]);
		assertEquals(expectedProgress, rpc.Result.getTaskId());
	}
}
