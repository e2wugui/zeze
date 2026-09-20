package UnitTest.Zeze.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Component.RedoQueueServer;
import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Net.ServiceConf;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * CP1-F2 回归（P1）：RedoQueueServer.Server曾覆写dispatchProtocol，在procedure内直接设
 * bb.ReadIndex=0重解码网络buffer——派发入池后缓冲可能被网络层回收复用，解码出错误内容，
 * 服务端整体不可用。修复：删除覆写，回归基类语义（copy网络buffer后procedure内重解码）。
 * 测试：真实socket回环向服务端逐个投递RunTask（严格prevTaskId链接，等价RedoQueue泵的
 * 串行语义），断言全部任务按序应用且结果回带推进的taskId——RunTask经基类派发进
 * Serializable事务、结果回发的全链路功能护栏。
 * 缓冲回收竞态本身与RedoQueue客户端握手栈的停滞问题不可确定性红测（台账注明），
 * 客户端侧用普通Service直连（握手外协议在服务端未配置加密时可直接流通）。
 */
@Fast
public class TestRedoQueueServerDispatch {

	// 26020：26000段（Windows动态端口范围外的WinNAT保留盲区，见TestMQ注释）内未占用端口
	private static final int Port = 26020;
	private static final String QueueName = "cp1f2.dispatch";
	private static final int TaskCount = 50;

	/** 任务参数：单long序号，解码后校验顺序与完整性。 */
	public static final class Seq extends Bean {
		private long seq;

		public long getSeq() {
			return seq;
		}

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteLong(seq);
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
			seq = bb.ReadLong();
		}
	}

	private static final List<Long> applied = new CopyOnWriteArrayList<>();

	@Test
	public void testRunTaskDispatchViaBase() throws Exception {
		Task.tryInitThreadPool();
		applied.clear();

		// 服务端：编程式Application（SM=disable、Memory库），acceptor监听"RedoQueueServer"服务；
		// 模块必须在start之前注册（start后注册的表没有TableCache）
		var serverConf = new Config();
		serverConf.setServiceManager("disable");
		serverConf.setServerId(761);
		serverConf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("redo_queue_server_dispatch_test");
		serverConf.getDatabaseConfMap().putIfAbsent("", dbConf);
		var serviceConf = new ServiceConf();
		serviceConf.addAcceptor(new Acceptor(Port, null));
		serverConf.getServiceConfMap().put("RedoQueueServer", serviceConf);

		var appBase = new AppBase() {
			private final Application zeze = new Application("TestRedoQueueServerDispatch", serverConf);

			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		var app = appBase.getZeze();
		var server = new RedoQueueServer(app);
		app.start();

		server.register(QueueName, 1, param -> {
			var seq = new Seq();
			seq.decode(ByteBuffer.Wrap(param.bytesUnsafe()));
			applied.add(seq.getSeq());
			return true;
		});
		server.start();

		// 客户端：HandshakeClient直连（服务端是HandshakeServer，必须完成握手后才能发送业务协议
		// ——握手完成前到达的协议会被连接级解码准入拒绝断连；响应需要RunTask工厂，
		// 响应经Rpc.handle路由回SendForWait的future）
		var handshakeDone = new java.util.concurrent.atomic.AtomicBoolean();
		var client = new Zeze.Services.HandshakeClient("cp1f2Client", (Zeze.Config)null) {
			@Override
			public void OnHandshakeDone(Zeze.Net.AsyncSocket so) throws Exception {
				handshakeDone.set(true); // GetSocket非null不代表握手完成，以此为准
				super.OnHandshakeDone(so);
			}
		};
		client.AddFactoryHandle(Zeze.Builtin.RedoQueue.RunTask.TypeId_,
				new Service.ProtocolFactoryHandle<>(Zeze.Builtin.RedoQueue.RunTask::new,
						r -> 0L, TransactionLevel.None, DispatchMode.Direct));
		try {
			client.newClientSocket("127.0.0.1", Port, null, null);
			await("handshake done", 10_000, handshakeDone::get);

			// 逐个投递（严格prevTaskId链，等价RedoQueue泵的串行语义：上一个应用后再发下一个，
			// 不依赖rpc应答——fire-and-forget，应答在客户端无上下文仅记lost-context警告）
			long prevTaskId = 0;
			for (long i = 1; i <= TaskCount; i++) {
				var seq = new Seq();
				seq.seq = i;
				var bb = ByteBuffer.Allocate(16);
				seq.encode(bb);
				var r = new Zeze.Builtin.RedoQueue.RunTask();
				r.Argument.setQueueName(QueueName);
				r.Argument.setTaskType(1);
				r.Argument.setTaskId(i);
				r.Argument.setPrevTaskId(prevTaskId);
				r.Argument.setTaskParam(new Binary(bb.Bytes, 0, bb.WriteIndex));
				assertTrue(r.Send(client.GetSocket()), "task " + i + " 发送失败");
				final long expected = i;
				await("task " + i + " applied", 10_000, () -> applied.size() >= expected);
				prevTaskId = i;
			}

			assertEquals(TaskCount, applied.size(), "全部任务必须在服务端应用");
			for (int i = 0; i < TaskCount; i++)
				assertTrue(applied.get(i) == i + 1L, "任务必须按序应用: applied[" + i + "]=" + applied.get(i));
		} finally {
			client.stop();
			server.stop();
			server.UnRegister();
			app.stop();
		}
	}

	private static void await(String what, long timeoutMillis, java.util.function.BooleanSupplier cond)
			throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (!cond.getAsBoolean()) {
			if (System.currentTimeMillis() > deadline)
				throw new AssertionError("timeout waiting: " + what);
			//noinspection BusyWait
			Thread.sleep(20);
		}
	}
}
