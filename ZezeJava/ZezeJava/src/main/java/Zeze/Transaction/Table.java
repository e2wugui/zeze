package Zeze.Transaction;

import Zeze.Application;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManager.Reduce;

public abstract class Table {
	private final String name;
	private final ChangeListenerMap changeListenerMap = new ChangeListenerMap();
	private Application zeze;
	private Config.TableConf tableConf;
	private Database database;

	public Table(String name) {
		this.name = name;

		// 新增属性Id，为了影响最小，采用virtual方式定义。
		// AddTable不能在这里调用。
		// 该调用移到Application.AddTable。
		// 影响：允许Table.Id重复，只要它没有加入zeze-app。
	}

	public final String getName() {
		return name;
	}

	public final ChangeListenerMap getChangeListenerMap() {
		return changeListenerMap;
	}

	public final Application getZeze() {
		return zeze;
	}

	final void setZeze(Application value) {
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

	final void setDatabase(Database db) {
		database = db;
	}

	abstract Storage<?, ?> open(Application app, Database database);

	abstract void close();

	abstract Storage<?, ?> getStorage();

	abstract Database.Table getOldTable();

	public abstract boolean isNew();

	public abstract Bean newValue();

	public boolean isMemory() {
		return true;
	}

	public boolean isAutoKey() {
		return false;
	}

	public int getId() {
		return 0; // 新增属性。为了增加顺利，提供默认实现。子类必须提供新的实现。
	}

	public abstract int reduceShare(Reduce rpc, ByteBuffer bbKey);

	public abstract int reduceInvalid(Reduce rpc, ByteBuffer bbKey);

	abstract void reduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex);

	public abstract void removeEncodedKey(Binary encodedKey);
}
