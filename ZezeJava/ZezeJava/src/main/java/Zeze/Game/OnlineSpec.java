package Zeze.Game;

import org.jetbrains.annotations.NotNull;
import java.util.Collection;

public sealed interface OnlineSpec permits RoleOnlineSpec, RolesOnlineSpec, AllOnlineSpec, ReliableOnlineSpec {
	static @NotNull RoleOnlineSpec ofRole(@NotNull Online online, long roleId) {
		return new RoleOnlineSpec(online, roleId);
	}

	static @NotNull RolesOnlineSpec ofRole(@NotNull Online online, Collection<Long> roleIds) {
		return new RolesOnlineSpec(online, roleIds);
	}

	static @NotNull AllOnlineSpec ofAllOnline(@NotNull Online online, long roleId) {
		return new AllOnlineSpec(online, roleId);
	}

	static @NotNull AllOnlineSpec ofAllOnline(@NotNull Online online, Collection<Long> roleId) {
		return new AllOnlineSpec(online, roleId);
	}

	static @NotNull ReliableOnlineSpec ofReliable(@NotNull Online online, long roleId, String listenerName) {
		return new ReliableOnlineSpec(online, roleId, listenerName);
	}
}
