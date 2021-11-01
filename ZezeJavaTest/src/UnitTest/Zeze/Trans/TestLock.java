package UnitTest.Zeze.Trans;

import java.lang.ref.WeakReference;

import Zeze.Transaction.Lockey;
import Zeze.Transaction.Locks;
import Zeze.Transaction.TableKey;
import Zeze.Util.OutObject;
import Zeze.Util.WeakHashSet;
import junit.framework.TestCase;

public class TestLock extends TestCase{
	
	public final void test() {
		// DEBUG 下垃圾回收策略导致 WeakReference 不回收。
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
		assert exist2.equals(key1);

		key1 = null;
		key2 = null;
		exist1 = null;
		exist2 = null;

		demo.Module1.Key k4 = new demo.Module1.Key((short)1);
		WeakReference<demo.Module1.Key> wref = new WeakReference<demo.Module1.Key>(k4);
		k4 = null;
		for (int i = 0; i < 10; ++i) {
			System.gc();
			System.runFinalization();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (null == wref.get())
				break;
		}

		assert null == wref.get();

		demo.Module1.Key key3 = new demo.Module1.Key((short)1);
		System.out.println("test: is null.");
		assert null == keys.get(key3);
//#endif
	}

	private Zeze.Transaction.Locks Locks = new Zeze.Transaction.Locks();
	public final void test1() {
		Locks locks = Locks;

		TableKey tk1 = new TableKey("1", 1);
		TableKey tk2 = new TableKey("1", 1);

		Lockey lock1 = new Lockey(tk1);
		Lockey lock2 = new Lockey(tk2);

		assert lock1.equals(lock2);

		Lockey lock1ref = locks.Get(lock1);
		assert lock1ref.equals(lock1); // first Get. self

		Lockey lock2ref = locks.Get(lock2);
		assert lock2ref.equals(lock1); // second Get. the exist

		TableKey tk3 = new TableKey("1", 2);
		Lockey lock3 = new Lockey(tk3);
		Lockey lock3ref = locks.Get(lock3);
		assert lock3ref.equals(lock3);
		assert !(lock3ref.equals(lock1));
	}

	public final void testRecursion1() {
		/*
		TableKey tkey = new TableKey(1, 1);
		Lockey lockey = Locks.Instance.Get(tkey);
		lockey.EnterWriteLock();
		lockey.EnterReadLock();
		lockey.ExitReadLock();
		lockey.ExitWriteLock();
		*/
	}

	public final void testRecursion2() {
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