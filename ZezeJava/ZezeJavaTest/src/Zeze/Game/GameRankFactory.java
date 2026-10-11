package Zeze.Game;

import Zeze.AppBase;
import Zeze.Arch.ProviderApp;
import Zeze.Application;

/**
 * GameSim 专用 Rank 模块本地工厂：基类直构（非 Rank.create 的 redirect 生成子类——
 * 裸环境无运行中的 redirect 服务，@RedirectHash 方法会被路由拦截，写不进本地表；
 * 先例 TestRankCountNeedKey.newRank 同型）。
 */
public final class GameRankFactory {
	private GameRankFactory() {
	}

	public static Rank createLocal(Application zeze) {
		new ProviderApp(zeze); // Initialize→RegisterProtocols 需要 redirect.providerApp 附着
		var app = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		var rank = new Rank(app); // protected 构造：本工厂与被测同包
		rank.Initialize(app);
		return rank;
	}
}
