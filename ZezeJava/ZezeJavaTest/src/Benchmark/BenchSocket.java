package Benchmark;

import java.util.concurrent.atomic.AtomicLong;
import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import demo.Module1.BValue;
import org.junit.Test;

public class BenchSocket {
	static class ServerService extends Service {
		public ServerService(String name, Config config) throws Throwable {
			super(name, config);
			AddFactoryHandle(new BenchProtocol().getTypeId(), new ProtocolFactoryHandle<>(BenchProtocol::new, this::ProcessBenchProtocol));
			AddFactoryHandle(new BenchEnd().getTypeId(), new ProtocolFactoryHandle<>(BenchEnd::new, this::ProcessBenchEnd));
		}

		public long ProcessBenchProtocol(BenchProtocol p) {
			return 0;
		}

		public long ProcessBenchEnd(BenchEnd r) {
			r.SendResult();
			return 0;
		}
	}

	static class ClientService extends Service {

		public ClientService(String name, Config config) throws Throwable {
			super(name, config);
			AddFactoryHandle(new BenchEnd().getTypeId(), new ProtocolFactoryHandle<>(BenchEnd::new));
		}
	}

	static class BenchProtocol extends Protocol<BValue> {
		public static final int ProtocolId_ = Bean.hash32(BenchProtocol.class.getName());

		@Override
		public int getModuleId() {
			return 0;
		}

		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}

		public BenchProtocol() {
			Argument = new BValue();
		}
	}

	static class BenchEnd extends Rpc<EmptyBean, EmptyBean> {
		public static final int ProtocolId_ = Bean.hash32(BenchEnd.class.getName());

		@Override
		public int getModuleId() {
			return 0;
		}

		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}

		public BenchEnd() {
			Argument = new EmptyBean();
			Result = new EmptyBean();
		}
	}

	@Test
	public void testBench() throws Throwable {
		// create server
		var serverConfig = new Config();
		var server = new ServerService("benchServer", serverConfig);
		server.getConfig().addAcceptor(new Acceptor(9797, "127.0.0.1"));

		// create client and connector
		var clientConfig = new Config();
		var client = new ClientService("benchClient", clientConfig);
		var connector = new Connector("127.0.0.1", 9797);
		client.getConfig().addConnector(connector);

		Zeze.Util.Task.tryInitThreadPool(null, null, null);
		server.Start();
		client.Start();
		try {

			connector.WaitReady();
			var socket = connector.getSocket();

			// bench
			var b = new Zeze.Util.Benchmark();
			long sum = 0;
			for (int i = 0; i < 500000; ++i) {
				var benchProtocol = new BenchProtocol().encode();
				sum += benchProtocol.size();
				socket.Send(benchProtocol);
			}
			var benchEnd = new BenchEnd();
			benchEnd.SendForWait(socket).await();
			b.report("BenchSocket", 1);
			System.out.println("sum=" + sum);
			/*
			50_0000个BenchProtocol一次性提交统计。
			BenchSocket tasks/s=0.14 time=7.03s cpu=25.53s concurrent=3.63
			sum=9500000
		 	*/
		} finally {
			client.Stop();
			server.Stop();
		}
	}
}
