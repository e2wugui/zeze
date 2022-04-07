package Zeze.Arch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class LinkdProviderSession extends ProviderSession {
	/**
	 维护此Provider上绑定的LinkSession，用来在Provider关闭的时候，进行 UnBind。
	 moduleId -> LinkSids
	 多线程：主要由LinkSession回调.  需要保护。

	*/
	private final HashMap<Integer, HashSet<Long>> LinkSessionIds = new HashMap<>();
	public final HashMap<Integer, HashSet<Long>> getLinkSessionIds() {
		return LinkSessionIds;
	}

	/**
	 维护此Provider上绑定的StaticBinds，用来在Provider关闭的时候，进行 UnBind。
	 同时，当此Provider第一次被选中时，所有的StaticBinds都会一起被绑定到LinkSession上，
	 多线程：这里面的数据访问都处于 lock (Zezex.App.Instance.gnet_Provider_Module.StaticBinds) 下
	 see Zezex.Provider.ModuleProvider
	*/
	private final ConcurrentHashMap<Integer, Integer> StaticBinds = new ConcurrentHashMap<>();
	public final ConcurrentHashMap<Integer, Integer> getStaticBinds() {
		return StaticBinds;
	}

	public LinkdProviderSession(long ssid) {
		super(ssid);
	}

	public final void AddLinkSession(int moduleId, long linkSessionId) {
		synchronized (LinkSessionIds) {
			LinkSessionIds.computeIfAbsent(moduleId, __ -> new HashSet<>()).add(linkSessionId);
		}
	}

	public final void RemoveLinkSession(int moduleId, long linkSessionId) {
		synchronized (LinkSessionIds) {
			var linkSids= LinkSessionIds.get(moduleId);
			if (null != linkSids) {
				if (linkSids.remove(linkSessionId)) {
					// 下线时Provider会进行统计，这里避免二次计数，
					// 没有扣除不会有问题，本来Load应该总是由Provider报告的。
					//--Load.Online;
					if (linkSids.isEmpty()) {
						LinkSessionIds.remove(moduleId);
					}
				}
			}
		}
	}
}
