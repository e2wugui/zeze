package Benchmark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import Zeze.Builtin.Provider.BSendResult;
import Zeze.Builtin.Provider.Send;
import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.BufferCodec;
import Zeze.Net.Codec;
import Zeze.Net.Compress;
import Zeze.Net.Connector;
import Zeze.Net.Encrypt;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Record;
import Zeze.Util.LongList;
import demo.Module1.BValue;
import org.junit.Test;

@SuppressWarnings({"unused", "NewClassNamingConvention"})
public class BenchSocket {
	static class ServerService extends Zeze.Services.HandshakeServer {
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

	static class ClientService extends Zeze.Services.HandshakeClient {

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
	public void testOutputBufferCodec() throws IOException {
		var selector = new Zeze.Net.Selector("dummySelector");
		var count = 200_0000;
		var key = new byte[16];
		Zeze.Util.Random.getInstance().nextBytes(key);

		var dataPieceLength = 100;
		// 预先分配足够的内存池，避免后面性能测试时，两次由于这个产生波动。
		var bufCount = (dataPieceLength * count) / selector.getBufferSize() + 1;
		for (int i = 0; i < bufCount; ++i)
			selector.free(java.nio.ByteBuffer.allocateDirect(selector.getBufferSize()));
		var datas = new byte[count][];
		for (var i = 0; i < count; ++i) {
			datas[i] = new byte[dataPieceLength];
			Zeze.Util.Random.getInstance().nextBytes(datas[i]);
		}
		// test
		{
			var out = new Zeze.Net.OutputBuffer(selector);
			var outCopy = new BufferCodec();
			Codec chain = outCopy;
			chain = new Encrypt(chain, key);
			chain = new Compress(chain);
			var b = new Zeze.Util.Benchmark();
			for (int i = 0; i < count; ++i) {
				chain.update(datas[i], 0, datas[i].length);
				chain.flush();
				var codecBuf = outCopy.getBuffer();
				out.put(codecBuf.Bytes, codecBuf.ReadIndex, codecBuf.size());
				codecBuf.FreeInternalBuffer();
			}
			var seconds = b.report("encrypt copy to OutputBuffer", count);
			System.out.println("speed=" + dataPieceLength * count / seconds / 1024 / 1024);
			out.close();
		}
		// test
		{
			var out = new Zeze.Net.OutputBuffer(selector);
			Codec chain = out;
			chain = new Encrypt(chain, key);
			chain = new Compress(chain);
			var b = new Zeze.Util.Benchmark();
			for (int i = 0; i < count; ++i) {
				chain.update(datas[i], 0, datas[i].length);
				chain.flush();
			}
			var seconds = b.report("encrypt direct to OutputBuffer", count);
			System.out.println("speed=" + dataPieceLength * count / seconds / 1024 / 1024);
			out.close();
		}
		selector.close();
	}

	@Test
	public void testSerialize() {
		{
			System.out.println("PMap");
			var bValue = new BValue();
			for (long i = 0; i < 100; ++i) {
				bValue.getMap15().put(i, i);
			}
			testSerialize(bValue);
		}
		{
			System.out.println("PList");
			var bValue = new BValue();
			for (long i = 0; i < 100; ++i) {
				bValue.getArray29().add((float)i);
			}
			testSerialize(bValue);
		}

		{
			System.out.println("PSet");
			var bValue = new BValue();
			for (int i = 0; i < 100; ++i) {
				bValue.getSet10().add(i);
			}
			testSerialize(bValue);
		}
	}

	public static void testSerialize(BValue bValue) {
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
			dummy += value.getArray29().size() + value.getMap15().size() + value.getSet10().size();
		}
		seconds = b2.report("decode", 20_0000);
		System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s");
		System.out.println("dummy=" + dummy);
	}

	public static class FastBSend extends Bean {
		private final LongList linkSids = new LongList();
		private long protocolType;
		private Binary protocolWholeData; // 完整的协议打包，包括了 type, size

		public LongList getLinkSids() {
			return linkSids;
		}

		public long getProtocolType() {
			return protocolType;
		}

		public void setProtocolType(long protocolType) {
			this.protocolType = protocolType;
		}

		public Binary getProtocolWholeData() {
			return protocolWholeData;
		}

		public void setProtocolWholeData(Binary protocolWholeData) {
			this.protocolWholeData = protocolWholeData;
		}

		@Override
		public void encode(ByteBuffer bb) {
			int i = 0;
			{
				var x = linkSids;
				int n = x.size();
				if (n != 0) {
					i = bb.WriteTag(i, 1, ByteBuffer.LIST);
					bb.WriteListType(n, ByteBuffer.INTEGER);
					for (int j = 0; j < n; j++)
						bb.WriteLong(x.get(j));
				}
			}
			{
				long x = protocolType;
				if (x != 0) {
					i = bb.WriteTag(i, 2, ByteBuffer.INTEGER);
					bb.WriteLong(x);
				}
			}
			{
				var x = protocolWholeData;
				if (x.size() != 0) {
					bb.WriteTag(i, 3, ByteBuffer.BYTES);
					bb.WriteBinary(x);
				}
			}
			bb.WriteByte(0);
		}

