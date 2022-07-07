package Zeze.Transaction;

import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManager.Reduce;

public abstract class Table {
	private final String Name;
	private final ChangeListenerMap ChangeListenerMap = new ChangeListenerMap();
	private Application Zeze;
	private Config.TableConf TableConf;
	private Database _Database;

	public Table(String name) {
		Name = name;

		// 新增属性Id，为了影响最小，采用virtual方式定义。
		// AddTable不能在这里调用。
		// 该调用移到Application.AddTable。
		// 影响：允许Table.Id重复，只要它没有加入zeze-app。
	}

	public final String getName() {
		return Name;
	}

	public final ChangeListenerMap getChangeListenerMap() {
		return ChangeListenerMap;
	}

	public final Application getZeze() {
		return Zeze;
	}

	final void setZeze(Application value) {
		Zeze = value;
	}

	public final Config.TableConf getTableConf() {
		return TableConf;
	}

	final void setTableConf(Config.TableConf value) {
		TableConf = value;
	}

	public final Database getDatabase() {
		return _Database;
	}

	final void setDatabase(Database db) {
		_Database = db;
	}

	abstract Storage<?, ?> Open(Application app, Database database);

	abstract void Close();

	abstract Storage<?, ?> GetStorage();

	public abstract boolean isNew();

	public abstract Bean NewValue();

	public boolean isMemory() {
		return true;
	}

	public boolean isAutoKey() {
		return false;
	}

	public int getId() {
		return 0; // 新增属性。为了增加顺利，提供默认实现。子类必须提供新的实现。
	}

	public abstract int ReduceShare(Reduce rpc, ByteBuffer bbKey);

	public abstract int ReduceInvalid(Reduce rpc, ByteBuffer bbKey);

	abstract void ReduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex);
}
