package Zeze.Transaction;

import Zeze.Serialize.*;
import Zeze.*;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;

/** 
 数据访问的效率主要来自TableCache的命中。根据以往的经验，命中率是很高的。
 所以数据库层就不要求很高的效率。马马虎虎就可以了。
*/
public abstract class Database {
	private static final Logger logger = LogManager.getLogger(Database.class);

	private HashMap<String, Zeze.Transaction.Table> tables = new HashMap<String, Zeze.Transaction.Table>();
	public ArrayList<Storage> storages = new ArrayList<Storage>();
	public final Collection<Zeze.Transaction.Table> getTables() {
		return tables.values();
	}

	private String DatabaseUrl;
	public final String getDatabaseUrl() {
		return DatabaseUrl;
	}

	public Database(String url) {
		this.DatabaseUrl = url;
	}

	public final Zeze.Transaction.Table GetTable(String name) {
		return tables.get(name);
	}

	public abstract Transaction BeginTransaction();

	public final void AddTable(Zeze.Transaction.Table table) {
		tables.put(table.getName(), table);
	}

	public final void RemoveTable(Zeze.Transaction.Table table) {
		tables.remove(table.getName());
	}

	public final void Open(Application app) {
		for (Zeze.Transaction.Table table : tables.values()) {
			Storage storage = table.Open(app, this);
			if (null != storage) {
				storages.add(storage);
			}
		}
	}

	public final void Close() {
		for (Zeze.Transaction.Table table : tables.values()) {
			table.Close();
		}
		tables.clear();
		storages.clear();
	}

	public final void EncodeN() {
		// try Encode. 可以多趟。
		for (int i = 1; i <= 1; ++i) {
			int countEncodeN = 0;
			for (Storage storage : storages) {
				countEncodeN += storage.EncodeN();
			}
			logger.debug("Checkpoint EncodeN {}@{}", i, countEncodeN);
		}
	}

	public final void Snapshot() {
		int countEncode0 = 0;
		int countSnapshot = 0;
		for (Storage storage : storages) {
			countEncode0 += storage.Encode0();
		}
		for (Storage storage : storages) {
			countSnapshot += storage.Snapshot();
		}

		logger.info("Checkpoint Encode0 And Snapshot countEncode0={} countSnapshot={}", countEncode0, countSnapshot);
	}

	public final void Flush(Transaction trans) {
		int countFlush = 0;
		for (Storage storage : storages) {
			countFlush += storage.Flush(trans);
		}
		logger.info("Checkpoint Flush count={}", countFlush);
	}

	public final void Cleanup() {
		for (Storage storage : storages) {
			storage.Cleanup();
		}
	}

	public abstract Database.Table OpenTable(String name);

	public interface Transaction extends Closeable {
		public void Commit();
		public void Rollback();
	}

	public interface Table {
		public Database getDatabase();
		public ByteBuffer Find(ByteBuffer key);
		public void Replace(Transaction t, ByteBuffer key, ByteBuffer value);
		public void Remove(Transaction t, ByteBuffer key);
		/** 
		 每一条记录回调。回调返回true继续遍历，false中断遍历。
		 
		 @param callback
		 @return 返回已经遍历的数量
		*/
		public long Walk(TableWalkHandleRaw callback);
		public void Close();
	}

	/** 
	 由后台数据库直接支持的存储过程。
	 直接操作后台数据库，不经过cache。
	*/
	public interface Operates {
		/*
		 table zeze_global {string global} 一条记录
		 table zeze_instances {int localId} 每个启动的gs一条记录
		 SetInUse(localId, global) // 没有启用cache-sync时，global是""
		   if (false == zeze_instances.insert(localId))
		   {
		     rollback;
		     return false; // 同一个localId只能启动一个。
		   }
		   globalNow = zeze_global.getOrAdd(global); // sql 应该是没有这样的方法的
		   if (globalNow != global)
		   {
		     // 不管是否启用cache-sync，global都必须一致
		     rollback;
		     return false;
		   }
		   if (zeze_instances.count == 1)
		     return true; // 只有一个实例，肯定成功。
		   if (global.Count == 0)
		   {
		     // 没有启用global，但是实例超过1。
		     rollback;
		     return false;
		   }
		   commit;
		   return true;
		 */
		public void SetInUse(int localId, String global);
		public int ClearInUse(int localId, String global);

		/** 
		 if (Exist(key))
		 {
			 if (CurrentVersion != version)
				 return false;
			 UpdateData(data);
			 ++CurrentVersion;
			 version = CurrentVersion;
			 return true;
		 }
		 InsertData(data);
		 CurrentVersion = version;
		 return true;
		 
		 @param data
		 @param version
		 @return 
		*/
		public boolean SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, tangible.RefObject<Long> version);
	}

	private Operates DirectOperates;
	public final Operates getDirectOperates() {
		return DirectOperates;
	}
	protected final void setDirectOperates(Operates value) {
		DirectOperates = value;
	}
}