		@Override
		public void decode(ByteBuffer bb) {
			int t = bb.ReadByte();
			int i = bb.ReadTagSize(t);
			if (i == 1) {
				var x = linkSids;
				x.clear();
				if ((t & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
					int n = bb.ReadTagSize(t = bb.ReadByte());
					if (x.capacity() < n)
						x.wraps(new long[Math.min(n, 0x10000)]);
					for (; n > 0; n--)
						x.add(bb.ReadLong(t));
				} else
					bb.SkipUnknownFieldOrThrow(t, "Collection");
				i += bb.ReadTagSize(t = bb.ReadByte());
			}
			if (i == 2) {
				protocolType = bb.ReadLong(t);
				i += bb.ReadTagSize(t = bb.ReadByte());
			}
			if (i == 3) {
				protocolWholeData = bb.ReadBinary(t);
				bb.ReadTagSize(t = bb.ReadByte());
			}
			while (t != 0) {
				bb.SkipUnknownField(t);
				bb.ReadTagSize(t = bb.ReadByte());
			}
		}

		@Override
		protected void resetChildrenRootInfo() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
			throw new UnsupportedOperationException();
		}
	}

	public static class FastSend extends Rpc<FastBSend, BSendResult> {
		public static final int ModuleId_ = Send.ModuleId_;
		public static final int ProtocolId_ = Send.ProtocolId_;
		public static final long TypeId_ = Send.TypeId_;

		@Override
		public int getModuleId() {
			return ModuleId_;
		}

		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}

		public FastSend() {
			this(new FastBSend());
		}

		public FastSend(FastBSend arg) {
			Argument = arg;
			Result = new Zeze.Builtin.Provider.BSendResult();
		}
	}

	@Test
	public void testSerializeSend() {
		for (int i = 0; i < 2; i++) {
			var bb0 = benchSendEncode();
			var bb1 = benchFastSendEncode();
			benchSendDecode(bb1);
			benchFastSendDecode(bb0);
		}
	}

	private static ByteBuffer benchSendEncode() {
		var rand = ThreadLocalRandom.current();
		var pdata = new byte[100];
		var bb = ByteBuffer.Allocate(1000);
		var b = new Zeze.Util.Benchmark();
		var count = 1_000_000;
		for (int i = 0; i < count; i++) {
			var p = new Send();
			for (int j = 0, n = rand.nextInt(1, 100); j < n; j++)
				p.Argument.getLinkSids().add((long)rand.nextInt());
			p.Argument.setProtocolType(rand.nextLong());
			p.Argument.setProtocolWholeData(new Binary(pdata, 0, rand.nextInt(100)));
			bb.Reset();
			p.encode(bb);
		}
		b.report("benchSendEncode    ", count);
		return bb;
	}

	private static void benchSendDecode(ByteBuffer bb) {
		var b = new Zeze.Util.Benchmark();
		var count = 1_000_000;
		for (int i = 0; i < count; i++) {
			var p = new Send();
			bb.ReadIndex = 0;
			p.decode(bb);
		}
		b.report("benchSendDecode    ", count);
	}

	private static ByteBuffer benchFastSendEncode() {
		var rand = ThreadLocalRandom.current();
		var pdata = new byte[100];
		var bb = ByteBuffer.Allocate(1000);
		var b = new Zeze.Util.Benchmark();
		var count = 1_000_000;
		for (int i = 0; i < count; i++) {
			var p = new FastSend();
			for (int j = 0, n = rand.nextInt(1, 100); j < n; j++)
				p.Argument.getLinkSids().add(rand.nextInt());
			p.Argument.setProtocolType(rand.nextLong());
			p.Argument.setProtocolWholeData(new Binary(pdata, 0, rand.nextInt(100)));
			bb.Reset();
			p.encode(bb);
		}
		b.report("benchFastSendEncode", count);
		return bb;
	}

	private static void benchFastSendDecode(ByteBuffer bb) {
		var b = new Zeze.Util.Benchmark();
		var count = 1_000_000;
		for (int i = 0; i < count; i++) {
			var p = new FastSend();
			bb.ReadIndex = 0;
			p.decode(bb);
		}
		b.report("benchFastSendDecode", count);
	}

	@Test
	public void testBench() throws Throwable {
		testBench(false);
		System.out.println("Encrypt");
		testBench(true);
	}

	public static void testBench(boolean encrypt) throws Throwable {
		// create server
		var serverConfig = new Config();
		var server = new ServerService("benchServer", serverConfig);
		server.getConfig().addAcceptor(new Acceptor(9797, "127.0.0.1"));
		server.getConfig().getHandshakeOptions().setEnableEncrypt(encrypt);

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
