package Zeze.Transaction;

import Zeze.Serialize.*;
import Zeze.*;

public abstract class Storage {
	private Database.Table DatabaseTable;
	public final Database.Table getDatabaseTable() {
		return DatabaseTable;
	}
	protected final void setDatabaseTable(Database.Table value) {
		DatabaseTable = value;
	}

	public abstract int EncodeN();

	public abstract int Encode0();

	public abstract int Snapshot();

	public abstract int Flush(Database.Transaction t);

	public abstract void Cleanup();

	public abstract void Close();
}