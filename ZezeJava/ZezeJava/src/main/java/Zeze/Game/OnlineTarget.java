package Zeze.Game;

import java.util.Collection;
import java.util.Set;

import Zeze.Net.Binary;
import org.jetbrains.annotations.NotNull;

/**
 * 在线发送目的地（包私有实现细节，OnlineSpec 的唯一变化点）。
 * 每种目的地只负责一件事：把编码好的协议投递到自己（立即语义；事务时机由 OnlineSpec 决定）。
 */
sealed interface OnlineTarget {

	/** 把编码好的协议投递到本目标，立即执行。返回发送数（仅供内部参考，OnlineSpec 不使用）。 */
	int send(@NotNull Online online, long typeId, @NotNull Binary data, boolean trying);

	/** 协议日志标识。需要 online 是因为日志包含 onlineSetName。 */
	@NotNull String describe(@NotNull Online online);

	/** 空目标：OnlineSpec 在编码前短路（对齐旧 API 空集合直接返回、不编码的行为）。 */
	default boolean isEmpty() {
		return false;
	}

	/**
	 * 0/1/N 分派，Roles 与 AllRoles 共用，全包唯一一份。
	 * 前置条件：roleIds 是不可变快照（各 record 规范构造器 Set.copyOf 保证），
	 * 因此 size==1 时必有元素，无需旧 API 针对调用方活集合的 hasNext 防御。
	 */
	static int dispatch(@NotNull Online online, @NotNull Collection<Long> roleIds, long typeId,
						@NotNull Binary data, boolean trying) {
		var size = roleIds.size();
		if (size == 0)
			return 0;
		if (size == 1)
			return online.sendDirect(roleIds.iterator().next(), typeId, data, trying) ? 1 : 0;
		return online.sendDirect(roleIds, typeId, data, trying);
	}

	/** 单个角色。 */
	record Role(long roleId) implements OnlineTarget {
		@Override
		public int send(@NotNull Online online, long typeId, @NotNull Binary data, boolean trying) {
			return online.sendDirect(roleId, typeId, data, trying) ? 1 : 0;
		}

		@Override
		public @NotNull String describe(@NotNull Online online) {
			return roleId + "@" + online.getOnlineSetName();
		}
	}

	/** 多个角色。构造时快照（去重）。 */
	record Roles(@NotNull Collection<Long> roleIds) implements OnlineTarget {
		public Roles { // 接口内嵌套类型隐式 public，规范构造器必须显式 public
			roleIds = Set.copyOf(roleIds); // 快照：发送（可能在 commit 时）不受调用方后续改集合影响
		}

		@Override
		public int send(@NotNull Online online, long typeId, @NotNull Binary data, boolean trying) {
			return dispatch(online, roleIds, typeId, data, trying);
		}

		@Override
		public boolean isEmpty() {
			return roleIds.isEmpty();
		}

		@Override
		public @NotNull String describe(@NotNull Online online) {
			return joinIds(roleIds) + "@" + online.getOnlineSetName();
		}
	}

	/** 跨所有 OnlineSet 广播。构造时快照（去重）。 */
	record AllRoles(@NotNull Collection<Long> roleIds) implements OnlineTarget {
		public AllRoles { // 接口内嵌套类型隐式 public，规范构造器必须显式 public
			roleIds = Set.copyOf(roleIds);
		}

		@Override
		public int send(@NotNull Online online, long typeId, @NotNull Binary data, boolean trying) {
			// P0 回归点：必须用 lambda 参数 o 投递，禁止引用外层任何名为 online 的引用。
			// 历史 bug（原 AbstractOnlineSpec.sendAll(Collection)）：lambda 遮蔽后误用 this，
			// 导致 N 个 OnlineSet 下同一实例重复发 N 次、其它 set 收不到。
			online.getProviderWithOnline().foreachOnline(o -> dispatch(o, roleIds, typeId, data, trying));
			return -1; // 跨集合发送数无意义
		}

		@Override
		public boolean isEmpty() {
			return roleIds.isEmpty();
		}

		@Override
		public @NotNull String describe(@NotNull Online online) {
			return joinIds(roleIds) + "@*"; // @* 表示全 OnlineSet 广播
		}
	}

	/** 可靠通知（Game 底层 sendReliableNotifyDirect 支持 trySend）。 */
	record Reliable(long roleId, @NotNull String listenerName) implements OnlineTarget {
		@Override
		public int send(@NotNull Online online, long typeId, @NotNull Binary data, boolean trying) {
			online.sendReliableNotifyDirect(roleId, listenerName, typeId, data, trying);
			return 1;
		}

		@Override
		public @NotNull String describe(@NotNull Online online) {
			return roleId + ":" + listenerName + "@" + online.getOnlineSetName();
		}
	}

	private static @NotNull String joinIds(@NotNull Collection<Long> roleIds) {
		var sb = new StringBuilder();
		for (var id : roleIds)
			sb.append(id).append(',');
		if (sb.length() > 0)
			sb.setLength(sb.length() - 1);
		return sb.toString();
	}
}
