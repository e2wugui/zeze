package Zeze.Onz;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Application;
import Zeze.Builtin.Onz.FuncProcedure;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import demo.Module1.Table1;
import harness.Fast;
import harness.FastServerIds;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@Fast
public class TestOnzResultRedoIsolation {

	public static final class ResultBean extends EmptyBean {
		public String staleOnly = "";
		public final ArrayList<String> items = new ArrayList<>();

		public ResultBean() {
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteString(staleOnly);
			bb.WriteInt(items.size());
			for (var item : items)
				bb.WriteString(item);
		}

		@Override
		public void decode(IByteBuffer bb) {
			staleOnly = bb.ReadString();
			items.clear();
			int count = bb.ReadInt();
			for (int i = 0; i < count; i++)
				items.add(bb.ReadString());
		}
	}

	@Test
	void procedureRedoDiscardsFirstAttemptResult() throws Exception {
		try (var env = new Fixture()) {
			long key = 101L;
			assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				env.table1.getOrAdd(key).setInt_1(0);
				return Procedure.Success;
			}, "seedProcedureKey").call());

			var attempt1Read = new CountDownLatch(1);
			var conflictCommitted = new CountDownLatch(1);
			var attempts = new AtomicInteger();
			var decodedResult = new ResultBean();
			var procedureRef = new AtomicReference<@Nullable OnzProcedure>();

			var req = new FuncProcedure() {
				@Override
				public void SendResult(@Nullable Binary result) {
					decodedResult.decode(ByteBuffer.Wrap(this.Result.getFuncResult()));
					requireNonNull(procedureRef.get()).commit();
				}
			};
			req.Argument.setOnzTid(1L);
			req.Argument.setFlushMode(Onz.eFlushAsync);
			req.Argument.setFlushTimeout(5_000);

			var stub = new OnzProcedureStub<EmptyBean, ResultBean>(
					env.app.getOnz(), "RedoProcedureResult",
					(proc, arg, res) -> {
						int round = attempts.incrementAndGet();
						int v = env.table1.getOrAdd(key).getInt_1();
						if (v == 0) {
							res.staleOnly = "dirty-from-attempt-1";
							res.items.add("attempt-" + round);
							env.table1.getOrAdd(key).setInt_1(10);
							attempt1Read.countDown();
							assertTrue(conflictCommitted.await(5, TimeUnit.SECONDS));
						} else {
							res.items.add("attempt-" + round);
							env.table1.getOrAdd(key).setInt_1(20);
						}
						return Procedure.Success;
					}, EmptyBean.class, ResultBean.class);

			var argBuf = ByteBuffer.Allocate();
			new EmptyBean().encode(argBuf);
			var proc = stub.newProcedure(req, req.Argument, argBuf);
			procedureRef.set(proc);

			var conflictError = new AtomicReference<@Nullable Throwable>();
			var conflictThread = new Thread(() -> {
				try {
					assertTrue(attempt1Read.await(5, TimeUnit.SECONDS));
					assertEquals(Procedure.Success, env.app.newProcedure(() -> {
						env.table1.getOrAdd(key).setInt_1(1);
						return Procedure.Success;
					}, "conflictProcedureKey").call());
				} catch (Throwable e) {
					conflictError.set(e);
				} finally {
					conflictCommitted.countDown();
				}
			}, "onz-proc-redo-conflict");
			conflictThread.start();
			try {
				assertEquals(Procedure.Success, env.app.newProcedure(proc, "RedoProcedureResult").call());
				conflictThread.join(5_000);
				assertFalse(conflictThread.isAlive());
				assertNull(conflictError.get());
			} finally {
				conflictCommitted.countDown();
				conflictThread.join(5_000);
			}

			assertEquals(2, attempts.get(), "conflicting commit must trigger a genuine transaction redo");
			assertEquals("", decodedResult.staleOnly, "encoded result must not retain fields from rolled-back attempt 1");
			assertEquals(List.of("attempt-2"), decodedResult.items,
					"encoded result list must not duplicate elements from rolled-back attempt 1");
			var finalResult = assertInstanceOf(ResultBean.class, proc.getResult());
			assertEquals("", finalResult.staleOnly);
			assertEquals(List.of("attempt-2"), finalResult.items);
		}
	}

	@Test
	void sagaRedoDiscardsFirstAttemptResult() throws Exception {
		try (var env = new Fixture()) {
			long key = 202L;
			assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				env.table1.getOrAdd(key).setInt_1(0);
				return Procedure.Success;
			}, "seedSagaKey").call());

			var attempt1Read = new CountDownLatch(1);
			var conflictCommitted = new CountDownLatch(1);
			var attempts = new AtomicInteger();
			var decodedResult = new ResultBean();
			var replyCount = new AtomicInteger();

			var req = new FuncSaga() {
				@Override
				public void SendResult(@Nullable Binary result) {
					decodedResult.decode(ByteBuffer.Wrap(this.Result.getFuncResult()));
					replyCount.incrementAndGet();
				}
			};
			req.Argument.setOnzTid(2L);
			req.Argument.setFlushMode(Onz.eFlushAsync);

			var stub = new OnzSagaStub<EmptyBean, ResultBean, EmptyBean>(
					env.app.getOnz(), "RedoSagaResult",
					(saga, arg, res) -> {
						int round = attempts.incrementAndGet();
						int v = env.table1.getOrAdd(key).getInt_1();
						if (v == 0) {
							res.staleOnly = "dirty-saga-attempt-1";
							res.items.add("attempt-" + round);
							env.table1.getOrAdd(key).setInt_1(10);
							attempt1Read.countDown();
							assertTrue(conflictCommitted.await(5, TimeUnit.SECONDS));
						} else {
							res.items.add("attempt-" + round);
							env.table1.getOrAdd(key).setInt_1(20);
						}
						return Procedure.Success;
					}, EmptyBean.class, ResultBean.class,
					(saga, cancelArg) -> Procedure.Success, EmptyBean.class);

			var argBuf = ByteBuffer.Allocate();
			new EmptyBean().encode(argBuf);
			var saga = stub.newProcedure(req, req.Argument, argBuf);

			var conflictError = new AtomicReference<@Nullable Throwable>();
			var conflictThread = new Thread(() -> {
				try {
					assertTrue(attempt1Read.await(5, TimeUnit.SECONDS));
					assertEquals(Procedure.Success, env.app.newProcedure(() -> {
						env.table1.getOrAdd(key).setInt_1(1);
						return Procedure.Success;
					}, "conflictSagaKey").call());
				} catch (Throwable e) {
					conflictError.set(e);
				} finally {
					conflictCommitted.countDown();
				}
			}, "onz-saga-redo-conflict");
			conflictThread.start();
			try {
				assertEquals(Procedure.Success, env.app.newProcedure(saga, "RedoSagaResult").call());
				conflictThread.join(5_000);
				assertFalse(conflictThread.isAlive());
				assertNull(conflictError.get());
			} finally {
				conflictCommitted.countDown();
				conflictThread.join(5_000);
			}

			assertEquals(2, attempts.get(), "conflicting commit must trigger a genuine transaction redo");
			assertEquals(1, replyCount.get());
			assertEquals("", decodedResult.staleOnly, "encoded saga result must not retain fields from rolled-back attempt 1");
			assertEquals(List.of("attempt-2"), decodedResult.items,
					"encoded saga result list must not duplicate elements from rolled-back attempt 1");
			var finalResult = assertInstanceOf(ResultBean.class, saga.getResult());
			assertEquals("", finalResult.staleOnly);
			assertEquals(List.of("attempt-2"), finalResult.items);
		}
	}

	private static final class Fixture implements AutoCloseable {
		final Application app;
		final Table1 table1;

		Fixture() throws Exception {
			int serverId = FastServerIds.takeoverPoolNext();
			var conf = new Config();
			conf.setServiceManager("disable");
			conf.setTakeoverMode("off");
			conf.setServerId(serverId);
			conf.setDefaultTableConf(new Config.TableConf());
			var dbConf = new Config.DatabaseConf();
			dbConf.setDatabaseType(Config.DbType.Memory);
			dbConf.setDatabaseUrl("onz_result_redo_" + serverId);
			conf.getDatabaseConfMap().put("", dbConf);

			app = new Application("OnzResultRedo" + serverId, conf);
			app.setSchemas(new demo.Schemas());
			app.addTable(requireNonNull(conf.getTableConf("demo_Module1_Table1")).getDatabaseName(), new Table1());
			app.start();
			table1 = (Table1)requireNonNull(app.getTable("demo_Module1_Table1"));
		}

		@Override
		public void close() throws Exception {
			app.stop();
		}
	}
}
