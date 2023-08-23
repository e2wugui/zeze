package Game.Equip;

import Game.Fight.IFighter;
import Zeze.Hot.HotService;

public interface IModuleEquip extends HotService {
	void calculateFighter(IFighter fighter);
	int hotHelloWorld(int oldAccess);

	// 下面的是程序状态，用于upgrade升级测试。
	long getRoleId();
	String getTimerHot();
	// 这些也是程序状态，用于验证timer，部分也是继承的。
	int increateAndGetOnlineTimerCount();
	int increateAndGetNamedTimerCount();
	int increateAndGetHotTimerCount(); // 继承的
}
