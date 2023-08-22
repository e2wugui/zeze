package Game.Equip;

import Game.Fight.IFighter;
import Zeze.Hot.HotService;

public interface IModuleEquip extends HotService {
	void calculateFighter(IFighter fighter);
	int hotHelloWorld(int oldAccess);

	// 下面的是程序状态，用于upgrade升级测试。
	long getRoleId();
	String getTimerHot();
}
