package Benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import Zeze.Builtin.Provider.Send;
import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.BufferCodec;
import Zeze.Net.Codec;
import Zeze.Net.Compress;
import Zeze.Net.Connector;
import Zeze.Net.Decompress;
import Zeze.Net.Decrypt;
import Zeze.Net.Decrypt2;
import Zeze.Net.Digest;
import Zeze.Net.Encrypt;
import Zeze.Net.Encrypt2;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.Handshake.Constant;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.OutInt;
import demo.Module1.BValue;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings({"unused", "NewClassNamingConvention"})
public class BenchSocket {
	@Test
	public void testCompressRate() {
		var rand = new java.util.Random(1); // 固定的随机种子。
		var count = 10 * 1024;
		for (var block = 2048; block <= 4096 + 1024; block += 1024) {
			var src = new byte[4][];
			for (int i = 0; i < src.length; ++i) {
				src[i] = new byte[block];
				rand.nextBytes(src[i]);
			}
			var b = new Zeze.Util.Benchmark();
			BufferCodec bufcp = new BufferCodec(count * block);
			Compress cp = new Compress(bufcp);
			var sum = 0L;
			for (int i = 0; i < count; ++i) {
				var index = rand.nextInt(src.length);
				var bb = src[index];
				cp.update(bb);
				cp.flush();
				sum += bb.length;
			}
			var seconds = b.report("compress " + block, count);
			var cpsize = bufcp.getBuffer().size();
			System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s " + (double)cpsize / sum);
		}
	}

	private static ArrayList<ByteBuffer> prepareDatas(int steps, OutInt max) {
		var src = new ArrayList<ByteBuffer>();
		var rand = new java.util.Random(2); // 固定随机种子
		for (int step = 0; step < steps; ++step) {
			{
				var bValue = new BValue();
				for (long i = 0; i < 100; ++i) {
					bValue.getMap15().put(i, rand.nextLong(20));
				}
				var bb = ByteBuffer.Allocate();
				bValue.encode(bb);
				src.add(bb);
				if (bb.size() > max.value)
					max.value = bb.size();
			}
			{
				var bValue = new BValue();
				for (long i = 0; i < 100; ++i) {
					bValue.getArray29().add((float)rand.nextInt(20));
				}
				var bb = ByteBuffer.Allocate();
				bValue.encode(bb);
				src.add(bb);
				if (bb.size() > max.value)
					max.value = bb.size();
			}
			{
				var bValue = new BValue();
				for (int i = 0; i < 100; ++i) {
					bValue.getSet10().add(rand.nextInt(20));
				}
				var bb = ByteBuffer.Allocate();
				bValue.encode(bb);
				src.add(bb);
				if (bb.size() > max.value)
					max.value = bb.size();
			}
		}
		return src;
	}

	@Test
	public void testCompressDecompress() {
		var count = 90_0000;
		var max = new OutInt(0);
		var src = prepareDatas(count / 3_0000, max);

		System.out.println("benchmark ...");
		BufferCodec bufcp = new BufferCodec(count * max.value);
		{
			var b = new Zeze.Util.Benchmark();
			Compress cp = new Compress(bufcp);
			var rand = new java.util.Random(1); // 固定的随机种子。
			var sum = 0L;
			for (int i = 0; i < count; ++i) {
				var index = rand.nextInt(src.size());
				var bb = src.get(index);
				cp.update(bb.Bytes, bb.ReadIndex, bb.size());
				cp.flush();
				sum += bb.size();
			}
			var seconds = b.report("compress", count);
			var cpsize = bufcp.getBuffer().size();
			System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s " + (double)cpsize / sum);
		}
		{
			var b = new Zeze.Util.Benchmark();
			BufferCodec bufdp = new BufferCodec(count * max.value);
			Decompress dp = new Decompress(bufdp);
			dp.update(bufcp.getBuffer().Bytes, bufcp.getBuffer().ReadIndex, bufcp.getBuffer().size());
			dp.flush();
			var seconds = b.report("decompress", count);
			var sum = bufdp.getBuffer().size();
			System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s");
		}
	}

	@Test
	public void testEncryptDecrypt() {
		var max = new OutInt(0);
		var src = prepareDatas(1, max);
		var count = 100_0000;

		BufferCodec encrypt = new BufferCodec(count * max.value);
		{
			var b = new Zeze.Util.Benchmark();
			byte[] key = {1, 2, 3, 4, 5};
			Encrypt en = new Encrypt(encrypt, key);
			var rand = new java.util.Random(1); // 固定的随机种子。
			for (int i = 0; i < count; ++i) {
				var index = rand.nextInt(src.size());
				var bb = src.get(index);
				en.update(bb.Bytes, bb.ReadIndex, bb.size());
				en.flush();
			}
			var seconds = b.report("encrypt", count);
			var sum = encrypt.getBuffer().size();
			System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s");
		}
		{
			BufferCodec decrypt = new BufferCodec(count * max.value);
			var b = new Zeze.Util.Benchmark();
			byte[] key = {1, 2, 3, 4, 5};
			var de = new Decrypt(decrypt, key);
			de.update(encrypt.getBuffer().Bytes, encrypt.getBuffer().ReadIndex, encrypt.getBuffer().size());
			de.flush();
			var seconds = b.report("decrypt", count);
			var sum = decrypt.getBuffer().size();
			System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s");
		}
	}

