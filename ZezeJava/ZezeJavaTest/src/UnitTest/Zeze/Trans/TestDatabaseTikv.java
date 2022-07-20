package UnitTest.Zeze.Trans;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import junit.framework.TestCase;
import org.junit.Ignore;
import org.tikv.common.BytePairWrapper;
import org.tikv.common.ByteWrapper;
import org.tikv.common.TiConfiguration;
import org.tikv.common.TiSession;
import org.tikv.common.operation.iterator.ConcreteScanIterator;
import org.tikv.common.util.ConcreteBackOffer;
import org.tikv.raw.RawKVClient;
import org.tikv.shade.com.google.protobuf.ByteString;
import org.tikv.txn.KVClient;
import org.tikv.txn.TwoPhaseCommitter;

@Ignore
public final class TestDatabaseTikv extends TestCase {
	private static final String serverAddr = "10.12.7.140:5379";

	public void testSimple() throws Exception {
		try (TiSession session = TiSession.create(TiConfiguration.createRawDefault(serverAddr));
			 RawKVClient client = session.createRawClient()) {
			var k = ByteString.copyFromUtf8("key");
			var v = ByteString.copyFromUtf8("Hello, World!");
			client.put(k, v);
			var v2 = client.get(k);
			assertTrue(v2.isPresent());
			assertEquals("Hello, World!", v2.get().toStringUtf8());
			client.put(k, ByteString.EMPTY);
			v2 = client.get(k);
			assertTrue(v2.isPresent());
			assertEquals(0, v2.get().size());
			client.delete(k);
			v2 = client.get(k);
			assertFalse(v2.isPresent());
		}
	}

	public void testBatch() throws Exception {
		try (TiSession session = TiSession.create(TiConfiguration.createRawDefault(serverAddr));
			 RawKVClient client = session.createRawClient()) {
			var k1 = ByteString.copyFromUtf8("k1");
			var v1 = ByteString.copyFromUtf8("v1");
			var k2 = ByteString.copyFromUtf8("k2");
			var v2 = ByteString.copyFromUtf8("");
			assertEquals(0, v2.size());
			client.batchPut(Map.of(k1, v1, k2, v2));
			var v1a = client.get(k1);
			var v2a = client.get(k2);
			assertTrue(v1a.isPresent());
			assertEquals("v1", v1a.get().toStringUtf8());
			assertTrue(v2a.isPresent());
			assertEquals(0, v2a.get().size());
		}
	}

	// 返回更新的version, 注意要求对es两次遍历的顺序一致
	private long txnBatchWrite(TiSession session, Iterable<Map.Entry<byte[], byte[]>> es, long version) throws Exception {
		var it = es.iterator();
		if (it.hasNext()) {
			try (var tpc = new TwoPhaseCommitter(session, version)) {
				var bo = ConcreteBackOffer.newCustomBackOff(1000);
				var e = it.next();
				var pKey = e.getKey();
				tpc.prewritePrimaryKey(bo, pKey, e.getValue());
				tpc.prewriteSecondaryKeys(pKey, new Iterator<>() {
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public BytePairWrapper next() {
						var e = it.next();
						return new BytePairWrapper(e.getKey(), e.getValue());
					}
				}, 1000);

				version = session.getTimestamp().getVersion();
				tpc.commitPrimaryKey(bo, pKey, version);
				var it2 = es.iterator();
				if (!it2.hasNext())
					throw new IllegalStateException(); // impossible
				it2.next();
				tpc.commitSecondaryKeys(new Iterator<>() {
					@Override
					public boolean hasNext() {
						return it2.hasNext();
					}

					@Override
					public ByteWrapper next() {
						return new ByteWrapper(it2.next().getKey());
					}
				}, version, 1000);
			}
		}
		return version;
	}

	public void testTxn() throws Exception {
		try (TiSession session = TiSession.create(TiConfiguration.createDefault(serverAddr));
			 KVClient kvClient = session.createKVClient()) {
			var key1 = "key1".getBytes(StandardCharsets.UTF_8);
			var key2 = "key2".getBytes(StandardCharsets.UTF_8);
			var val1 = "val1".getBytes(StandardCharsets.UTF_8);
			var val2 = "val2".getBytes(StandardCharsets.UTF_8);
			var key1b = ByteString.copyFrom(key1);
			var key2b = ByteString.copyFrom(key2);

			long version = session.getTimestamp().getVersion();
			System.out.println("key1: " + kvClient.get(key1b, version));
			System.out.println("key2: " + kvClient.get(key2b, version));

			version = txnBatchWrite(session, Map.of(key1, val2, key2, val1).entrySet(), version);
			version = txnBatchWrite(session, Map.of(key1, val1, key2, val2).entrySet(), version);

			ByteString val = kvClient.get(key1b, version);
			assertNotNull(val);
			assertEquals("val1", val.toString(StandardCharsets.UTF_8));

			var kvMap = new HashMap<ByteString, ByteString>();
			var kvPairs = kvClient.scan(ByteString.copyFrom(key1), ByteString.copyFromUtf8("key3"), version);
			kvPairs.forEach(kv -> kvMap.put(kv.getKey(), kv.getValue()));
			assertNotNull(kvMap.get(key1b));
			assertNotNull(kvMap.get(key2b));
			assertEquals("val1", kvMap.get(key1b).toString(StandardCharsets.UTF_8));
			assertEquals("val2", kvMap.get(key2b).toString(StandardCharsets.UTF_8));
		}
	}

