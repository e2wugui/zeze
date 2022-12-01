package Zeze.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Config.DatabaseConf;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import Zeze.Util.ShutdownHook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 数据访问的效率主要来自TableCache的命中。根据以往的经验，命中率是很高的。
 * 所以数据库层就不要求很高的效率。马马虎虎就可以了。
 */
public abstract class Database {
	protected static final Logger logger = LogManager.getLogger(Database.class);

	// 当数据库对Key长度有限制时，使用这个常量。这个数字来自 PorlarDb-X。其中MySql 8是3072.
	// 以后需要升级时，修改这个常量。但是对于已经存在的表，需要自己完成Alter。
	public final static int eMaxKeyLength = 3070;

	private static final boolean isDebugEnabled = logger.isDebugEnabled();

	static {
		ShutdownHook.init();
	}

	private final ConcurrentHashMap<String, Zeze.Transaction.Table> tables = new ConcurrentHashMap<>();
	private final ArrayList<Storage<?, ?>> storages = new ArrayList<>();
	private final DatabaseConf conf;
	private final String databaseUrl;
	private Operates directOperates;

	public Database(DatabaseConf conf) {
		this.conf = conf;
		databaseUrl = conf.getDatabaseUrl();
	}

	public final Collection<Zeze.Transaction.Table> getTables() {
		return tables.values();
	}

	public final Zeze.Transaction.Table getTable(String name) {
		return tables.get(name);
	}

	public final void addTable(Zeze.Transaction.Table table) {
		if (null != tables.putIfAbsent(table.getName(), table))
			throw new IllegalStateException("duplicate table=" + table.getName());
	}

	public final void removeTable(Zeze.Transaction.Table table) {
		table.close();
		tables.remove(table.getName());
	}

	public final DatabaseConf getConf() {
		return conf;
	}

	public final String getDatabaseUrl() {
		return databaseUrl;
	}

	public final Operates getDirectOperates() {
		return directOperates;
	}

	protected final void setDirectOperates(Operates value) {
		directOperates = value;
	}

	public final void open(Application app) {
		for (Zeze.Transaction.Table table : tables.values()) {
			var storage = table.open(app, this);
			if (storage != null)
				storages.add(storage);
		}
	}

	public final void openDynamicTable(Application app, Zeze.Transaction.Table table) {
		var storage = table.open(app, this);
		if (null != storage)
			storages.add(storage);
	}

	public void close() {
		for (Zeze.Transaction.Table table : tables.values())
			table.close();
		tables.clear();
		storages.clear();
	}

	public final void encodeN() {
		// try encode. 可以多趟。
		for (int i = 1; i <= 1; ++i) {
			int countEncodeN = 0;
			for (var storage : storages)
				countEncodeN += storage.encodeN();
			if (isDebugEnabled)
				logger.debug("Checkpoint EncodeN {}@{}", i, countEncodeN);
		}
	}

	public final void snapshot() {
		int countEncode0 = 0;
		int countSnapshot = 0;
		for (var storage : storages)
			countEncode0 += storage.encode0();
		for (var storage : storages)
			countSnapshot += storage.snapshot();

		logger.info("Checkpoint Encode0 And Snapshot countEncode0={} countSnapshot={}", countEncode0, countSnapshot);
	}

	public final void flush(Database.Transaction t, HashMap<Database, Transaction> tss, Database.Transaction lct) {
		int countFlush = 0;
		for (var storage : storages)
			countFlush += storage.flush(t, tss, lct);
		logger.info("Checkpoint Flush count={}", countFlush);
	}

	public final void cleanup() {
		for (var storage : storages)
			storage.cleanup();
	}

	public abstract Table openTable(String name);

	public static byte[] copyIf(java.nio.ByteBuffer bb) {
		if (bb.limit() == bb.capacity() && bb.arrayOffset() == 0)
			return bb.array();
		return Arrays.copyOfRange(bb.array(), bb.arrayOffset(), bb.limit());
	}

	public static byte[] copyIf(ByteBuffer bb) {
		if (bb.ReadIndex == 0 && bb.WriteIndex == bb.capacity())
			return bb.Bytes;
		return Arrays.copyOfRange(bb.Bytes, bb.ReadIndex, bb.WriteIndex);
	}

	public interface Table {
		boolean isNew();

		Database getDatabase();

		ByteBuffer find(ByteBuffer key);

		void replace(Transaction t, ByteBuffer key, ByteBuffer value);

		void remove(Transaction t, ByteBuffer key);

		/**
		 * 每一条记录回调。回调返回true继续遍历，false中断遍历。
		 *
		 * @return 返回已经遍历的数量
		 */
		long walk(TableWalkHandleRaw callback);

		long walkKey(TableWalkKeyRaw callback);

		ByteBuffer walk(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback);

		ByteBuffer walkKey(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback);

		void close();
	}

	public abstract Transaction beginTransaction();

	public interface Transaction extends AutoCloseable {
		void commit();

		void rollback();
	}

	public static class DataWithVersion implements Zeze.Serialize.Serializable {
		public ByteBuffer data;
		public long version;

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteByteBuffer(data);
			bb.WriteLong(version);
		}

		@Override
		public void decode(ByteBuffer bb) {
			data = ByteBuffer.Wrap(bb.ReadBytes());
			version = bb.ReadLong();
		}

		public static DataWithVersion decode(byte[] bytes) {
			var dv = new DataWithVersion();
			if (bytes != null)
				dv.decode(ByteBuffer.Wrap(bytes));
			return dv;
		}
	}

	/**
	 * 由后台数据库直接支持的存储过程。
	 * 直接操作后台数据库，不经过cache。
	 */
	public interface Operates {
		/*
		  table zeze_global {string global} 一条记录
		  table zeze_instances {int localId} 每个启动的gs一条记录
		  SetInUse(localId, global) // 没有启用cache-sync时，global是""
		    if (false == zeze_instances.insert(localId)) {
		      rollback;
		      return false; // 同一个localId只能启动一个。
		    }
		    globalNow = zeze_global.getOrAdd(global); // sql 应该是没有这样的方法的
		    if (globalNow != global) {
		      // 不管是否启用cache-sync，global都必须一致
		      rollback;
		      return false;
		    }
		    if (zeze_instances.count == 1)
		      return true; // 只有一个实例，肯定成功。
		    if (global.Count == 0) {
		      // 没有启用global，但是实例超过1。
		      rollback;
		      return false;
		    }
		    commit;
		    return true;
		*/
		void setInUse(int localId, String global);

		int clearInUse(int localId, String global);

		/*
		  if (Exist(key)) {
		    if (CurrentVersion != version)
		      return false;
		    UpdateData(data);
		    return (++CurrentVersion, true);
		  }
		  InsertData(data);
		  return (CurrentVersion = version, true);
		*/
		KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version);

		DataWithVersion getDataWithVersion(ByteBuffer key);
	}

	public static class NullOperates implements Operates {

		@Override
		public void setInUse(int localId, String global) {

		}

		@Override
		public int clearInUse(int localId, String global) {
			return 0;
		}

		@Override
		public KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			return KV.create(version, true);
		}

		@Override
		public DataWithVersion getDataWithVersion(ByteBuffer key) {
			return null;
		}
	}
}