	@Test
	public void testEncryptDecrypt2() {
		var max = new OutInt(0);
		var src = prepareDatas(1, max);
		var count = 20_0000;
		var rand = new Random();
		byte[] key = {1, 2, 3, 4, 5};

		var encrypt = new BufferCodec(count * max.value);
		var decrypt = new BufferCodec(count * max.value);
		int enHashCode, deHashCode;
		{
			var b = new Zeze.Util.Benchmark();
			var en = new Encrypt(encrypt, key);
			rand.setSeed(1); // 固定的随机种子。
			for (int i = 0; i < count; ++i) {
				var index = rand.nextInt(src.size());
				var bb = src.get(index);
				en.update(bb.Bytes, bb.ReadIndex, bb.size());
				en.flush();
			}
			var seconds = b.report("encrypt", count);
			var sum = encrypt.getBuffer().size();
			enHashCode = encrypt.getBuffer().hashCode();
			System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s; hash=" + enHashCode);
		}
		{
			var b = new Zeze.Util.Benchmark();
			var de = new Decrypt(decrypt, key);
			de.update(encrypt.getBuffer().Bytes, encrypt.getBuffer().ReadIndex, encrypt.getBuffer().size());
			de.flush();
			var seconds = b.report("decrypt", count);
			var sum = decrypt.getBuffer().size();
			deHashCode = decrypt.getBuffer().hashCode();
			System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s; hash=" + deHashCode);
		}

		encrypt = new BufferCodec(count * max.value);
		decrypt = new BufferCodec(count * max.value);
		int en2HashCode, de2HashCode;
		{
			var b = new Zeze.Util.Benchmark();
			var keyMd5 = Digest.md5(key);
			var en = new Encrypt2(encrypt, keyMd5, keyMd5);
			rand.setSeed(1); // 固定的随机种子。
			for (int i = 0; i < count; ++i) {
				var index = rand.nextInt(src.size());
				var bb = src.get(index);
				en.update(bb.Bytes, bb.ReadIndex, bb.size());
				en.flush();
			}
			var seconds = b.report("encrypt2", count);
			var sum = encrypt.getBuffer().size();
			en2HashCode = encrypt.getBuffer().hashCode();
			System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s; hash=" + en2HashCode);
		}
		{
			var b = new Zeze.Util.Benchmark();
			var keyMd5 = Digest.md5(key);
			var de = new Decrypt2(decrypt, keyMd5, keyMd5);
			de.update(encrypt.getBuffer().Bytes, encrypt.getBuffer().ReadIndex, encrypt.getBuffer().size());
			de.flush();
			var seconds = b.report("decrypt2", count);
			var sum = decrypt.getBuffer().size();
			de2HashCode = decrypt.getBuffer().hashCode();
			System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s; hash=" + de2HashCode);
		}

		Assert.assertEquals(enHashCode, en2HashCode);
		Assert.assertEquals(deHashCode, de2HashCode);
	}

	static class ServerService extends Zeze.Services.HandshakeServer {
		public ServerService(String name, Config config) {
			super(name, config);
			//AddFactoryHandle(new BenchProtocol().getTypeId(), new ProtocolFactoryHandle<>(BenchProtocol::new, this::ProcessBenchProtocol));
			AddFactoryHandle(new BenchEnd().getTypeId(), new ProtocolFactoryHandle<>(BenchEnd::new, this::ProcessBenchEnd));
		}

		@SuppressWarnings("MethodMayBeStatic")
		public long ProcessBenchProtocol(BenchProtocol p) {
			return 0;
		}

		@SuppressWarnings("MethodMayBeStatic")
		public long ProcessBenchEnd(BenchEnd r) {
			r.SendResult();
			return 0;
		}

		@Override
		public void dispatchUnknownProtocol(AsyncSocket so, int moduleId, int protocolId, ByteBuffer data) throws Exception {
			if (moduleId == 0 && protocolId == BenchProtocol.ProtocolId_)
				return; // 忽略压测协议，不进行decode，增加流量。

			super.dispatchUnknownProtocol(so, moduleId, protocolId, data);
		}
	}

	static class ClientService extends Zeze.Services.HandshakeClient {

