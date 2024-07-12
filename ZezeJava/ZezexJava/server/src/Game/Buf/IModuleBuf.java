package Game.Buf;

import Zeze.Hot.HotService;

public interface IModuleBuf extends HotService {
	IBufs getBufs(long roleId);

	int getCounter();

	void setCounter(int value);
}
