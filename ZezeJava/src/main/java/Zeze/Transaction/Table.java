package Zeze.Transaction;

import Zeze.Services.GlobalCacheManager.*;
import Zeze.*;

public abstract class Table {
	public Table(String name) {
		this.Name = name;
	}

	protected String Name;
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

	int ReduceShare(Reduce rpc) {
		throw new UnsupportedOperationException();
	}

	int ReduceInvalid(Reduce rpc) {
		throw new UnsupportedOperationException();
	}

	void ReduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex) {
		throw new UnsupportedOperationException();
	}

	private final ChangeListenerMap ChangeListenerMap = new ChangeListenerMap();
	public final ChangeListenerMap getChangeListenerMap() {
		return ChangeListenerMap;
	}

	public abstract ChangeVariableCollector CreateChangeVariableCollector(int variableId);

	abstract Storage GetStorage();
}