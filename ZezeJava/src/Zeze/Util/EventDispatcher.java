package Zeze.Util;

import Zeze.*;

/** 
 这个类恐怕没什么用。写在这里主要是为了一个建议：
 即事件应该在新的事务中执行。不要嵌套到触发者的事务中，否则可能无法控制。
*/
public class EventDispatcher {
	private java.util.concurrent.ConcurrentHashMap<String, tangible.Func2Param<Object, tangible.EventArgs, Integer>> Handles = new java.util.concurrent.ConcurrentHashMap<String, tangible.Func2Param<Object, tangible.EventArgs, Integer>> ();
	private java.util.concurrent.ConcurrentHashMap<String, tangible.Func2Param<Object, tangible.EventArgs, Integer>> getHandles() {
		return Handles;
	}


	public final void AddEventHandle(Func<Object, EventArgs, Integer> handle) {
		AddEventHandle(handle, null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void AddEventHandle(Func<object, EventArgs, int> handle, string name = null)
	public final void AddEventHandle(tangible.Func2Param<Object, tangible.EventArgs, Integer> handle, String name) {
		if (name.equals(null)) {
			name = handle.Method.Name;
		}
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (false == getHandles().TryAdd(name, handle)) {
			throw new RuntimeException(String.format("Handle for '%1$s' exist.", name));
		}
	}


	public final void RemoveEventHandle(Func<Object, EventArgs, Integer> handle) {
		RemoveEventHandle(handle, null);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void RemoveEventHandle(Func<object, EventArgs, int> handle, string name = null)
	public final void RemoveEventHandle(tangible.Func2Param<Object, tangible.EventArgs, Integer> handle, String name) {
		if (name.equals(null)) {
			name = handle.Method.Name;
		}
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getHandles().TryRemove(KeyValuePair.Create(name, handle));
	}

	public final void Dispatch(Application app, Object sender, tangible.EventArgs args) {
		for (var e : getHandles()) {
			Zeze.Util.Task.Run(app.NewProcedure(() -> e.Value(sender, args), e.Key));
		}
	}
}