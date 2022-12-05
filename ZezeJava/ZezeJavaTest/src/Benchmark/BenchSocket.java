package Benchmark;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import demo.Bean1;
import demo.Module1.BValue;
import org.junit.Test;

public class BenchSocket {
	static class ServerService extends Service {
		public ServerService(String name, Config config) throws Throwable {
			super(name, config);
			//AddFactoryHandle(new BenchProtocol().getTypeId(), new ProtocolFactoryHandle<>(BenchProtocol::new, this::ProcessBenchProtocol));
			AddFactoryHandle(new BenchEnd().getTypeId(), new ProtocolFactoryHandle<>(BenchEnd::new, this::ProcessBenchEnd));
		}

		public long ProcessBenchProtocol(BenchProtocol p) {
			return 0;
		}

		public long ProcessBenchEnd(BenchEnd r) {
			r.SendResult();
			return 0;
		}

		@Override
		public void dispatchUnknownProtocol(AsyncSocket so, int moduleId, int protocolId, ByteBuffer data) throws Throwable {
			if (moduleId == 0 && protocolId == BenchProtocol.ProtocolId_)
				return; // 忽略压测协议，不进行decode，增加流量。

			super.dispatchUnknownProtocol(so, moduleId, protocolId, data);
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

		public BenchProtocol(BValue argument) {
			Argument = argument;
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
	public void testSerialize() {
		var bValue = new BValue();
		for (long i = 0; i < 100; ++i) {
			bValue.getMap15().put(i, i);
			bValue.getArray29().add((float)i);
		}
		long sum = 0;
		var b = new Zeze.Util.Benchmark();
		for (var i = 0; i < 100_0000; ++i) {
			var bb = ByteBuffer.Allocate();
			bValue.encode(bb);
			sum += bb.size();
		}
		var seconds = b.report("encode", 100_0000);
		System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s");

		// decode
		var b2 = new Zeze.Util.Benchmark();
		var encoded = ByteBuffer.Allocate();
		bValue.encode(encoded);
		var dummy = 0;
		for (var i = 0; i < 20_0000; ++i) {
			var bb = ByteBuffer.Wrap(encoded.Bytes, encoded.ReadIndex, encoded.size());
			var value = new BValue();
			value.decode(bb);
			dummy += value.getArray29().size() + value.getMap15().size();
		}
		seconds = b2.report("decode", 20_0000);
		System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s");
		System.out.println("dummy=" + dummy);
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
		int maxOutputBufferSize = 200 * 1024 * 1024;
		client.getSocketOptions().setOutputBufferMaxSize(maxOutputBufferSize);

		Zeze.Util.Task.tryInitThreadPool(null, null, null);

		// 生成一些随机大小的协议参数Bean。
		var bValues = new ArrayList<BValue>();
		for (var i = 0; i < 10; ++i) {
			bValues.add(new BValue());
			var bValue = bValues.get(0);
			var bytes = new byte[Zeze.Util.Random.getInstance().nextInt(200) + 50];
			Zeze.Util.Random.getInstance().nextBytes(bytes);
			bValue.setBytes8(new Binary(bytes));
		}

		server.Start();
		client.Start();
		try {
			connector.WaitReady();
			var socket = connector.getSocket();

			// bench
			var b = new Zeze.Util.Benchmark();
			long sum = 0;
			long count = 0;
			while (sum < maxOutputBufferSize) {
				var randIndex = Zeze.Util.Random.getInstance().nextInt(bValues.size());
				var randBValue = bValues.get(randIndex);
				// 预先完成 encode 效率会高些，但不符合实际情况。
				var benchProtocol = new BenchProtocol(randBValue).encode();
				sum += benchProtocol.size();
				++count;
				socket.Send(benchProtocol);
			}
			var benchEnd = new BenchEnd();
			benchEnd.SendForWait(socket, Integer.MAX_VALUE).await();

			var seconds = b.report("BenchSocket", count);
			System.out.println("sum=" + sum + " bytes, speed=" + sum / seconds / 1024 / 1024 + "M");
		} finally {
			client.Stop();
			server.Stop();
		}
	}
}
