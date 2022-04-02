package Zeze.Arch;

import java.util.HashMap;
import java.util.HashSet;
import Zeze.Beans.Provider.*;

public class LinkdProviderSession {
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
	private volatile BLoad Load;
	private  BAnnounceProviderInfo Info;
	public final BAnnounceProviderInfo getInfo() {
		return Info;
	}
	public final void setInfo(BAnnounceProviderInfo value) {
		Info = value;
	}
	public final int getProposeMaxOnline() {
		return Load.getProposeMaxOnline();
	}
	public final int getOnline() {
		return Load.getOnline();
	}
	public final int getOnlineNew() {
		return Load.getOnlineNew();
	}

	private long SessionId;
	public final long getSessionId() {
		return SessionId;
	}

	public LinkdProviderSession(long ssid) {
		SessionId = ssid;
	}

	public final void SetLoad(BLoad load) {
		synchronized (getLinkSessionIds()) {
			Load = load.Copy(); // 复制一次吧。
		}
	}

	public final void AddLinkSession(int moduleId, long linkSessionId) {
		synchronized (getLinkSessionIds()) {
			var linkSids = LinkSessionIds.get(moduleId);
			if (null == linkSids) {
				linkSids = new HashSet<Long>();
				LinkSessionIds.put(moduleId, linkSids);
			}
			linkSids.add(linkSessionId);
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