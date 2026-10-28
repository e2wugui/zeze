package Zeze.Onz;

import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import harness.Fast;
import harness.FastServerIds;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestSagaReplyAfterCommit {
	private static Application application() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setTakeoverMode("off");
		conf.setServerId(FastServerIds.TEST_SAGA_REPLY_AFTER_COMMIT);
		conf.setDefaultTableConf(new Config.TableConf());
		var database = new Config.DatabaseConf();
		database.setDatabaseType(Config.DbType.Memory);
		database.setDatabaseUrl("saga_reply_" + conf.getServerId());
		conf.getDatabaseConfMap().put("", database);
		var app = new Application("SagaReplyAfterCommit", conf);
		app.start();
		return app;
	}

	private static void execute(boolean rejectCommit) throws Exception {
		execute(rejectCommit, Onz.eFlushAsync);
	}

	private static void execute(boolean rejectCommit, int flushMode) throws Exception {
		var app = application();
		var checkpoint = app.getCheckpoint();
		var checkpointField = Application.class.getDeclaredField("checkpoint");
		checkpointField.setAccessible(true);
		try {
			var replies = new AtomicInteger();
			var premature = new AtomicInteger();
			var rpc = new FuncSaga() {
				@Override
				public void SendResult(@Nullable Binary result) {
					replies.incrementAndGet();
					if (!Transaction.getCurrent().isCompleted())
						premature.incrementAndGet();
				}
			};
			rpc.Argument.setFlushMode(flushMode);
			var stub = new OnzSagaStub<EmptyBean, EmptyBean, EmptyBean>(app.getOnz(), "SagaReply",
					(saga, argument, result) -> 0, EmptyBean.class, EmptyBean.class,
					(saga, argument) -> 0, EmptyBean.class);
			var saga = new OnzSaga(rpc, rpc.Argument, stub, new EmptyBean(), new EmptyBean()) {
				@Override
				public void sendReadyAndWait() {
					super.sendReadyAndWait();
					if (rejectCommit) {
						try {
							checkpointField.set(app, null); // The actual finalCommit admission check rejects.
						} catch (IllegalAccessException e) {
							throw new AssertionError(e);
						}
					}
				}
			};
			assertEquals(rejectCommit ? Procedure.Closed : Procedure.Success,
					app.newProcedure(saga, "SagaReply").call());
			assertEquals(rejectCommit ? 0 : 1, replies.get());
			assertEquals(0, premature.get(), "A successful step reply requires a completed local commit");
		} finally {
			checkpointField.set(app, checkpoint);
			app.stop();
		}
	}

	@Test
	void rejectedCommitSendsNoSuccess() throws Exception {
		execute(true);
	}

	@Test
	void successReplyObservesCompletedCommit() throws Exception {
		execute(false);
	}

	@Test
	void immediateFlushCompletesBeforeStepReply() throws Exception {
		execute(false, Onz.eFlushImmediately);
	}
}
