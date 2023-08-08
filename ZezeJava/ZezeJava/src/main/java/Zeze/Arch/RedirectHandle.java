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
		Object call(int hash, Binary encodedParams) throws Exception;
	}

	public final TransactionLevel requestTransactionLevel;
	public final IRequestHandle requestHandle;
	public final Function<Object, Binary> resultEncoder;
	public final int version;

	public RedirectHandle(TransactionLevel requestTransactionLevel,
						  IRequestHandle requestHandle,
						  Function<Object, Binary> resultEncoder) {
		this(requestTransactionLevel, requestHandle, resultEncoder, 0);
	}

	public RedirectHandle(TransactionLevel requestTransactionLevel,
						  IRequestHandle requestHandle,
						  Function<Object, Binary> resultEncoder,
						  int version) {
		this.requestTransactionLevel = requestTransactionLevel;
		this.requestHandle = requestHandle;
		this.resultEncoder = resultEncoder;
		this.version = version;
	}
}
