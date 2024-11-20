package UnitTest.Zeze.Trans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import Zeze.Transaction.Changes;
import Zeze.Transaction.Collections.LogMap1;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogSet1;
import Zeze.Transaction.Log;
import demo.Module1.BSimple;
import demo.Module2.BValue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import Zeze.Net.Binary;
import Zeze.Transaction.Bean;
import Zeze.Transaction.ChangeListener;
import Zeze.Transaction.Procedure;

public class TestChangeListener {
	@Before
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Exception {
		//demo.App.getInstance().Stop();
	}

	private static void prepare() {
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			demo.App.getInstance().demo_Module1.getTable1().remove(1L);
			return Procedure.Success;
		}, "TestChangeListener.Remove").call());

		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			demo.Module1.BValue value = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(1L);
			value.setInt_1(123);
			value.setLong2(123);
			value.setString3("123");
			value.setBool4(true);
			value.setShort5((short)123);
			value.setFloat6(123);
			value.setDouble7(123);
			value.setBytes8(Binary.Empty);

			demo.Bean1 tempVar = new demo.Bean1();
			tempVar.setV1(1);
			value.getList9().add(tempVar);
			demo.Bean1 tempVar2 = new demo.Bean1();
			tempVar2.setV1(2);
			value.getList9().add(tempVar2);
			value.getSet10().add(123);
			value.getSet10().add(124);
			value.getMap11().put(1L, new BValue());
			value.getMap11().put(2L, new BValue());
			value.getBean12().setInt_1(123);
			value.setByte13((byte)12);
			value.setDynamic14(new BSimple());
			value.getDynamic14_demo_Module1_BSimple().setInt_1(123);
			value.getMap15().put(1L, 1L);
			value.getMap15().put(2L, 2L);
			return Procedure.Success;
		}, "TestChangeListener.Prepare").call());
	}

	@SuppressWarnings("OverwrittenKey")
	@Test
	public final void testAllType() {
		prepare();
		AddListener();

		init();
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			demo.Module1.BValue value = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(1L);
			value.setInt_1(124);
			value.setLong2(124);
			value.setString3("124");
			value.setBool4(true);
			value.setShort5((short)124);
			value.setFloat6(124);
			value.setDouble7(124);
			value.setBytes8(new Binary(new byte[4]));

			demo.Bean1 tempVar = new demo.Bean1();
			tempVar.setV1(1);
			value.getList9().add(tempVar);
			demo.Bean1 tempVar2 = new demo.Bean1();
			tempVar2.setV1(2);
			value.getList9().add(tempVar2);
			value.getSet10().add(124);
			value.getSet10().add(124);
			value.getMap11().put(1L, new BValue());
			value.getMap11().put(2L, new BValue());
			value.getBean12().setInt_1(124);
			value.setByte13((byte)13);
			value.setDynamic14(new BSimple());
			value.getDynamic14_demo_Module1_BSimple().setInt_1(124);
			value.getMap15().put(3L, 3L);
			value.getMap15().put(4L, 4L);

			return Procedure.Success;
		}, "TestChangeListener.Modify").call());
		verify();

		init();
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			demo.Module1.BValue value = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(1L);
			value.getSet10().add(127);
			value.getSet10().remove(124);
			value.getMap11().put(5L, new BValue());
			value.getMap11().put(6L, new BValue());
			value.getMap11().remove(1L);
			value.getMap11().remove(2L);
			value.getMap15().put(5L, 5L);
			value.getMap15().put(6L, 6L);
			value.getMap15().remove(1L);
			value.getMap15().remove(2L);
			return Procedure.Success;
		}, "TestChangeListener.ModifyCollections").call());
		verify();

		init();
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			demo.Module1.BValue value = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(1L);
			ArrayList<Integer> except = new ArrayList<>(Arrays.asList(1, 2));
			//noinspection SlowAbstractSetRemoveAll
			value.getSet10().removeAll(except);
			return Procedure.Success;
		}, "TestChangeListener.ModifySetExcept").call());
		verify();

		init();
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			demo.Module1.BValue value = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(1L);
			ArrayList<Integer> intersect = new ArrayList<>(Arrays.asList(123, 126));
			//value.getSet10().IntersectWith(intersect);
			var set10 = value.getSet10();
			List<Integer> temp = new ArrayList<>(intersect.size());
			intersect.forEach(i -> {
				if (set10.contains(i)) {
					temp.add(i);
				}
			});
			set10.clear();
			set10.addAll(temp);
			return Procedure.Success;
		}, "TestChangeListener.ModifySetIntersect").call());
		verify();

		init();
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			demo.Module1.BValue value = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(1L);
			ArrayList<Integer> SymmetricExcept = new ArrayList<>(Arrays.asList(123, 140));
			//noinspection SlowAbstractSetRemoveAll
			value.getSet10().removeAll(SymmetricExcept);
			return Procedure.Success;
		}, "TestChangeListener.ModifySetSymmetricExcept").call());
		verify();

		init();
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			demo.Module1.BValue value = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(1L);
			ArrayList<Integer> Union = new ArrayList<>(Arrays.asList(123, 140));
			//value.getSet10().UnionWith(Union);
			value.getSet10().addAll(Union);
			return Procedure.Success;
		}, "TestChangeListener.ModifySetUnion").call());
		verify();

		init();
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			demo.App.getInstance().demo_Module1.getTable1().put(1L, new demo.Module1.BValue());
			return Procedure.Success;
		}, "TestChangeListener.PutRecord").call());
		verify();

		init();
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			demo.App.getInstance().demo_Module1.getTable1().remove(1L);
			return Procedure.Success;
		}, "TestChangeListener.RemoveRecord").call());
		verify();
	}

	private demo.Module1.BValue localValue;

	private void init() {
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			demo.Module1.BValue value = demo.App.getInstance().demo_Module1.getTable1().get(1L);
			localValue = value == null ? null : value.copy();
			return Procedure.Success;
		}, "TestChangeListener.CopyLocal").call());

		_CLInt1.Init(localValue);
		_ClLong2.Init(localValue);
		_CLString3.Init(localValue);
		_CLBool4.Init(localValue);
		_CLShort5.Init(localValue);
		_CLFloat6.Init(localValue);
		_CLDouble7.Init(localValue);
		_CLBytes8.Init(localValue);
		_CLList9.Init(localValue);
		_CLSet10.Init(localValue);
		_CLMap11.Init(localValue);
		_CLBean12.Init(localValue);
		_CLByte13.Init(localValue);
		_ClDynamic14.Init(localValue);
		_CLMap15.Init(localValue);
	}

	private void verify() {
		Assert.assertEquals(Procedure.Success, demo.App.getInstance().Zeze.newProcedure(() -> {
			demo.Module1.BValue value = demo.App.getInstance().demo_Module1.getTable1().get(1L);
			localValue = value == null ? null : value.copy();
			return Procedure.Success;
		}, "TestChangeListener.CopyLocal").call());

		_CLInt1.Verify(localValue);
		_ClLong2.Verify(localValue);
		_CLString3.Verify(localValue);
		_CLBool4.Verify(localValue);
		_CLShort5.Verify(localValue);
		_CLFloat6.Verify(localValue);
		_CLDouble7.Verify(localValue);
		_CLBytes8.Verify(localValue);
		_CLList9.Verify(localValue);
		_CLSet10.Verify(localValue);
		_CLMap11.Verify(localValue);
		_CLBean12.Verify(localValue);
		_CLByte13.Verify(localValue);
		_ClDynamic14.Verify(localValue);
		_CLMap15.Verify(localValue);
	}

	private void AddListener() {
		var ls = new Listener();

		ls.Vars.put(demo.Module1.Table1.VAR_int_1, _CLInt1);
		ls.Vars.put(demo.Module1.Table1.VAR_long2, _ClLong2);
		ls.Vars.put(demo.Module1.Table1.VAR_string3, _CLString3);
		ls.Vars.put(demo.Module1.Table1.VAR_bool4, _CLBool4);
		ls.Vars.put(demo.Module1.Table1.VAR_short5, _CLShort5);
		ls.Vars.put(demo.Module1.Table1.VAR_float6, _CLFloat6);
		ls.Vars.put(demo.Module1.Table1.VAR_double7, _CLDouble7);
		ls.Vars.put(demo.Module1.Table1.VAR_bytes8, _CLBytes8);
		ls.Vars.put(demo.Module1.Table1.VAR_list9, _CLList9);
		ls.Vars.put(demo.Module1.Table1.VAR_set10, _CLSet10);
		ls.Vars.put(demo.Module1.Table1.VAR_map11, _CLMap11);
		ls.Vars.put(demo.Module1.Table1.VAR_bean12, _CLBean12);
		ls.Vars.put(demo.Module1.Table1.VAR_byte13, _CLByte13);
		ls.Vars.put(demo.Module1.Table1.VAR_dynamic14, _ClDynamic14);
		ls.Vars.put(demo.Module1.Table1.VAR_map15, _CLMap15);

		demo.App.getInstance().demo_Module1.getTable1().getChangeListenerMap().addListener(ls);
	}

	private final CLInt1 _CLInt1 = new CLInt1();
	private final ClLong2 _ClLong2 = new ClLong2();
	private final CLString3 _CLString3 = new CLString3();
	private final CLBool4 _CLBool4 = new CLBool4();
	private final CLShort5 _CLShort5 = new CLShort5();
	private final CLFloat6 _CLFloat6 = new CLFloat6();
	private final CLDouble7 _CLDouble7 = new CLDouble7();
	private final CLBytes8 _CLBytes8 = new CLBytes8();
	private final CLList9 _CLList9 = new CLList9();
	private final CLSet10 _CLSet10 = new CLSet10();
	private final CLMap11 _CLMap11 = new CLMap11();
	private final CLBean12 _CLBean12 = new CLBean12();
	private final CLByte13 _CLByte13 = new CLByte13();
	private final ClDynamic14 _ClDynamic14 = new ClDynamic14();
	private final CLMap15 _CLMap15 = new CLMap15();

	static class Listener implements ChangeListener {
		public final HashMap<Integer, VarListener> Vars = new HashMap<>();

		@Override
		public void OnChanged(Object key, Changes.Record r) {
			switch (r.getState()) {
			case Changes.Record.Remove:
				for (var var : Vars.values())
					var.OnRemoved(key);
				break;
			case Changes.Record.Put:
				for (var var : Vars.values())
					var.OnChanged(key, r.getValue());
				break;
			case Changes.Record.Edit:
				var logbean = r.getLogBean();
				for (var e : Vars.entrySet()) {
					//noinspection DataFlowIssue
					var vlog = logbean.getVariables().get(e.getKey());
					if (vlog != null)
						e.getValue().OnChanged(key, vlog);
				}
				break;
			}
		}
	}

	interface VarListener {
		void OnChanged(Object key, Bean value);

		void OnChanged(Object key, Log log);

		void OnRemoved(Object key);
	}

	@SuppressWarnings("UseBulkOperation")
	private static class CLMap15 implements VarListener {
		private HashMap<Long, Long> newValue;

		public final void Init(demo.Module1.BValue current) {
			if (null != current) {
				newValue = new HashMap<>();
				for (var e : current.getMap15().entrySet()) {
					newValue.put(e.getKey(), e.getValue());
				}
			} else {
				newValue = null;
			}
		}

		public final void Verify(demo.Module1.BValue current) {
			if (null == current) {
				Assert.assertNull(newValue);
				return;
			}
			HashMap<Long, Long> newValueCopy = new HashMap<>();
			for (var e : current.getMap15().entrySet()) {
				newValueCopy.put(e.getKey(), e.getValue());
			}
			Assert.assertEquals(newValue.size(), newValueCopy.size());
			for (var e : newValue.entrySet()) {

				Long exist = newValueCopy.get(e.getKey());
				Assert.assertTrue(newValueCopy.containsKey(e.getKey()));
				Assert.assertEquals(e.getValue(), exist);
			}
		}

		@Override
		public final void OnChanged(Object key, Bean value) {
			newValue = new HashMap<>();
			for (var e : ((demo.Module1.BValue)value).getMap15().entrySet()) {
				newValue.put(e.getKey(), e.getValue());
			}
		}

		@Override
		public final void OnChanged(Object key, Log note) {
			@SuppressWarnings("unchecked")
			var noteMap1 = (LogMap1<Long, Long>)note;

			for (var a : noteMap1.getReplaced().entrySet()) {
				newValue.put(a.getKey(), a.getValue());
			}
			for (var r : noteMap1.getRemoved()) {
				newValue.remove(r);
			}
		}

		@Override
		public final void OnRemoved(Object key) {
			newValue = null;
		}
	}

	private static class ClDynamic14 implements VarListener {
		private Bean newValue;

		public final void Init(demo.Module1.BValue current) {
			newValue = current == null ? null : current.getDynamic14().copy();
		}

		public final void Verify(demo.Module1.BValue current) {
			if (null == current) {
				Assert.assertNull(newValue);
				return;
			}
			Assert.assertEquals(newValue.typeId(), current.getDynamic14().typeId());
			if (newValue.typeId() == BSimple.TYPEID) {
				BSimple newSimple = newValue instanceof BSimple ? (BSimple)newValue : null;
				Zeze.Transaction.Bean tempVar = current.getDynamic14().getBean();
				BSimple currentSimple = tempVar instanceof BSimple ? (BSimple)tempVar : null;
				Assert.assertNotNull(newSimple);
				Assert.assertNotNull(currentSimple);
				Assert.assertEquals(newSimple.getInt_1(), currentSimple.getInt_1());
			}
		}

		@Override
		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.BValue)value).getDynamic14();
		}

		@Override
		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.BValue)note.getBelong()).getDynamic14();
		}

		@Override
		public final void OnRemoved(Object key) {
			newValue = null;
		}
	}

	private static class CLByte13 implements VarListener {
		private byte newValue;

		public final void Init(demo.Module1.BValue current) {
			newValue = (null != current) ? current.getByte13() : (byte)255;
		}

		public final void Verify(demo.Module1.BValue current) {
			if (null == current) {
				Assert.assertEquals(((byte)255), newValue);
				return;
			}
			Assert.assertEquals(newValue, current.getByte13());
		}

		@Override
		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.BValue)value).getByte13();
		}

		@Override
		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.BValue)note.getBelong()).getByte13();
		}

		@Override
		public final void OnRemoved(Object key) {
			newValue = (byte)255;
		}
	}

	private static class CLBean12 implements VarListener {
		private BSimple newValue;

		public final void Init(demo.Module1.BValue current) {
			if (null != current) {
				newValue = current.getBean12().copy();
			} else {
				newValue = null;
			}
		}

		public final void Verify(demo.Module1.BValue current) {
			if (null == current) {
				Assert.assertNull(newValue);
				return;
			}
			Assert.assertEquals(newValue.getInt_1(), current.getBean12().getInt_1());
		}

		@Override
		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.BValue)value).getBean12().copy();
		}

		@Override
		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.BValue)note.getBelong()).getBean12().copy();
		}

		@Override
		public final void OnRemoved(Object key) {
			newValue = null;
		}
	}

	@SuppressWarnings("UseBulkOperation")
	private static class CLMap11 implements VarListener {
		private HashMap<Long, BValue> newValue;

		public final void Init(demo.Module1.BValue current) {
			if (null != current) {
				newValue = new HashMap<>();
				for (var e : current.getMap11().entrySet()) {
					newValue.put(e.getKey(), e.getValue().copy());
				}
			} else {
				newValue = null;
			}
		}

		public final void Verify(demo.Module1.BValue current) {
			if (null == current) {
				Assert.assertNull(newValue);
				return;
			}
			HashMap<Long, BValue> newValueCopy = new HashMap<>();
			for (var e : current.getMap11().entrySet()) {
				newValueCopy.put(e.getKey(), e.getValue().copy());
			}
			Assert.assertEquals(newValue.size(), newValueCopy.size());
			for (var e : newValue.entrySet()) {
				BValue exist = newValueCopy.get(e.getKey());
				Assert.assertTrue(newValueCopy.containsKey(e.getKey()));
				Assert.assertEquals(e.getValue().getS(), exist.getS());
			}
		}

		@Override
		public final void OnChanged(Object key, Bean value) {
			newValue = new HashMap<>();
			for (var e : ((demo.Module1.BValue)value).getMap11().entrySet()) {
				newValue.put(e.getKey(), e.getValue().copy());
			}
		}

		@Override
		public final void OnChanged(Object key, Log note) {
			@SuppressWarnings("unchecked")
			var noteMap2 = (LogMap2<Long, BValue>)note;
			noteMap2.mergeChangedToReplaced();

			for (var a : noteMap2.getReplaced().entrySet()) {
				newValue.put(a.getKey(), a.getValue());
			}
			for (var r : noteMap2.getRemoved()) {
				newValue.remove(r);
			}
		}

		@Override
		public final void OnRemoved(Object key) {
			newValue = null;
		}
	}

	@SuppressWarnings("UseBulkOperation")
	private static class CLSet10 implements VarListener {
		private HashSet<Integer> newValue;

		public final void Init(demo.Module1.BValue current) {
			if (null != current) {
				newValue = new HashSet<>();
				for (var i : current.getSet10()) {
					newValue.add(i);
				}
			} else {
				newValue = null;
			}
		}

		public final void Verify(demo.Module1.BValue current) {
			if (null == current) {
				Assert.assertNull(newValue);
				return;
			}
			HashSet<Integer> newValueCopy = new HashSet<>();
			for (var i : current.getSet10()) {
				newValueCopy.add(i);
			}
			Assert.assertEquals(newValue.size(), newValueCopy.size());
			for (var i : newValue) {
				Assert.assertTrue(newValueCopy.contains(i));
			}
		}

		@Override
		public final void OnChanged(Object key, Bean value) {
			newValue = new HashSet<>();
			for (var i : ((demo.Module1.BValue)value).getSet10()) {
				newValue.add(i);
			}
		}

		@Override
		public final void OnChanged(Object key, Log note) {
			@SuppressWarnings("unchecked")
			var noteSet = (LogSet1<Integer>)note;
			for (var a : noteSet.getAdded()) {
				newValue.add(a);
			}
			for (var r : noteSet.getRemoved()) {
				newValue.remove(r);
			}
		}

		@Override
		public final void OnRemoved(Object key) {
			newValue = null;
		}
	}

	private static class CLList9 implements VarListener {
		private ArrayList<demo.Bean1> newValue;

		public final void Init(demo.Module1.BValue current) {
			if (null != current) {
				newValue = new ArrayList<>();
				for (var e : current.getList9()) {
					newValue.add(e.copy());
				}
			} else {
				newValue = null;
			}
		}

		public final void Verify(demo.Module1.BValue current) {
			if (null == current) {
				Assert.assertNull(newValue);
				return;
			}
			Assert.assertEquals(newValue.size(), current.getList9().size());
			for (int i = 0; i < newValue.size(); ++i) {
				Assert.assertEquals(newValue.get(i).getV1(), current.getList9().get(i).getV1());
			}
		}

		@Override
		public final void OnChanged(Object key, Bean value) {
			newValue = new ArrayList<>();
			for (var e : ((demo.Module1.BValue)value).getList9()) {
				newValue.add(e.copy());
			}
		}

		@Override
		public final void OnChanged(Object key, Log note) {
			newValue = new ArrayList<>();
			for (var e : ((demo.Module1.BValue)note.getBelong()).getList9()) {
				newValue.add(e.copy());
			}
		}

		@Override
		public final void OnRemoved(Object key) {
			newValue = null;
		}
	}

	private static class CLBytes8 implements VarListener {
		private Binary newValue;

		public final void Init(demo.Module1.BValue current) {
			newValue = current == null ? null : current.getBytes8();
		}

		public final void Verify(demo.Module1.BValue current) {
			if (null == current) {
				Assert.assertNull(newValue);
				return;
			}
			Assert.assertEquals(newValue, current.getBytes8());
		}

		@Override
		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.BValue)value).getBytes8();
		}

		@Override
		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.BValue)note.getBelong()).getBytes8();
		}

		@Override
		public final void OnRemoved(Object key) {
			newValue = null;
		}
	}

	private static class CLDouble7 implements VarListener {
		private double newValue;

		public final void Init(demo.Module1.BValue current) {
			newValue = (null != current) ? current.getDouble7() : 0;
		}

		public final void Verify(demo.Module1.BValue current) {
			if (null == current) {
				Assert.assertEquals(0, newValue, 0.0001);
				return;
			}
			Assert.assertEquals(newValue, current.getDouble7(), 0.0001);
		}

		@Override
		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.BValue)value).getDouble7();
		}

		@Override
		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.BValue)note.getBelong()).getDouble7();
		}

		@Override
		public final void OnRemoved(Object key) {
			newValue = 0;
		}
	}

	private static class CLFloat6 implements VarListener {
		private float newValue;

		public final void Init(demo.Module1.BValue current) {
			newValue = (null != current) ? current.getFloat6() : 0;
		}

		public final void Verify(demo.Module1.BValue current) {
			if (null == current) {
				Assert.assertEquals(0, newValue, 0.001);
				return;
			}
			Assert.assertEquals(newValue, current.getFloat6(), 0.001);
		}

		@Override
		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.BValue)value).getFloat6();
		}

		@Override
		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.BValue)note.getBelong()).getFloat6();
		}

		@Override
		public final void OnRemoved(Object key) {
			newValue = 0;
		}
	}

	private static class CLShort5 implements VarListener {
		private short newValue;

		public final void Init(demo.Module1.BValue current) {
			newValue = (null != current) ? current.getShort5() : (short)-1;
		}

		public final void Verify(demo.Module1.BValue current) {
			if (null == current) {
				Assert.assertEquals(-1, newValue);
				return;
			}
			Assert.assertEquals(newValue, current.getShort5());
		}

		@Override
		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.BValue)value).getShort5();
		}

		@Override
		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.BValue)note.getBelong()).getShort5();
		}

		@Override
		public final void OnRemoved(Object key) {
			newValue = -1;
		}
	}

	private static class CLBool4 implements VarListener {
		private boolean newValue;

		public final void Init(demo.Module1.BValue current) {
			newValue = (null != current) && current.isBool4();
		}

		public final void Verify(demo.Module1.BValue current) {
			if (null == current) {
				Assert.assertFalse(newValue);
				return;
			}
			Assert.assertEquals(newValue, current.isBool4());
		}

		@Override
		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.BValue)value).isBool4();
		}

		@Override
		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.BValue)note.getBelong()).isBool4();
		}

		@Override
		public final void OnRemoved(Object key) {
			newValue = false;
		}

	}

	private static class CLString3 implements VarListener {
		private String newValue;

		public final void Init(demo.Module1.BValue current) {
			newValue = current == null ? null : current.getString3();
		}

		public final void Verify(demo.Module1.BValue current) {
			if (null == current) {
				Assert.assertNull(newValue);
				return;
			}
			Assert.assertEquals(newValue, current.getString3());
		}

		@Override
		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.BValue)value).getString3();
		}

		@Override
		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.BValue)note.getBelong()).getString3();
		}

		@Override
		public final void OnRemoved(Object key) {
			newValue = null;
		}
	}

	private static class ClLong2 implements VarListener {
		private long newValue;

		public final void Init(demo.Module1.BValue current) {
			newValue = (null != current) ? current.getLong2() : -1;
		}

		public final void Verify(demo.Module1.BValue current) {
			if (null == current) {
				Assert.assertEquals(-1, newValue);
				return;
			}
			Assert.assertEquals(newValue, current.getLong2());
		}

		@Override
		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.BValue)value).getLong2();
		}

		@Override
		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.BValue)note.getBelong()).getLong2();
		}

		@Override
		public final void OnRemoved(Object key) {
			newValue = -1;
		}
	}

	private static class CLInt1 implements VarListener {
		private int newValue;

		public final void Init(demo.Module1.BValue current) {
			newValue = (null != current) ? current.getInt_1() : -1;
		}

		public final void Verify(demo.Module1.BValue current) {
			if (null == current) {
				Assert.assertEquals(-1, newValue);
				return;
			}
			Assert.assertEquals(newValue, current.getInt_1());
		}

		@Override
		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.BValue)value).getInt_1();
		}

		@Override
		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.BValue)note.getBelong()).getInt_1();
		}

		@Override
		public final void OnRemoved(Object key) {
			newValue = -1;
		}
	}
}
