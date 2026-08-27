package Zeze.Arch;

import java.util.Collection;
import java.util.Set;

import Zeze.Builtin.ProviderDirect.BLoginKey;
import Zeze.Net.Binary;
import org.jetbrains.annotations.NotNull;

/**
 * 在线发送目的地（包私有实现细节，OnlineSpec 的唯一变化点）。
 * 每种目的地只负责一件事：把编码好的协议投递到自己（立即语义；事务时机由 OnlineSpec 决定）。
 */
sealed interface OnlineTarget {

	/** 把编码好的协议投递到本目标，立即执行。返回发送数（仅供内部参考，OnlineSpec 不使用）。 */
	int send(@NotNull Online online, long typeId, @NotNull Binary data, boolean trying);

	/** 协议日志标识。Arch 无 OnlineSet 概念，不需要 online 参数。 */
	@NotNull String describe();

	/** 空目标：OnlineSpec 在编码前短路（对齐旧 API 空集合直接返回、不编码的行为）。 */
	default boolean isEmpty() {
		return false;
	}

	/**
	 * 0/1/N 分派：登录端点维度，全包唯一一份。
	 * 前置条件：logins 是不可变快照（Logins 规范构造器 Set.copyOf 保证），
	 * 因此 size==1 时必有元素，无需旧 API 针对调用方活集合的 hasNext 防御。
	 */
	static int dispatchLogins(@NotNull Online online, @NotNull Set<BLoginKey> logins, long typeId,
							  @NotNull Binary data, boolean trying) {
		var size = logins.size();
		if (size == 0)
			return 0;
		if (size == 1) {
			var login = logins.iterator().next();
			return online.sendDirect(login.getAccount(), login.getClientId(), typeId, data, trying) ? 1 : 0;
		}
		return online.sendDirect(logins, typeId, data, trying);
	}

	/**
	 * 0/1/N 分派：账号维度，全包唯一一份。
	 * 前置条件同 dispatchLogins（Accounts 规范构造器保证不可变快照）。
	 */
	static int dispatchAccounts(@NotNull Online online, @NotNull Collection<String> accounts, long typeId,
								@NotNull Binary data, boolean trying) {
		var size = accounts.size();
		if (size == 0)
			return 0;
		if (size == 1)
			return online.sendAccountDirect(accounts.iterator().next(), typeId, data, trying);
		return online.sendAccountsDirect(accounts, typeId, data, trying);
	}

	/** 单个登录端点。 */
	record Login(@NotNull String account, @NotNull String clientId) implements OnlineTarget {
		@Override
		public int send(@NotNull Online online, long typeId, @NotNull Binary data, boolean trying) {
			return online.sendDirect(account, clientId, typeId, data, trying) ? 1 : 0;
		}

		@Override
		public @NotNull String describe() {
			return account + ',' + clientId;
		}
	}

	/** 多个登录端点。构造时快照（去重）。 */
	record Logins(@NotNull Set<BLoginKey> logins) implements OnlineTarget {
		public Logins { // 接口内嵌套类型隐式 public，规范构造器必须显式 public
			logins = Set.copyOf(logins); // 快照：发送（可能在 commit 时）不受调用方后续改集合影响
		}

		/** Collection 直传（工厂路径）。Set.copyOf 转换后规范构造器是零拷贝别名，全程只分配一次。 */
		public Logins(@NotNull Collection<BLoginKey> logins) {
			this(Set.copyOf(logins));
		}

		@Override
		public int send(@NotNull Online online, long typeId, @NotNull Binary data, boolean trying) {
			return dispatchLogins(online, logins, typeId, data, trying);
		}

		@Override
		public boolean isEmpty() {
			return logins.isEmpty();
		}

		@Override
		public @NotNull String describe() {
			var sb = new StringBuilder();
			for (var login : logins)
				sb.append(login.getAccount()).append(',').append(login.getClientId()).append(';');
			if (!sb.isEmpty())
				sb.setLength(sb.length() - 1);
			return sb.toString();
		}
	}

	/** 单个账号（所有登录终端）。 */
	record Account(@NotNull String account) implements OnlineTarget {
		@Override
		public int send(@NotNull Online online, long typeId, @NotNull Binary data, boolean trying) {
			return online.sendAccountDirect(account, typeId, data, trying);
		}

		@Override
		public @NotNull String describe() {
			return account;
		}
	}

	/** 多个账号（所有登录终端）。构造时快照（去重）。 */
	record Accounts(@NotNull Collection<String> accounts) implements OnlineTarget {
		public Accounts { // 接口内嵌套类型隐式 public，规范构造器必须显式 public
			accounts = Set.copyOf(accounts);
		}

		@Override
		public int send(@NotNull Online online, long typeId, @NotNull Binary data, boolean trying) {
			return dispatchAccounts(online, accounts, typeId, data, trying);
		}

		@Override
		public boolean isEmpty() {
			return accounts.isEmpty();
		}

		@Override
		public @NotNull String describe() {
			return String.join(",", accounts);
		}
	}

	/** 可靠通知（Arch 底层 sendReliableNotifyDirect 不支持 trySend，trying 参数被忽略）。 */
	record Reliable(@NotNull String account, @NotNull String clientId,
					@NotNull String listenerName) implements OnlineTarget {
		@Override
		public int send(@NotNull Online online, long typeId, @NotNull Binary data, boolean trying) {
			online.sendReliableNotifyDirect(account, clientId, listenerName, typeId, data);
			return 1;
		}

		@Override
		public @NotNull String describe() {
			return account + ',' + clientId + ':' + listenerName;
		}
	}
}
