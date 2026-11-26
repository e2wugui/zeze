package Zeze.Services;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AllocateId128;
import Zeze.Services.ServiceManager.Id128UdpClient;
import Zeze.Services.ServiceManager.Id128UdpServer;
import Zeze.Services.ServiceManager.Tid128Cache;
import Zeze.Util.Id128;
import Zeze.Util.Task;
import harness.DualStackPortAllocator;
import harness.Fast;

/**
 * FND2-S2-2：Id128UdpServer.process 入口校验对端可控的 count 与 name。
 * <p>
 * 修复前 count 无校验：count&lt;0 使号段回退（current倒退）→ 同name空间向两个客户端发放
 * 重复tid；count=0 同一startId发给两个客户端；巨量count每唯一name写盘巨量预留。
 * 唯一name无界则撑爆cache/RocksDB。
 * <p>
 * 用真实loopback Id128UdpServer（table=null，纯内存）按协议收发：
 * 非法报文被run()内层catch记日志丢弃整包（无应答，客户端soTimeout）；
 * 诚实报文照常工作，号段不因攻击报文回退；边界值（name恰好128字节、count==1）仍合法。
 * 伪造非法count的rpc直接走process——无需TCP/SM环境。
 */
@Fast
public class TestId128UdpServerValidation {
	private static final String NAME = "UnitTest.Id128UdpValidate";
	private static @Nullable Id128UdpServer server;
	private static @Nullable DatagramSocket client;
	private static @Nullable InetSocketAddress serverAddress;

	@BeforeAll
	public static void setUp() throws Exception {
		server = new Id128UdpServer(); // table=null：纯内存发号，自包含。
		server.start();
		client = new DatagramSocket();
		client.setSoTimeout(1500);
		serverAddress = new InetSocketAddress("127.0.0.1", server.getLocalPort());
	}

	@AfterAll
	public static void tearDown() throws Exception {
		requireNonNull(client).close();
		requireNonNull(server).stop();
	}

	/** 发一个请求并等待应答（每个报文恰好一个rpc，应答也恰好一个）。 */
	private static AllocateId128 alloc(String name, int count) throws Exception {
		var r = new AllocateId128();
		r.Argument.setName(name);
		r.Argument.setCount(count);
		var bb = ByteBuffer.Allocate();
		r.encode(bb);
		requireNonNull(client).send(new DatagramPacket(bb.Bytes, bb.ReadIndex, bb.size(), requireNonNull(serverAddress)));
		return receiveOne();
	}

	/** 发一个非法请求：应被入口校验拒绝（整包丢弃，无应答）。 */
	private static void allocExpectRejected(String name, int count) throws Exception {
		var r = new AllocateId128();
		r.Argument.setName(name);
		r.Argument.setCount(count);
		var bb = ByteBuffer.Allocate();
		r.encode(bb);
		requireNonNull(client).send(new DatagramPacket(bb.Bytes, bb.ReadIndex, bb.size(), requireNonNull(serverAddress)));
		Assertions.assertThrows(SocketTimeoutException.class, TestId128UdpServerValidation::receiveOne,
				"invalid request must be dropped without response: count=" + count);
	}

	private static AllocateId128 receiveOne() throws Exception {
		var buf = new byte[2048];
		var p = new DatagramPacket(buf, buf.length);
		requireNonNull(client).receive(p);
		var r = new AllocateId128();
		r.decode(ByteBuffer.Wrap(p.getData(), p.getOffset(), p.getLength()));
		return r;
	}

	@Test
	public void testBatchRepliesSplitAtRpcBoundaries() throws Exception {
		final int requestCount = 350;
		var requests = ByteBuffer.Allocate();
		var modeledReplies = ByteBuffer.Allocate();
		for (int i = 1; i <= requestCount; ++i) {
			var r = new AllocateId128();
			r.setSessionId(i);
			r.Argument.setName("b");
			r.Argument.setCount(1);
			r.encode(requests);
			// 相同name从0发号：用实际编码确认响应总量确实越过分包及旧发送数组的上限。
			r.setRequest(false);
			r.Result.getStartId().assign(0, i - 1);
			r.Result.setCount(1);
			r.encode(modeledReplies);
		}
		Assertions.assertTrue(requests.size() <= 2048, "请求批次应能放入旧协议接收容量");
		Assertions.assertTrue(modeledReplies.size() > 2048, "用例须触发应答扩张，不能依赖小值个数猜测字节数");
		requireNonNull(client).send(new DatagramPacket(requests.Bytes, requests.ReadIndex, requests.size(), requireNonNull(serverAddress)));
		var sessions = new java.util.HashSet<Long>();
		var starts = new java.util.HashSet<String>();
		int datagrams = 0;
		while (sessions.size() < requestCount) {
			var bytes = new byte[2048];
			var p = new DatagramPacket(bytes, bytes.length);
			client.receive(p);
			Assertions.assertTrue(p.getLength() <= 1472, "每个完整RPC分包须兼容旧客户端接收容量");
			++datagrams;
			var replies = ByteBuffer.Wrap(bytes, p.getOffset(), p.getLength());
			while (!replies.isEmpty()) {
				var r = new AllocateId128();
				r.decode(replies);
				Assertions.assertTrue(sessions.add(r.getSessionId()), "一个请求只能收到一次完整应答");
				Assertions.assertTrue(starts.add(r.Result.getStartId().toString()), "发出的号段不得重复");
				Assertions.assertEquals(1, r.Result.getCount());
			}
		}
		Assertions.assertTrue(datagrams >= 2, "应答应按完整RPC切成多个兼容旧客户端的报文");
	}

