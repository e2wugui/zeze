package UnitTest.Zeze.Trans;

import Zeze.Transaction.*;
import Zeze.Util.*;
import UnitTest.*;

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestClass] public class TestLock
public class TestLock {
//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void Test()
	public final void Test() {
		// DEBUG 下垃圾回收策略导致 WeakReference 不回收。
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if RELEASE
		WeakHashSet<demo.Module1.Key> keys = new WeakHashSet<demo.Module1.Key>();
		demo.Module1.Key key1 = new demo.Module1.Key((short)1);
		demo.Module1.Key key2 = new demo.Module1.Key((short)1);

		assert null == keys.get(key1);
		keys.add(key1);

		demo.Module1.Key exist1 = keys.get(key1);
		assert null != exist1;
		assert exist1 == key1;

		demo.Module1.Key exist2 = keys.get(key2);
		assert null != exist2;
		assert exist2 == key1;

		key1 = null;
		key2 = null;
		exist1 = null;
		exist2 = null;

		demo.Module1.Key k4 = new demo.Module1.Key((short)1);
		WeakReference<demo.Module1.Key> wref = new WeakReference<demo.Module1.Key>(k4);
		k4 = null;
		for (int i = 0; i < 10; ++i) {
			System.gc();
			GC.WaitForFullGCComplete();
			System.runFinalization();
			Thread.sleep(200);

			T notusedk4ref;
			tangible.OutObject<demo.Module1.Key> tempOut_notusedk4ref = new tangible.OutObject<demo.Module1.Key>();
			if (false == wref.TryGetTarget(tempOut_notusedk4ref)) {
			notusedk4ref = tempOut_notusedk4ref.outArgValue;
				break;
			}
		else {
			notusedk4ref = tempOut_notusedk4ref.outArgValue;
		}
		}

		demo.Module1.Key k4ref;
		tangible.OutObject<demo.Module1.Key> tempOut_k4ref = new tangible.OutObject<demo.Module1.Key>();
		assert false == wref.TryGetTarget(tempOut_k4ref);
	k4ref = tempOut_k4ref.outArgValue;
		assert null == k4ref;

		demo.Module1.Key key3 = new demo.Module1.Key((short)1);
		System.out.println("test: is null.");
		assert null == keys.get(key3);
//#endif
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void Test1()
	public final void Test1() {
		Locks locks = Locks.Instance;

		TableKey tk1 = new TableKey(1, 1);
		TableKey tk2 = new TableKey(1, 1);

		Lockey lock1 = new Lockey(tk1);
		Lockey lock2 = new Lockey(tk2);

		assert lock1 == lock2;

		Lockey lock1ref = locks.Get(lock1);
		assert lock1ref == lock1; // first Get. self

		Lockey lock2ref = locks.Get(lock2);
		assert lock2ref == lock1; // second Get. the exist

		TableKey tk3 = new TableKey(1, 2);
		Lockey lock3 = new Lockey(tk3);
		Lockey lock3ref = locks.Get(lock3);
		assert lock3ref == lock3;
		assert!lock3ref == lock1;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void TestRecursion1()
	public final void TestRecursion1() {
		/*
		TableKey tkey = new TableKey(1, 1);
		Lockey lockey = Locks.Instance.Get(tkey);
		lockey.EnterWriteLock();
		lockey.EnterReadLock();
		lockey.ExitReadLock();
		lockey.ExitWriteLock();
		*/
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [TestMethod] public void TestRecursion2()
	public final void TestRecursion2() {
		/*
		TableKey tkey = new TableKey(1, 1);
		Lockey lockey = Locks.Instance.Get(tkey);
		lockey.EnterReadLock();
		lockey.EnterWriteLock();
		lockey.ExitWriteLock();
		lockey.ExitReadLock();
		*/
	}
}