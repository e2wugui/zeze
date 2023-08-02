package Game.LongSet;

import java.util.Map;
import java.util.function.Predicate;
import Zeze.Hot.HotService;

public interface IModuleLongSet extends HotService {
	boolean add(String name, NameValue value);
	boolean remove(String name, NameValue value);
	void clear(String name);
	void foreach(String name, Predicate<Map.Entry<NameValue, Timestamp>> func1);
	void foreach(long first, long last, Predicate<Map.Entry<NameValue, Timestamp>> func1);
}
