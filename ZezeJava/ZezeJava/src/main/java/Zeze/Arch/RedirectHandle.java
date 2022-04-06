package Zeze.Arch;

import Zeze.Net.Binary;
import Zeze.Util.Func3;
import Zeze.Transaction.TransactionLevel;

public class RedirectHandle {
	public TransactionLevel RequestTransactionLevel = TransactionLevel.Serializable;
	/**
	 0) long [in] sessionid
	 1) int [in] hash
	 2) Zeze.Net.Binary [in] encoded parameters
	 3) List<Zeze.Beans.Provider.BActionParam> [result] result for callback. avoid copy.
	 4) Return [return]
	 Func不能使用ref，而Zeze.Net.Binary是只读的。就这样吧。
	 */
	public Func3<Long, Integer, Binary, Binary> RequestHandle;
}
