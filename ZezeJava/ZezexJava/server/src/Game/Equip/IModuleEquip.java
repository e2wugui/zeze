package Game.Equip;

import Game.Fight.IFighter;
import Zeze.Hot.HotService;

public interface IModuleEquip extends HotService {
	void calculateFighter(IFighter fighter);
	int hotHelloworld();
}
