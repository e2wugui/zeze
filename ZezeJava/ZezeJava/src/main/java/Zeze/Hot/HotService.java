package Zeze.Hot;

import Zeze.AppBase;

public interface HotService {
	void start() throws Exception;
	void stop() throws Exception;

	/////////////////////////////////////////////////////////////////

	// 用于升级有状态服务
	void upgrade(HotService old) throws Exception;

	// 当缓存了别的模块创建的数据，“别的”模块更新以后，通过这个方法通知刷新要求。
	//void refresh(HotService cur);
}
