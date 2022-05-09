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
import demo.Module2.Value;
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
	public final void testInit() throws Throwable {
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Throwable {
		demo.App.getInstance().Stop();
	}

	private void Prepare() throws Throwable {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			demo.App.getInstance().demo_Module1.getTable1().remove(1L);
			return (long)Procedure.Success;
		}, "TestChangeListener.Remove").Call();

		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			demo.Module1.Value value = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(1L);
			value.setInt1(123);
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
			value.getMap11().put(1L, new demo.Module2.Value());
			value.getMap11().put(2L, new demo.Module2.Value());
			value.getBean12().setInt1(123);
			value.setByte13((byte)12);
			value.setDynamic14(new demo.Module1.Simple());
			value.getDynamic14_demo_Module1_Simple().setInt1(123);
			value.getMap15().put(1L, 1L);
			value.getMap15().put(2L, 2L);
			return Procedure.Success;
		}, "TestChangeListener.Prepare").Call();
	}

	@Test
	public final void testAllType() throws Throwable {
		Prepare();
		AddListener();

		Init();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			demo.Module1.Value value = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(1L);
			value.setInt1(124);
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
			value.getMap11().put(1L, new demo.Module2.Value());
			value.getMap11().put(2L, new demo.Module2.Value());
			value.getBean12().setInt1(124);
			value.setByte13((byte)13);
			value.setDynamic14(new demo.Module1.Simple());
			value.getDynamic14_demo_Module1_Simple().setInt1(124);
			value.getMap15().put(3L, 3L);
			value.getMap15().put(4L, 4L);

			return Procedure.Success;
		}, "TestChangeListener.Modify").Call();
		Verify();

		Init();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			demo.Module1.Value value = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(1L);
			value.getSet10().add(127);
			value.getSet10().remove(124);
			value.getMap11().put(5L, new demo.Module2.Value());
			value.getMap11().put(6L, new demo.Module2.Value());
			value.getMap11().remove(1L);
			value.getMap11().remove(2L);
			value.getMap15().put(5L, 5L);
			value.getMap15().put(6L, 6L);
			value.getMap15().remove(1L);
			value.getMap15().remove(2L);
			return Procedure.Success;
		}, "TestChangeListener.ModifyCollections").Call();
		Verify();

		Init();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			demo.Module1.Value value = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(1L);
			ArrayList<Integer> except = new ArrayList<Integer>(Arrays.asList(1, 2));
			value.getSet10().removeAll(except);
			return Procedure.Success;
		}, "TestChangeListener.ModifySetExcept").Call();
		Verify();

		Init();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			demo.Module1.Value value = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(1L);
			ArrayList<Integer> intersect = new ArrayList<Integer>(Arrays.asList(123, 126));
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
		}, "TestChangeListener.ModifySetIntersect").Call();
		Verify();

		Init();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			demo.Module1.Value value = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(1L);
			ArrayList<Integer> SymmetricExcept = new ArrayList<Integer>(Arrays.asList(123, 140));
			value.getSet10().removeAll(SymmetricExcept);
			return Procedure.Success;
		}, "TestChangeListener.ModifySetSymmetricExcept").Call();
		Verify();

		Init();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			demo.Module1.Value value = demo.App.getInstance().demo_Module1.getTable1().getOrAdd(1L);
			ArrayList<Integer> Union = new ArrayList<Integer>(Arrays.asList(123, 140));
			//value.getSet10().UnionWith(Union);
			value.getSet10().addAll(Union);
			return Procedure.Success;
		}, "TestChangeListener.ModifySetUnion").Call();
		Verify();

		Init();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			demo.App.getInstance().demo_Module1.getTable1().put(1L, new demo.Module1.Value());
			return Procedure.Success;
		}, "TestChangeListener.PutRecord").Call();
		Verify();

		Init();
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			demo.App.getInstance().demo_Module1.getTable1().remove(1L);
			return Procedure.Success;
		}, "TestChangeListener.RemoveRecord").Call();
		Verify();
	}

	private demo.Module1.Value localValue;

	private void Init() throws Throwable {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			demo.Module1.Value value = demo.App.getInstance().demo_Module1.getTable1().get(1L);
			localValue = value == null ? null : value.Copy();
			return Procedure.Success;
		}, "TestChangeListener.CopyLocal").Call();

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

	private void Verify() throws Throwable {
		assert Procedure.Success == demo.App.getInstance().Zeze.NewProcedure(() -> {
			demo.Module1.Value value = demo.App.getInstance().demo_Module1.getTable1().get(1L);
			localValue = value == null ? null : value.Copy();
			return Procedure.Success;
		}, "TestChangeListener.CopyLocal").Call();

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

		ls.Vars.put(demo.Module1.Table1.VAR_int1, _CLInt1);
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

		demo.App.getInstance().demo_Module1.getTable1().getChangeListenerMap().AddListener(ls);
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
		public HashMap<Integer, VarListener> Vars = new HashMap<>();
		@Override
		public void OnChanged(Object key, Changes.Record r) {
			switch (r.getState()) {
			case Changes.Record.Remove:
				for (var var : Vars.values())
					var.OnRemoved(key);
				break;
			case Changes.Record.Put:
				for (var var : Vars.values())
					var.OnChanged(key, r.getPutValue());
				break;
			case Changes.Record.Edit:
				var logbean = r.logBean();
				for (var e : Vars.entrySet()) {
					var vlog = logbean.getVariables().get(e.getKey());
					if (null != vlog) {
						e.getValue().OnChanged(key, vlog);
					}
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

	private static class CLMap15 implements VarListener {
		private HashMap<Long, Long> newValue;

		public final void Init(demo.Module1.Value current) {
			if (null != current) {
				newValue = new HashMap<Long, Long>();
				for (var e : current.getMap15().entrySet()) {
					newValue.put(e.getKey(), e.getValue());
				}
			} else {
				newValue = null;
			}
		}

		public final void Verify(demo.Module1.Value current) {
			if (null == current) {
				Assert.assertEquals(null, newValue);
				return;
			}
			HashMap<Long, Long> newValueCopy = new HashMap<Long, Long>();
			for (var e : ((demo.Module1.Value)current).getMap15().entrySet()) {
				newValueCopy.put(e.getKey(), e.getValue());
			}
			Assert.assertEquals(newValue.size(), newValueCopy.size());
			for (var e : newValue.entrySet()) {

				Long exist = newValueCopy.get(e.getKey());
				Assert.assertTrue(newValueCopy.containsKey(e.getKey()));
				Assert.assertEquals(e.getValue(), exist);
			}
		}

		public final void OnChanged(Object key, Bean value) {
			newValue = new HashMap<Long, Long>();
			for (var e : ((demo.Module1.Value)value).getMap15().entrySet()) {
				newValue.put(e.getKey(), e.getValue());
			}
		}

		public final void OnChanged(Object key, Log note) {
			@SuppressWarnings("unchecked")
			var notemap1 = (LogMap1<Long, Long>)note;

			for (var a : notemap1.getReplaced().entrySet()) {
				newValue.put(a.getKey(), a.getValue());
			}
			for (var r : notemap1.getRemoved()) {
				newValue.remove(r);
			}
		}

		public final void OnRemoved(Object key) {
			newValue = null;
		}
	}

	private static class ClDynamic14 implements VarListener {
		private Bean newValue;

		public final void Init(demo.Module1.Value current) {
			newValue = current == null ? null : current.getDynamic14().CopyBean();
		}

		public final void Verify(demo.Module1.Value current) {
			if (null == current) {
				Assert.assertEquals(null, newValue);
				return;
			}
			Assert.assertEquals(newValue.getTypeId(), current.getDynamic14().getTypeId());
			if (newValue.getTypeId() == demo.Module1.Simple.TYPEID) {
				demo.Module1.Simple newSimple = newValue instanceof demo.Module1.Simple ? (demo.Module1.Simple)newValue : null;
				Zeze.Transaction.Bean tempVar = current.getDynamic14().getBean();
				demo.Module1.Simple currentSimple = tempVar instanceof demo.Module1.Simple ? (demo.Module1.Simple)tempVar : null;
				Assert.assertNotNull(newSimple);
				Assert.assertNotNull(currentSimple);
				Assert.assertEquals(newSimple.getInt1(), currentSimple.getInt1());
			}
		}

		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.Value)value).getDynamic14();
		}

		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.Value)note.getBelong()).getDynamic14();
		}

		public final void OnRemoved(Object key) {
			newValue = null;
		}
	}

	private static class CLByte13 implements VarListener {
		private byte newValue;

		public final void Init(demo.Module1.Value current) {
			newValue = (null != current) ? current.getByte13() : (byte)255;
		}

		public final void Verify(demo.Module1.Value current) {
			if (null == current) {
				Assert.assertEquals(((byte)255), newValue);
				return;
			}
			Assert.assertEquals(newValue, current.getByte13());
		}

		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.Value)value).getByte13();
		}

		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.Value)note.getBelong()).getByte13();
		}

		public final void OnRemoved(Object key) {
			newValue = (byte)255;
		}
	}

	private static class CLBean12 implements VarListener {
		private demo.Module1.Simple newValue;

		public final void Init(demo.Module1.Value current) {
			if (null != current) {
				newValue = current.getBean12().Copy();
			} else {
				newValue = null;
			}
		}

		public final void Verify(demo.Module1.Value current) {
			if (null == current) {
				Assert.assertEquals(null, newValue);
				return;
			}
			Assert.assertEquals(newValue.getInt1(), current.getBean12().getInt1());
		}

		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.Value)value).getBean12().Copy();
		}

		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.Value)note.getBelong()).getBean12().Copy();
		}

		public final void OnRemoved(Object key) {
			newValue = null;
		}
	}

	private static class CLMap11 implements VarListener {
		private HashMap<Long, demo.Module2.Value> newValue;

		public final void Init(demo.Module1.Value current) {
			if (null != current) {
				newValue = new HashMap<Long, demo.Module2.Value>();
				for (var e : current.getMap11().entrySet()) {
					newValue.put(e.getKey(), e.getValue().Copy());
				}
			} else {
				newValue = null;
			}
		}

		public final void Verify(demo.Module1.Value current) {
			if (null == current) {
				Assert.assertEquals(null, newValue);
				return;
			}
			HashMap<Long, demo.Module2.Value> newValueCopy = new HashMap<Long, demo.Module2.Value>();
			for (var e : ((demo.Module1.Value)current).getMap11().entrySet()) {
				newValueCopy.put(e.getKey(), e.getValue().Copy());
			}
			Assert.assertEquals(newValue.size(), newValueCopy.size());
			for (var e : newValue.entrySet()) {
				demo.Module2.Value exist = newValueCopy.get(e.getKey());
				Assert.assertTrue(newValueCopy.containsKey(e.getKey()));
				Assert.assertEquals(e.getValue().getS(), exist.getS());
			}
		}

		public final void OnChanged(Object key, Bean value) {
			newValue = new HashMap<Long, demo.Module2.Value>();
			for (var e : ((demo.Module1.Value)value).getMap11().entrySet()) {
				newValue.put(e.getKey(), e.getValue().Copy());
			}
		}

		public final void OnChanged(Object key, Log note) {
			@SuppressWarnings("unchecked")
			var notemap2 = (LogMap2<Long, Value>)note;
			notemap2.MergeChangedToReplaced();

			for (var a : notemap2.getReplaced().entrySet()) {
				newValue.put(a.getKey(), a.getValue());
			}
			for (var r : notemap2.getRemoved()) {
				newValue.remove(r);
			}
		}

		public final void OnRemoved(Object key) {
			newValue = null;
		}
	}

	private static class CLSet10 implements VarListener {
		private HashSet<Integer> newValue;

		public final void Init(demo.Module1.Value current) {
			if (null != current) {
				newValue = new HashSet<Integer>();
				for (var i : current.getSet10()) {
					newValue.add(i);
				}
			} else {
				newValue = null;
			}
		}

		public final void Verify(demo.Module1.Value current) {
			if (null == current) {
				Assert.assertEquals(null, newValue);
				return;
			}
			HashSet<Integer> newValueCopy = new HashSet<Integer>();
			for (var i : ((demo.Module1.Value)current).getSet10()) {
				newValueCopy.add(i);
			}
			Assert.assertEquals(newValue.size(), newValueCopy.size());
			for (var i : newValue) {
				Assert.assertTrue(newValueCopy.contains(i));
			}
		}

		public final void OnChanged(Object key, Bean value) {
			newValue = new HashSet<Integer>();
			for (var i : ((demo.Module1.Value)value).getSet10()) {
				newValue.add(i);
			}
		}

		public final void OnChanged(Object key, Log note) {
			@SuppressWarnings("unchecked")
			var noteset = (LogSet1<Integer>)note;
			for (var a : noteset.getAdded()) {
				newValue.add(a);
			}
			for (var r : noteset.getRemoved()) {
				newValue.remove((Integer)r);
			}
		}

		public final void OnRemoved(Object key) {
			newValue = null;
		}
	}

	private static class CLList9 implements VarListener {
		private ArrayList<demo.Bean1> newValue;

		public final void Init(demo.Module1.Value current) {
			if (null != current) {
				newValue = new ArrayList<demo.Bean1>();
				for (var e : current.getList9()) {
					newValue.add(e.Copy());
				}
			} else {
				newValue = null;
			}
		}

		public final void Verify(demo.Module1.Value current) {
			if (null == current) {
				Assert.assertEquals(null, newValue);
				return;
			}
			Assert.assertEquals(newValue.size(), current.getList9().size());
			for (int i = 0; i < newValue.size(); ++i) {
				Assert.assertEquals(newValue.get(i).getV1(), current.getList9().get(i).getV1());
			}
		}

		public final void OnChanged(Object key, Bean value) {
			newValue = new ArrayList<demo.Bean1>();
			for (var e : ((demo.Module1.Value)value).getList9()) {
				newValue.add(e.Copy());
			}
		}

		public final void OnChanged(Object key, Log note) {
			newValue = new ArrayList<demo.Bean1>();
			for (var e : ((demo.Module1.Value)note.getBelong()).getList9()) {
				newValue.add(e.Copy());
			}
		}

		public final void OnRemoved(Object key) {
			newValue = null;
		}
	}

	private static class CLBytes8 implements VarListener {
		private Binary newValue;

		public final void Init(demo.Module1.Value current) {
			newValue = current == null ? null : current.getBytes8();
		}

		public final void Verify(demo.Module1.Value current) {
			if (null == current) {
				Assert.assertEquals(null, newValue);
				return;
			}
			Assert.assertEquals(newValue, current.getBytes8());
		}

		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.Value)value).getBytes8();
		}

		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.Value)note.getBelong()).getBytes8();
		}

		public final void OnRemoved(Object key) {
			newValue = null;
		}
	}

	private static class CLDouble7 implements VarListener {
		private double newValue;

		public final void Init(demo.Module1.Value current) {
			newValue = (null != current) ? current.getDouble7() : 0;
		}

		public final void Verify(demo.Module1.Value current) {
			if (null == current) {
				Assert.assertEquals(0, newValue, 0.0001);
				return;
			}
			Assert.assertEquals(newValue, current.getDouble7(), 0.0001);
		}

		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.Value)value).getDouble7();
		}

		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.Value)note.getBelong()).getDouble7();
		}

		public final void OnRemoved(Object key) {
			newValue = 0;
		}
	}

	private static class CLFloat6 implements VarListener {
		private float newValue;

		public final void Init(demo.Module1.Value current) {
			newValue = (null != current) ? current.getFloat6() : 0;
		}

		public final void Verify(demo.Module1.Value current) {
			if (null == current) {
				Assert.assertEquals(0, newValue, 0.001);
				return;
			}
			Assert.assertEquals(newValue, current.getFloat6(), 0.001);
		}

		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.Value)value).getFloat6();
		}

		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.Value)note.getBelong()).getFloat6();
		}

		public final void OnRemoved(Object key) {
			newValue = 0;
		}
	}

	private static class CLShort5 implements VarListener {
		private short newValue;

		public final void Init(demo.Module1.Value current) {
			newValue = (null != current) ? current.getShort5() : (short)-1;
		}

		public final void Verify(demo.Module1.Value current) {
			if (null == current) {
				Assert.assertEquals(-1, newValue);
				return;
			}
			Assert.assertEquals(newValue, current.getShort5());
		}

		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.Value)value).getShort5();
		}

		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.Value)note.getBelong()).getShort5();
		}

		public final void OnRemoved(Object key) {
			newValue = -1;
		}
	}

	private static class CLBool4 implements VarListener {
		private boolean newValue;

		public final void Init(demo.Module1.Value current) {
			newValue = (null != current) && current.isBool4();
		}

		public final void Verify(demo.Module1.Value current) {
			if (null == current) {
				Assert.assertEquals(false, newValue);
				return;
			}
			Assert.assertEquals(newValue, current.isBool4());
		}

		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.Value)value).isBool4();
		}

		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.Value)note.getBelong()).isBool4();
		}

		public final void OnRemoved(Object key) {
			newValue = false;
		}

	}

	private static class CLString3 implements VarListener {
		private String newValue;

		public final void Init(demo.Module1.Value current) {
			newValue = current == null ? null : current.getString3();
		}

		public final void Verify(demo.Module1.Value current) {
			if (null == current) {
				Assert.assertEquals(newValue, null);
				return;
			}
			Assert.assertEquals(newValue, current.getString3());
		}

		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.Value)value).getString3();
		}

		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.Value)note.getBelong()).getString3();
		}

		public final void OnRemoved(Object key) {
			newValue = null;
		}
	}

	private static class ClLong2 implements VarListener {
		private long newValue;

		public final void Init(demo.Module1.Value current) {
			newValue = (null != current) ? current.getLong2() : -1;
		}

		public final void Verify(demo.Module1.Value current) {
			if (null == current) {
				Assert.assertEquals(-1, newValue);
				return;
			}
			Assert.assertEquals(newValue, current.getLong2());
		}

		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.Value)value).getLong2();
		}

		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.Value)note.getBelong()).getLong2();
		}

		public final void OnRemoved(Object key) {
			newValue = -1;
		}
	}

	private static class CLInt1 implements VarListener {
		private int newValue;

		public final void Init(demo.Module1.Value current) {
			newValue = (null != current) ? current.getInt1() : -1;
		}

		public final void Verify(demo.Module1.Value current) {
			if (null == current) {
				Assert.assertEquals(-1, newValue);
				return;
			}
			Assert.assertEquals(newValue, current.getInt1());
		}

		public final void OnChanged(Object key, Bean value) {
			newValue = ((demo.Module1.Value)value).getInt1();
		}

		public final void OnChanged(Object key, Log note) {
			newValue = ((demo.Module1.Value)note.getBelong()).getInt1();
		}

		public final void OnRemoved(Object key) {
			newValue = -1;
		}
	}
}