package Zeze.Util;

import Zeze.*;

/** 
 这个类恐怕没什么用。写在这里主要是为了一个建议：
 即事件应该在新的事务中执行。不要嵌套到触发者的事务中，否则可能无法控制。
*/
public class EventDispatcher {

	@FunctionalInterface
	public static interface EventHandle {
		int invoke(Object sender, EventArg arg);
	}

	public static class EventArg {
		public static EventArg Empty = new EventArg();
	}

	private java.util.concurrent.ConcurrentHashMap<String, EventHandle> Handles
		= new java.util.concurrent.ConcurrentHashMap<String, EventHandle> ();

	public final void AddEventHandle(EventHandle handle) {
		AddEventHandle(handle, null);
	}

	public final void AddEventHandle(EventHandle handle, String name) {
		if (name == null) {
			name = handle.getClass().getName();
		}
		if (null != Handles.putIfAbsent(name, handle)) {
			throw new RuntimeException(String.format("Handle for '%1$s' exist.", name));
		}
	}


	public final void RemoveEventHandle(EventHandle handle) {
		RemoveEventHandle(handle, null);
	}

	public final void RemoveEventHandle(EventHandle handle, String name) {
		if (name == null) {
			name = handle.getClass().getName();
		}
		Handles.remove(name, handle);
	}

	public final void Dispatch(Application app, Object sender, EventArg arg) {
		for (var e : Handles.entrySet()) {
			Zeze.Util.Task.Run(app.NewProcedure(() -> e.getValue().invoke(sender, arg), e.getKey()));
		}
	}
}