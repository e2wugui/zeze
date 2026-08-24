package Zeze.Game;

import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.List;

public sealed interface OnlineSpec permits RoleOnlineSpec, RolesOnlineSpec,
	AllOnlinesSpec, ReliableOnlineSpec, TransmitOnlineSpec {
	static @NotNull RoleOnlineSpec ofRole(@NotNull Online online, long roleId) {
		return new RoleOnlineSpec(online, roleId);
	}

	static @NotNull RolesOnlineSpec ofRoles(@NotNull Online online, Collection<Long> roleIds) {
		return new RolesOnlineSpec(online, roleIds);
	}

	static @NotNull AllOnlinesSpec ofAllOnline(@NotNull Online online, long roleId) {
		return new AllOnlinesSpec(online, roleId);
	}

	static @NotNull AllOnlinesSpec ofAllOnline(@NotNull Online online, Collection<Long> roleIds) {
		return new AllOnlinesSpec(online, roleIds);
	}

	static @NotNull TransmitOnlineSpec ofTransmit(@NotNull Online online,
												  long sender, @NotNull String actionName, long target) {
		return ofTransmit(online, sender, actionName, List.of(target));
	}

	static @NotNull TransmitOnlineSpec ofTransmit(@NotNull Online online,
												  long sender, @NotNull String actionName, @NotNull Iterable<Long> targets) {
		return new TransmitOnlineSpec(online, sender, actionName, targets);
	}

	static @NotNull ReliableOnlineSpec ofReliableNotify(@NotNull Online online, long roleId, String listenerName) {
		return new ReliableOnlineSpec(online, roleId, listenerName);
	}
}
