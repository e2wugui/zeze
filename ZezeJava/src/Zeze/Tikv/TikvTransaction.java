package Zeze.Tikv;

import Zeze.*;
import java.io.*;

public class TikvTransaction implements Closeable {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	private TikvConnection Connection;
	public final TikvConnection getConnection() {
		return Connection;
	}
	private long TransactionId;
	public final long getTransactionId() {
		return TransactionId;
	}
	private int FinishState;
	public final int getFinishState() {
		return FinishState;
	}
	private void setFinishState(int value) {
		FinishState = value;
	}

	public TikvTransaction(TikvConnection conn) {
		Connection = conn;
		TransactionId = Tikv.Driver.Begin(conn.getClientId());
	}

	public final void Commit() {
		if (getFinishState() != 0) {
			return;
		}

		setFinishState(1);
		Tikv.Driver.Commit(getTransactionId());
	}

	public final void Rollback() {
		if (getFinishState() != 0) {
			return;
		}

		setFinishState(2);
		try {
			Tikv.Driver.Rollback(getTransactionId());
		}
		catch (RuntimeException ex) {
			// long rollback error only
			logger.Error(ex, "TiKv Transaction Rollback");
		}
	}

	public final void close() throws IOException {
		Rollback();
	}
}