		public ClientService(String name, Config config) {
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
	public void testOutputBufferCodec() {
		var selectors = new Zeze.Net.Selectors("dummySelector");
		var selector = selectors.choice();
		var count = 20_0000;
		var key = new byte[16];
		Zeze.Util.Random.getInstance().nextBytes(key);

		var dataPieceLength = 100;
		// 预先分配足够的内存池，避免后面性能测试时，两次由于这个产生波动。
		var bufCount = (dataPieceLength * count) / selectors.getBbPoolBlockSize() + 1;
		for (int i = 0; i < bufCount; ++i)
			selector.free(java.nio.ByteBuffer.allocateDirect(selectors.getBbPoolBlockSize()));
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
		var count = 5_0000;
		for (var i = 0; i < count; ++i) {
			var bb = ByteBuffer.Allocate();
			bValue.encode(bb);
			sum += bb.size();
		}
		var seconds = b.report("encode", count);
		System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s");

		// decode
		var b2 = new Zeze.Util.Benchmark();
		var encoded = ByteBuffer.Allocate();
		bValue.encode(encoded);
		var dummy = 0;
		for (var i = 0; i < count; ++i) {
			var bb = ByteBuffer.Wrap(encoded.Bytes, encoded.ReadIndex, encoded.size());
			var value = new BValue();
			value.decode(bb);
			dummy += value.getArray29().size() + value.getMap15().size() + value.getSet10().size();
		}
		seconds = b2.report("decode", count);
		System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s");
		System.out.println("dummy=" + dummy);
	}

	public static class SlowSend extends Rpc<Zeze.Builtin.Provider.BSend, Zeze.Builtin.Provider.BSendResult> {
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

		public SlowSend() {
			this(new Zeze.Builtin.Provider.BSend());
		}

		public SlowSend(Zeze.Builtin.Provider.BSend arg) {
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

	private static List<ByteBuffer> benchSendEncode() {
		var rand = ThreadLocalRandom.current();
		var pdata = new byte[100];
		var b = new Zeze.Util.Benchmark();
		var count = 100_000;
		var result = new ArrayList<ByteBuffer>(count);
		for (int i = 0; i < count; i++) {
			var p = new SlowSend();
			for (int j = 0, n = rand.nextInt(1, 100); j < n; j++)
				p.Argument.getLinkSids().add((long)rand.nextInt());
			p.Argument.setProtocolType(rand.nextLong());
			p.Argument.setProtocolWholeData(new Binary(pdata, rand.nextInt(100)));
			var bb = ByteBuffer.Allocate(1000);
			p.encode(bb);
			result.add(bb);
		}
		b.report("benchSendEncode    ", count);
		return result;
	}

	private static void benchSendDecode(List<ByteBuffer> bbs) {
		var b = new Zeze.Util.Benchmark();
		var sum = 0L;
		for (var bb : bbs) {
			var p = new SlowSend();
			bb.ReadIndex = 0;
			p.decode(bb);
			sum += p.Argument.getLinkSids().size() + p.Argument.getProtocolType();
		}
		b.report("benchSendDecode    ", bbs.size());
		System.out.println("sum=" + sum);
	}

	private static List<ByteBuffer> benchFastSendEncode() {
		var rand = ThreadLocalRandom.current();
		var pdata = new byte[100];
		var b = new Zeze.Util.Benchmark();
		var count = 100_000;
		var result = new ArrayList<ByteBuffer>(count);
		for (int i = 0; i < count; i++) {
			var p = new Send();
			for (int j = 0, n = rand.nextInt(1, 100); j < n; j++)
				p.Argument.getLinkSids().add(rand.nextInt());
			p.Argument.setProtocolType(rand.nextLong());
			p.Argument.setProtocolWholeData(new Binary(pdata, rand.nextInt(100)));
			var bb = ByteBuffer.Allocate(1000);
			p.encode(bb);
			result.add(bb);
		}
		b.report("benchFastSendEncode", count);
		return result;
	}

	private static void benchFastSendDecode(List<ByteBuffer> bbs) {
		var b = new Zeze.Util.Benchmark();
		var sum = 0L;
		for (var bb : bbs) {
			var p = new Send();
			bb.ReadIndex = 0;
			p.decode(bb);
			sum += p.Argument.getLinkSids().size() + p.Argument.getProtocolType();
		}
		b.report("benchFastSendDecode", bbs.size());
		System.out.println("sum=" + sum);
	}

	@Test
	public void testBench() throws Exception {
		testBench(false);
		System.out.println("Encrypt");
		testBench(true);
	}

	public static void testBench(boolean encrypt) throws Exception {
		// create server
		var serverConfig = new Config();
		var server = new ServerService("benchServer", serverConfig);
		server.getConfig().addAcceptor(new Acceptor(9797, "127.0.0.1"));
		server.getConfig().getHandshakeOptions().setEncryptType(encrypt ? Constant.eEncryptTypeAes : 0);

		// create client and connector
		var clientConfig = new Config();
		var client = new ClientService("benchClient", clientConfig);
		var connector = new Connector("127.0.0.1", 9797);
		client.getConfig().addConnector(connector);
		int maxOutputBufferSize = 100 * 1024 * 1024;
		client.getSocketOptions().setOutputBufferMaxSize(maxOutputBufferSize);
		maxOutputBufferSize -= 1024;

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

		server.start();
		client.start();
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
			client.stop();
			server.stop();
		}
	}
}
