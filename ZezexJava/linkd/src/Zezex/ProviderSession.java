package Zezex;

import java.util.*;

public class ProviderSession {
	/** 
	 维护此Provider上绑定的LinkSession，用来在Provider关闭的时候，进行 UnBind。
	 moduleId -> LinkSids
	 多线程：主要由LinkSession回调.  需要保护。
	 
	*/
	private HashMap<Integer, HashSet<Long>> LinkSessionIds = new HashMap<Integer, HashSet<Long>> ();
	public final HashMap<Integer, HashSet<Long>> getLinkSessionIds() {
		return LinkSessionIds;
	}

	/** 
	 维护此Provider上绑定的StaticBinds，用来在Provider关闭的时候，进行 UnBind。
	 同时，当此Provider第一次被选中时，所有的StaticBinds都会一起被绑定到LinkSession上，
	 多线程：这里面的数据访问都处于 lock (Zezex.App.Instance.gnet_Provider_Module.StaticBinds) 下
	 see Zezex.Provider.ModuleProvider
	*/
	private java.util.concurrent.ConcurrentHashMap<Integer, Integer> StaticBinds = new java.util.concurrent.ConcurrentHashMap<Integer, Integer> ();
	public final java.util.concurrent.ConcurrentHashMap<Integer, Integer> getStaticBinds() {
		return StaticBinds;
	}
	private Zezex.Provider.BLoad Load;
	private Zezex.Provider.BLoad getLoad() {
		return Load;
	}
	private void setLoad(Zezex.Provider.BLoad value) {
		Load = value;
	}
	private Zezex.Provider.BAnnounceProviderInfo Info;
	public final Zezex.Provider.BAnnounceProviderInfo getInfo() {
		return Info;
	}
	public final void setInfo(Zezex.Provider.BAnnounceProviderInfo value) {
		Info = value;
	}
	public final int getProposeMaxOnline() {
		return getLoad().getProposeMaxOnline();
	}
	public final int getOnline() {
		return getLoad().getOnline();
	}
	public final int getOnlineNew() {
		return getLoad().getOnlineNew();
	}

	private long SessionId;
	public final long getSessionId() {
		return SessionId;
	}

	public ProviderSession(long ssid) {
		SessionId = ssid;
	}

	public final void SetLoad(Zezex.Provider.BLoad load) {
		synchronized (getLinkSessionIds()) {
			setLoad(load.Copy()); // 复制一次吧。
		}
	}

	public final void AddLinkSession(int moduleId, long linkSessionId) {
		synchronized (getLinkSessionIds()) {
			var linkSids = LinkSessionIds.get(moduleId);
			if (null == linkSids) {
				linkSids = new HashSet<Long>();
				LinkSessionIds.put(moduleId, linkSids);
			}
			if (linkSids.add(linkSessionId)) {
				Load.setOnline(Load.getOnline() + App.Instance.getConfig().getApproximatelyLinkdCount());
				// 在真正的数据报告回来之前，临时增加统计。仅包括本linkd分配的。
				// 本来Load应该总是由Provider报告的。
				// linkd 的临时增加是为了能快速反应出报告间隔期间的分配。
			}
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