	@Test
	public void testIllegalCountRejectedAndNoRegression() throws Exception {
		var r1 = alloc(NAME, 100);
		Assertions.assertEquals(100, r1.Result.getCount());
		var start1 = r1.Result.getStartId();

		// count<0：修复前会使号段回退→下一个诚实客户端拿到与r1重叠的号段（重复tid）。
		allocExpectRejected(NAME, -100);
		allocExpectRejected(NAME, 0);
		// count巨大：每唯一name写盘巨量预留。
		allocExpectRejected(NAME, Tid128Cache.ALLOCATE_COUNT_MAX + 1);
		allocExpectRejected(NAME, Integer.MAX_VALUE);

		// 攻击报文全部被丢弃后，同name的下一次分配严格顺延r1的号段（无回退、无重复）。
		var r2 = alloc(NAME, 10);
		Assertions.assertEquals(10, r2.Result.getCount());
		Assertions.assertEquals(start1.add(100), r2.Result.getStartId());
	}

	@Test
	public void testNameLengthLimit() throws Exception {
		// 超长name：无界唯一name撑cache/RocksDB，拒绝。
		allocExpectRejected("n".repeat(129), 1);
		allocExpectRejected("x".repeat(256), 1);
		// 边界：恰好128字节仍合法（应答到达即通过，无soTimeout抛出）。
		var r = alloc("b".repeat(128), 1);
		Assertions.assertEquals(1, r.Result.getCount());
	}

	@Test
	public void testBadRpcDroppedGoodRpcInSamePacketAnswered() throws Exception {
		// 一个报文内[好,坏]：好的先处理并应答，坏的触发校验异常丢弃，worker存活。
		var good = new AllocateId128();
		good.Argument.setName(NAME);
		good.Argument.setCount(1);
		var bad = new AllocateId128();
		bad.Argument.setName(NAME);
		bad.Argument.setCount(0);
		var bb = ByteBuffer.Allocate();
		good.encode(bb);
		bad.encode(bb);
		requireNonNull(client).send(new DatagramPacket(bb.Bytes, bb.ReadIndex, bb.size(), requireNonNull(serverAddress)));

		var resp = receiveOne(); // 只有good的应答。
		Assertions.assertEquals(1, resp.Result.getCount());
		Assertions.assertThrows(SocketTimeoutException.class, TestId128UdpServerValidation::receiveOne,
				"bad rpc in the same packet must be dropped without response");

		// worker未被坏报文杀掉：后续正常分配照常工作。
		var r = alloc(NAME, 1);
		Assertions.assertEquals(1, r.Result.getCount());
	}

	@Test
	public void testResponsePacketRejectedWithoutReusingPreviousArgument() throws Exception {
		var name = "UnitTest.Id128ResponseDirection";
		var r1 = alloc(name, 50);
		Assertions.assertEquals(50, r1.Result.getCount());
		var start1 = r1.Result.getStartId();

		// 构造 FamilyClass.Response 报文发向服务端：修复前复用循环外 AllocateId128.Argument，
		// 不会改写 Argument 却推进前一个合法 name 的号段并回包。
		var forgedResponse = new AllocateId128();
		forgedResponse.setSessionId(999_001);
		forgedResponse.setRequest(false);
		forgedResponse.Result.getStartId().assign(0, 1);
		forgedResponse.Result.setCount(1);
		var bb = ByteBuffer.Allocate();
		forgedResponse.encode(bb);
		requireNonNull(client).send(new DatagramPacket(bb.Bytes, bb.ReadIndex, bb.size(), requireNonNull(serverAddress)));
		Assertions.assertThrows(SocketTimeoutException.class, TestId128UdpServerValidation::receiveOne,
				"Response packet sent to Id128UdpServer must be rejected without reply");

		// 紧随其后的合法请求必须严格顺延 r1，说明伪造 Response 未推进号段。
		var r2 = alloc(name, 10);
		Assertions.assertEquals(10, r2.Result.getCount());
		Assertions.assertEquals(start1.add(50), r2.Result.getStartId(),
				"forged Response packet must not advance allocator using stale Argument");
	}

