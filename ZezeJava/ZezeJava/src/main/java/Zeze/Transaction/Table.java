package Zeze.Transaction;

import Zeze.Application;
import Zeze.Config;
import Zeze.Services.GlobalCacheManager.Reduce;
import org.apache.commons.lang3.NotImplementedException;

public abstract class Table {
	public Table(String name) {
		this.Name = name;
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

	public ChangeVariableCollector CreateChangeVariableCollector(int variableId)
	{
		// TODO delete me
		throw new NotImplementedException("");
	}

	abstract Storage GetStorage();

	private Database _Database;
	public final Database getDatabase() { return _Database; }
	protected final void setDatabase(Database db) { _Database = db; }
	public abstract boolean isNew();
	public abstract Bean NewBeanValue();
}
