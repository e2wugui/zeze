package Zeze.Tikv;

import Zeze.*;
import java.io.*;

public class TikvConnection implements Closeable {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	private long ClientId;
	public final long getClientId() {
		return ClientId;
	}
	private TikvTransaction Transaction;
	public final TikvTransaction getTransaction() {
		return Transaction;
	}
	private void setTransaction(TikvTransaction value) {
		Transaction = value;
	}
	private boolean Disposed = false;
	public final boolean getDisposed() {
		return Disposed;
	}
	private void setDisposed(boolean value) {
		Disposed = value;
	}

	public static final int MinPoolSize = 4;
	public static final int MaxPoolSize = 16;

	private static BlockingCollection<Long> Pools = new BlockingCollection<Long>();
	private static Zeze.Util.AtomicLong UsingCount = new Util.AtomicLong();
	public static Zeze.Util.AtomicLong getUsingCount() {
		return UsingCount;
	}

	public TikvConnection(String databaseUrl) {
		getUsingCount().IncrementAndGet();
		while (true) {
			T clientId;
			tangible.OutObject<Long> tempOut_clientId = new tangible.OutObject<Long>();
			if (false == Pools.TryTake(tempOut_clientId)) {
			clientId = tempOut_clientId.outArgValue;
				break;
			}
		else {
			clientId = tempOut_clientId.outArgValue;
		}
			ClientId = clientId;
			if (getClientId() >= 0) {
				return;
			}
		}
		while (getUsingCount().Get() > MaxPoolSize) {
			// 使用timeout，更可靠点。
			Object clientId;
//C# TO JAVA CONVERTER TODO TASK: The following method call contained an unresolved 'out' keyword - these cannot be converted using the 'OutObject' helper class unless the method is within the code being modified:
			if (Pools.TryTake(out clientId, 1000)) {
				ClientId = clientId;
				if (getClientId() >= 0) {
					return;
				}
			}
		}
		ClientId = Tikv.Driver.NewClient(databaseUrl);
	}

	public final void Open() {
		// 不需要实现，和Sql,Mysql的一致，先保留。
	}

	public final void close() throws IOException {
		if (this.getDisposed()) {
			return;
		}
		this.setDisposed(true);
		getUsingCount().AddAndGet(-1);

		// 没做事情或者事务成功时，保存到Pool中。其他情况都关闭连接。
		if ((null == getTransaction() || getTransaction().getFinishState() == 1) && Pools.Count < MinPoolSize) {
			Pools.Add(getClientId());
			return;
		}
		// 不加入池子也要添加一个id到Pools中，用于唤醒Take并进行UsingCount的判断。
		// see 上面的构造函数。 
		Pools.Add(-1);
		try {
			Tikv.Driver.CloseClient(getClientId());
		}
		catch (RuntimeException ex) {
			// log close error only.
			logger.Error(ex, "Tikv Connection Close");
		}
	}

	public final TikvTransaction BeginTransaction() {
		if (null != getTransaction()) {
			throw new RuntimeException("Transaction Has Begin.");
		}
		setTransaction(new TikvTransaction(this));
		return getTransaction();
	}
}