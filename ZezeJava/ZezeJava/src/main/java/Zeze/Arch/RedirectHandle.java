package Zeze.Arch;

import java.util.List;
import Zeze.Beans.ProviderDirect.BActionParam;
import Zeze.Net.Binary;
import Zeze.Util.Func4;
import Zeze.Transaction.TransactionLevel;

public class RedirectHandle {
	public TransactionLevel RequestTransactionLevel;

	/**
	 0) long [in] sessionid
	 1) int [in] hash
	 2) Zeze.Net.Binary [in] encoded parameters
	 3) List<Zeze.Beans.Provider.BActionParam> [result] result for callback. avoid copy.
	 4) Return [return]
	 Func不能使用ref，而Zeze.Net.Binary是只读的。就这样吧。
	 */
	public Func4<Long, Integer, Binary, List<BActionParam>, Return> RequestHandle;

	public TransactionLevel ResponseTransactionLevel;
	public int ResponseHandle;
}