	@Test
	public void testClientIgnoresRequestPacketAndZeroCountResponse() throws Exception {
		var sessionSeq = new AtomicLong(100);
		try (var fakeServer = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
			fakeServer.setSoTimeout(5_000);
			var udpClient = new Id128UdpClient(null, "127.0.0.1", fakeServer.getLocalPort(), sessionSeq::incrementAndGet);
			udpClient.start();
			try {
				var future = udpClient.allocateFuture("UnitTest.Id128ClientDirection", 16);

				var recvBuf = new byte[1472];
				var recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
				fakeServer.receive(recvPacket);
				var req = new AllocateId128();
				req.decode(ByteBuffer.Wrap(recvPacket.getData(), recvPacket.getOffset(), recvPacket.getLength()));
				Assertions.assertTrue(req.isRequest());

				// 1. 回发相同 sessionId 的 Request 报文：客户端必须忽略，不得产出 count=0 的畸形缓存
				var echoedReq = new AllocateId128();
				echoedReq.setSessionId(req.getSessionId());
				echoedReq.Argument.setName(req.Argument.getName());
				echoedReq.Argument.setCount(req.Argument.getCount());
				var bbReq = ByteBuffer.Allocate();
				echoedReq.encode(bbReq);
				fakeServer.send(new DatagramPacket(bbReq.Bytes, bbReq.ReadIndex, bbReq.size(), recvPacket.getSocketAddress()));
				Thread.sleep(80);
				Assertions.assertFalse(future.isDone(), "client must ignore inbound Request packet");

				// 2. 回发 count==0 的畸形 Response 报文：客户端必须忽略
				var zeroResp = new AllocateId128();
				zeroResp.setSessionId(req.getSessionId());
				zeroResp.setRequest(false);
				zeroResp.Result.setCount(0);
				var bbZero = ByteBuffer.Allocate();
				zeroResp.encode(bbZero);
				fakeServer.send(new DatagramPacket(bbZero.Bytes, bbZero.ReadIndex, bbZero.size(), recvPacket.getSocketAddress()));
				Thread.sleep(80);
				Assertions.assertFalse(future.isDone(), "client must ignore Response with count <= 0");

				// 3. 回发合法 Response：客户端正常完成 future
				var validResp = new AllocateId128();
				validResp.setSessionId(req.getSessionId());
				validResp.setRequest(false);
				validResp.Result.getStartId().assign(0, 100);
				validResp.Result.setCount(16);
				var bbValid = ByteBuffer.Allocate();
				validResp.encode(bbValid);
				fakeServer.send(new DatagramPacket(bbValid.Bytes, bbValid.ReadIndex, bbValid.size(), recvPacket.getSocketAddress()));

				var cache = future.get(5, TimeUnit.SECONDS);
				Assertions.assertEquals(new Id128(0, 100), cache.getStart());
				Assertions.assertEquals(new Id128(0, 100), cache.next());
			} finally {
				udpClient.stop();
			}
		}
	}

	@Test
	public void testServiceManagerServerBindsId128UdpToConfiguredAddress(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var loop = InetAddress.getLoopbackAddress();
		// 发号制带外选口（harness.DualStackPortAllocator）：OS临时口派发在8路并行测试类
		// 高churn下源端口同带狙击，64次重试全败（soak-20261125-1807 R7/20261126-0519 R2两现）。
		int port = DualStackPortAllocator.probe(loop);

		var conf = new Config();
		var doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		var el = doc.createElement("CustomizeConf");
		el.setAttribute("DbHome", tempDir.toString());
		conf.getCustomizes().put("Zeze.Services.ServiceManager", el);
		var sm = new ServiceManagerServer(loop, port, conf, "autokeys");
		try {
			var id128Field = ServiceManagerServer.class.getDeclaredField("id128Server");
			id128Field.setAccessible(true);
			var id128 = (Id128UdpServer)requireNonNull(id128Field.get(sm));
			var localAddr = id128.getLocalSocketAddress();
			Assertions.assertTrue(localAddr.getAddress().isLoopbackAddress(),
					"Id128UdpServer must bind to configured loopback address, actual=" + localAddr);
			Assertions.assertEquals(port, localAddr.getPort());
		} finally {
			sm.close();
		}
	}
}
