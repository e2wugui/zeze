package Zeze.Arch;

import Zeze.Builtin.ProviderDirect.BLoginKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public sealed interface OnlineSpec permits LoginOnlineSpec, LoginsOnlineSpec,
		AccountOnlineSpec, AccountsOnlineSpec, ReliableOnlineSpec, TransmitOnlineSpec {

	static @NotNull LoginOnlineSpec ofLogin(@NotNull Online online,
											@NotNull String account, @NotNull String clientId) {
		return new LoginOnlineSpec(online, account, clientId);
	}

	static @NotNull LoginsOnlineSpec ofLogins(@NotNull Online online, @NotNull Collection<BLoginKey> logins) {
		return new LoginsOnlineSpec(online, logins);
	}

	static @NotNull AccountOnlineSpec ofAccount(@NotNull Online online, @NotNull String account) {
		return new AccountOnlineSpec(online, account);
	}

	static @NotNull AccountsOnlineSpec ofAccounts(@NotNull Online online, @NotNull Collection<String> accounts) {
		return new AccountsOnlineSpec(online, accounts);
	}

	static @NotNull ReliableOnlineSpec ofReliableNotify(@NotNull Online online,
														@NotNull String account, @NotNull String clientId,
														@NotNull String listenerName) {
		return new ReliableOnlineSpec(online, account, clientId, listenerName);
	}

	static @NotNull TransmitOnlineSpec ofTransmit(@NotNull Online online,
												  @NotNull String senderAccount, @NotNull String senderClientId,
												  @NotNull String actionName,
												  @NotNull String targetAccount, @NotNull String targetClientId) {
		return ofTransmit(online, senderAccount, senderClientId, actionName,
				List.of(new BLoginKey(targetAccount, targetClientId)));
	}

	static @NotNull TransmitOnlineSpec ofTransmit(@NotNull Online online,
												  @NotNull String senderAccount, @NotNull String senderClientId,
												  @NotNull String actionName, @NotNull Collection<BLoginKey> targets) {
		return new TransmitOnlineSpec(online, senderAccount, senderClientId, actionName, targets);
	}
}
