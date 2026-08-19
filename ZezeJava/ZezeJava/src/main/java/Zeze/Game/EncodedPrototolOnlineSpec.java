package Zeze.Game;

import Zeze.Net.Binary;
import Zeze.Transaction.Transaction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public sealed class EncodedPrototolOnlineSpec extends AbstractOnlineSpec implements OnlineSpec permits ProtocolOnlineSpec {
	protected long typeId;
	protected Binary fullEncodedProtocol;

	EncodedPrototolOnlineSpec(long typeId, Binary fullEncodedProtocol) {
		this.typeId = typeId;
		this.fullEncodedProtocol = fullEncodedProtocol;
	}

	public EncodedPrototolOnlineSpec role(long roleId) {
		roleIds.add(roleId);
		return this;
	}

	public EncodedPrototolOnlineSpec roles(Iterable<Long> roleIds) {
		roleIds.forEach(this.roleIds::add);
		return this;
	}

	public EncodedPrototolOnlineSpec roles(Collection<Long> roleIds) {
		this.roleIds.addAll(roleIds);
		return this;
	}

	public EncodedPrototolOnlineSpec trying() {
		this.trySend = true;
		return this;
	}

	public EncodedPrototolOnlineSpec withContext() {
		this.withContext = true;
		return this;
	}

	public EncodedPrototolOnlineSpec reliable() {
		this.reliable = true;
		return this;
	}

	public EncodedPrototolOnlineSpec listener(String listenerName) {
		this.listenerName = listenerName;
		return this;
	}

	public int send(@NotNull Online online) {
		if (roleIds.isEmpty()) {
			throw new IllegalArgumentException("roleIds cannot be empty");
		}

		if (reliable) {
			if (roleIds.size() != 1) {
				throw new IllegalArgumentException("roleIds cannot be multiple");
			}
			if (null == listenerName || listenerName.isBlank()) {
				throw new IllegalArgumentException("listenerName cannot be empty");
			}
			var t = Transaction.getCurrent();
			if (t != null && t.isRunning()) {
				t.runWhileCommit(() -> sendReliableNotifyDirect(withContext(online), typeId, fullEncodedProtocol));
			}
			return sendReliableNotifyDirect(withContext(online), typeId, fullEncodedProtocol);
		} else {
			var t = Transaction.getCurrent();
			if (t != null && t.isRunning()) {
				t.runWhileCommit(() -> sendDirect(withContext(online), typeId, fullEncodedProtocol));
			}
			return sendDirect(withContext(online), typeId, fullEncodedProtocol);
		}
	}
}
