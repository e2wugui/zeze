package Game.Fight;

import Zeze.Hot.HotService;

public interface IModuleFight extends HotService {
	void StartCalculateFighter(long roleId);
	boolean isAreYouFightDone();
	void setAreYouFightResult(boolean value);
}
