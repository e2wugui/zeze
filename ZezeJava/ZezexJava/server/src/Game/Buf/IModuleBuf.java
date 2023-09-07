package Game.Buf;

import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Hot.HotService;

public interface IModuleBuf extends HotService {
	IBufs getBufs(long roleId);
	int getCounter();
	void setCounter(int value);
}
