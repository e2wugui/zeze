package Zeze.Game;

import java.util.Collection;
import java.util.Set;

import Zeze.Net.Binary;

/**
 * 在线发送目标：负责将已编码协议立即投递到指定目标（事务时机由 {@link OnlineSpec} 控制）。
 */
sealed interface OnlineTarget {

	/** 立即投递协议到本目标，返回发送数（空目标返回 0，AllRoles 恒返回 -1）。 */
	int send(Online online, long typeId, Binary data, boolean quietWhenAbsent);

	/** 返回用于日志输出的投递目标描述。 */
	String describe(Online online);

	/** 是否为空目标（调用方可在编码前短路跳过）。 */
	default boolean isEmpty() {
		return false;
	}

	static int dispatch(Online online, Collection<Long> roleIds, long typeId,
						Binary data, boolean quietWhenAbsent) {
		var size = roleIds.size();
		if (size == 0)
			return 0;
		if (size == 1)
			return online.sendDirect(roleIds.iterator().next(), typeId, data, quietWhenAbsent) ? 1 : 0;
		return online.sendDirect(roleIds, typeId, data, quietWhenAbsent);
	}

	/** 单个角色。 */
	record Role(long roleId) implements OnlineTarget {
		@Override
		public int send(Online online, long typeId, Binary data, boolean quietWhenAbsent) {
			return online.sendDirect(roleId, typeId, data, quietWhenAbsent) ? 1 : 0;
		}

		@Override
		public String describe(Online online) {
			return roleId + "@" + online.getOnlineSetName();
		}
	}

	/** 多个角色（构造时去重并快照）。 */
	record Roles(Collection<Long> roleIds) implements OnlineTarget {
		public Roles {
			roleIds = Set.copyOf(roleIds); // 快照避免延迟提交发送时受外部修改影响
		}

		@Override
		public int send(Online online, long typeId, Binary data, boolean quietWhenAbsent) {
			return dispatch(online, roleIds, typeId, data, quietWhenAbsent);
		}

		@Override
		public boolean isEmpty() {
			return roleIds.isEmpty();
		}

		@Override
		public String describe(Online online) {
			return joinIds(roleIds) + "@" + online.getOnlineSetName();
		}
	}

	/** 跨所有 OnlineSet 广播（构造时去重并快照）。 */
	record AllRoles(Collection<Long> roleIds) implements OnlineTarget {
		public AllRoles {
			roleIds = Set.copyOf(roleIds);
		}

		@Override
		public int send(Online online, long typeId, Binary data, boolean quietWhenAbsent) {
			online.getProviderWithOnline().foreachOnline(o -> dispatch(o, roleIds, typeId, data, quietWhenAbsent));
			return -1;
		}

		@Override
		public boolean isEmpty() {
			return roleIds.isEmpty();
		}

		@Override
		public String describe(Online online) {
			return joinIds(roleIds) + "@*";
		}
	}

	/** 可靠通知目标。 */
	record Reliable(long roleId, String listenerName) implements OnlineTarget {
		@Override
		public int send(Online online, long typeId, Binary data, boolean quietWhenAbsent) {
			online.sendReliableNotifyDirect(roleId, listenerName, typeId, data, quietWhenAbsent);
			return 1;
		}

		@Override
		public String describe(Online online) {
			return roleId + ":" + listenerName + "@" + online.getOnlineSetName();
		}
	}

	private static String joinIds(Collection<Long> roleIds) {
		var sb = new StringBuilder();
		for (var id : roleIds)
			sb.append(id).append(',');
		if (!sb.isEmpty())
			sb.setLength(sb.length() - 1);
		return sb.toString();
	}
}
