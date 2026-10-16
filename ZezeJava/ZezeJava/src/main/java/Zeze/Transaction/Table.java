package Zeze.Transaction;

import Zeze.Application;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManager.Reduce;
import org.jspecify.annotations.Nullable;
import Zeze.Config.TableConf;
import Zeze.Schemas.RelationalTable;

/** 表基类：持有 id/名字/配置/数据库绑定与变更监听器，定义存储打开与 GCM 降级等表级抽象。 */
public abstract class Table {
	private final int originalId;
	private final int id;
	private final String originalName;
	private final String name;
	private final String suffix;
	private final ChangeListenerMap changeListenerMap = new ChangeListenerMap();
	private @Nullable Application zeze;
	private @Nullable TableConf tableConf;
	private @Nullable Database database;

	public Table(int id, String name) {
		this(id, name, null);
	}

	public Table(int id, String name, @Nullable String suffix) {
		this.originalId = id;
		this.originalName = name;
		if (suffix != null && !suffix.isEmpty()) {
			name += suffix;
			id = Bean.hash32(name);
		}
		this.id = id;
		this.name = name;
		this.suffix = suffix != null ? suffix : "";

		// AddTable不能在这里调用。
		// 该调用移到Application.AddTable。
		// 影响：允许Table.Id重复，只要它没有加入zeze-app。
	}

	public final int getOriginalId() {
		return originalId;
	}

	public final int getId() {
		return id;
	}

	public final String getOriginalName() {
		return originalName;
	}

	public final String getName() {
		return name;
	}

	public String getSuffix() {
		return suffix;
	}

	public final ChangeListenerMap getChangeListenerMap() {
		return changeListenerMap;
	}

	// 生命周期注入字段：open() 前为 null，getter 以显式异常给出可诊断失败（房例同 TableX.getCache）。
	public final Application getZeze() {
		var app = zeze;
		if (app == null)
			throw new IllegalStateException("table not opened: " + name);
		return app;
	}

	final void setZeze(Application value) {
		zeze = value;
	}

	public final Config.TableConf getTableConf() {
		var conf = tableConf;
		if (conf == null)
			throw new IllegalStateException("table not opened: " + name);
		return conf;
	}

	final void setTableConf(Config.@Nullable TableConf value) {
		tableConf = value;
	}

	public final Database getDatabase() {
		var db = database;
		if (db == null)
			throw new IllegalStateException("table not opened: " + name);
		return db;
	}

	final void setDatabase(Database db) {
		database = db;
	}

	public abstract @Nullable Storage<?, ?> open(Application app, Database database,
												 DatabaseRocksDb.@Nullable Table localTable);

	abstract void close();

	abstract @Nullable Storage<?, ?> getStorage();

	abstract Database.@Nullable Table getOldTable();

	public abstract boolean isNew();

	public abstract Bean newValue();

	public boolean isMemory() {
		return false;
	}

	public boolean isAutoKey() {
		return false;
	}

	public abstract void reduceShare(Reduce rpc, ByteBuffer bbKey);

	public abstract void reduceInvalid(Reduce rpc, ByteBuffer bbKey);

	abstract void reduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex);

	public abstract void removeEncodedKey(Binary encodedKey);

	public boolean isRelationalMapping() {
		return false;
	}

	public abstract void tryAlter();

	public abstract @Nullable RelationalTable getRelationalTable();

	public abstract void open(Table exist, Application app);

	public abstract void disable();

	public abstract DatabaseRocksDb.Table getLocalRocksCacheTable();

	public abstract long walkMemoryAny(TableWalkHandle<Object, Bean> handle) throws Exception;

	public abstract void __direct_put_cache__(Object key, Bean value, int state);

	public abstract ByteBuffer encodeKey(Object key);

	public abstract Object decodeKey(ByteBuffer bb);

	public abstract Class<? extends Comparable<?>> getKeyClass();

	public abstract Class<? extends Bean> getValueClass();

}