	public void runTxnPerf(int base) throws Exception {
		try (TiSession session = TiSession.create(TiConfiguration.createDefault(serverAddr));
			 KVClient kvClient = session.createKVClient()) {
			var kvs = new HashMap<byte[], byte[]>();
			for (int i = 0; i < 1000; i++) {
				var ks = String.valueOf(base + i);
				kvs.put(("t" + ks).getBytes(StandardCharsets.UTF_8), ("v" + ks).getBytes(StandardCharsets.UTF_8));
			}

			var t = System.currentTimeMillis();
			long version = txnBatchWrite(session, kvs.entrySet(), session.getTimestamp().getVersion());
			var t2 = System.currentTimeMillis();
			System.out.println(t2 + " batchWrite: " + (t2 - t) + " ms");

			t = System.currentTimeMillis();
			for (int i = 0; i < 1000; i++) {
				var ks = String.valueOf(base + i);
				var v = kvClient.get(ByteString.copyFromUtf8("t" + ks), version);
				assertNotNull(v);
				assertTrue(v.size() > 1);
				var vs = v.toString(StandardCharsets.UTF_8);
				assertEquals('v', vs.charAt(0));
				assertEquals(ks, vs.substring(1));
			}
			t2 = System.currentTimeMillis();
			System.out.println(t2 + " readAll: " + (t2 - t) + " ms");
		}
	}

	public void testTxnPerf() throws Exception {
		var t = System.currentTimeMillis();
		System.out.println(t + " begin");
		var ts = new Thread[10];
		for (int i = 0; i < 10; i++) {
			int j = i;
			ts[i] = new Thread(() -> {
				try {
					runTxnPerf(j * 10000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			ts[i].start();
		}
		for (int i = 0; i < 10; i++)
			ts[i].join();
		var t2 = System.currentTimeMillis();
		System.out.println(t2 + " end " + (t2 - t) + " ms");
	}

	public void runRawPerf(int base) throws Exception {
		try (TiSession session = TiSession.create(TiConfiguration.createRawDefault(serverAddr));
			 RawKVClient client = session.createRawClient()) {
			var kvs = new HashMap<ByteString, ByteString>();
			for (int i = 0; i < 1000; i++) {
				var ks = String.valueOf(base + i);
				kvs.put(ByteString.copyFromUtf8("r" + ks), ByteString.copyFromUtf8("v" + ks));
			}

			var t = System.currentTimeMillis();
			client.batchPut(kvs);
			var t2 = System.currentTimeMillis();
			System.out.println(t2 + " batchPut end: " + (t2 - t) + " ms");

			t = System.currentTimeMillis();
			for (int i = 0; i < 1000; i++) {
				var ks = String.valueOf(base + i);
				var ov = client.get(ByteString.copyFromUtf8("r" + ks));
				assertNotNull(ov);
				assertTrue(ov.isPresent());
				var v = ov.get();
				assertTrue(v.size() > 1);
				var vs = v.toString(StandardCharsets.UTF_8);
				assertEquals('v', vs.charAt(0));
				assertEquals(ks, vs.substring(1));
			}
			t2 = System.currentTimeMillis();
			System.out.println(t2 + " getAll: " + (t2 - t) + " ms");
		}
	}

	public void testRawPerf() throws Exception {
		var t = System.currentTimeMillis();
		System.out.println(t + " begin");
		var ts = new Thread[10];
		for (int i = 0; i < 10; i++) {
			int j = i;
			ts[i] = new Thread(() -> {
				try {
					runRawPerf(j * 10000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			ts[i].start();
		}
		for (int i = 0; i < 10; i++)
			ts[i].join();
		var t2 = System.currentTimeMillis();
		System.out.println(t2 + " end " + (t2 - t) + " ms");
	}

	public void testRawScan() throws Exception {
		try (TiSession session = TiSession.create(TiConfiguration.createRawDefault(serverAddr));
			 RawKVClient client = session.createRawClient()) {
			var it = client.scan0(ByteString.copyFromUtf8("k1"), ByteString.copyFromUtf8("k3"));
			while (it.hasNext()) {
				var e = it.next();
				System.out.println(e.getKey() + " : " + e.getValue());
			}
		}
	}

	public void testTxnScan() throws Exception {
		var config = TiConfiguration.createDefault(serverAddr);
		try (TiSession session = TiSession.create(config)) {
			var it = new ConcreteScanIterator(config, session.getRegionStoreClientBuilder(),
					ByteString.copyFromUtf8("key1"), ByteString.copyFromUtf8("key3"),
					session.getTimestamp().getVersion());
			while (it.hasNext()) {
				var e = it.next();
				System.out.println(e.getKey() + " : " + e.getValue());
			}
		}
	}
}
