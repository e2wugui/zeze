package Zeze.Transaction;

import Zeze.Application;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Schemas;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManager.Reduce;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Table {
	private final int originalId;
	private final int id;
	private final @NotNull String originalName;
	private final @NotNull String name;
	private final @NotNull String suffix;
	private final ChangeListenerMap changeListenerMap = new ChangeListenerMap();
	private Application zeze;
	private Config.TableConf tableConf;
	private Database database;

	public Table(int id, @NotNull String name) {
		this(id, name, null);
	}

	public Table(int id, @NotNull String name, @Nullable String suffix) {
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

	public final @NotNull String getOriginalName() {
		return originalName;
	}

	public final @NotNull String getName() {
		return name;
	}

	public @NotNull String getSuffix() {
		return suffix;
	}

	public final @NotNull ChangeListenerMap getChangeListenerMap() {
		return changeListenerMap;
	}

	public final Application getZeze() {
		return zeze;
	}

	final void setZeze(@NotNull Application value) {
		zeze = value;
	}

	public final Config.TableConf getTableConf() {
		return tableConf;
	}

	final void setTableConf(Config.TableConf value) {
		tableConf = value;
	}

	public final Database getDatabase() {
		return database;
	}

	final void setDatabase(@NotNull Database db) {
		database = db;
	}

	public abstract @Nullable Storage<?, ?> open(@NotNull Application app, @NotNull Database database,
												 @Nullable DatabaseRocksDb.Table localTable);

	abstract void close();

	abstract @Nullable Storage<?, ?> getStorage();

	abstract @Nullable Database.Table getOldTable();

	public abstract boolean isNew();

	public abstract @NotNull Bean newValue();

	public boolean isMemory() {
		return false;
	}

	public boolean isAutoKey() {
		return false;
	}

	public abstract int reduceShare(@NotNull Reduce rpc, @NotNull ByteBuffer bbKey);

	public abstract int reduceInvalid(@NotNull Reduce rpc, @NotNull ByteBuffer bbKey);

	abstract void reduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex);

	public abstract void removeEncodedKey(@NotNull Binary encodedKey);

	public boolean isRelationalMapping() {
		return false;
	}

	public abstract void tryAlter();

	public abstract Schemas.RelationalTable getRelationalTable();

	public abstract void open(Table exist, Application app);

	public abstract void disable();

	public abstract DatabaseRocksDb.Table getLocalRocksCacheTable();

	public abstract long walkMemoryAny(TableWalkHandle<Object, Bean> handle);

	public abstract void __direct_put_cache__(Object key, Bean value, int state);

	public abstract @NotNull ByteBuffer encodeKey(@NotNull Object key);

	public abstract @NotNull Object decodeKey(@NotNull ByteBuffer bb);

	public abstract Class<? extends Comparable<?>> getKeyClass();

	public abstract Class<? extends Bean> getValueClass();
}
