package Zeze.Transaction;

import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Util.FuncLong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProtocolProcedure extends Procedure {
	private final @NotNull String protocolClassName;
	private final @NotNull Binary protocolRawArgument;

	public ProtocolProcedure(@NotNull Application app, @Nullable FuncLong action,
							 @Nullable String actionName, @Nullable TransactionLevel level,
							 @NotNull String protocolClassName, @NotNull Binary protocolRawArgument) {
		super(app, action, actionName, level);
		this.protocolClassName = protocolClassName;
		this.protocolRawArgument = protocolRawArgument;
	}

	public @NotNull Binary getProtocolRawArgument() {
		return protocolRawArgument;
	}

	public @NotNull String getProtocolClassName() {
		return protocolClassName;
	}
}
