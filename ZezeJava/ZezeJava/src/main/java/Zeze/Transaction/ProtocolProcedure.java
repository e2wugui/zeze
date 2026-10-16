package Zeze.Transaction;

import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Util.FuncLong;
import org.jspecify.annotations.Nullable;

/** 由协议处理函数发起的过程：额外携带协议类名与原始参数，供 History 记录变更来源。 */
public class ProtocolProcedure extends Procedure {
	private final String protocolClassName;
	private final Binary protocolRawArgument;

	public ProtocolProcedure(Application app, @Nullable FuncLong action,
							 @Nullable String actionName, @Nullable TransactionLevel level,
							 String protocolClassName, Binary protocolRawArgument) {
		super(app, action, actionName, level);
		this.protocolClassName = protocolClassName;
		this.protocolRawArgument = protocolRawArgument;
	}

	public Binary getProtocolRawArgument() {
		return protocolRawArgument;
	}

	public String getProtocolClassName() {
		return protocolClassName;
	}
}
