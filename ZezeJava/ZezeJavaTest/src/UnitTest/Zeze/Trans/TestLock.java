package UnitTest.Zeze.Trans;

import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import Zeze.Transaction.Lockey;
import Zeze.Transaction.Locks;
import Zeze.Transaction.TableKey;
import Zeze.Util.WeakHashSet;
import junit.framework.TestCase;
import org.junit.Assert;

public class TestLock extends TestCase {

	@SuppressWarnings("UnusedAssignment")
	public final void test() {
		// DEBUG 下垃圾回收策略导致 WeakReference 不回收。
//#if RELEASE
		WeakHashSet<demo.Module1.Key> keys = new WeakHashSet<>();
		demo.Module1.Key key1 = new demo.Module1.Key((short)1, "");
		demo.Module1.Key key2 = new demo.Module1.Key((short)1, "");

		Assert.assertNull(keys.get(key1));
		keys.add(key1);

		demo.Module1.Key exist1 = keys.get(key1);
		Assert.assertNotNull(exist1);
		Assert.assertEquals(exist1, key1);

		demo.Module1.Key exist2 = keys.get(key2);
		Assert.assertNotNull(exist2);
		Assert.assertEquals(exist2, key1);

		key1 = null;
		key2 = null;
		exist1 = null;
		exist2 = null;

		demo.Module1.Key k4 = new demo.Module1.Key((short)1, "");
		WeakReference<demo.Module1.Key> wref = new WeakReference<>(k4);
		k4 = null;
		for (int i = 0; i < 10; ++i) {
			System.gc();
			// System.runFinalization();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				//noinspection CallToPrintStackTrace
				e.printStackTrace();
			}

			if (null == wref.get())
				break;
		}

		Assert.assertNull(wref.get());

		demo.Module1.Key key3 = new demo.Module1.Key((short)1, "");
		System.out.println("test: is null.");
		Assert.assertNull(keys.get(key3));
//#endif
	}

	private final Locks Locks = new Locks();

	public final void test1() {
		Locks locks = Locks;

		TableKey tk1 = new TableKey(1, 1);
		TableKey tk2 = new TableKey(1, 1);

		Lockey lock1 = new Lockey(tk1);
		Lockey lock2 = new Lockey(tk2);

		Assert.assertEquals(lock1, lock2);

		Lockey lock1ref = locks.get(lock1);
		Assert.assertEquals(lock1ref, lock1); // first Get. self

		Lockey lock2ref = locks.get(lock2);
		Assert.assertEquals(lock2ref, lock1); // second Get. the exist

		TableKey tk3 = new TableKey(1, 2);
		Lockey lock3 = new Lockey(tk3);
		Lockey lock3ref = locks.get(lock3);
		Assert.assertEquals(lock3ref, lock3);
		Assert.assertNotEquals(lock3ref, lock1);
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

	public final void testRwlock() {
		var rw = new ReentrantReadWriteLock();
		rw.readLock().lock();
		//rw.writeLock().lock(); // 会死锁。java没有对这种情况报错。
		//rw.writeLock().unlock();
		rw.readLock().unlock();
	}
}
