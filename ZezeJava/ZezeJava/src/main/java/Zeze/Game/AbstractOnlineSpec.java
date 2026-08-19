package Zeze.Game;

import Zeze.Net.Binary;

import java.util.HashSet;
import java.util.Set;

abstract class AbstractOnlineSpec {
	Set<Long> roleIds = new HashSet<>();
	boolean trySend;
	boolean withContext;
	boolean reliable;
	String listenerName;

	Online withContext(Online online) {
		return withContext ? online.getOnlineByContext() : online;
	}

	int sendDirect(Online online, long typeId, Binary protocol) {
		if (roleIds.size() == 1) {
			return online.sendDirect(roleIds.iterator().next(), typeId, protocol, trySend) ? 0 : -1;
		} else {
			return online.sendDirect(roleIds, typeId, protocol, trySend);
		}
	}

	int sendReliableNotifyDirect(Online online, long typeId, Binary protocol) {
		online.sendReliableNotifyDirect(roleIds.iterator().next(), listenerName, typeId, protocol);
		return 0;
	}

	String logName(Online online) {
		return roleIds + ":" + listenerName + "@" + online.getOnlineSetName();
	}
}
