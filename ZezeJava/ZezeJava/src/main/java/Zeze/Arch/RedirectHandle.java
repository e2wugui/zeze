package Zeze.Arch;

import java.util.function.Function;
import Zeze.Net.Binary;
import Zeze.Transaction.TransactionLevel;

public class RedirectHandle {
	public interface IRequestHandle {
		/**
		 * @param hash          serverId或hash
		 * @param encodedParams 输入参数的序列化
		 * @return RedirectFuture(用于非All模式); RedirectAllFuture(用于All模式); null(用于没有结果返回或异常)
		 */
		Object call(int hash, Binary encodedParams) throws Throwable;
	}

	public final TransactionLevel RequestTransactionLevel;
	public final IRequestHandle RequestHandle;
	public final Function<Object, Binary> ResultEncoder;

	public RedirectHandle(TransactionLevel requestTransactionLevel,
						  IRequestHandle requestHandle,
						  Function<Object, Binary> resultEncoder) {
		RequestTransactionLevel = requestTransactionLevel;
		RequestHandle = requestHandle;
		ResultEncoder = resultEncoder;
	}
}
