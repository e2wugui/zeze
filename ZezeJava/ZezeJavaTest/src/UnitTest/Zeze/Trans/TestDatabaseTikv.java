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

	private void batchWrite(TiSession session, Iterable<Map.Entry<byte[], byte[]>> es) throws Exception {
		var it = es.iterator();
		if (!it.hasNext())
			return;
		try (var tpc = new TwoPhaseCommitter(session, session.getTimestamp().getVersion())) {
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

			long commitTS = session.getTimestamp().getVersion();
			tpc.commitPrimaryKey(bo, pKey, commitTS);
			var it2 = es.iterator();
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
			}, commitTS, 1000);
		}
	}

	public void testTxn() throws Exception {
		try (TiSession session = TiSession.create(TiConfiguration.createDefault(serverAddr))) {
			try (KVClient kvClient = session.createKVClient()) {
				var key1 = "key1".getBytes(StandardCharsets.UTF_8);
				var key2 = "key2".getBytes(StandardCharsets.UTF_8);
				var val1 = "val1".getBytes(StandardCharsets.UTF_8);
				var val2 = "val2".getBytes(StandardCharsets.UTF_8);
				var key1b = ByteString.copyFrom(key1);
				var key2b = ByteString.copyFrom(key2);

				batchWrite(session, Map.of(key1, val1, key2, val2).entrySet());

				long version = session.getTimestamp().getVersion();

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
	}

	public void runTxnPerf(int base) throws Exception {
		try (TiSession session = TiSession.create(TiConfiguration.createDefault(serverAddr))) {
			try (KVClient kvClient = session.createKVClient()) {
				var kvs = new HashMap<byte[], byte[]>();
				for (int i = 0; i < 1000; i++) {
					var ks = String.valueOf(base + i);
					kvs.put(ks.getBytes(StandardCharsets.UTF_8), ("v" + ks).getBytes(StandardCharsets.UTF_8));
				}

				long t = System.currentTimeMillis();
				batchWrite(session, kvs.entrySet());
				System.out.println("batchWrite: " + (System.currentTimeMillis() - t) + " ms");

				t = System.currentTimeMillis();
				long version = session.getTimestamp().getVersion();
				for (int i = 0; i < 1000; i++) {
					var ks = String.valueOf(base + i);
					var v = kvClient.get(ByteString.copyFromUtf8(ks), version);
					assertNotNull(v);
					assertTrue(v.size() > 1);
					var vs = v.toString(StandardCharsets.UTF_8);
					assertEquals('v', vs.charAt(0));
					assertEquals(ks, vs.substring(1));
				}
				System.out.println("readAll: " + (System.currentTimeMillis() - t) + " ms");
			}
		}
	}

	public void testTxnPerf() throws Exception {
		var t = System.currentTimeMillis();
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
		System.out.println("end " + (System.currentTimeMillis() - t) + " ms");
	}
}
