package Zeze.Transaction;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Config;
import Zeze.Services.GlobalCacheManager.Reduce;

public abstract class Table {
	public Table(String name) {
		this.Name = name;

		// 新增属性Id，为了影响最小，采用virtual方式定义。
		// AddTable不能在这里调用。
		// 该调用移到Application.AddTable。
		// 影响：允许Table.Id重复，只要它没有加入zeze-app。
	}

	protected final String Name;
	public final String getName() {
		return Name;
	}
	public boolean isMemory() {
		return true;
	}
	public boolean isAutoKey() {
		return false;
	}

	private Application Zeze;
	public final Application getZeze() {
		return Zeze;
	}
	protected void setZeze(Application value) {
		Zeze = value;
	}

	private Config.TableConf TableConf;
	public final Config.TableConf getTableConf() {
		return TableConf;
	}
	protected final void setTableConf(Config.TableConf value) {
		TableConf = value;
	}

	abstract Storage Open(Application app, Database database);
	abstract void Close();

	public int getId() {
		return 0; // 新增属性。为了增加顺利，提供默认实现。子类必须提供新的实现。
	}

	public int ReduceShare(Reduce rpc) {
		throw new UnsupportedOperationException();
	}

	public int ReduceInvalid(Reduce rpc) {
		throw new UnsupportedOperationException();
	}

	void ReduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex) {
		throw new UnsupportedOperationException();
	}

	private final ChangeListenerMap ChangeListenerMap = new ChangeListenerMap();
	public final ChangeListenerMap getChangeListenerMap() {
		return ChangeListenerMap;
	}

	abstract Storage GetStorage();

	private Database _Database;
	public final Database getDatabase() { return _Database; }
	protected final void setDatabase(Database db) { _Database = db; }
	public abstract boolean isNew();
	public abstract Bean NewBeanValue();
}
