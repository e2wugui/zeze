package Benchmark;

import java.util.concurrent.atomic.AtomicLong;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import demo.Module1.BValue;
import org.junit.Test;

public class BenchSocket {
	class ServerService extends Service {
		public ServerService(String name) {
			super(name);
		}

		public AtomicLong sum = new AtomicLong();

		@Override
		public void OnSocketProcessInputBuffer(AsyncSocket so, ByteBuffer input) throws Throwable {
			sum.addAndGet(input.size());
			input.ReadIndex = input.WriteIndex; // discard all
		}
	}

	class ClientService extends Service {

		public ClientService(String name) {
			super(name);
		}
	}

	class BenchProtocol extends Protocol<BValue> {
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

	@Test
	public void testBench() throws Throwable {
		// create server
		var server = new ServerService("benchServer");
		server.getConfig().addAcceptor(new Acceptor(9797, "127.0.0.1"));
		server.Start();

		// create client and connect
		var client = new ClientService("benchClient");
		var connector = new Connector("127.0.0.1", 9797);
		client.getConfig().addConnector(connector);
		client.Start();
		connector.WaitReady();
		var socket = connector.getSocket();

		// bench
		for (int i = 0; i < 1000; ++i) {
			var benchProtocol = new BenchProtocol().encode();
			socket.Send(benchProtocol);
		}
	}
}
