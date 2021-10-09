package Zeze.Transaction;

import Zeze.Serialize.*;
import Zeze.Services.*;
import Zeze.*;
import java.util.*;

public abstract class Table {
	private static ArrayList<Table> Tables = new ArrayList<Table> ();
	private static ArrayList<Table> getTables() {
		return Tables;
	}
	public static Table GetTable(int id) {
		return getTables().get(id);
	}

	public Table(String name) {
		this.Name = name;

		synchronized (getTables()) {
			this.Id = getTables().size();
			getTables().add(this);
		}
	}

	private String Name;
	public final String getName() {
		return Name;
	}
	private int Id;
	public final int getId() {
		return Id;
	}
	public boolean isMemory() {
		return true;
	}
	public boolean isAutoKey() {
		return false;
	}

	private Config.TableConf TableConf;
	public final Config.TableConf getTableConf() {
		return TableConf;
	}
	protected final void setTableConf(Config.TableConf value) {
		TableConf = value;
	}

	public abstract Storage Open(Application app, Database database);
	public abstract void Close();

	public int ReduceShare(GlobalCacheManager.Reduce rpc) {
		throw new UnsupportedOperationException();
	}

	public int ReduceInvalid(GlobalCacheManager.Reduce rpc) {
		throw new UnsupportedOperationException();
	}

	public void ReduceInvalidAllLocalOnly(int GlobalCacheManagerHashIndex) {
		throw new UnsupportedOperationException();
	}

	private ChangeListenerMap ChangeListenerMap = new ChangeListenerMap();
	public final ChangeListenerMap getChangeListenerMap() {
		return ChangeListenerMap;
	}

	public abstract ChangeVariableCollector CreateChangeVariableCollector(int variableId);

	public abstract Storage getStorage();
}