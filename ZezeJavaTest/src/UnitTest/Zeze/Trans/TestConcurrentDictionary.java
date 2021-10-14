package UnitTest.Zeze.Trans;

import UnitTest.*;

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestClass] public class TestConcurrentDictionary
public class TestConcurrentDictionary {
//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void TestRemoveInForeach()
	public final void TestRemoveInForeach() {
		java.util.concurrent.ConcurrentHashMap<Integer, Integer> cd = new java.util.concurrent.ConcurrentHashMap<Integer, Integer>();

//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		cd.TryAdd(1, 1);
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		cd.TryAdd(2, 2);
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		cd.TryAdd(3, 3);
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		cd.TryAdd(4, 4);
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		cd.TryAdd(5, 5);

		int i = 6;
		for (var e : cd) {
			if (e.Key < 3) {
				TValue _;
				tangible.OutObject<Integer> tempOut__ = new tangible.OutObject<Integer>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
				cd.TryRemove(e.Key, tempOut__);
			_ = tempOut__.outArgValue;
				System.out.println("remove key=" + e.Key);
			}
			else {
				if (i < 10) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
					cd.TryAdd(i, i);
					++i;
				}
				System.out.println("key=" + e.Key);
			}
		}
	}
}