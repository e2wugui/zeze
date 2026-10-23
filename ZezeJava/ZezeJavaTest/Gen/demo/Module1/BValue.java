// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnnecessarilyQualifiedInnerClassAccess", "UnusedAssignment", "override"})
public final class BValue extends Zeze.Transaction.Bean implements BValueReadOnly, Zeze.ILong2 {
    public static final long TYPEID = -9185494755353500202L;

    public static final int Enum1 = 4; // enum的注释

    private int _int_1; // com aa
    private long _long2; // com aa
    private String _string3; // com aa
    private boolean _bool4; // com aa
    private short _short5; // com aa
    private float _float6; // com aa
    private double _double7; // com aa
    private Zeze.Net.Binary _bytes8; // com aa
    private final Zeze.Transaction.Collections.PList2<demo.Bean1> _list9; // com aa
    private static final Zeze.Transaction.Collections.List2Meta<demo.Bean1> meta1_list9
            = Zeze.Transaction.Collections.List2Meta.get(demo.Bean1.class);

    private final Zeze.Transaction.Collections.PSet1<Integer> _set10; // com aa
    private static final Zeze.Transaction.Collections.Set1Meta<Integer> meta1_set10
            = Zeze.Transaction.Collections.Set1Meta.get(Integer.class);

    private final Zeze.Transaction.Collections.PMap2<Long, demo.Module2.BValue> _map11; // com aa
    private static final Zeze.Transaction.Collections.Map2Meta<Long, demo.Module2.BValue> meta2_map11
            = Zeze.Transaction.Collections.Map2Meta.get(Long.class, demo.Module2.BValue.class);

    private final Zeze.Transaction.Collections.CollOne<demo.Module1.BSimple> _bean12; // simple
    private static final Zeze.Transaction.Collections.LogOneMeta<demo.Module1.BSimple> meta1_bean12
            = Zeze.Transaction.Collections.LogOneMeta.get(demo.Module1.BSimple.class);

    private byte _byte13; // com aa
    private final Zeze.Transaction.DynamicBean _dynamic14;

    public static final long DynamicTypeId_Dynamic14_demo_Bean1 = 1L;

    public static final long DynamicTypeId_Dynamic14_demo_Module1_BSimple = 2L;

    public static final long DynamicTypeId_Dynamic14_demo_Module1_BItem = 3L;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Dynamic14() {
        return new Zeze.Transaction.DynamicBean(14, BValue::getSpecialTypeIdFromBean_14, BValue::createBeanFromSpecialTypeId_14);
    }

    public static long getSpecialTypeIdFromBean_14(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == -410057899348847631L)
            return 1L; // demo.Bean1
        if (_t_ == 4513771153805810055L)
            return 2L; // demo.Module1.BSimple
        if (_t_ == -1770669781233931946L)
            return 3L; // demo.Module1.BItem
        throw new UnsupportedOperationException("Unknown Bean! dynamic@demo.Module1.BValue:dynamic14:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_14(long _t_) {
        if (_t_ == 1L)
            return new demo.Bean1();
        if (_t_ == 2L)
            return new demo.Module1.BSimple();
        if (_t_ == 3L)
            return new demo.Module1.BItem();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private final Zeze.Transaction.Collections.PMap1<Long, Long> _map15; // com aa
    private static final Zeze.Transaction.Collections.Map1Meta<Long, Long> meta2_map15
            = Zeze.Transaction.Collections.Map1Meta.get(Long.class, Long.class);

    private final Zeze.Transaction.Collections.PMap2<demo.Module1.Key, demo.Module1.BSimple> _map16; // com aa
    private static final Zeze.Transaction.Collections.Map2Meta<demo.Module1.Key, demo.Module1.BSimple> meta2_map16
            = Zeze.Transaction.Collections.Map2Meta.get(demo.Module1.Key.class, demo.Module1.BSimple.class);

    private Zeze.Serialize.Vector2 _vector2;
    private Zeze.Serialize.Vector2Int _vector2Int;
    private Zeze.Serialize.Vector3 _vector3;
    private Zeze.Serialize.Vector3Int _vector3Int;
    private Zeze.Serialize.Vector4 _vector4;
    private Zeze.Serialize.Quaternion _quaternion;
    private final Zeze.Transaction.DynamicBean _dynamic23;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Dynamic23() {
        return new Zeze.Transaction.DynamicBean(23, demo.Module1.ModuleModule1::getSpecialTypeIdFromBean, demo.Module1.ModuleModule1::createBeanFromSpecialTypeId);
    }

    public static long getSpecialTypeIdFromBean_23(Zeze.Transaction.Bean _b_) {
        return demo.Module1.ModuleModule1.getSpecialTypeIdFromBean(_b_);
    }

    public static Zeze.Transaction.Bean createBeanFromSpecialTypeId_23(long _t_) {
        return demo.Module1.ModuleModule1.createBeanFromSpecialTypeId(_t_);
    }

    private final Zeze.Transaction.Collections.PList1<Zeze.Serialize.Vector2Int> _listVector2Int;
    private static final Zeze.Transaction.Collections.List1Meta<Zeze.Serialize.Vector2Int> meta1_listVector2Int
            = Zeze.Transaction.Collections.List1Meta.get(Zeze.Serialize.Vector2Int.class);

    private final Zeze.Transaction.Collections.PMap2<demo.Module1.Key, demo.Module1.BSimple> _map25;
    private static final Zeze.Transaction.Collections.Map2Meta<demo.Module1.Key, demo.Module1.BSimple> meta2_map25
            = Zeze.Transaction.Collections.Map2Meta.get(demo.Module1.Key.class, demo.Module1.BSimple.class);

    private final Zeze.Transaction.Collections.PMap2<demo.Module1.Key, Zeze.Transaction.DynamicBean> _map26;
    private static final Zeze.Transaction.Collections.Map2Meta<demo.Module1.Key, Zeze.Transaction.DynamicBean> meta2_map26
            = Zeze.Transaction.Collections.Map2Meta.createDynamic(demo.Module1.Key.class, BValue::getSpecialTypeIdFromBean_26, BValue::createBeanFromSpecialTypeId_26);

    public static Zeze.Transaction.DynamicBean newDynamicBean_Map26() {
        return new Zeze.Transaction.DynamicBean(26, BValue::getSpecialTypeIdFromBean_26, BValue::createBeanFromSpecialTypeId_26);
    }

    public static long getSpecialTypeIdFromBean_26(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == 4513771153805810055L)
            return 4513771153805810055L; // demo.Module1.BSimple
        throw new UnsupportedOperationException("Unknown Bean! dynamic@demo.Module1.BValue:map26:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_26(long _t_) {
        if (_t_ == 4513771153805810055L)
            return new demo.Module1.BSimple();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private final Zeze.Transaction.DynamicBean _dynamic27;

    public static final long DynamicTypeId_Dynamic27_demo_Module1_BSimple = 4513771153805810055L;

    public static Zeze.Transaction.DynamicBean newDynamicBean_Dynamic27() {
        return new Zeze.Transaction.DynamicBean(27, BValue::getSpecialTypeIdFromBean_27, BValue::createBeanFromSpecialTypeId_27);
    }

    public static long getSpecialTypeIdFromBean_27(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == 4513771153805810055L)
            return 4513771153805810055L; // demo.Module1.BSimple
        throw new UnsupportedOperationException("Unknown Bean! dynamic@demo.Module1.BValue:dynamic27:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_27(long _t_) {
        if (_t_ == 4513771153805810055L)
            return new demo.Module1.BSimple();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private demo.Module1.Key _key28;
    private final Zeze.Transaction.Collections.PList1<Float> _array29;
    private static final Zeze.Transaction.Collections.List1Meta<Float> meta1_array29
            = Zeze.Transaction.Collections.List1Meta.get(Float.class);

    private final Zeze.Transaction.Collections.PList1<Integer> _list30;
    private static final Zeze.Transaction.Collections.List1Meta<Integer> meta1_list30
            = Zeze.Transaction.Collections.List1Meta.get(Integer.class);

    private final Zeze.Transaction.Collections.PList1<Long> _list31;
    private static final Zeze.Transaction.Collections.List1Meta<Long> meta1_list31
            = Zeze.Transaction.Collections.List1Meta.get(Long.class);

    private final Zeze.Transaction.Collections.PList1<Float> _list32;
    private static final Zeze.Transaction.Collections.List1Meta<Float> meta1_list32
            = Zeze.Transaction.Collections.List1Meta.get(Float.class);

    private final Zeze.Transaction.Collections.PList1<Zeze.Serialize.Vector2> _list33;
    private static final Zeze.Transaction.Collections.List1Meta<Zeze.Serialize.Vector2> meta1_list33
            = Zeze.Transaction.Collections.List1Meta.get(Zeze.Serialize.Vector2.class);

    private final Zeze.Transaction.Collections.PList1<Zeze.Serialize.Vector3> _list34;
    private static final Zeze.Transaction.Collections.List1Meta<Zeze.Serialize.Vector3> meta1_list34
            = Zeze.Transaction.Collections.List1Meta.get(Zeze.Serialize.Vector3.class);

    private final Zeze.Transaction.Collections.PList1<Zeze.Serialize.Vector4> _list35;
    private static final Zeze.Transaction.Collections.List1Meta<Zeze.Serialize.Vector4> meta1_list35
            = Zeze.Transaction.Collections.List1Meta.get(Zeze.Serialize.Vector4.class);

    private final Zeze.Transaction.Collections.PList1<Zeze.Serialize.Vector2Int> _list36;
    private static final Zeze.Transaction.Collections.List1Meta<Zeze.Serialize.Vector2Int> meta1_list36
            = Zeze.Transaction.Collections.List1Meta.get(Zeze.Serialize.Vector2Int.class);

    private final Zeze.Transaction.Collections.PList1<Zeze.Serialize.Vector3Int> _list37;
    private static final Zeze.Transaction.Collections.List1Meta<Zeze.Serialize.Vector3Int> meta1_list37
            = Zeze.Transaction.Collections.List1Meta.get(Zeze.Serialize.Vector3Int.class);

    private final Zeze.Transaction.Collections.PSet1<Integer> _set38;
    private static final Zeze.Transaction.Collections.Set1Meta<Integer> meta1_set38
            = Zeze.Transaction.Collections.Set1Meta.get(Integer.class);

    private final Zeze.Transaction.Collections.PSet1<Long> _set39;
    private static final Zeze.Transaction.Collections.Set1Meta<Long> meta1_set39
            = Zeze.Transaction.Collections.Set1Meta.get(Long.class);

    private final Zeze.Transaction.Collections.PMap1<Integer, Integer> _map40;
    private static final Zeze.Transaction.Collections.Map1Meta<Integer, Integer> meta2_map40
            = Zeze.Transaction.Collections.Map1Meta.get(Integer.class, Integer.class);

    private final Zeze.Transaction.Collections.PMap2<Long, demo.Module1.BSimple> _map41;
    private static final Zeze.Transaction.Collections.Map2Meta<Long, demo.Module1.BSimple> meta2_map41
            = Zeze.Transaction.Collections.Map2Meta.get(Long.class, demo.Module1.BSimple.class);

    private final Zeze.Transaction.Collections.PMap2<Long, demo.Module1.BValue> _map42Recursive;
    private static final Zeze.Transaction.Collections.Map2Meta<Long, demo.Module1.BValue> meta2_map42Recursive
            = Zeze.Transaction.Collections.Map2Meta.get(Long.class, demo.Module1.BValue.class);

    private final Zeze.Transaction.Collections.PList2<Zeze.Transaction.DynamicBean> _list43;
    private static final Zeze.Transaction.Collections.List2Meta<Zeze.Transaction.DynamicBean> meta1_list43
            = Zeze.Transaction.Collections.List2Meta.createDynamic(BValue::getSpecialTypeIdFromBean_43, BValue::createBeanFromSpecialTypeId_43);

    public static Zeze.Transaction.DynamicBean newDynamicBean_List43() {
        return new Zeze.Transaction.DynamicBean(43, BValue::getSpecialTypeIdFromBean_43, BValue::createBeanFromSpecialTypeId_43);
    }

    public static long getSpecialTypeIdFromBean_43(Zeze.Transaction.Bean _b_) {
        var _t_ = _b_.typeId();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return Zeze.Transaction.EmptyBean.TYPEID;
        if (_t_ == 4513771153805810055L)
            return 4513771153805810055L; // demo.Module1.BSimple
        throw new UnsupportedOperationException("Unknown Bean! dynamic@demo.Module1.BValue:list43:" + _t_);
    }

    public static Zeze.Transaction.@Nullable Bean createBeanFromSpecialTypeId_43(long _t_) {
        if (_t_ == 4513771153805810055L)
            return new demo.Module1.BSimple();
        if (_t_ == Zeze.Transaction.EmptyBean.TYPEID)
            return new Zeze.Transaction.EmptyBean();
        return null;
    }

    private String _jsonObject;
    private String _jsonArray;
    private long _relationalMappingAlter;
    private final Zeze.Transaction.Collections.PSortedMap1<Integer, Integer> _sortedmap1;
    private static final Zeze.Transaction.Collections.SortedMap1Meta<Integer, Integer> meta2_sortedmap1
            = Zeze.Transaction.Collections.SortedMap1Meta.get(Integer.class, Integer.class);

    private final Zeze.Transaction.Collections.PSortedMap2<Integer, demo.Module1.BSimple> _sortedmap2;
    private static final Zeze.Transaction.Collections.SortedMap2Meta<Integer, demo.Module1.BSimple> meta2_sortedmap2
            = Zeze.Transaction.Collections.SortedMap2Meta.get(Integer.class, demo.Module1.BSimple.class);

    private long _version;
    private final Zeze.Transaction.Collections.PList1<Long> _LongList;
    private static final Zeze.Transaction.Collections.List1Meta<Long> meta1_LongList
            = Zeze.Transaction.Collections.List1Meta.get(Long.class);

    private transient @Nullable Object __zeze_map_key__;

    @Override
    public @Nullable Object mapKey() {
        return __zeze_map_key__;
    }

    @Override
    public void mapKey(Object _v_) {
        __zeze_map_key__ = _v_;
    }

    private static final Zeze.Transaction.Collections.BeanKeyMeta<demo.Module1.Key> meta_key28
            = Zeze.Transaction.Collections.BeanKeyMeta.get(demo.Module1.Key.class);

    private static final java.lang.invoke.VarHandle vh_int_1;
    private static final java.lang.invoke.VarHandle vh_long2;
    private static final java.lang.invoke.VarHandle vh_string3;
    private static final java.lang.invoke.VarHandle vh_bool4;
    private static final java.lang.invoke.VarHandle vh_short5;
    private static final java.lang.invoke.VarHandle vh_float6;
    private static final java.lang.invoke.VarHandle vh_double7;
    private static final java.lang.invoke.VarHandle vh_bytes8;
    private static final java.lang.invoke.VarHandle vh_byte13;
    private static final java.lang.invoke.VarHandle vh_vector2;
    private static final java.lang.invoke.VarHandle vh_vector2Int;
    private static final java.lang.invoke.VarHandle vh_vector3;
    private static final java.lang.invoke.VarHandle vh_vector3Int;
    private static final java.lang.invoke.VarHandle vh_vector4;
    private static final java.lang.invoke.VarHandle vh_quaternion;
    private static final java.lang.invoke.VarHandle vh_key28;
    private static final java.lang.invoke.VarHandle vh_jsonObject;
    private static final java.lang.invoke.VarHandle vh_jsonArray;
    private static final java.lang.invoke.VarHandle vh_relationalMappingAlter;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_int_1 = _l_.findVarHandle(BValue.class, "_int_1", int.class);
            vh_long2 = _l_.findVarHandle(BValue.class, "_long2", long.class);
            vh_string3 = _l_.findVarHandle(BValue.class, "_string3", String.class);
            vh_bool4 = _l_.findVarHandle(BValue.class, "_bool4", boolean.class);
            vh_short5 = _l_.findVarHandle(BValue.class, "_short5", short.class);
            vh_float6 = _l_.findVarHandle(BValue.class, "_float6", float.class);
            vh_double7 = _l_.findVarHandle(BValue.class, "_double7", double.class);
            vh_bytes8 = _l_.findVarHandle(BValue.class, "_bytes8", Zeze.Net.Binary.class);
            vh_byte13 = _l_.findVarHandle(BValue.class, "_byte13", byte.class);
            vh_vector2 = _l_.findVarHandle(BValue.class, "_vector2", Zeze.Serialize.Vector2.class);
            vh_vector2Int = _l_.findVarHandle(BValue.class, "_vector2Int", Zeze.Serialize.Vector2Int.class);
            vh_vector3 = _l_.findVarHandle(BValue.class, "_vector3", Zeze.Serialize.Vector3.class);
            vh_vector3Int = _l_.findVarHandle(BValue.class, "_vector3Int", Zeze.Serialize.Vector3Int.class);
            vh_vector4 = _l_.findVarHandle(BValue.class, "_vector4", Zeze.Serialize.Vector4.class);
            vh_quaternion = _l_.findVarHandle(BValue.class, "_quaternion", Zeze.Serialize.Quaternion.class);
            vh_key28 = _l_.findVarHandle(BValue.class, "_key28", demo.Module1.Key.class);
            vh_jsonObject = _l_.findVarHandle(BValue.class, "_jsonObject", String.class);
            vh_jsonArray = _l_.findVarHandle(BValue.class, "_jsonArray", String.class);
            vh_relationalMappingAlter = _l_.findVarHandle(BValue.class, "_relationalMappingAlter", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public int getInt_1() {
        if (!isManaged())
            return _int_1;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _int_1;
        var log = (Zeze.Transaction.Logs.LogInt)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _int_1;
    }

    public void setInt_1(int _v_) {
        if (!isManaged()) {
            _int_1 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogInt(this, 1, vh_int_1, _v_));
    }

    @Override
    public long getLong2() {
        if (!isManaged())
            return _long2;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _long2;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _long2;
    }

    public void setLong2(long _v_) {
        if (!isManaged()) {
            _long2 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_long2, _v_));
    }

    @Override
    public String getString3() {
        if (!isManaged())
            return _string3;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _string3;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 3);
        return log != null ? log.stringValue() : _string3;
    }

    public void setString3(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _string3 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 3, vh_string3, _v_));
    }

    @Override
    public boolean isBool4() {
        if (!isManaged())
            return _bool4;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _bool4;
        var log = (Zeze.Transaction.Logs.LogBool)_t_.getLog(objectId() + 4);
        return log != null ? log.value : _bool4;
    }

    public void setBool4(boolean _v_) {
        if (!isManaged()) {
            _bool4 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBool(this, 4, vh_bool4, _v_));
    }

    @Override
    public short getShort5() {
        if (!isManaged())
            return _short5;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _short5;
        var log = (Zeze.Transaction.Logs.LogShort)_t_.getLog(objectId() + 5);
        return log != null ? log.value : _short5;
    }

    public void setShort5(short _v_) {
        if (!isManaged()) {
            _short5 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogShort(this, 5, vh_short5, _v_));
    }

    @Override
    public float getFloat6() {
        if (!isManaged())
            return _float6;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _float6;
        var log = (Zeze.Transaction.Logs.LogFloat)_t_.getLog(objectId() + 6);
        return log != null ? log.value : _float6;
    }

    public void setFloat6(float _v_) {
        if (!isManaged()) {
            _float6 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogFloat(this, 6, vh_float6, _v_));
    }

    @Override
    public double getDouble7() {
        if (!isManaged())
            return _double7;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _double7;
        var log = (Zeze.Transaction.Logs.LogDouble)_t_.getLog(objectId() + 7);
        return log != null ? log.value : _double7;
    }

    public void setDouble7(double _v_) {
        if (!isManaged()) {
            _double7 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogDouble(this, 7, vh_double7, _v_));
    }

    @Override
    public Zeze.Net.Binary getBytes8() {
        if (!isManaged())
            return _bytes8;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _bytes8;
        var log = (Zeze.Transaction.Logs.LogBinary)_t_.getLog(objectId() + 8);
        return log != null ? log.value : _bytes8;
    }

    public void setBytes8(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _bytes8 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBinary(this, 8, vh_bytes8, _v_));
    }

    public Zeze.Transaction.Collections.PList2<demo.Bean1> getList9() {
        return _list9;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<demo.Bean1, demo.Bean1ReadOnly> getList9ReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_list9);
    }

    public Zeze.Transaction.Collections.PSet1<Integer> getSet10() {
        return _set10;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getSet10ReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_set10);
    }

    public Zeze.Transaction.Collections.PMap2<Long, demo.Module2.BValue> getMap11() {
        return _map11;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, demo.Module2.BValue, demo.Module2.BValueReadOnly> getMap11ReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_map11);
    }

    public demo.Module1.BSimple getBean12() {
        return _bean12.getValue();
    }

    public void setBean12(demo.Module1.BSimple _v_) {
        _bean12.setValue(_v_);
    }

    @Override
    public demo.Module1.BSimpleReadOnly getBean12ReadOnly() {
        return _bean12.getValue();
    }

    @Override
    public byte getByte13() {
        if (!isManaged())
            return _byte13;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _byte13;
        var log = (Zeze.Transaction.Logs.LogByte)_t_.getLog(objectId() + 13);
        return log != null ? log.value : _byte13;
    }

    public void setByte13(byte _v_) {
        if (!isManaged()) {
            _byte13 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogByte(this, 13, vh_byte13, _v_));
    }

    public Zeze.Transaction.DynamicBean getDynamic14() {
        return _dynamic14;
    }

    public demo.Bean1 getDynamic14_demo_Bean1() {
        return (demo.Bean1)_dynamic14.getBean();
    }

    public void setDynamic14(demo.Bean1 _v_) {
        _dynamic14.setBean(_v_);
    }

    public demo.Module1.BSimple getDynamic14_demo_Module1_BSimple() {
        return (demo.Module1.BSimple)_dynamic14.getBean();
    }

    public void setDynamic14(demo.Module1.BSimple _v_) {
        _dynamic14.setBean(_v_);
    }

    public demo.Module1.BItem getDynamic14_demo_Module1_BItem() {
        return (demo.Module1.BItem)_dynamic14.getBean();
    }

    public void setDynamic14(demo.Module1.BItem _v_) {
        _dynamic14.setBean(_v_);
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getDynamic14ReadOnly() {
        return _dynamic14;
    }

    @Override
    public demo.Bean1ReadOnly getDynamic14_demo_Bean1ReadOnly() {
        return (demo.Bean1)_dynamic14.getBean();
    }

    @Override
    public demo.Module1.BSimpleReadOnly getDynamic14_demo_Module1_BSimpleReadOnly() {
        return (demo.Module1.BSimple)_dynamic14.getBean();
    }

    @Override
    public demo.Module1.BItemReadOnly getDynamic14_demo_Module1_BItemReadOnly() {
        return (demo.Module1.BItem)_dynamic14.getBean();
    }

    public Zeze.Transaction.Collections.PMap1<Long, Long> getMap15() {
        return _map15;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<Long, Long> getMap15ReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_map15);
    }

    public Zeze.Transaction.Collections.PMap2<demo.Module1.Key, demo.Module1.BSimple> getMap16() {
        return _map16;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<demo.Module1.Key, demo.Module1.BSimple, demo.Module1.BSimpleReadOnly> getMap16ReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_map16);
    }

    @Override
    public Zeze.Serialize.Vector2 getVector2() {
        if (!isManaged())
            return _vector2;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _vector2;
        var log = (Zeze.Transaction.Logs.LogVector2)_t_.getLog(objectId() + 17);
        return log != null ? log.value : _vector2;
    }

    public void setVector2(Zeze.Serialize.Vector2 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _vector2 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogVector2(this, 17, vh_vector2, _v_));
    }

    @Override
    public Zeze.Serialize.Vector2Int getVector2Int() {
        if (!isManaged())
            return _vector2Int;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _vector2Int;
        var log = (Zeze.Transaction.Logs.LogVector2Int)_t_.getLog(objectId() + 18);
        return log != null ? log.value : _vector2Int;
    }

    public void setVector2Int(Zeze.Serialize.Vector2Int _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _vector2Int = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogVector2Int(this, 18, vh_vector2Int, _v_));
    }

    @Override
    public Zeze.Serialize.Vector3 getVector3() {
        if (!isManaged())
            return _vector3;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _vector3;
        var log = (Zeze.Transaction.Logs.LogVector3)_t_.getLog(objectId() + 19);
        return log != null ? log.value : _vector3;
    }

    public void setVector3(Zeze.Serialize.Vector3 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _vector3 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogVector3(this, 19, vh_vector3, _v_));
    }

    @Override
    public Zeze.Serialize.Vector3Int getVector3Int() {
        if (!isManaged())
            return _vector3Int;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _vector3Int;
        var log = (Zeze.Transaction.Logs.LogVector3Int)_t_.getLog(objectId() + 20);
        return log != null ? log.value : _vector3Int;
    }

    public void setVector3Int(Zeze.Serialize.Vector3Int _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _vector3Int = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogVector3Int(this, 20, vh_vector3Int, _v_));
    }

    @Override
    public Zeze.Serialize.Vector4 getVector4() {
        if (!isManaged())
            return _vector4;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _vector4;
        var log = (Zeze.Transaction.Logs.LogVector4)_t_.getLog(objectId() + 21);
        return log != null ? log.value : _vector4;
    }

    public void setVector4(Zeze.Serialize.Vector4 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _vector4 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogVector4(this, 21, vh_vector4, _v_));
    }

    @Override
    public Zeze.Serialize.Quaternion getQuaternion() {
        if (!isManaged())
            return _quaternion;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _quaternion;
        var log = (Zeze.Transaction.Logs.LogQuaternion)_t_.getLog(objectId() + 22);
        return log != null ? log.value : _quaternion;
    }

    public void setQuaternion(Zeze.Serialize.Quaternion _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _quaternion = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogQuaternion(this, 22, vh_quaternion, _v_));
    }

    public Zeze.Transaction.DynamicBean getDynamic23() {
        return _dynamic23;
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getDynamic23ReadOnly() {
        return _dynamic23;
    }

    public Zeze.Transaction.Collections.PList1<Zeze.Serialize.Vector2Int> getListVector2Int() {
        return _listVector2Int;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Serialize.Vector2Int> getListVector2IntReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_listVector2Int);
    }

    public Zeze.Transaction.Collections.PMap2<demo.Module1.Key, demo.Module1.BSimple> getMap25() {
        return _map25;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<demo.Module1.Key, demo.Module1.BSimple, demo.Module1.BSimpleReadOnly> getMap25ReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_map25);
    }

    public Zeze.Transaction.Collections.PMap2<demo.Module1.Key, Zeze.Transaction.DynamicBean> getMap26() {
        return _map26;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<demo.Module1.Key, Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getMap26ReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_map26);
    }

    public Zeze.Transaction.DynamicBean getDynamic27() {
        return _dynamic27;
    }

    public demo.Module1.BSimple getDynamic27_demo_Module1_BSimple() {
        return (demo.Module1.BSimple)_dynamic27.getBean();
    }

    public void setDynamic27(demo.Module1.BSimple _v_) {
        _dynamic27.setBean(_v_);
    }

    @Override
    public Zeze.Transaction.DynamicBeanReadOnly getDynamic27ReadOnly() {
        return _dynamic27;
    }

    @Override
    public demo.Module1.BSimpleReadOnly getDynamic27_demo_Module1_BSimpleReadOnly() {
        return (demo.Module1.BSimple)_dynamic27.getBean();
    }

    @Override
    public demo.Module1.Key getKey28() {
        if (!isManaged())
            return _key28;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _key28;
        @SuppressWarnings("unchecked")
        var log = (Zeze.Transaction.Logs.LogBeanKey<demo.Module1.Key>)_t_.getLog(objectId() + 28);
        return log != null ? log.value : _key28;
    }

    public void setKey28(demo.Module1.Key _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _key28 = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogBeanKey<>(this, 28, vh_key28, meta_key28, _v_));
    }

    public Zeze.Transaction.Collections.PList1<Float> getArray29() {
        return _array29;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Float> getArray29ReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_array29);
    }

    public Zeze.Transaction.Collections.PList1<Integer> getList30() {
        return _list30;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Integer> getList30ReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_list30);
    }

    public Zeze.Transaction.Collections.PList1<Long> getList31() {
        return _list31;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getList31ReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_list31);
    }

    public Zeze.Transaction.Collections.PList1<Float> getList32() {
        return _list32;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Float> getList32ReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_list32);
    }

    public Zeze.Transaction.Collections.PList1<Zeze.Serialize.Vector2> getList33() {
        return _list33;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Serialize.Vector2> getList33ReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_list33);
    }

    public Zeze.Transaction.Collections.PList1<Zeze.Serialize.Vector3> getList34() {
        return _list34;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Serialize.Vector3> getList34ReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_list34);
    }

    public Zeze.Transaction.Collections.PList1<Zeze.Serialize.Vector4> getList35() {
        return _list35;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Serialize.Vector4> getList35ReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_list35);
    }

    public Zeze.Transaction.Collections.PList1<Zeze.Serialize.Vector2Int> getList36() {
        return _list36;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Serialize.Vector2Int> getList36ReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_list36);
    }

    public Zeze.Transaction.Collections.PList1<Zeze.Serialize.Vector3Int> getList37() {
        return _list37;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Serialize.Vector3Int> getList37ReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_list37);
    }

    public Zeze.Transaction.Collections.PSet1<Integer> getSet38() {
        return _set38;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Integer> getSet38ReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_set38);
    }

    public Zeze.Transaction.Collections.PSet1<Long> getSet39() {
        return _set39;
    }

    @Override
    public Zeze.Transaction.Collections.PSet1ReadOnly<Long> getSet39ReadOnly() {
        return new Zeze.Transaction.Collections.PSet1ReadOnly<>(_set39);
    }

    public Zeze.Transaction.Collections.PMap1<Integer, Integer> getMap40() {
        return _map40;
    }

    @Override
    public Zeze.Transaction.Collections.PMap1ReadOnly<Integer, Integer> getMap40ReadOnly() {
        return new Zeze.Transaction.Collections.PMap1ReadOnly<>(_map40);
    }

    public Zeze.Transaction.Collections.PMap2<Long, demo.Module1.BSimple> getMap41() {
        return _map41;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, demo.Module1.BSimple, demo.Module1.BSimpleReadOnly> getMap41ReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_map41);
    }

    public Zeze.Transaction.Collections.PMap2<Long, demo.Module1.BValue> getMap42Recursive() {
        return _map42Recursive;
    }

    @Override
    public Zeze.Transaction.Collections.PMap2ReadOnly<Long, demo.Module1.BValue, demo.Module1.BValueReadOnly> getMap42RecursiveReadOnly() {
        return new Zeze.Transaction.Collections.PMap2ReadOnly<>(_map42Recursive);
    }

    public Zeze.Transaction.Collections.PList2<Zeze.Transaction.DynamicBean> getList43() {
        return _list43;
    }

    @Override
    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Transaction.DynamicBean, Zeze.Transaction.DynamicBeanReadOnly> getList43ReadOnly() {
        return new Zeze.Transaction.Collections.PList2ReadOnly<>(_list43);
    }

    @Override
    public String getJsonObject() {
        if (!isManaged())
            return _jsonObject;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _jsonObject;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 44);
        return log != null ? log.stringValue() : _jsonObject;
    }

    public void setJsonObject(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _jsonObject = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 44, vh_jsonObject, _v_));
    }

    public com.alibaba.fastjson2.JSONObject getJsonObject_JSON_OBJECT() {
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 44);
        if (null == log || log.getValue() instanceof String) {
            var _v_ = null == log ? _jsonObject : log.stringValue();
            var _j_ = _v_.isEmpty() ? new com.alibaba.fastjson2.JSONObject() : com.alibaba.fastjson2.JSON.parseObject(_v_);
            _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 44, vh_jsonObject, _j_));
            return _j_;
        }
        return (com.alibaba.fastjson2.JSONObject)log.getValue();
    }

    @Override
    public String getJsonArray() {
        if (!isManaged())
            return _jsonArray;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _jsonArray;
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 45);
        return log != null ? log.stringValue() : _jsonArray;
    }

    public void setJsonArray(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        if (!isManaged()) {
            _jsonArray = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 45, vh_jsonArray, _v_));
    }

    public com.alibaba.fastjson2.JSONArray getJsonArray_JSON_ARRAY() {
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        var log = (Zeze.Transaction.Logs.LogString)_t_.getLog(objectId() + 45);
        if (null == log || log.getValue() instanceof String) {
            var _v_ = null == log ? _jsonArray : log.stringValue();
            var _j_ = _v_.isEmpty() ? new com.alibaba.fastjson2.JSONArray() : com.alibaba.fastjson2.JSON.parseArray(_v_);
            _t_.putLog(new Zeze.Transaction.Logs.LogString(this, 45, vh_jsonArray, _j_));
            return _j_;
        }
        return (com.alibaba.fastjson2.JSONArray)log.getValue();
    }

    @Override
    public long getRelationalMappingAlter() {
        if (!isManaged())
            return _relationalMappingAlter;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _relationalMappingAlter;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 46);
        return log != null ? log.value : _relationalMappingAlter;
    }

    public void setRelationalMappingAlter(long _v_) {
        if (!isManaged()) {
            _relationalMappingAlter = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 46, vh_relationalMappingAlter, _v_));
    }

    public Zeze.Transaction.Collections.PSortedMap1<Integer, Integer> getSortedmap1() {
        return _sortedmap1;
    }

    @Override
    public Zeze.Transaction.Collections.PSortedMap1ReadOnly<Integer, Integer> getSortedmap1ReadOnly() {
        return new Zeze.Transaction.Collections.PSortedMap1ReadOnly<>(_sortedmap1);
    }

    public Zeze.Transaction.Collections.PSortedMap2<Integer, demo.Module1.BSimple> getSortedmap2() {
        return _sortedmap2;
    }

    @Override
    public Zeze.Transaction.Collections.PSortedMap2ReadOnly<Integer, demo.Module1.BSimple, demo.Module1.BSimpleReadOnly> getSortedmap2ReadOnly() {
        return new Zeze.Transaction.Collections.PSortedMap2ReadOnly<>(_sortedmap2);
    }

    long getVersion() {
        return _version;
    }

    private void setVersion(long _v_) {
        _version = _v_;
    }

    @Override
    public long version() {
        return _version;
    }

    @Override
    public String versionVarName() {
        return "version";
    }

    @Override
    protected void version(long _v_) {
        _version = _v_;
    }

    public Zeze.Transaction.Collections.PList1<Long> getLongList() {
        return _LongList;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getLongListReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_LongList);
    }

    @SuppressWarnings("deprecation")
    public BValue() {
        _string3 = "";
        _bytes8 = Zeze.Net.Binary.Empty;
        _list9 = new Zeze.Transaction.Collections.PList2<>(meta1_list9);
        _list9.variableId(9);
        _set10 = new Zeze.Transaction.Collections.PSet1<>(meta1_set10);
        _set10.variableId(10);
        _map11 = new Zeze.Transaction.Collections.PMap2<>(meta2_map11);
        _map11.variableId(11);
        _bean12 = new Zeze.Transaction.Collections.CollOne<>(new demo.Module1.BSimple(), meta1_bean12);
        _bean12.variableId(12);
        _dynamic14 = newDynamicBean_Dynamic14();
        _map15 = new Zeze.Transaction.Collections.PMap1<>(meta2_map15);
        _map15.variableId(15);
        _map16 = new Zeze.Transaction.Collections.PMap2<>(meta2_map16);
        _map16.variableId(16);
        _vector2 = Zeze.Serialize.Vector2.ZERO;
        _vector2Int = Zeze.Serialize.Vector2Int.ZERO;
        _vector3 = Zeze.Serialize.Vector3.ZERO;
        _vector3Int = Zeze.Serialize.Vector3Int.ZERO;
        _vector4 = Zeze.Serialize.Vector4.ZERO;
        _quaternion = Zeze.Serialize.Quaternion.ZERO;
        _dynamic23 = newDynamicBean_Dynamic23();
        _listVector2Int = new Zeze.Transaction.Collections.PList1<>(meta1_listVector2Int);
        _listVector2Int.variableId(24);
        _map25 = new Zeze.Transaction.Collections.PMap2<>(meta2_map25);
        _map25.variableId(25);
        _map26 = new Zeze.Transaction.Collections.PMap2<>(meta2_map26);
        _map26.variableId(26);
        _dynamic27 = newDynamicBean_Dynamic27();
        _key28 = new demo.Module1.Key();
        _array29 = new Zeze.Transaction.Collections.PList1<>(meta1_array29);
        _array29.variableId(29);
        _list30 = new Zeze.Transaction.Collections.PList1<>(meta1_list30);
        _list30.variableId(30);
        _list31 = new Zeze.Transaction.Collections.PList1<>(meta1_list31);
        _list31.variableId(31);
        _list32 = new Zeze.Transaction.Collections.PList1<>(meta1_list32);
        _list32.variableId(32);
        _list33 = new Zeze.Transaction.Collections.PList1<>(meta1_list33);
        _list33.variableId(33);
        _list34 = new Zeze.Transaction.Collections.PList1<>(meta1_list34);
        _list34.variableId(34);
        _list35 = new Zeze.Transaction.Collections.PList1<>(meta1_list35);
        _list35.variableId(35);
        _list36 = new Zeze.Transaction.Collections.PList1<>(meta1_list36);
        _list36.variableId(36);
        _list37 = new Zeze.Transaction.Collections.PList1<>(meta1_list37);
        _list37.variableId(37);
        _set38 = new Zeze.Transaction.Collections.PSet1<>(meta1_set38);
        _set38.variableId(38);
        _set39 = new Zeze.Transaction.Collections.PSet1<>(meta1_set39);
        _set39.variableId(39);
        _map40 = new Zeze.Transaction.Collections.PMap1<>(meta2_map40);
        _map40.variableId(40);
        _map41 = new Zeze.Transaction.Collections.PMap2<>(meta2_map41);
        _map41.variableId(41);
        _map42Recursive = new Zeze.Transaction.Collections.PMap2<>(meta2_map42Recursive);
        _map42Recursive.variableId(42);
        _list43 = new Zeze.Transaction.Collections.PList2<>(meta1_list43);
        _list43.variableId(43);
        _jsonObject = "";
        _jsonArray = "";
        _sortedmap1 = new Zeze.Transaction.Collections.PSortedMap1<>(meta2_sortedmap1);
        _sortedmap1.variableId(47);
        _sortedmap2 = new Zeze.Transaction.Collections.PSortedMap2<>(meta2_sortedmap2);
        _sortedmap2.variableId(48);
        _LongList = new Zeze.Transaction.Collections.PList1<>(meta1_LongList);
        _LongList.variableId(99);
    }

    @SuppressWarnings("deprecation")
    public BValue(int _int_1_, long _long2_, @Nullable String _string3_, boolean _bool4_, short _short5_, float _float6_, double _double7_, Zeze.Net.@Nullable Binary _bytes8_, byte _byte13_, Zeze.Serialize.@Nullable Vector2 _vector2_, Zeze.Serialize.@Nullable Vector2Int _vector2Int_, Zeze.Serialize.@Nullable Vector3 _vector3_, Zeze.Serialize.@Nullable Vector3Int _vector3Int_, Zeze.Serialize.@Nullable Vector4 _vector4_, Zeze.Serialize.@Nullable Quaternion _quaternion_, demo.Module1.@Nullable Key _key28_, @Nullable String _jsonObject_, @Nullable String _jsonArray_, long _relationalMappingAlter_) {
        _int_1 = _int_1_;
        _long2 = _long2_;
        if (_string3_ == null)
            _string3_ = "";
        _string3 = _string3_;
        _bool4 = _bool4_;
        _short5 = _short5_;
        _float6 = _float6_;
        _double7 = _double7_;
        if (_bytes8_ == null)
            _bytes8_ = Zeze.Net.Binary.Empty;
        _bytes8 = _bytes8_;
        _list9 = new Zeze.Transaction.Collections.PList2<>(meta1_list9);
        _list9.variableId(9);
        _set10 = new Zeze.Transaction.Collections.PSet1<>(meta1_set10);
        _set10.variableId(10);
        _map11 = new Zeze.Transaction.Collections.PMap2<>(meta2_map11);
        _map11.variableId(11);
        _bean12 = new Zeze.Transaction.Collections.CollOne<>(new demo.Module1.BSimple(), meta1_bean12);
        _bean12.variableId(12);
        _byte13 = _byte13_;
        _dynamic14 = newDynamicBean_Dynamic14();
        _map15 = new Zeze.Transaction.Collections.PMap1<>(meta2_map15);
        _map15.variableId(15);
        _map16 = new Zeze.Transaction.Collections.PMap2<>(meta2_map16);
        _map16.variableId(16);
        if (_vector2_ == null)
            _vector2_ = Zeze.Serialize.Vector2.ZERO;
        _vector2 = _vector2_;
        if (_vector2Int_ == null)
            _vector2Int_ = Zeze.Serialize.Vector2Int.ZERO;
        _vector2Int = _vector2Int_;
        if (_vector3_ == null)
            _vector3_ = Zeze.Serialize.Vector3.ZERO;
        _vector3 = _vector3_;
        if (_vector3Int_ == null)
            _vector3Int_ = Zeze.Serialize.Vector3Int.ZERO;
        _vector3Int = _vector3Int_;
        if (_vector4_ == null)
            _vector4_ = Zeze.Serialize.Vector4.ZERO;
        _vector4 = _vector4_;
        if (_quaternion_ == null)
            _quaternion_ = Zeze.Serialize.Quaternion.ZERO;
        _quaternion = _quaternion_;
        _dynamic23 = newDynamicBean_Dynamic23();
        _listVector2Int = new Zeze.Transaction.Collections.PList1<>(meta1_listVector2Int);
        _listVector2Int.variableId(24);
        _map25 = new Zeze.Transaction.Collections.PMap2<>(meta2_map25);
        _map25.variableId(25);
        _map26 = new Zeze.Transaction.Collections.PMap2<>(meta2_map26);
        _map26.variableId(26);
        _dynamic27 = newDynamicBean_Dynamic27();
        if (_key28_ == null)
            _key28_ = new demo.Module1.Key();
        _key28 = _key28_;
        _array29 = new Zeze.Transaction.Collections.PList1<>(meta1_array29);
        _array29.variableId(29);
        _list30 = new Zeze.Transaction.Collections.PList1<>(meta1_list30);
        _list30.variableId(30);
        _list31 = new Zeze.Transaction.Collections.PList1<>(meta1_list31);
        _list31.variableId(31);
        _list32 = new Zeze.Transaction.Collections.PList1<>(meta1_list32);
        _list32.variableId(32);
        _list33 = new Zeze.Transaction.Collections.PList1<>(meta1_list33);
        _list33.variableId(33);
        _list34 = new Zeze.Transaction.Collections.PList1<>(meta1_list34);
        _list34.variableId(34);
        _list35 = new Zeze.Transaction.Collections.PList1<>(meta1_list35);
        _list35.variableId(35);
        _list36 = new Zeze.Transaction.Collections.PList1<>(meta1_list36);
        _list36.variableId(36);
        _list37 = new Zeze.Transaction.Collections.PList1<>(meta1_list37);
        _list37.variableId(37);
        _set38 = new Zeze.Transaction.Collections.PSet1<>(meta1_set38);
        _set38.variableId(38);
        _set39 = new Zeze.Transaction.Collections.PSet1<>(meta1_set39);
        _set39.variableId(39);
        _map40 = new Zeze.Transaction.Collections.PMap1<>(meta2_map40);
        _map40.variableId(40);
        _map41 = new Zeze.Transaction.Collections.PMap2<>(meta2_map41);
        _map41.variableId(41);
        _map42Recursive = new Zeze.Transaction.Collections.PMap2<>(meta2_map42Recursive);
        _map42Recursive.variableId(42);
        _list43 = new Zeze.Transaction.Collections.PList2<>(meta1_list43);
        _list43.variableId(43);
        if (_jsonObject_ == null)
            _jsonObject_ = "";
        _jsonObject = _jsonObject_;
        if (_jsonArray_ == null)
            _jsonArray_ = "";
        _jsonArray = _jsonArray_;
        _relationalMappingAlter = _relationalMappingAlter_;
        _sortedmap1 = new Zeze.Transaction.Collections.PSortedMap1<>(meta2_sortedmap1);
        _sortedmap1.variableId(47);
        _sortedmap2 = new Zeze.Transaction.Collections.PSortedMap2<>(meta2_sortedmap2);
        _sortedmap2.variableId(48);
        _LongList = new Zeze.Transaction.Collections.PList1<>(meta1_LongList);
        _LongList.variableId(99);
    }

    @Override
    public void reset() {
        setInt_1(0);
        setLong2(0);
        setString3("");
        setBool4(false);
        setShort5((short)0);
        setFloat6(0);
        setDouble7(0);
        setBytes8(Zeze.Net.Binary.Empty);
        _list9.clear();
        _set10.clear();
        _map11.clear();
        _bean12.reset();
        setByte13((byte)0);
        _dynamic14.reset();
        _map15.clear();
        _map16.clear();
        setVector2(Zeze.Serialize.Vector2.ZERO);
        setVector2Int(Zeze.Serialize.Vector2Int.ZERO);
        setVector3(Zeze.Serialize.Vector3.ZERO);
        setVector3Int(Zeze.Serialize.Vector3Int.ZERO);
        setVector4(Zeze.Serialize.Vector4.ZERO);
        setQuaternion(Zeze.Serialize.Quaternion.ZERO);
        _dynamic23.reset();
        _listVector2Int.clear();
        _map25.clear();
        _map26.clear();
        _dynamic27.reset();
        setKey28(new demo.Module1.Key());
        _array29.clear();
        _list30.clear();
        _list31.clear();
        _list32.clear();
        _list33.clear();
        _list34.clear();
        _list35.clear();
        _list36.clear();
        _list37.clear();
        _set38.clear();
        _set39.clear();
        _map40.clear();
        _map41.clear();
        _map42Recursive.clear();
        _list43.clear();
        setJsonObject("");
        setJsonArray("");
        setRelationalMappingAlter(0);
        _sortedmap1.clear();
        _sortedmap2.clear();
        setVersion(0);
        _LongList.clear();
        _unknown_ = null;
    }

    @Override
    public demo.Module1.BValue.Data toData() {
        var _d_ = new demo.Module1.BValue.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((demo.Module1.BValue.Data)_o_);
    }

    public void assign(BValue.Data _o_) {
        setInt_1(_o_._int_1);
        setLong2(_o_._long2);
        setString3(_o_._string3);
        setBool4(_o_._bool4);
        setShort5(_o_._short5);
        setFloat6(_o_._float6);
        setDouble7(_o_._double7);
        setBytes8(_o_._bytes8);
        _list9.clear();
        for (var _e_ : _o_._list9) {
            var _v_ = new demo.Bean1();
            _v_.assign(_e_);
            _list9.add(_v_);
        }
        _set10.clear();
        _set10.addAll(_o_._set10);
        _map11.clear();
        for (var _e_ : _o_._map11.entrySet()) {
            var _v_ = new demo.Module2.BValue();
            _v_.assign(_e_.getValue());
            _map11.put(_e_.getKey(), _v_);
        }
        var _d__bean12 = new demo.Module1.BSimple();
        _d__bean12.assign(_o_._bean12);
        _bean12.setValue(_d__bean12);
        setByte13(_o_._byte13);
        _dynamic14.assign(_o_._dynamic14);
        _map15.clear();
        _map15.putAll(_o_._map15);
        _map16.clear();
        for (var _e_ : _o_._map16.entrySet()) {
            var _v_ = new demo.Module1.BSimple();
            _v_.assign(_e_.getValue());
            _map16.put(_e_.getKey(), _v_);
        }
        setVector2(_o_._vector2);
        setVector2Int(_o_._vector2Int);
        setVector3(_o_._vector3);
        setVector3Int(_o_._vector3Int);
        setVector4(_o_._vector4);
        setQuaternion(_o_._quaternion);
        _dynamic23.assign(_o_._dynamic23);
        _listVector2Int.clear();
        _listVector2Int.addAll(_o_._listVector2Int);
        _map25.clear();
        for (var _e_ : _o_._map25.entrySet()) {
            var _v_ = new demo.Module1.BSimple();
            _v_.assign(_e_.getValue());
            _map25.put(_e_.getKey(), _v_);
        }
        _map26.clear();
        for (var _e_ : _o_._map26.entrySet()) {
            var _v_ = new Zeze.Transaction.DynamicBean(0, BValue::getSpecialTypeIdFromBean_26, BValue::createBeanFromSpecialTypeId_26);
            _v_.assign(_e_.getValue());
            _map26.put(_e_.getKey(), _v_);
        }
        _dynamic27.assign(_o_._dynamic27);
        setKey28(_o_._key28);
        _array29.clear();
        _array29.addAll(_o_._array29);
        _list30.clear();
        _o_._list30.addAllTo(_list30);
        _list31.clear();
        _o_._list31.addAllTo(_list31);
        _list32.clear();
        _o_._list32.addAllTo(_list32);
        _list33.clear();
        _o_._list33.addAllToVector(_list33);
        _list34.clear();
        _o_._list34.addAllToVector(_list34);
        _list35.clear();
        _o_._list35.addAllToVector(_list35);
        _list36.clear();
        _o_._list36.addAllToVector(_list36);
        _list37.clear();
        _o_._list37.addAllToVector(_list37);
        _set38.clear();
        _o_._set38.addAllTo(_set38);
        _set39.clear();
        _o_._set39.addAllTo(_set39);
        _map40.clear();
        _o_._map40.putAllTo(_map40);
        _map41.clear();
        _o_._map41.foreach((k, v) -> {
            var _v_ = new demo.Module1.BSimple();
            _v_.assign(v);
            _map41.put(k, _v_);
        });
        _map42Recursive.clear();
        for (var _e_ : _o_._map42Recursive.entrySet()) {
            var _v_ = new demo.Module1.BValue();
            _v_.assign(_e_.getValue());
            _map42Recursive.put(_e_.getKey(), _v_);
        }
        _list43.clear();
        for (var _e_ : _o_._list43) {
            var _v_ = new Zeze.Transaction.DynamicBean(0, BValue::getSpecialTypeIdFromBean_43, BValue::createBeanFromSpecialTypeId_43);
            _v_.assign(_e_);
            _list43.add(_v_);
        }
        setJsonObject(_o_._jsonObject);
        setJsonArray(_o_._jsonArray);
        setRelationalMappingAlter(_o_._relationalMappingAlter);
        _sortedmap1.clear();
        _sortedmap1.putAll(_o_._sortedmap1);
        _sortedmap2.clear();
        for (var _e_ : _o_._sortedmap2.entrySet()) {
            var _v_ = new demo.Module1.BSimple();
            _v_.assign(_e_.getValue());
            _sortedmap2.put(_e_.getKey(), _v_);
        }
        setVersion(_o_._version);
        _LongList.clear();
        _LongList.addAll(_o_._LongList);
        _unknown_ = null;
    }

    public void assign(BValue _o_) {
        setInt_1(_o_.getInt_1());
        setLong2(_o_.getLong2());
        setString3(_o_.getString3());
        setBool4(_o_.isBool4());
        setShort5(_o_.getShort5());
        setFloat6(_o_.getFloat6());
        setDouble7(_o_.getDouble7());
        setBytes8(_o_.getBytes8());
        _list9.clear();
        for (var _e_ : _o_._list9)
            _list9.add(_e_.copy());
        _set10.assign(_o_._set10);
        _map11.clear();
        for (var _e_ : _o_._map11.entrySet())
            _map11.put(_e_.getKey(), _e_.getValue().copy());
        _bean12.assign(_o_._bean12);
        setByte13(_o_.getByte13());
        _dynamic14.assign(_o_._dynamic14);
        _map15.assign(_o_._map15);
        _map16.clear();
        for (var _e_ : _o_._map16.entrySet())
            _map16.put(_e_.getKey(), _e_.getValue().copy());
        setVector2(_o_.getVector2());
        setVector2Int(_o_.getVector2Int());
        setVector3(_o_.getVector3());
        setVector3Int(_o_.getVector3Int());
        setVector4(_o_.getVector4());
        setQuaternion(_o_.getQuaternion());
        _dynamic23.assign(_o_._dynamic23);
        _listVector2Int.assign(_o_._listVector2Int);
        _map25.clear();
        for (var _e_ : _o_._map25.entrySet())
            _map25.put(_e_.getKey(), _e_.getValue().copy());
        _map26.clear();
        for (var _e_ : _o_._map26.entrySet())
            _map26.put(_e_.getKey(), _e_.getValue().copy());
        _dynamic27.assign(_o_._dynamic27);
        setKey28(_o_.getKey28());
        _array29.assign(_o_._array29);
        _list30.assign(_o_._list30);
        _list31.assign(_o_._list31);
        _list32.assign(_o_._list32);
        _list33.assign(_o_._list33);
        _list34.assign(_o_._list34);
        _list35.assign(_o_._list35);
        _list36.assign(_o_._list36);
        _list37.assign(_o_._list37);
        _set38.assign(_o_._set38);
        _set39.assign(_o_._set39);
        _map40.assign(_o_._map40);
        _map41.clear();
        for (var _e_ : _o_._map41.entrySet())
            _map41.put(_e_.getKey(), _e_.getValue().copy());
        _map42Recursive.clear();
        for (var _e_ : _o_._map42Recursive.entrySet())
            _map42Recursive.put(_e_.getKey(), _e_.getValue().copy());
        _list43.clear();
        for (var _e_ : _o_._list43)
            _list43.add(_e_.copy());
        setJsonObject(_o_.getJsonObject());
        setJsonArray(_o_.getJsonArray());
        setRelationalMappingAlter(_o_.getRelationalMappingAlter());
        _sortedmap1.assign(_o_._sortedmap1);
        _sortedmap2.clear();
        for (var _e_ : _o_._sortedmap2.entrySet())
            _sortedmap2.put(_e_.getKey(), _e_.getValue().copy());
        setVersion(_o_.getVersion());
        _LongList.assign(_o_._LongList);
        _unknown_ = _o_._unknown_;
    }

    public BValue copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BValue copy() {
        var _c_ = new BValue();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BValue _a_, BValue _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("demo.Module1.BValue: {\n");
        _s_.append(_i1_).append("int_1=").append(getInt_1()).append(",\n");
        _s_.append(_i1_).append("long2=").append(getLong2()).append(",\n");
        _s_.append(_i1_).append("string3=").append(getString3()).append(",\n");
        _s_.append(_i1_).append("bool4=").append(isBool4()).append(",\n");
        _s_.append(_i1_).append("short5=").append(getShort5()).append(",\n");
        _s_.append(_i1_).append("float6=").append(getFloat6()).append(",\n");
        _s_.append(_i1_).append("double7=").append(getDouble7()).append(",\n");
        _s_.append(_i1_).append("bytes8=").append(getBytes8()).append(",\n");
        _s_.append(_i1_).append("list9=[");
        if (!_list9.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _list9) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_list9.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("set10={");
        if (!_set10.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _set10) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_set10.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("map11={");
        if (!_map11.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _map11.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_map11.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("bean12=");
        _bean12.buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("byte13=").append(getByte13()).append(",\n");
        _s_.append(_i1_).append("dynamic14=");
        _dynamic14.getBean().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("map15={");
        if (!_map15.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _map15.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_map15.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("map16={");
        if (!_map16.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _map16.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_map16.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=");
                _e_.getKey().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("vector2=").append(getVector2()).append(",\n");
        _s_.append(_i1_).append("vector2Int=").append(getVector2Int()).append(",\n");
        _s_.append(_i1_).append("vector3=").append(getVector3()).append(",\n");
        _s_.append(_i1_).append("vector3Int=").append(getVector3Int()).append(",\n");
        _s_.append(_i1_).append("vector4=").append(getVector4()).append(",\n");
        _s_.append(_i1_).append("quaternion=").append(getQuaternion()).append(",\n");
        _s_.append(_i1_).append("dynamic23=");
        _dynamic23.getBean().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("listVector2Int=[");
        if (!_listVector2Int.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _listVector2Int) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_listVector2Int.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("map25={");
        if (!_map25.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _map25.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_map25.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=");
                _e_.getKey().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("map26={");
        if (!_map26.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _map26.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_map26.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=");
                _e_.getKey().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().getBean().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("dynamic27=");
        _dynamic27.getBean().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("key28=");
        getKey28().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("array29=[");
        if (!_array29.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _array29) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_array29.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("list30=[");
        if (!_list30.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _list30) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_list30.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("list31=[");
        if (!_list31.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _list31) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_list31.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("list32=[");
        if (!_list32.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _list32) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_list32.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("list33=[");
        if (!_list33.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _list33) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_list33.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("list34=[");
        if (!_list34.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _list34) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_list34.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("list35=[");
        if (!_list35.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _list35) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_list35.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("list36=[");
        if (!_list36.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _list36) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_list36.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("list37=[");
        if (!_list37.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _list37) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_list37.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("set38={");
        if (!_set38.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _set38) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_set38.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("set39={");
        if (!_set39.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _set39) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_set39.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("map40={");
        if (!_map40.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _map40.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_map40.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("map41={");
        if (!_map41.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _map41.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_map41.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("map42Recursive={");
        if (!_map42Recursive.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _map42Recursive.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_map42Recursive.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("list43=[");
        if (!_list43.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _list43) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_list43.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.getBean().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("jsonObject=").append(getJsonObject()).append(",\n");
        _s_.append(_i1_).append("jsonArray=").append(getJsonArray()).append(",\n");
        _s_.append(_i1_).append("relationalMappingAlter=").append(getRelationalMappingAlter()).append(",\n");
        _s_.append(_i1_).append("sortedmap1={");
        if (!_sortedmap1.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _sortedmap1.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_sortedmap1.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("sortedmap2={");
        if (!_sortedmap2.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _sortedmap2.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_sortedmap2.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("version=").append(getVersion()).append(",\n");
        _s_.append(_i1_).append("LongList=[");
        if (!_LongList.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _LongList) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_LongList.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("]\n");
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
    }

    private byte @Nullable [] _unknown_;

    public byte @Nullable [] unknown() {
        return _unknown_;
    }

    public void clearUnknown() {
        _unknown_ = null;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        var _ua_ = _unknown_;
        var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
        int _i_ = 0;
        {
            int _x_ = getInt_1();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = getLong2();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = getString3();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            boolean _x_ = isBool4();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            int _x_ = getShort5();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            float _x_ = getFloat6();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.FLOAT);
                _o_.WriteFloat(_x_);
            }
        }
        {
            double _x_ = getDouble7();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.DOUBLE);
                _o_.WriteDouble(_x_);
            }
        }
        {
            var _x_ = getBytes8();
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _list9;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _set10;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 10, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _map11;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 11, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 12, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _bean12.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _x_ = getByte13();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 13, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _dynamic14;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 14, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            var _x_ = _map15;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 15, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _map16;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 16, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BEAN, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _e_.getKey().encode(_o_);
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = getVector2();
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 17, ByteBuffer.VECTOR2);
                _o_.WriteVector2(_x_);
            }
        }
        {
            var _x_ = getVector2Int();
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 18, ByteBuffer.VECTOR2INT);
                _o_.WriteVector2Int(_x_);
            }
        }
        {
            var _x_ = getVector3();
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 19, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            var _x_ = getVector3Int();
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 20, ByteBuffer.VECTOR3INT);
                _o_.WriteVector3Int(_x_);
            }
        }
        {
            var _x_ = getVector4();
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 21, ByteBuffer.VECTOR4);
                _o_.WriteVector4(_x_);
            }
        }
        {
            var _x_ = getQuaternion();
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 22, ByteBuffer.VECTOR4);
                _o_.WriteQuaternion(_x_);
            }
        }
        {
            var _x_ = _dynamic23;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 23, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            var _x_ = _listVector2Int;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 24, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.VECTOR2INT);
                for (var _v_ : _x_) {
                    _o_.WriteVector2Int(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _map25;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 25, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BEAN, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _e_.getKey().encode(_o_);
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _map26;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 26, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BEAN, ByteBuffer.DYNAMIC);
                for (var _e_ : _x_.entrySet()) {
                    _e_.getKey().encode(_o_);
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _dynamic27;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 27, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 28, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            getKey28().encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            var _x_ = _array29;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 29, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.FLOAT);
                for (var _v_ : _x_) {
                    _o_.WriteFloat(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _list30;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 30, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _list31;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 31, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _list32;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 32, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.FLOAT);
                for (var _v_ : _x_) {
                    _o_.WriteFloat(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _list33;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 33, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.VECTOR2);
                for (var _v_ : _x_) {
                    _o_.WriteVector2(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _list34;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 34, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.VECTOR3);
                for (var _v_ : _x_) {
                    _o_.WriteVector3(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _list35;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 35, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.VECTOR4);
                for (var _v_ : _x_) {
                    _o_.WriteVector4(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _list36;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 36, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.VECTOR2INT);
                for (var _v_ : _x_) {
                    _o_.WriteVector2Int(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _list37;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 37, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.VECTOR3INT);
                for (var _v_ : _x_) {
                    _o_.WriteVector3Int(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _set38;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 38, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _set39;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 39, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _map40;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 40, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _map41;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 41, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _map42Recursive;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 42, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _list43;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 43, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.DYNAMIC);
                for (var _v_ : _x_) {
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            String _x_ = getJsonObject();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 44, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = getJsonArray();
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 45, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = getRelationalMappingAlter();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 46, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _sortedmap1;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 47, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _sortedmap2;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 48, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        while (_ui_ < 50) {
            _i_ = _o_.writeUnknownField(_i_, _ui_, _u_);
            _ui_ = _u_.readUnknownIndex();
        }
        {
            long _x_ = getVersion();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 50, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        while (_ui_ < 99) {
            _i_ = _o_.writeUnknownField(_i_, _ui_, _u_);
            _ui_ = _u_.readUnknownIndex();
        }
        {
            var _x_ = _LongList;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 99, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setInt_1(_o_.ReadInt(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setLong2(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            setString3(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            setBool4(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            setShort5(_o_.ReadShort(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            setFloat6(_o_.ReadFloat(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            setDouble7(_o_.ReadDouble(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            setBytes8(_o_.ReadBinary(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            var _x_ = _list9;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new demo.Bean1(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 10) {
            var _x_ = _set10;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 11) {
            var _x_ = _map11;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new demo.Module2.BValue(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 12) {
            _o_.ReadBean(_bean12, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 13) {
            setByte13(_o_.ReadByte(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 14) {
            _o_.ReadDynamic(_dynamic14, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 15) {
            var _x_ = _map15;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadLong(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 16) {
            var _x_ = _map16;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBean(new demo.Module1.Key(), _s_);
                    var _v_ = _o_.ReadBean(new demo.Module1.BSimple(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 17) {
            setVector2(_o_.ReadVector2(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 18) {
            setVector2Int(_o_.ReadVector2Int(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 19) {
            setVector3(_o_.ReadVector3(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 20) {
            setVector3Int(_o_.ReadVector3Int(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 21) {
            setVector4(_o_.ReadVector4(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 22) {
            setQuaternion(_o_.ReadQuaternion(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 23) {
            _o_.ReadDynamic(_dynamic23, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 24) {
            var _x_ = _listVector2Int;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadVector2Int(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 25) {
            var _x_ = _map25;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBean(new demo.Module1.Key(), _s_);
                    var _v_ = _o_.ReadBean(new demo.Module1.BSimple(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 26) {
            var _x_ = _map26;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBean(new demo.Module1.Key(), _s_);
                    var _v_ = new Zeze.Transaction.DynamicBean(0, BValue::getSpecialTypeIdFromBean_26, BValue::createBeanFromSpecialTypeId_26);
                    _o_.ReadDynamic(_v_, _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 27) {
            _o_.ReadDynamic(_dynamic27, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 28) {
            _o_.ReadBean(getKey28(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 29) {
            var _x_ = _array29;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadFloat(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 30) {
            var _x_ = _list30;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 31) {
            var _x_ = _list31;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 32) {
            var _x_ = _list32;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadFloat(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 33) {
            var _x_ = _list33;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadVector2(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 34) {
            var _x_ = _list34;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadVector3(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 35) {
            var _x_ = _list35;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadVector4(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 36) {
            var _x_ = _list36;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadVector2Int(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 37) {
            var _x_ = _list37;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadVector3Int(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 38) {
            var _x_ = _set38;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 39) {
            var _x_ = _set39;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 40) {
            var _x_ = _map40;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadInt(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 41) {
            var _x_ = _map41;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new demo.Module1.BSimple(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 42) {
            var _x_ = _map42Recursive;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new demo.Module1.BValue(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 43) {
            var _x_ = _list43;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                    var _e_ = new Zeze.Transaction.DynamicBean(0, BValue::getSpecialTypeIdFromBean_43, BValue::createBeanFromSpecialTypeId_43);
                    _o_.ReadDynamic(_e_, _t_);
                    _x_.add(_e_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 44) {
            setJsonObject(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 45) {
            setJsonArray(_o_.ReadString(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 46) {
            setRelationalMappingAlter(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 47) {
            var _x_ = _sortedmap1;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadInt(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 48) {
            var _x_ = _sortedmap2;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new demo.Module1.BSimple(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while ((_t_ & 0xff) > 1 && _i_ < 50) {
            _u_ = _o_.readUnknownField(_i_, _t_, _u_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 50) {
            setVersion(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while ((_t_ & 0xff) > 1 && _i_ < 99) {
            _u_ = _o_.readUnknownField(_i_, _t_, _u_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 99) {
            var _x_ = _LongList;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BValue))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BValue)_o_;
        if (getInt_1() != _b_.getInt_1())
            return false;
        if (getLong2() != _b_.getLong2())
            return false;
        if (!getString3().equals(_b_.getString3()))
            return false;
        if (isBool4() != _b_.isBool4())
            return false;
        if (getShort5() != _b_.getShort5())
            return false;
        if (getFloat6() != _b_.getFloat6())
            return false;
        if (getDouble7() != _b_.getDouble7())
            return false;
        if (!getBytes8().equals(_b_.getBytes8()))
            return false;
        if (!_list9.equals(_b_._list9))
            return false;
        if (!_set10.equals(_b_._set10))
            return false;
        if (!_map11.equals(_b_._map11))
            return false;
        if (!_bean12.equals(_b_._bean12))
            return false;
        if (getByte13() != _b_.getByte13())
            return false;
        if (!_dynamic14.equals(_b_._dynamic14))
            return false;
        if (!_map15.equals(_b_._map15))
            return false;
        if (!_map16.equals(_b_._map16))
            return false;
        if (!getVector2().equals(_b_.getVector2()))
            return false;
        if (!getVector2Int().equals(_b_.getVector2Int()))
            return false;
        if (!getVector3().equals(_b_.getVector3()))
            return false;
        if (!getVector3Int().equals(_b_.getVector3Int()))
            return false;
        if (!getVector4().equals(_b_.getVector4()))
            return false;
        if (!getQuaternion().equals(_b_.getQuaternion()))
            return false;
        if (!_dynamic23.equals(_b_._dynamic23))
            return false;
        if (!_listVector2Int.equals(_b_._listVector2Int))
            return false;
        if (!_map25.equals(_b_._map25))
            return false;
        if (!_map26.equals(_b_._map26))
            return false;
        if (!_dynamic27.equals(_b_._dynamic27))
            return false;
        if (!getKey28().equals(_b_.getKey28()))
            return false;
        if (!_array29.equals(_b_._array29))
            return false;
        if (!_list30.equals(_b_._list30))
            return false;
        if (!_list31.equals(_b_._list31))
            return false;
        if (!_list32.equals(_b_._list32))
            return false;
        if (!_list33.equals(_b_._list33))
            return false;
        if (!_list34.equals(_b_._list34))
            return false;
        if (!_list35.equals(_b_._list35))
            return false;
        if (!_list36.equals(_b_._list36))
            return false;
        if (!_list37.equals(_b_._list37))
            return false;
        if (!_set38.equals(_b_._set38))
            return false;
        if (!_set39.equals(_b_._set39))
            return false;
        if (!_map40.equals(_b_._map40))
            return false;
        if (!_map41.equals(_b_._map41))
            return false;
        if (!_map42Recursive.equals(_b_._map42Recursive))
            return false;
        if (!_list43.equals(_b_._list43))
            return false;
        if (!getJsonObject().equals(_b_.getJsonObject()))
            return false;
        if (!getJsonArray().equals(_b_.getJsonArray()))
            return false;
        if (getRelationalMappingAlter() != _b_.getRelationalMappingAlter())
            return false;
        if (!_sortedmap1.equals(_b_._sortedmap1))
            return false;
        if (!_sortedmap2.equals(_b_._sortedmap2))
            return false;
        if (!_LongList.equals(_b_._LongList))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + Integer.hashCode(getInt_1());
        _h_ = _h_ * _p_ + Long.hashCode(getLong2());
        _h_ = _h_ * _p_ + getString3().hashCode();
        _h_ = _h_ * _p_ + Boolean.hashCode(isBool4());
        _h_ = _h_ * _p_ + Short.hashCode(getShort5());
        _h_ = _h_ * _p_ + Float.hashCode(getFloat6());
        _h_ = _h_ * _p_ + Double.hashCode(getDouble7());
        _h_ = _h_ * _p_ + getBytes8().hashCode();
        _h_ = _h_ * _p_ + _list9.hashCode();
        _h_ = _h_ * _p_ + _set10.hashCode();
        _h_ = _h_ * _p_ + _map11.hashCode();
        _h_ = _h_ * _p_ + _bean12.hashCode();
        _h_ = _h_ * _p_ + Byte.hashCode(getByte13());
        _h_ = _h_ * _p_ + _dynamic14.hashCode();
        _h_ = _h_ * _p_ + _map15.hashCode();
        _h_ = _h_ * _p_ + _map16.hashCode();
        _h_ = _h_ * _p_ + getVector2().hashCode();
        _h_ = _h_ * _p_ + getVector2Int().hashCode();
        _h_ = _h_ * _p_ + getVector3().hashCode();
        _h_ = _h_ * _p_ + getVector3Int().hashCode();
        _h_ = _h_ * _p_ + getVector4().hashCode();
        _h_ = _h_ * _p_ + getQuaternion().hashCode();
        _h_ = _h_ * _p_ + _dynamic23.hashCode();
        _h_ = _h_ * _p_ + _listVector2Int.hashCode();
        _h_ = _h_ * _p_ + _map25.hashCode();
        _h_ = _h_ * _p_ + _map26.hashCode();
        _h_ = _h_ * _p_ + _dynamic27.hashCode();
        _h_ = _h_ * _p_ + getKey28().hashCode();
        _h_ = _h_ * _p_ + _array29.hashCode();
        _h_ = _h_ * _p_ + _list30.hashCode();
        _h_ = _h_ * _p_ + _list31.hashCode();
        _h_ = _h_ * _p_ + _list32.hashCode();
        _h_ = _h_ * _p_ + _list33.hashCode();
        _h_ = _h_ * _p_ + _list34.hashCode();
        _h_ = _h_ * _p_ + _list35.hashCode();
        _h_ = _h_ * _p_ + _list36.hashCode();
        _h_ = _h_ * _p_ + _list37.hashCode();
        _h_ = _h_ * _p_ + _set38.hashCode();
        _h_ = _h_ * _p_ + _set39.hashCode();
        _h_ = _h_ * _p_ + _map40.hashCode();
        _h_ = _h_ * _p_ + _map41.hashCode();
        _h_ = _h_ * _p_ + _map42Recursive.hashCode();
        _h_ = _h_ * _p_ + _list43.hashCode();
        _h_ = _h_ * _p_ + getJsonObject().hashCode();
        _h_ = _h_ * _p_ + getJsonArray().hashCode();
        _h_ = _h_ * _p_ + Long.hashCode(getRelationalMappingAlter());
        _h_ = _h_ * _p_ + _sortedmap1.hashCode();
        _h_ = _h_ * _p_ + _sortedmap2.hashCode();
        _h_ = _h_ * _p_ + _LongList.hashCode();
        return _h_;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _list9.initRootInfo(_r_, this);
        _set10.initRootInfo(_r_, this);
        _map11.initRootInfo(_r_, this);
        _bean12.initRootInfo(_r_, this);
        _dynamic14.initRootInfo(_r_, this);
        _map15.initRootInfo(_r_, this);
        _map16.initRootInfo(_r_, this);
        _dynamic23.initRootInfo(_r_, this);
        _listVector2Int.initRootInfo(_r_, this);
        _map25.initRootInfo(_r_, this);
        _map26.initRootInfo(_r_, this);
        _dynamic27.initRootInfo(_r_, this);
        _array29.initRootInfo(_r_, this);
        _list30.initRootInfo(_r_, this);
        _list31.initRootInfo(_r_, this);
        _list32.initRootInfo(_r_, this);
        _list33.initRootInfo(_r_, this);
        _list34.initRootInfo(_r_, this);
        _list35.initRootInfo(_r_, this);
        _list36.initRootInfo(_r_, this);
        _list37.initRootInfo(_r_, this);
        _set38.initRootInfo(_r_, this);
        _set39.initRootInfo(_r_, this);
        _map40.initRootInfo(_r_, this);
        _map41.initRootInfo(_r_, this);
        _map42Recursive.initRootInfo(_r_, this);
        _list43.initRootInfo(_r_, this);
        _sortedmap1.initRootInfo(_r_, this);
        _sortedmap2.initRootInfo(_r_, this);
        _LongList.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _list9.initRootInfoWithRedo(_r_, this);
        _set10.initRootInfoWithRedo(_r_, this);
        _map11.initRootInfoWithRedo(_r_, this);
        _bean12.initRootInfoWithRedo(_r_, this);
        _dynamic14.initRootInfoWithRedo(_r_, this);
        _map15.initRootInfoWithRedo(_r_, this);
        _map16.initRootInfoWithRedo(_r_, this);
        _dynamic23.initRootInfoWithRedo(_r_, this);
        _listVector2Int.initRootInfoWithRedo(_r_, this);
        _map25.initRootInfoWithRedo(_r_, this);
        _map26.initRootInfoWithRedo(_r_, this);
        _dynamic27.initRootInfoWithRedo(_r_, this);
        _array29.initRootInfoWithRedo(_r_, this);
        _list30.initRootInfoWithRedo(_r_, this);
        _list31.initRootInfoWithRedo(_r_, this);
        _list32.initRootInfoWithRedo(_r_, this);
        _list33.initRootInfoWithRedo(_r_, this);
        _list34.initRootInfoWithRedo(_r_, this);
        _list35.initRootInfoWithRedo(_r_, this);
        _list36.initRootInfoWithRedo(_r_, this);
        _list37.initRootInfoWithRedo(_r_, this);
        _set38.initRootInfoWithRedo(_r_, this);
        _set39.initRootInfoWithRedo(_r_, this);
        _map40.initRootInfoWithRedo(_r_, this);
        _map41.initRootInfoWithRedo(_r_, this);
        _map42Recursive.initRootInfoWithRedo(_r_, this);
        _list43.initRootInfoWithRedo(_r_, this);
        _sortedmap1.initRootInfoWithRedo(_r_, this);
        _sortedmap2.initRootInfoWithRedo(_r_, this);
        _LongList.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getLong2() < 0)
            return true;
        if (getShort5() < 0)
            return true;
        for (var _v_ : _list9) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _set10) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _map11.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        if (_bean12.negativeCheck())
            return true;
        if (_dynamic14.negativeCheck())
            return true;
        for (var _v_ : _map15.values()) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _map16.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _map25.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _map26.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        if (_dynamic27.negativeCheck())
            return true;
        if (getKey28().negativeCheck())
            return true;
        for (var _v_ : _list30) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _list31) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _set38) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _set39) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _map40.values()) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _map41.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _map42Recursive.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        for (var _v_ : _list43) {
            if (_v_.negativeCheck())
                return true;
        }
        if (getRelationalMappingAlter() < 0)
            return true;
        for (var _v_ : _sortedmap1.values()) {
            if (_v_ < 0)
                return true;
        }
        for (var _v_ : _sortedmap2.values()) {
            if (_v_.negativeCheck())
                return true;
        }
        if (getVersion() < 0)
            return true;
        for (var _v_ : _LongList) {
            if (_v_ < 0)
                return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _int_1 = _v_.intValue(); break;
                case 2: _long2 = _v_.longValue(); break;
                case 3: _string3 = _v_.stringValue(); break;
                case 4: _bool4 = _v_.booleanValue(); break;
                case 5: _short5 = _v_.shortValue(); break;
                case 6: _float6 = _v_.floatValue(); break;
                case 7: _double7 = _v_.doubleValue(); break;
                case 8: _bytes8 = _v_.binaryValue(); break;
                case 9: _list9.followerApply(_v_); break;
                case 10: _set10.followerApply(_v_); break;
                case 11: _map11.followerApply(_v_); break;
                case 12: _bean12.followerApply(_v_); break;
                case 13: _byte13 = _v_.byteValue(); break;
                case 14: _dynamic14.followerApply(_v_); break;
                case 15: _map15.followerApply(_v_); break;
                case 16: _map16.followerApply(_v_); break;
                case 17: _vector2 = _v_.vector2Value(); break;
                case 18: _vector2Int = _v_.vector2IntValue(); break;
                case 19: _vector3 = _v_.vector3Value(); break;
                case 20: _vector3Int = _v_.vector3IntValue(); break;
                case 21: _vector4 = _v_.vector4Value(); break;
                case 22: _quaternion = _v_.quaternionValue(); break;
                case 23: _dynamic23.followerApply(_v_); break;
                case 24: _listVector2Int.followerApply(_v_); break;
                case 25: _map25.followerApply(_v_); break;
                case 26: _map26.followerApply(_v_); break;
                case 27: _dynamic27.followerApply(_v_); break;
                case 28: _key28 = ((Zeze.Transaction.Logs.LogBeanKeyValue<demo.Module1.Key>)_v_).value; break;
                case 29: _array29.followerApply(_v_); break;
                case 30: _list30.followerApply(_v_); break;
                case 31: _list31.followerApply(_v_); break;
                case 32: _list32.followerApply(_v_); break;
                case 33: _list33.followerApply(_v_); break;
                case 34: _list34.followerApply(_v_); break;
                case 35: _list35.followerApply(_v_); break;
                case 36: _list36.followerApply(_v_); break;
                case 37: _list37.followerApply(_v_); break;
                case 38: _set38.followerApply(_v_); break;
                case 39: _set39.followerApply(_v_); break;
                case 40: _map40.followerApply(_v_); break;
                case 41: _map41.followerApply(_v_); break;
                case 42: _map42Recursive.followerApply(_v_); break;
                case 43: _list43.followerApply(_v_); break;
                case 44: _jsonObject = _v_.stringValue(); break;
                case 45: _jsonArray = _v_.stringValue(); break;
                case 46: _relationalMappingAlter = _v_.longValue(); break;
                case 47: _sortedmap1.followerApply(_v_); break;
                case 48: _sortedmap2.followerApply(_v_); break;
                case 99: _LongList.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setInt_1(_r_.getInt(_pn_ + "int_1"));
        setLong2(_r_.getLong(_pn_ + "long2"));
        setString3(_r_.getString(_pn_ + "string3"));
        if (getString3() == null)
            setString3("");
        setBool4(_r_.getBoolean(_pn_ + "bool4"));
        setShort5(_r_.getShort(_pn_ + "short5"));
        setFloat6(_r_.getFloat(_pn_ + "float6"));
        setDouble7(_r_.getDouble(_pn_ + "double7"));
        setBytes8(new Zeze.Net.Binary(_r_.getBytes(_pn_ + "bytes8")));
        Zeze.Serialize.Helper.decodeJsonList(_list9, demo.Bean1.class, _r_.getString(_pn_ + "list9"));
        Zeze.Serialize.Helper.decodeJsonSet(_set10, Integer.class, _r_.getString(_pn_ + "set10"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "map11", _map11, _r_.getString(_pn_ + "map11"));
        _p_.add("bean12");
        _bean12.decodeResultSet(_p_, _r_);
        _p_.removeLast();
        setByte13(_r_.getByte(_pn_ + "byte13"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_dynamic14, _r_.getString(_pn_ + "dynamic14"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "map15", _map15, _r_.getString(_pn_ + "map15"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "map16", _map16, _r_.getString(_pn_ + "map16"));
        _p_.add("vector2");
        setVector2(Zeze.Serialize.Helper.decodeVector2(_p_, _r_));
        _p_.removeLast();
        _p_.add("vector2Int");
        setVector2Int(Zeze.Serialize.Helper.decodeVector2Int(_p_, _r_));
        _p_.removeLast();
        _p_.add("vector3");
        setVector3(Zeze.Serialize.Helper.decodeVector3(_p_, _r_));
        _p_.removeLast();
        _p_.add("vector3Int");
        setVector3Int(Zeze.Serialize.Helper.decodeVector3Int(_p_, _r_));
        _p_.removeLast();
        _p_.add("vector4");
        setVector4(Zeze.Serialize.Helper.decodeVector4(_p_, _r_));
        _p_.removeLast();
        _p_.add("quaternion");
        setQuaternion(Zeze.Serialize.Helper.decodeQuaternion(_p_, _r_));
        _p_.removeLast();
        Zeze.Serialize.Helper.decodeJsonDynamic(_dynamic23, _r_.getString(_pn_ + "dynamic23"));
        Zeze.Serialize.Helper.decodeJsonList(_listVector2Int, Zeze.Serialize.Vector2Int.class, _r_.getString(_pn_ + "listVector2Int"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "map25", _map25, _r_.getString(_pn_ + "map25"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "map26", _map26, _r_.getString(_pn_ + "map26"));
        Zeze.Serialize.Helper.decodeJsonDynamic(_dynamic27, _r_.getString(_pn_ + "dynamic27"));
        _p_.add("key28");
        getKey28().decodeResultSet(_p_, _r_);
        _p_.removeLast();
        Zeze.Serialize.Helper.decodeJsonList(_array29, Float.class, _r_.getString(_pn_ + "array29"));
        Zeze.Serialize.Helper.decodeJsonList(_list30, Integer.class, _r_.getString(_pn_ + "list30"));
        Zeze.Serialize.Helper.decodeJsonList(_list31, Long.class, _r_.getString(_pn_ + "list31"));
        Zeze.Serialize.Helper.decodeJsonList(_list32, Float.class, _r_.getString(_pn_ + "list32"));
        Zeze.Serialize.Helper.decodeJsonList(_list33, Zeze.Serialize.Vector2.class, _r_.getString(_pn_ + "list33"));
        Zeze.Serialize.Helper.decodeJsonList(_list34, Zeze.Serialize.Vector3.class, _r_.getString(_pn_ + "list34"));
        Zeze.Serialize.Helper.decodeJsonList(_list35, Zeze.Serialize.Vector4.class, _r_.getString(_pn_ + "list35"));
        Zeze.Serialize.Helper.decodeJsonList(_list36, Zeze.Serialize.Vector2Int.class, _r_.getString(_pn_ + "list36"));
        Zeze.Serialize.Helper.decodeJsonList(_list37, Zeze.Serialize.Vector3Int.class, _r_.getString(_pn_ + "list37"));
        Zeze.Serialize.Helper.decodeJsonSet(_set38, Integer.class, _r_.getString(_pn_ + "set38"));
        Zeze.Serialize.Helper.decodeJsonSet(_set39, Long.class, _r_.getString(_pn_ + "set39"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "map40", _map40, _r_.getString(_pn_ + "map40"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "map41", _map41, _r_.getString(_pn_ + "map41"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "map42Recursive", _map42Recursive, _r_.getString(_pn_ + "map42Recursive"));
        Zeze.Serialize.Helper.decodeJsonList(_list43, Zeze.Transaction.DynamicBean.class, _r_.getString(_pn_ + "list43"));
        setJsonObject(_r_.getString(_pn_ + "jsonObject"));
        if (getJsonObject() == null)
            setJsonObject("");
        setJsonArray(_r_.getString(_pn_ + "jsonArray"));
        if (getJsonArray() == null)
            setJsonArray("");
        setRelationalMappingAlter(_r_.getLong(_pn_ + "relationalMappingAlter"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "sortedmap1", _sortedmap1, _r_.getString(_pn_ + "sortedmap1"));
        Zeze.Serialize.Helper.decodeJsonMap(this, "sortedmap2", _sortedmap2, _r_.getString(_pn_ + "sortedmap2"));
        setVersion(_r_.getLong(_pn_ + "version"));
        Zeze.Serialize.Helper.decodeJsonList(_LongList, Long.class, _r_.getString(_pn_ + "LongList"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "int_1", getInt_1());
        _s_.appendLong(_pn_ + "long2", getLong2());
        _s_.appendString(_pn_ + "string3", getString3());
        _s_.appendBoolean(_pn_ + "bool4", isBool4());
        _s_.appendShort(_pn_ + "short5", getShort5());
        _s_.appendFloat(_pn_ + "float6", getFloat6());
        _s_.appendDouble(_pn_ + "double7", getDouble7());
        _s_.appendBinary(_pn_ + "bytes8", getBytes8());
        _s_.appendString(_pn_ + "list9", Zeze.Serialize.Helper.encodeJson(_list9));
        _s_.appendString(_pn_ + "set10", Zeze.Serialize.Helper.encodeJson(_set10));
        _s_.appendString(_pn_ + "map11", Zeze.Serialize.Helper.encodeJson(_map11));
        _p_.add("bean12");
        _bean12.encodeSQLStatement(_p_, _s_);
        _p_.removeLast();
        _s_.appendByte(_pn_ + "byte13", getByte13());
        _s_.appendString(_pn_ + "dynamic14", Zeze.Serialize.Helper.encodeJson(_dynamic14));
        _s_.appendString(_pn_ + "map15", Zeze.Serialize.Helper.encodeJson(_map15));
        _s_.appendString(_pn_ + "map16", Zeze.Serialize.Helper.encodeJson(_map16));
        _p_.add("vector2");
        Zeze.Serialize.Helper.encodeVector2(getVector2(), _p_, _s_);
        _p_.removeLast();
        _p_.add("vector2Int");
        Zeze.Serialize.Helper.encodeVector2Int(getVector2Int(), _p_, _s_);
        _p_.removeLast();
        _p_.add("vector3");
        Zeze.Serialize.Helper.encodeVector3(getVector3(), _p_, _s_);
        _p_.removeLast();
        _p_.add("vector3Int");
        Zeze.Serialize.Helper.encodeVector3Int(getVector3Int(), _p_, _s_);
        _p_.removeLast();
        _p_.add("vector4");
        Zeze.Serialize.Helper.encodeVector4(getVector4(), _p_, _s_);
        _p_.removeLast();
        _p_.add("quaternion");
        Zeze.Serialize.Helper.encodeQuaternion(getQuaternion(), _p_, _s_);
        _p_.removeLast();
        _s_.appendString(_pn_ + "dynamic23", Zeze.Serialize.Helper.encodeJson(_dynamic23));
        _s_.appendString(_pn_ + "listVector2Int", Zeze.Serialize.Helper.encodeJson(_listVector2Int));
        _s_.appendString(_pn_ + "map25", Zeze.Serialize.Helper.encodeJson(_map25));
        _s_.appendString(_pn_ + "map26", Zeze.Serialize.Helper.encodeJson(_map26));
        _s_.appendString(_pn_ + "dynamic27", Zeze.Serialize.Helper.encodeJson(_dynamic27));
        _p_.add("key28");
        getKey28().encodeSQLStatement(_p_, _s_);
        _p_.removeLast();
        _s_.appendString(_pn_ + "array29", Zeze.Serialize.Helper.encodeJson(_array29));
        _s_.appendString(_pn_ + "list30", Zeze.Serialize.Helper.encodeJson(_list30));
        _s_.appendString(_pn_ + "list31", Zeze.Serialize.Helper.encodeJson(_list31));
        _s_.appendString(_pn_ + "list32", Zeze.Serialize.Helper.encodeJson(_list32));
        _s_.appendString(_pn_ + "list33", Zeze.Serialize.Helper.encodeJson(_list33));
        _s_.appendString(_pn_ + "list34", Zeze.Serialize.Helper.encodeJson(_list34));
        _s_.appendString(_pn_ + "list35", Zeze.Serialize.Helper.encodeJson(_list35));
        _s_.appendString(_pn_ + "list36", Zeze.Serialize.Helper.encodeJson(_list36));
        _s_.appendString(_pn_ + "list37", Zeze.Serialize.Helper.encodeJson(_list37));
        _s_.appendString(_pn_ + "set38", Zeze.Serialize.Helper.encodeJson(_set38));
        _s_.appendString(_pn_ + "set39", Zeze.Serialize.Helper.encodeJson(_set39));
        _s_.appendString(_pn_ + "map40", Zeze.Serialize.Helper.encodeJson(_map40));
        _s_.appendString(_pn_ + "map41", Zeze.Serialize.Helper.encodeJson(_map41));
        _s_.appendString(_pn_ + "map42Recursive", Zeze.Serialize.Helper.encodeJson(_map42Recursive));
        _s_.appendString(_pn_ + "list43", Zeze.Serialize.Helper.encodeJson(_list43));
        _s_.appendString(_pn_ + "jsonObject", getJsonObject());
        _s_.appendString(_pn_ + "jsonArray", getJsonArray());
        _s_.appendLong(_pn_ + "relationalMappingAlter", getRelationalMappingAlter());
        _s_.appendString(_pn_ + "sortedmap1", Zeze.Serialize.Helper.encodeJson(_sortedmap1));
        _s_.appendString(_pn_ + "sortedmap2", Zeze.Serialize.Helper.encodeJson(_sortedmap2));
        _s_.appendLong(_pn_ + "version", getVersion());
        _s_.appendString(_pn_ + "LongList", Zeze.Serialize.Helper.encodeJson(_LongList));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "int_1", "int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "long2", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "string3", "string", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "bool4", "bool", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "short5", "short", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(6, "float6", "float", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(7, "double7", "double", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(8, "bytes8", "binary", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(9, "list9", "list", "", "demo.Bean1"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(10, "set10", "set", "", "int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(11, "map11", "map", "long", "demo.Module2.BValue"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(12, "bean12", "demo.Module1.BSimple", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(13, "byte13", "byte", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(14, "dynamic14", "dynamic", "", "demo.Bean1,demo.Module1.BSimple,demo.Module1.BItem"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(15, "map15", "map", "long", "long"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(16, "map16", "map", "demo.Module1.Key", "demo.Module1.BSimple"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(17, "vector2", "vector2", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(18, "vector2Int", "vector2int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(19, "vector3", "vector3", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(20, "vector3Int", "vector3int", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(21, "vector4", "vector4", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(22, "quaternion", "quaternion", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(23, "dynamic23", "dynamic", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(24, "listVector2Int", "list", "", "vector2int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(25, "map25", "map", "demo.Module1.Key", "demo.Module1.BSimple"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(26, "map26", "map", "demo.Module1.Key", "dynamic"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(27, "dynamic27", "dynamic", "", "demo.Module1.BSimple"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(28, "key28", "demo.Module1.Key", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(29, "array29", "array", "", "float"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(30, "list30", "list", "", "int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(31, "list31", "list", "", "long"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(32, "list32", "list", "", "float"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(33, "list33", "list", "", "vector2"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(34, "list34", "list", "", "vector3"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(35, "list35", "list", "", "vector4"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(36, "list36", "list", "", "vector2int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(37, "list37", "list", "", "vector3int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(38, "set38", "set", "", "int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(39, "set39", "set", "", "long"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(40, "map40", "map", "int", "int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(41, "map41", "map", "long", "demo.Module1.BSimple"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(42, "map42Recursive", "map", "long", "demo.Module1.BValue"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(43, "list43", "list", "", "dynamic"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(44, "jsonObject", "string", "", "JSON_OBJECT"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(45, "jsonArray", "string", "", "JSON_ARRAY"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(46, "relationalMappingAlter", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(47, "sortedmap1", "sortedmap", "int", "int"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(48, "sortedmap2", "sortedmap", "int", "demo.Module1.BSimple"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(50, "version", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(99, "LongList", "list", "", "long"));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data implements Zeze.ILong2 {
    public static final long TYPEID = -9185494755353500202L;

    public static final int Enum1 = 4; // enum的注释

    private int _int_1; // com aa
    private long _long2; // com aa
    private String _string3; // com aa
    private boolean _bool4; // com aa
    private short _short5; // com aa
    private float _float6; // com aa
    private double _double7; // com aa
    private Zeze.Net.Binary _bytes8; // com aa
    private java.util.ArrayList<demo.Bean1.Data> _list9; // com aa
    private java.util.HashSet<Integer> _set10; // com aa
    private java.util.HashMap<Long, demo.Module2.BValue.Data> _map11; // com aa
    private demo.Module1.BSimple.Data _bean12; // simple
    private byte _byte13; // com aa
    private final DynamicData_dynamic14 _dynamic14;

    public static final class DynamicData_dynamic14 extends Zeze.Transaction.DynamicData {
        public static final long DynamicTypeId_Dynamic14_demo_Bean1 = 1L;
        public static final long DynamicTypeId_Dynamic14_demo_Module1_BSimple = 2L;
        public static final long DynamicTypeId_Dynamic14_demo_Module1_BItem = 3L;

        static {
            registerJsonParser(DynamicData_dynamic14.class);
        }

        @Override
        public long toTypeId(Zeze.Transaction.Data _d_) {
            var _typeId_ = _d_.typeId();
            if (_typeId_ == Zeze.Transaction.EmptyBean.Data.TYPEID)
                return Zeze.Transaction.EmptyBean.Data.TYPEID;
            if (_typeId_ == -410057899348847631L)
                return 1L; // demo.Bean1
            if (_typeId_ == 4513771153805810055L)
                return 2L; // demo.Module1.BSimple
            if (_typeId_ == -1770669781233931946L)
                return 3L; // demo.Module1.BItem
            throw new UnsupportedOperationException("Unknown Bean! dynamic@demo.Module1.BValue:dynamic14:" + _typeId_);
        }

        @Override
        public Zeze.Transaction.@Nullable Data toData(long _t_) {
            if (_t_ == 1L)
                return new demo.Bean1.Data();
            if (_t_ == 2L)
                return new demo.Module1.BSimple.Data();
            if (_t_ == 3L)
                return new demo.Module1.BItem.Data();
            if (_t_ == Zeze.Transaction.EmptyBean.Data.TYPEID)
                return Zeze.Transaction.EmptyBean.Data.instance;
            return null;
        }

        @Override
        public DynamicData_dynamic14 copy() {
            return (DynamicData_dynamic14)super.copy();
        }
    }

    private java.util.HashMap<Long, Long> _map15; // com aa
    private java.util.HashMap<demo.Module1.Key, demo.Module1.BSimple.Data> _map16; // com aa
    private Zeze.Serialize.Vector2 _vector2;
    private Zeze.Serialize.Vector2Int _vector2Int;
    private Zeze.Serialize.Vector3 _vector3;
    private Zeze.Serialize.Vector3Int _vector3Int;
    private Zeze.Serialize.Vector4 _vector4;
    private Zeze.Serialize.Quaternion _quaternion;
    private final DynamicData_dynamic23 _dynamic23;

    public static final class DynamicData_dynamic23 extends Zeze.Transaction.DynamicData {
        static {
            registerJsonParser(DynamicData_dynamic23.class);
        }

        @Override
        public long toTypeId(Zeze.Transaction.Data _d_) {
            return demo.Module1.ModuleModule1.getSpecialTypeIdFromBean(_d_);
        }

        @Override
        public Zeze.Transaction.Data toData(long _t_) {
            return demo.Module1.ModuleModule1.createDataFromSpecialTypeId(_t_);
        }

        @Override
        public DynamicData_dynamic23 copy() {
            return (DynamicData_dynamic23)super.copy();
        }
    }

    private java.util.ArrayList<Zeze.Serialize.Vector2Int> _listVector2Int;
    private java.util.HashMap<demo.Module1.Key, demo.Module1.BSimple.Data> _map25;
    private java.util.HashMap<demo.Module1.Key, DynamicData_map26> _map26;

    public static final class DynamicData_map26 extends Zeze.Transaction.DynamicData {
        static {
            registerJsonParser(DynamicData_map26.class);
        }

        @Override
        public long toTypeId(Zeze.Transaction.Data _d_) {
            var _typeId_ = _d_.typeId();
            if (_typeId_ == Zeze.Transaction.EmptyBean.Data.TYPEID)
                return Zeze.Transaction.EmptyBean.Data.TYPEID;
            if (_typeId_ == 4513771153805810055L)
                return 4513771153805810055L; // demo.Module1.BSimple
            throw new UnsupportedOperationException("Unknown Bean! dynamic@demo.Module1.BValue:map26:" + _typeId_);
        }

        @Override
        public Zeze.Transaction.@Nullable Data toData(long _t_) {
            if (_t_ == 4513771153805810055L)
                return new demo.Module1.BSimple.Data();
            if (_t_ == Zeze.Transaction.EmptyBean.Data.TYPEID)
                return Zeze.Transaction.EmptyBean.Data.instance;
            return null;
        }

        @Override
        public DynamicData_map26 copy() {
            return (DynamicData_map26)super.copy();
        }
    }

    private final DynamicData_dynamic27 _dynamic27;

    public static final class DynamicData_dynamic27 extends Zeze.Transaction.DynamicData {
        public static final long DynamicTypeId_Dynamic27_demo_Module1_BSimple = 4513771153805810055L;

        static {
            registerJsonParser(DynamicData_dynamic27.class);
        }

        @Override
        public long toTypeId(Zeze.Transaction.Data _d_) {
            var _typeId_ = _d_.typeId();
            if (_typeId_ == Zeze.Transaction.EmptyBean.Data.TYPEID)
                return Zeze.Transaction.EmptyBean.Data.TYPEID;
            if (_typeId_ == 4513771153805810055L)
                return 4513771153805810055L; // demo.Module1.BSimple
            throw new UnsupportedOperationException("Unknown Bean! dynamic@demo.Module1.BValue:dynamic27:" + _typeId_);
        }

        @Override
        public Zeze.Transaction.@Nullable Data toData(long _t_) {
            if (_t_ == 4513771153805810055L)
                return new demo.Module1.BSimple.Data();
            if (_t_ == Zeze.Transaction.EmptyBean.Data.TYPEID)
                return Zeze.Transaction.EmptyBean.Data.instance;
            return null;
        }

        @Override
        public DynamicData_dynamic27 copy() {
            return (DynamicData_dynamic27)super.copy();
        }
    }

    private demo.Module1.Key _key28;
    private java.util.ArrayList<Float> _array29;
    private Zeze.Util.IntList _list30;
    private Zeze.Util.LongList _list31;
    private Zeze.Util.FloatList _list32;
    private Zeze.Util.Vector2List _list33;
    private Zeze.Util.Vector3List _list34;
    private Zeze.Util.Vector4List _list35;
    private Zeze.Util.Vector2IntList _list36;
    private Zeze.Util.Vector3IntList _list37;
    private Zeze.Util.IntHashSet _set38;
    private Zeze.Util.LongHashSet _set39;
    private Zeze.Util.IntHashMap<Integer> _map40;
    private Zeze.Util.LongHashMap<demo.Module1.BSimple.Data> _map41;
    private java.util.HashMap<Long, demo.Module1.BValue.Data> _map42Recursive;
    private java.util.ArrayList<DynamicData_list43> _list43;

    public static final class DynamicData_list43 extends Zeze.Transaction.DynamicData {
        static {
            registerJsonParser(DynamicData_list43.class);
        }

        @Override
        public long toTypeId(Zeze.Transaction.Data _d_) {
            var _typeId_ = _d_.typeId();
            if (_typeId_ == Zeze.Transaction.EmptyBean.Data.TYPEID)
                return Zeze.Transaction.EmptyBean.Data.TYPEID;
            if (_typeId_ == 4513771153805810055L)
                return 4513771153805810055L; // demo.Module1.BSimple
            throw new UnsupportedOperationException("Unknown Bean! dynamic@demo.Module1.BValue:list43:" + _typeId_);
        }

        @Override
        public Zeze.Transaction.@Nullable Data toData(long _t_) {
            if (_t_ == 4513771153805810055L)
                return new demo.Module1.BSimple.Data();
            if (_t_ == Zeze.Transaction.EmptyBean.Data.TYPEID)
                return Zeze.Transaction.EmptyBean.Data.instance;
            return null;
        }

        @Override
        public DynamicData_list43 copy() {
            return (DynamicData_list43)super.copy();
        }
    }

    private String _jsonObject;
    private String _jsonArray;
    private long _relationalMappingAlter;
    private java.util.TreeMap<Integer, Integer> _sortedmap1;
    private java.util.TreeMap<Integer, demo.Module1.BSimple.Data> _sortedmap2;
    private long _version;
    private java.util.ArrayList<Long> _LongList;

    public int getInt_1() {
        return _int_1;
    }

    public void setInt_1(int _v_) {
        _int_1 = _v_;
    }

    public long getLong2() {
        return _long2;
    }

    public void setLong2(long _v_) {
        _long2 = _v_;
    }

    public String getString3() {
        return _string3;
    }

    public void setString3(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _string3 = _v_;
    }

    public boolean isBool4() {
        return _bool4;
    }

    public void setBool4(boolean _v_) {
        _bool4 = _v_;
    }

    public short getShort5() {
        return _short5;
    }

    public void setShort5(short _v_) {
        _short5 = _v_;
    }

    public float getFloat6() {
        return _float6;
    }

    public void setFloat6(float _v_) {
        _float6 = _v_;
    }

    public double getDouble7() {
        return _double7;
    }

    public void setDouble7(double _v_) {
        _double7 = _v_;
    }

    public Zeze.Net.Binary getBytes8() {
        return _bytes8;
    }

    public void setBytes8(Zeze.Net.Binary _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _bytes8 = _v_;
    }

    public java.util.ArrayList<demo.Bean1.Data> getList9() {
        return _list9;
    }

    public void setList9(java.util.ArrayList<demo.Bean1.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _list9 = _v_;
    }

    public java.util.HashSet<Integer> getSet10() {
        return _set10;
    }

    public void setSet10(java.util.HashSet<Integer> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _set10 = _v_;
    }

    public java.util.HashMap<Long, demo.Module2.BValue.Data> getMap11() {
        return _map11;
    }

    public void setMap11(java.util.HashMap<Long, demo.Module2.BValue.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _map11 = _v_;
    }

    public demo.Module1.BSimple.Data getBean12() {
        return _bean12;
    }

    public void setBean12(demo.Module1.BSimple.Data _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _bean12 = _v_;
    }

    public byte getByte13() {
        return _byte13;
    }

    public void setByte13(byte _v_) {
        _byte13 = _v_;
    }

    public DynamicData_dynamic14 getDynamic14() {
        return _dynamic14;
    }

    public demo.Bean1.Data getDynamic14_demo_Bean1() {
        return (demo.Bean1.Data)_dynamic14.getData();
    }

    public void setDynamic14(demo.Bean1.Data _v_) {
        _dynamic14.setData(_v_);
    }

    public demo.Module1.BSimple.Data getDynamic14_demo_Module1_BSimple() {
        return (demo.Module1.BSimple.Data)_dynamic14.getData();
    }

    public void setDynamic14(demo.Module1.BSimple.Data _v_) {
        _dynamic14.setData(_v_);
    }

    public demo.Module1.BItem.Data getDynamic14_demo_Module1_BItem() {
        return (demo.Module1.BItem.Data)_dynamic14.getData();
    }

    public void setDynamic14(demo.Module1.BItem.Data _v_) {
        _dynamic14.setData(_v_);
    }

    public java.util.HashMap<Long, Long> getMap15() {
        return _map15;
    }

    public void setMap15(java.util.HashMap<Long, Long> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _map15 = _v_;
    }

    public java.util.HashMap<demo.Module1.Key, demo.Module1.BSimple.Data> getMap16() {
        return _map16;
    }

    public void setMap16(java.util.HashMap<demo.Module1.Key, demo.Module1.BSimple.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _map16 = _v_;
    }

    public Zeze.Serialize.Vector2 getVector2() {
        return _vector2;
    }

    public void setVector2(Zeze.Serialize.Vector2 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _vector2 = _v_;
    }

    public Zeze.Serialize.Vector2Int getVector2Int() {
        return _vector2Int;
    }

    public void setVector2Int(Zeze.Serialize.Vector2Int _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _vector2Int = _v_;
    }

    public Zeze.Serialize.Vector3 getVector3() {
        return _vector3;
    }

    public void setVector3(Zeze.Serialize.Vector3 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _vector3 = _v_;
    }

    public Zeze.Serialize.Vector3Int getVector3Int() {
        return _vector3Int;
    }

    public void setVector3Int(Zeze.Serialize.Vector3Int _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _vector3Int = _v_;
    }

    public Zeze.Serialize.Vector4 getVector4() {
        return _vector4;
    }

    public void setVector4(Zeze.Serialize.Vector4 _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _vector4 = _v_;
    }

    public Zeze.Serialize.Quaternion getQuaternion() {
        return _quaternion;
    }

    public void setQuaternion(Zeze.Serialize.Quaternion _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _quaternion = _v_;
    }

    public DynamicData_dynamic23 getDynamic23() {
        return _dynamic23;
    }

    public java.util.ArrayList<Zeze.Serialize.Vector2Int> getListVector2Int() {
        return _listVector2Int;
    }

    public void setListVector2Int(java.util.ArrayList<Zeze.Serialize.Vector2Int> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _listVector2Int = _v_;
    }

    public java.util.HashMap<demo.Module1.Key, demo.Module1.BSimple.Data> getMap25() {
        return _map25;
    }

    public void setMap25(java.util.HashMap<demo.Module1.Key, demo.Module1.BSimple.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _map25 = _v_;
    }

    public java.util.HashMap<demo.Module1.Key, DynamicData_map26> getMap26() {
        return _map26;
    }

    public void setMap26(java.util.HashMap<demo.Module1.Key, DynamicData_map26> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _map26 = _v_;
    }

    public DynamicData_dynamic27 getDynamic27() {
        return _dynamic27;
    }

    public demo.Module1.BSimple.Data getDynamic27_demo_Module1_BSimple() {
        return (demo.Module1.BSimple.Data)_dynamic27.getData();
    }

    public void setDynamic27(demo.Module1.BSimple.Data _v_) {
        _dynamic27.setData(_v_);
    }

    public demo.Module1.Key getKey28() {
        return _key28;
    }

    public void setKey28(demo.Module1.Key _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _key28 = _v_;
    }

    public java.util.ArrayList<Float> getArray29() {
        return _array29;
    }

    public void setArray29(java.util.ArrayList<Float> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _array29 = _v_;
    }

    public Zeze.Util.IntList getList30() {
        return _list30;
    }

    public void setList30(Zeze.Util.IntList _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _list30 = _v_;
    }

    public Zeze.Util.LongList getList31() {
        return _list31;
    }

    public void setList31(Zeze.Util.LongList _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _list31 = _v_;
    }

    public Zeze.Util.FloatList getList32() {
        return _list32;
    }

    public void setList32(Zeze.Util.FloatList _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _list32 = _v_;
    }

    public Zeze.Util.Vector2List getList33() {
        return _list33;
    }

    public void setList33(Zeze.Util.Vector2List _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _list33 = _v_;
    }

    public Zeze.Util.Vector3List getList34() {
        return _list34;
    }

    public void setList34(Zeze.Util.Vector3List _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _list34 = _v_;
    }

    public Zeze.Util.Vector4List getList35() {
        return _list35;
    }

    public void setList35(Zeze.Util.Vector4List _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _list35 = _v_;
    }

    public Zeze.Util.Vector2IntList getList36() {
        return _list36;
    }

    public void setList36(Zeze.Util.Vector2IntList _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _list36 = _v_;
    }

    public Zeze.Util.Vector3IntList getList37() {
        return _list37;
    }

    public void setList37(Zeze.Util.Vector3IntList _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _list37 = _v_;
    }

    public Zeze.Util.IntHashSet getSet38() {
        return _set38;
    }

    public void setSet38(Zeze.Util.IntHashSet _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _set38 = _v_;
    }

    public Zeze.Util.LongHashSet getSet39() {
        return _set39;
    }

    public void setSet39(Zeze.Util.LongHashSet _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _set39 = _v_;
    }

    public Zeze.Util.IntHashMap<Integer> getMap40() {
        return _map40;
    }

    public void setMap40(Zeze.Util.IntHashMap<Integer> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _map40 = _v_;
    }

    public Zeze.Util.LongHashMap<demo.Module1.BSimple.Data> getMap41() {
        return _map41;
    }

    public void setMap41(Zeze.Util.LongHashMap<demo.Module1.BSimple.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _map41 = _v_;
    }

    public java.util.HashMap<Long, demo.Module1.BValue.Data> getMap42Recursive() {
        return _map42Recursive;
    }

    public void setMap42Recursive(java.util.HashMap<Long, demo.Module1.BValue.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _map42Recursive = _v_;
    }

    public java.util.ArrayList<DynamicData_list43> getList43() {
        return _list43;
    }

    public void setList43(java.util.ArrayList<DynamicData_list43> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _list43 = _v_;
    }

    public String getJsonObject() {
        return _jsonObject;
    }

    public void setJsonObject(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _jsonObject = _v_;
    }

    public String getJsonArray() {
        return _jsonArray;
    }

    public void setJsonArray(String _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _jsonArray = _v_;
    }

    public long getRelationalMappingAlter() {
        return _relationalMappingAlter;
    }

    public void setRelationalMappingAlter(long _v_) {
        _relationalMappingAlter = _v_;
    }

    public java.util.TreeMap<Integer, Integer> getSortedmap1() {
        return _sortedmap1;
    }

    public void setSortedmap1(java.util.TreeMap<Integer, Integer> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _sortedmap1 = _v_;
    }

    public java.util.TreeMap<Integer, demo.Module1.BSimple.Data> getSortedmap2() {
        return _sortedmap2;
    }

    public void setSortedmap2(java.util.TreeMap<Integer, demo.Module1.BSimple.Data> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _sortedmap2 = _v_;
    }

    public long getVersion() {
        return _version;
    }

    public void setVersion(long _v_) {
        _version = _v_;
    }

    public java.util.ArrayList<Long> getLongList() {
        return _LongList;
    }

    public void setLongList(java.util.ArrayList<Long> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _LongList = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _string3 = "";
        _bytes8 = Zeze.Net.Binary.Empty;
        _list9 = new java.util.ArrayList<>();
        _set10 = new java.util.HashSet<>();
        _map11 = new java.util.HashMap<>();
        _bean12 = new demo.Module1.BSimple.Data();
        _dynamic14 = new DynamicData_dynamic14();
        _map15 = new java.util.HashMap<>();
        _map16 = new java.util.HashMap<>();
        _vector2 = Zeze.Serialize.Vector2.ZERO;
        _vector2Int = Zeze.Serialize.Vector2Int.ZERO;
        _vector3 = Zeze.Serialize.Vector3.ZERO;
        _vector3Int = Zeze.Serialize.Vector3Int.ZERO;
        _vector4 = Zeze.Serialize.Vector4.ZERO;
        _quaternion = Zeze.Serialize.Quaternion.ZERO;
        _dynamic23 = new DynamicData_dynamic23();
        _listVector2Int = new java.util.ArrayList<>();
        _map25 = new java.util.HashMap<>();
        _map26 = new java.util.HashMap<>();
        _dynamic27 = new DynamicData_dynamic27();
        _key28 = new demo.Module1.Key();
        _array29 = new java.util.ArrayList<>();
        _list30 = new Zeze.Util.IntList();
        _list31 = new Zeze.Util.LongList();
        _list32 = new Zeze.Util.FloatList();
        _list33 = new Zeze.Util.Vector2List();
        _list34 = new Zeze.Util.Vector3List();
        _list35 = new Zeze.Util.Vector4List();
        _list36 = new Zeze.Util.Vector2IntList();
        _list37 = new Zeze.Util.Vector3IntList();
        _set38 = new Zeze.Util.IntHashSet();
        _set39 = new Zeze.Util.LongHashSet();
        _map40 = new Zeze.Util.IntHashMap<>();
        _map41 = new Zeze.Util.LongHashMap<>();
        _map42Recursive = new java.util.HashMap<>();
        _list43 = new java.util.ArrayList<>();
        _jsonObject = "";
        _jsonArray = "";
        _sortedmap1 = new java.util.TreeMap<>();
        _sortedmap2 = new java.util.TreeMap<>();
        _LongList = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(int _int_1_, long _long2_, @Nullable String _string3_, boolean _bool4_, short _short5_, float _float6_, double _double7_, Zeze.Net.@Nullable Binary _bytes8_, java.util.@Nullable ArrayList<demo.Bean1.Data> _list9_, java.util.@Nullable HashSet<Integer> _set10_, java.util.@Nullable HashMap<Long, demo.Module2.BValue.Data> _map11_, demo.Module1.BSimple.@Nullable Data _bean12_, byte _byte13_, @Nullable DynamicData_dynamic14 _dynamic14_, java.util.@Nullable HashMap<Long, Long> _map15_, java.util.@Nullable HashMap<demo.Module1.Key, demo.Module1.BSimple.Data> _map16_, Zeze.Serialize.@Nullable Vector2 _vector2_, Zeze.Serialize.@Nullable Vector2Int _vector2Int_, Zeze.Serialize.@Nullable Vector3 _vector3_, Zeze.Serialize.@Nullable Vector3Int _vector3Int_, Zeze.Serialize.@Nullable Vector4 _vector4_, Zeze.Serialize.@Nullable Quaternion _quaternion_, @Nullable DynamicData_dynamic23 _dynamic23_, java.util.@Nullable ArrayList<Zeze.Serialize.Vector2Int> _listVector2Int_, java.util.@Nullable HashMap<demo.Module1.Key, demo.Module1.BSimple.Data> _map25_, java.util.@Nullable HashMap<demo.Module1.Key, DynamicData_map26> _map26_, @Nullable DynamicData_dynamic27 _dynamic27_, demo.Module1.@Nullable Key _key28_, java.util.@Nullable ArrayList<Float> _array29_, Zeze.Util.@Nullable IntList _list30_, Zeze.Util.@Nullable LongList _list31_, Zeze.Util.@Nullable FloatList _list32_, Zeze.Util.@Nullable Vector2List _list33_, Zeze.Util.@Nullable Vector3List _list34_, Zeze.Util.@Nullable Vector4List _list35_, Zeze.Util.@Nullable Vector2IntList _list36_, Zeze.Util.@Nullable Vector3IntList _list37_, Zeze.Util.@Nullable IntHashSet _set38_, Zeze.Util.@Nullable LongHashSet _set39_, Zeze.Util.@Nullable IntHashMap<Integer> _map40_, Zeze.Util.@Nullable LongHashMap<demo.Module1.BSimple.Data> _map41_, java.util.@Nullable HashMap<Long, demo.Module1.BValue.Data> _map42Recursive_, java.util.@Nullable ArrayList<DynamicData_list43> _list43_, @Nullable String _jsonObject_, @Nullable String _jsonArray_, long _relationalMappingAlter_, java.util.@Nullable TreeMap<Integer, Integer> _sortedmap1_, java.util.@Nullable TreeMap<Integer, demo.Module1.BSimple.Data> _sortedmap2_, java.util.@Nullable ArrayList<Long> _LongList_) {
        _int_1 = _int_1_;
        _long2 = _long2_;
        if (_string3_ == null)
            _string3_ = "";
        _string3 = _string3_;
        _bool4 = _bool4_;
        _short5 = _short5_;
        _float6 = _float6_;
        _double7 = _double7_;
        if (_bytes8_ == null)
            _bytes8_ = Zeze.Net.Binary.Empty;
        _bytes8 = _bytes8_;
        if (_list9_ == null)
            _list9_ = new java.util.ArrayList<>();
        _list9 = _list9_;
        if (_set10_ == null)
            _set10_ = new java.util.HashSet<>();
        _set10 = _set10_;
        if (_map11_ == null)
            _map11_ = new java.util.HashMap<>();
        _map11 = _map11_;
        if (_bean12_ == null)
            _bean12_ = new demo.Module1.BSimple.Data();
        _bean12 = _bean12_;
        _byte13 = _byte13_;
        if (_dynamic14_ == null)
            _dynamic14_ = new DynamicData_dynamic14();
        _dynamic14 = _dynamic14_;
        if (_map15_ == null)
            _map15_ = new java.util.HashMap<>();
        _map15 = _map15_;
        if (_map16_ == null)
            _map16_ = new java.util.HashMap<>();
        _map16 = _map16_;
        if (_vector2_ == null)
            _vector2_ = Zeze.Serialize.Vector2.ZERO;
        _vector2 = _vector2_;
        if (_vector2Int_ == null)
            _vector2Int_ = Zeze.Serialize.Vector2Int.ZERO;
        _vector2Int = _vector2Int_;
        if (_vector3_ == null)
            _vector3_ = Zeze.Serialize.Vector3.ZERO;
        _vector3 = _vector3_;
        if (_vector3Int_ == null)
            _vector3Int_ = Zeze.Serialize.Vector3Int.ZERO;
        _vector3Int = _vector3Int_;
        if (_vector4_ == null)
            _vector4_ = Zeze.Serialize.Vector4.ZERO;
        _vector4 = _vector4_;
        if (_quaternion_ == null)
            _quaternion_ = Zeze.Serialize.Quaternion.ZERO;
        _quaternion = _quaternion_;
        if (_dynamic23_ == null)
            _dynamic23_ = new DynamicData_dynamic23();
        _dynamic23 = _dynamic23_;
        if (_listVector2Int_ == null)
            _listVector2Int_ = new java.util.ArrayList<>();
        _listVector2Int = _listVector2Int_;
        if (_map25_ == null)
            _map25_ = new java.util.HashMap<>();
        _map25 = _map25_;
        if (_map26_ == null)
            _map26_ = new java.util.HashMap<>();
        _map26 = _map26_;
        if (_dynamic27_ == null)
            _dynamic27_ = new DynamicData_dynamic27();
        _dynamic27 = _dynamic27_;
        if (_key28_ == null)
            _key28_ = new demo.Module1.Key();
        _key28 = _key28_;
        if (_array29_ == null)
            _array29_ = new java.util.ArrayList<>();
        _array29 = _array29_;
        if (_list30_ == null)
            _list30_ = new Zeze.Util.IntList();
        _list30 = _list30_;
        if (_list31_ == null)
            _list31_ = new Zeze.Util.LongList();
        _list31 = _list31_;
        if (_list32_ == null)
            _list32_ = new Zeze.Util.FloatList();
        _list32 = _list32_;
        if (_list33_ == null)
            _list33_ = new Zeze.Util.Vector2List();
        _list33 = _list33_;
        if (_list34_ == null)
            _list34_ = new Zeze.Util.Vector3List();
        _list34 = _list34_;
        if (_list35_ == null)
            _list35_ = new Zeze.Util.Vector4List();
        _list35 = _list35_;
        if (_list36_ == null)
            _list36_ = new Zeze.Util.Vector2IntList();
        _list36 = _list36_;
        if (_list37_ == null)
            _list37_ = new Zeze.Util.Vector3IntList();
        _list37 = _list37_;
        if (_set38_ == null)
            _set38_ = new Zeze.Util.IntHashSet();
        _set38 = _set38_;
        if (_set39_ == null)
            _set39_ = new Zeze.Util.LongHashSet();
        _set39 = _set39_;
        if (_map40_ == null)
            _map40_ = new Zeze.Util.IntHashMap<>();
        _map40 = _map40_;
        if (_map41_ == null)
            _map41_ = new Zeze.Util.LongHashMap<>();
        _map41 = _map41_;
        if (_map42Recursive_ == null)
            _map42Recursive_ = new java.util.HashMap<>();
        _map42Recursive = _map42Recursive_;
        if (_list43_ == null)
            _list43_ = new java.util.ArrayList<>();
        _list43 = _list43_;
        if (_jsonObject_ == null)
            _jsonObject_ = "";
        _jsonObject = _jsonObject_;
        if (_jsonArray_ == null)
            _jsonArray_ = "";
        _jsonArray = _jsonArray_;
        _relationalMappingAlter = _relationalMappingAlter_;
        if (_sortedmap1_ == null)
            _sortedmap1_ = new java.util.TreeMap<>();
        _sortedmap1 = _sortedmap1_;
        if (_sortedmap2_ == null)
            _sortedmap2_ = new java.util.TreeMap<>();
        _sortedmap2 = _sortedmap2_;
        if (_LongList_ == null)
            _LongList_ = new java.util.ArrayList<>();
        _LongList = _LongList_;
    }

    @Override
    public void reset() {
        _int_1 = 0;
        _long2 = 0;
        _string3 = "";
        _bool4 = false;
        _short5 = (short)0;
        _float6 = 0;
        _double7 = 0;
        _bytes8 = Zeze.Net.Binary.Empty;
        _list9.clear();
        _set10.clear();
        _map11.clear();
        _bean12.reset();
        _byte13 = (byte)0;
        _dynamic14.reset();
        _map15.clear();
        _map16.clear();
        _vector2 = Zeze.Serialize.Vector2.ZERO;
        _vector2Int = Zeze.Serialize.Vector2Int.ZERO;
        _vector3 = Zeze.Serialize.Vector3.ZERO;
        _vector3Int = Zeze.Serialize.Vector3Int.ZERO;
        _vector4 = Zeze.Serialize.Vector4.ZERO;
        _quaternion = Zeze.Serialize.Quaternion.ZERO;
        _dynamic23.reset();
        _listVector2Int.clear();
        _map25.clear();
        _map26.clear();
        _dynamic27.reset();
        _key28 = new demo.Module1.Key();
        _array29.clear();
        _list30.clear();
        _list31.clear();
        _list32.clear();
        _list33.clear();
        _list34.clear();
        _list35.clear();
        _list36.clear();
        _list37.clear();
        _set38.clear();
        _set39.clear();
        _map40.clear();
        _map41.clear();
        _map42Recursive.clear();
        _list43.clear();
        _jsonObject = "";
        _jsonArray = "";
        _relationalMappingAlter = 0;
        _sortedmap1.clear();
        _sortedmap2.clear();
        _version = 0;
        _LongList.clear();
    }

    @Override
    public demo.Module1.BValue toBean() {
        var _b_ = new demo.Module1.BValue();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BValue)_o_);
    }

    public void assign(BValue _o_) {
        _int_1 = _o_.getInt_1();
        _long2 = _o_.getLong2();
        _string3 = _o_.getString3();
        _bool4 = _o_.isBool4();
        _short5 = _o_.getShort5();
        _float6 = _o_.getFloat6();
        _double7 = _o_.getDouble7();
        _bytes8 = _o_.getBytes8();
        _list9.clear();
        for (var _e_ : _o_._list9) {
            var _v_ = new demo.Bean1.Data();
            _v_.assign(_e_);
            _list9.add(_v_);
        }
        _set10.clear();
        _set10.addAll(_o_._set10);
        _map11.clear();
        for (var _e_ : _o_._map11.entrySet()) {
            var _v_ = new demo.Module2.BValue.Data();
            _v_.assign(_e_.getValue());
            _map11.put(_e_.getKey(), _v_);
        }
        _bean12.assign(_o_._bean12.getValue());
        _byte13 = _o_.getByte13();
        _dynamic14.assign(_o_._dynamic14);
        _map15.clear();
        _map15.putAll(_o_._map15);
        _map16.clear();
        for (var _e_ : _o_._map16.entrySet()) {
            var _v_ = new demo.Module1.BSimple.Data();
            _v_.assign(_e_.getValue());
            _map16.put(_e_.getKey(), _v_);
        }
        _vector2 = _o_.getVector2();
        _vector2Int = _o_.getVector2Int();
        _vector3 = _o_.getVector3();
        _vector3Int = _o_.getVector3Int();
        _vector4 = _o_.getVector4();
        _quaternion = _o_.getQuaternion();
        _dynamic23.assign(_o_._dynamic23);
        _listVector2Int.clear();
        _listVector2Int.addAll(_o_._listVector2Int);
        _map25.clear();
        for (var _e_ : _o_._map25.entrySet()) {
            var _v_ = new demo.Module1.BSimple.Data();
            _v_.assign(_e_.getValue());
            _map25.put(_e_.getKey(), _v_);
        }
        _map26.clear();
        for (var _e_ : _o_._map26.entrySet()) {
            var _v_ = new DynamicData_map26();
            _v_.assign(_e_.getValue());
            _map26.put(_e_.getKey(), _v_);
        }
        _dynamic27.assign(_o_._dynamic27);
        _key28 = _o_.getKey28();
        _array29.clear();
        _array29.addAll(_o_._array29);
        _list30.clear();
        _list30.addAll(_o_._list30);
        _list31.clear();
        _list31.addAll(_o_._list31);
        _list32.clear();
        _list32.addAll(_o_._list32);
        _list33.clear();
        _list33.addAllVector(_o_._list33);
        _list34.clear();
        _list34.addAllVector(_o_._list34);
        _list35.clear();
        _list35.addAllVector(_o_._list35);
        _list36.clear();
        _list36.addAllVector(_o_._list36);
        _list37.clear();
        _list37.addAllVector(_o_._list37);
        _set38.clear();
        _set38.addAll(_o_._set38);
        _set39.clear();
        _set39.addAll(_o_._set39);
        _map40.clear();
        _map40.putAll(_o_._map40);
        _map41.clear();
        for (var _e_ : _o_._map41.entrySet()) {
            var _v_ = new demo.Module1.BSimple.Data();
            _v_.assign(_e_.getValue());
            _map41.put(_e_.getKey(), _v_);
        }
        _map42Recursive.clear();
        for (var _e_ : _o_._map42Recursive.entrySet()) {
            var _v_ = new demo.Module1.BValue.Data();
            _v_.assign(_e_.getValue());
            _map42Recursive.put(_e_.getKey(), _v_);
        }
        _list43.clear();
        for (var _e_ : _o_._list43) {
            var _v_ = new DynamicData_list43();
            _v_.assign(_e_);
            _list43.add(_v_);
        }
        _jsonObject = _o_.getJsonObject();
        _jsonArray = _o_.getJsonArray();
        _relationalMappingAlter = _o_.getRelationalMappingAlter();
        _sortedmap1.clear();
        _sortedmap1.putAll(_o_._sortedmap1);
        _sortedmap2.clear();
        for (var _e_ : _o_._sortedmap2.entrySet()) {
            var _v_ = new demo.Module1.BSimple.Data();
            _v_.assign(_e_.getValue());
            _sortedmap2.put(_e_.getKey(), _v_);
        }
        _version = _o_.getVersion();
        _LongList.clear();
        _LongList.addAll(_o_._LongList);
    }

    public void assign(BValue.Data _o_) {
        _int_1 = _o_._int_1;
        _long2 = _o_._long2;
        _string3 = _o_._string3;
        _bool4 = _o_._bool4;
        _short5 = _o_._short5;
        _float6 = _o_._float6;
        _double7 = _o_._double7;
        _bytes8 = _o_._bytes8;
        _list9.clear();
        for (var _e_ : _o_._list9)
            _list9.add(_e_.copy());
        _set10.clear();
        _set10.addAll(_o_._set10);
        _map11.clear();
        for (var _e_ : _o_._map11.entrySet())
            _map11.put(_e_.getKey(), _e_.getValue().copy());
        _bean12.assign(_o_._bean12);
        _byte13 = _o_._byte13;
        _dynamic14.assign(_o_._dynamic14);
        _map15.clear();
        _map15.putAll(_o_._map15);
        _map16.clear();
        for (var _e_ : _o_._map16.entrySet())
            _map16.put(_e_.getKey(), _e_.getValue().copy());
        _vector2 = _o_._vector2;
        _vector2Int = _o_._vector2Int;
        _vector3 = _o_._vector3;
        _vector3Int = _o_._vector3Int;
        _vector4 = _o_._vector4;
        _quaternion = _o_._quaternion;
        _dynamic23.assign(_o_._dynamic23);
        _listVector2Int.clear();
        _listVector2Int.addAll(_o_._listVector2Int);
        _map25.clear();
        for (var _e_ : _o_._map25.entrySet())
            _map25.put(_e_.getKey(), _e_.getValue().copy());
        _map26.clear();
        for (var _e_ : _o_._map26.entrySet())
            _map26.put(_e_.getKey(), _e_.getValue().copy());
        _dynamic27.assign(_o_._dynamic27);
        _key28 = _o_._key28;
        _array29.clear();
        _array29.addAll(_o_._array29);
        _list30.clear();
        _list30.addAll(_o_._list30);
        _list31.clear();
        _list31.addAll(_o_._list31);
        _list32.clear();
        _list32.addAll(_o_._list32);
        _list33.clear();
        _list33.addAll(_o_._list33);
        _list34.clear();
        _list34.addAll(_o_._list34);
        _list35.clear();
        _list35.addAll(_o_._list35);
        _list36.clear();
        _list36.addAll(_o_._list36);
        _list37.clear();
        _list37.addAll(_o_._list37);
        _set38.clear();
        _set38.addAll(_o_._set38);
        _set39.clear();
        _set39.addAll(_o_._set39);
        _map40.clear();
        _map40.putAll(_o_._map40);
        _map41.clear();
        _o_._map41.foreach((k, v) -> _map41.put(k, v.copy()));
        _map42Recursive.clear();
        for (var _e_ : _o_._map42Recursive.entrySet())
            _map42Recursive.put(_e_.getKey(), _e_.getValue().copy());
        _list43.clear();
        for (var _e_ : _o_._list43)
            _list43.add(_e_.copy());
        _jsonObject = _o_._jsonObject;
        _jsonArray = _o_._jsonArray;
        _relationalMappingAlter = _o_._relationalMappingAlter;
        _sortedmap1.clear();
        _sortedmap1.putAll(_o_._sortedmap1);
        _sortedmap2.clear();
        for (var _e_ : _o_._sortedmap2.entrySet())
            _sortedmap2.put(_e_.getKey(), _e_.getValue().copy());
        _version = _o_._version;
        _LongList.clear();
        _LongList.addAll(_o_._LongList);
    }

    @Override
    public BValue.Data copy() {
        var _c_ = new BValue.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BValue.Data _a_, BValue.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BValue.Data clone() {
        return (BValue.Data)super.clone();
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("demo.Module1.BValue: {\n");
        _s_.append(_i1_).append("int_1=").append(_int_1).append(",\n");
        _s_.append(_i1_).append("long2=").append(_long2).append(",\n");
        _s_.append(_i1_).append("string3=").append(_string3).append(",\n");
        _s_.append(_i1_).append("bool4=").append(_bool4).append(",\n");
        _s_.append(_i1_).append("short5=").append(_short5).append(",\n");
        _s_.append(_i1_).append("float6=").append(_float6).append(",\n");
        _s_.append(_i1_).append("double7=").append(_double7).append(",\n");
        _s_.append(_i1_).append("bytes8=").append(_bytes8).append(",\n");
        _s_.append(_i1_).append("list9=[");
        if (!_list9.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _list9) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_list9.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("set10={");
        if (!_set10.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _set10) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_set10.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("map11={");
        if (!_map11.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _map11.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_map11.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("bean12=");
        _bean12.buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("byte13=").append(_byte13).append(",\n");
        _s_.append(_i1_).append("dynamic14=");
        _dynamic14.getData().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("map15={");
        if (!_map15.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _map15.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_map15.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("map16={");
        if (!_map16.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _map16.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_map16.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=");
                _e_.getKey().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("vector2=").append(_vector2).append(",\n");
        _s_.append(_i1_).append("vector2Int=").append(_vector2Int).append(",\n");
        _s_.append(_i1_).append("vector3=").append(_vector3).append(",\n");
        _s_.append(_i1_).append("vector3Int=").append(_vector3Int).append(",\n");
        _s_.append(_i1_).append("vector4=").append(_vector4).append(",\n");
        _s_.append(_i1_).append("quaternion=").append(_quaternion).append(",\n");
        _s_.append(_i1_).append("dynamic23=");
        _dynamic23.getData().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("listVector2Int=[");
        if (!_listVector2Int.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _listVector2Int) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_listVector2Int.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("map25={");
        if (!_map25.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _map25.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_map25.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=");
                _e_.getKey().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("map26={");
        if (!_map26.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _map26.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_map26.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=");
                _e_.getKey().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().getData().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("dynamic27=");
        _dynamic27.getData().buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("key28=");
        _key28.buildString(_s_, _l_ + 8);
        _s_.append(",\n");
        _s_.append(_i1_).append("array29=[");
        if (!_array29.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _array29) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_array29.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("list30=[");
        if (!_list30.isEmpty()) {
            _s_.append('\n');
            for (int _i_ = 0, _n_ = _list30.size(); _i_ < _n_; _i_++) {
                if (_i_ == 1000) {
                    _s_.append(_i2_).append("...[").append(_n_).append("]\n");
                    break;
                }
                var _v_ = _list30.get(_i_);
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("list31=[");
        if (!_list31.isEmpty()) {
            _s_.append('\n');
            for (int _i_ = 0, _n_ = _list31.size(); _i_ < _n_; _i_++) {
                if (_i_ == 1000) {
                    _s_.append(_i2_).append("...[").append(_n_).append("]\n");
                    break;
                }
                var _v_ = _list31.get(_i_);
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("list32=[");
        if (!_list32.isEmpty()) {
            _s_.append('\n');
            for (int _i_ = 0, _n_ = _list32.size(); _i_ < _n_; _i_++) {
                if (_i_ == 1000) {
                    _s_.append(_i2_).append("...[").append(_n_).append("]\n");
                    break;
                }
                var _v_ = _list32.get(_i_);
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("list33=[");
        if (!_list33.isEmpty()) {
            _s_.append('\n');
            for (int _i_ = 0, _n_ = _list33.size(); _i_ < _n_; _i_++) {
                if (_i_ == 1000) {
                    _s_.append(_i2_).append("...[").append(_n_).append("]\n");
                    break;
                }
                var _v_ = _list33.get(_i_);
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("list34=[");
        if (!_list34.isEmpty()) {
            _s_.append('\n');
            for (int _i_ = 0, _n_ = _list34.size(); _i_ < _n_; _i_++) {
                if (_i_ == 1000) {
                    _s_.append(_i2_).append("...[").append(_n_).append("]\n");
                    break;
                }
                var _v_ = _list34.get(_i_);
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("list35=[");
        if (!_list35.isEmpty()) {
            _s_.append('\n');
            for (int _i_ = 0, _n_ = _list35.size(); _i_ < _n_; _i_++) {
                if (_i_ == 1000) {
                    _s_.append(_i2_).append("...[").append(_n_).append("]\n");
                    break;
                }
                var _v_ = _list35.get(_i_);
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("list36=[");
        if (!_list36.isEmpty()) {
            _s_.append('\n');
            for (int _i_ = 0, _n_ = _list36.size(); _i_ < _n_; _i_++) {
                if (_i_ == 1000) {
                    _s_.append(_i2_).append("...[").append(_n_).append("]\n");
                    break;
                }
                var _v_ = _list36.get(_i_);
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("list37=[");
        if (!_list37.isEmpty()) {
            _s_.append('\n');
            for (int _i_ = 0, _n_ = _list37.size(); _i_ < _n_; _i_++) {
                if (_i_ == 1000) {
                    _s_.append(_i2_).append("...[").append(_n_).append("]\n");
                    break;
                }
                var _v_ = _list37.get(_i_);
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("set38={");
        if (!_set38.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _i_ = _set38.iterator(); _i_.moveToNext(); ) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_set38.size()).append("]\n");
                    break;
                }
                var _v_ = _i_.value();
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("set39={");
        if (!_set39.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _i_ = _set39.iterator(); _i_.moveToNext(); ) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_set39.size()).append("]\n");
                    break;
                }
                var _v_ = _i_.value();
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("map40={");
        if (!_map40.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _i_ = _map40.iterator(); _i_.moveToNext(); ) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_map40.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_i_.key()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_i_.value()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("map41={");
        if (!_map41.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _i_ = _map41.iterator(); _i_.moveToNext(); ) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_map41.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_i_.key()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _i_.value().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("map42Recursive={");
        if (!_map42Recursive.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _map42Recursive.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_map42Recursive.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("list43=[");
        if (!_list43.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _list43) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_list43.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=");
                _v_.getData().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("jsonObject=").append(_jsonObject).append(",\n");
        _s_.append(_i1_).append("jsonArray=").append(_jsonArray).append(",\n");
        _s_.append(_i1_).append("relationalMappingAlter=").append(_relationalMappingAlter).append(",\n");
        _s_.append(_i1_).append("sortedmap1={");
        if (!_sortedmap1.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _sortedmap1.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_sortedmap1.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=").append(_e_.getValue()).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("sortedmap2={");
        if (!_sortedmap2.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _e_ : _sortedmap2.entrySet()) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_sortedmap2.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Key=").append(_e_.getKey()).append(",\n");
                _s_.append(_i2_).append("Value=");
                _e_.getValue().buildString(_s_, _l_ + 12);
                _s_.append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("},\n");
        _s_.append(_i1_).append("version=").append(_version).append(",\n");
        _s_.append(_i1_).append("LongList=[");
        if (!_LongList.isEmpty()) {
            _s_.append('\n');
            int _n_ = 0;
            for (var _v_ : _LongList) {
                if (++_n_ > 1000) {
                    _s_.append(_i2_).append("...[").append(_LongList.size()).append("]\n");
                    break;
                }
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("]\n");
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            int _x_ = _int_1;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = _long2;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            String _x_ = _string3;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            boolean _x_ = _bool4;
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
            }
        }
        {
            int _x_ = _short5;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            float _x_ = _float6;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.FLOAT);
                _o_.WriteFloat(_x_);
            }
        }
        {
            double _x_ = _double7;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.DOUBLE);
                _o_.WriteDouble(_x_);
            }
        }
        {
            var _x_ = _bytes8;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        {
            var _x_ = _list9;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BEAN);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _set10;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 10, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _map11;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 11, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 12, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _bean12.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            int _x_ = _byte13;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 13, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _dynamic14;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 14, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            var _x_ = _map15;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 15, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _map16;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 16, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BEAN, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _e_.getKey().encode(_o_);
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _vector2;
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 17, ByteBuffer.VECTOR2);
                _o_.WriteVector2(_x_);
            }
        }
        {
            var _x_ = _vector2Int;
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 18, ByteBuffer.VECTOR2INT);
                _o_.WriteVector2Int(_x_);
            }
        }
        {
            var _x_ = _vector3;
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 19, ByteBuffer.VECTOR3);
                _o_.WriteVector3(_x_);
            }
        }
        {
            var _x_ = _vector3Int;
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 20, ByteBuffer.VECTOR3INT);
                _o_.WriteVector3Int(_x_);
            }
        }
        {
            var _x_ = _vector4;
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 21, ByteBuffer.VECTOR4);
                _o_.WriteVector4(_x_);
            }
        }
        {
            var _x_ = _quaternion;
            if (_x_ != null && !_x_.isZero()) {
                _i_ = _o_.WriteTag(_i_, 22, ByteBuffer.VECTOR4);
                _o_.WriteQuaternion(_x_);
            }
        }
        {
            var _x_ = _dynamic23;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 23, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            var _x_ = _listVector2Int;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 24, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.VECTOR2INT);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _o_.WriteVector2Int(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _map25;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 25, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BEAN, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _e_.getKey().encode(_o_);
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _map26;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 26, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.BEAN, ByteBuffer.DYNAMIC);
                for (var _e_ : _x_.entrySet()) {
                    _e_.getKey().encode(_o_);
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _dynamic27;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 27, ByteBuffer.DYNAMIC);
                _x_.encode(_o_);
            }
        }
        {
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 28, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _key28.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            var _x_ = _array29;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 29, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.FLOAT);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _o_.WriteFloat(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _list30;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 30, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                _x_.encode(_o_, _n_);
            }
        }
        {
            var _x_ = _list31;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 31, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                _x_.encode(_o_, _n_);
            }
        }
        {
            var _x_ = _list32;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 32, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.FLOAT);
                _x_.encode(_o_, _n_);
            }
        }
        {
            var _x_ = _list33;
            int _n_ = _x_.vectorSize();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 33, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.VECTOR2);
                _x_.encode(_o_, _n_);
            }
        }
        {
            var _x_ = _list34;
            int _n_ = _x_.vectorSize();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 34, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.VECTOR3);
                _x_.encode(_o_, _n_);
            }
        }
        {
            var _x_ = _list35;
            int _n_ = _x_.vectorSize();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 35, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.VECTOR4);
                _x_.encode(_o_, _n_);
            }
        }
        {
            var _x_ = _list36;
            int _n_ = _x_.vectorSize();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 36, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.VECTOR2INT);
                _x_.encode(_o_, _n_);
            }
        }
        {
            var _x_ = _list37;
            int _n_ = _x_.vectorSize();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 37, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.VECTOR3INT);
                _x_.encode(_o_, _n_);
            }
        }
        {
            var _x_ = _set38;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 38, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _j_ = _x_.iterator(); _j_.moveToNext(); ) {
                    var _v_ = _j_.value();
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _set39;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 39, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _j_ = _x_.iterator(); _j_.moveToNext(); ) {
                    var _v_ = _j_.value();
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _map40;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 40, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.INTEGER);
                for (var _j_ = _x_.iterator(); _j_.moveToNext(); ) {
                    _o_.WriteLong(_j_.key());
                    _o_.WriteLong(_j_.value());
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _map41;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 41, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _j_ = _x_.iterator(); _j_.moveToNext(); ) {
                    _o_.WriteLong(_j_.key());
                    _j_.value().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _map42Recursive;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 42, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _list43;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 43, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.DYNAMIC);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _v_.encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            String _x_ = _jsonObject;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 44, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _jsonArray;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 45, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = _relationalMappingAlter;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 46, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _sortedmap1;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 47, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.INTEGER);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _o_.WriteLong(_e_.getValue());
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            var _x_ = _sortedmap2;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 48, ByteBuffer.MAP);
                _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                for (var _e_ : _x_.entrySet()) {
                    _o_.WriteLong(_e_.getKey());
                    _e_.getValue().encode(_o_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            long _x_ = _version;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 50, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            var _x_ = _LongList;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 99, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _int_1 = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _long2 = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _string3 = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _bool4 = _o_.ReadBool(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _short5 = _o_.ReadShort(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 6) {
            _float6 = _o_.ReadFloat(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 7) {
            _double7 = _o_.ReadDouble(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 8) {
            _bytes8 = _o_.ReadBinary(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 9) {
            var _x_ = _list9;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBean(new demo.Bean1.Data(), _t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 10) {
            var _x_ = _set10;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 11) {
            var _x_ = _map11;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new demo.Module2.BValue.Data(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 12) {
            _o_.ReadBean(_bean12, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 13) {
            _byte13 = _o_.ReadByte(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 14) {
            _o_.ReadDynamic(_dynamic14, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 15) {
            var _x_ = _map15;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadLong(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 16) {
            var _x_ = _map16;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBean(new demo.Module1.Key(), _s_);
                    var _v_ = _o_.ReadBean(new demo.Module1.BSimple.Data(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 17) {
            _vector2 = _o_.ReadVector2(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 18) {
            _vector2Int = _o_.ReadVector2Int(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 19) {
            _vector3 = _o_.ReadVector3(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 20) {
            _vector3Int = _o_.ReadVector3Int(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 21) {
            _vector4 = _o_.ReadVector4(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 22) {
            _quaternion = _o_.ReadQuaternion(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 23) {
            _o_.ReadDynamic(_dynamic23, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 24) {
            var _x_ = _listVector2Int;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadVector2Int(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 25) {
            var _x_ = _map25;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBean(new demo.Module1.Key(), _s_);
                    var _v_ = _o_.ReadBean(new demo.Module1.BSimple.Data(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 26) {
            var _x_ = _map26;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadBean(new demo.Module1.Key(), _s_);
                    var _v_ = new DynamicData_map26();
                    _o_.ReadDynamic(_v_, _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 27) {
            _o_.ReadDynamic(_dynamic27, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 28) {
            _o_.ReadBean(_key28, _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 29) {
            var _x_ = _array29;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadFloat(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 30) {
            var _x_ = _list30;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                _t_ = _o_.ReadByte();
                int _n_ = _o_.ReadTagSize(_t_);
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.INTEGER)
                    _x_.decode(_o_, _n_);
                else {
                    for (; _n_ > 0; _n_--)
                        _x_.add(_o_.ReadInt(_t_));
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 31) {
            var _x_ = _list31;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                _t_ = _o_.ReadByte();
                int _n_ = _o_.ReadTagSize(_t_);
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.INTEGER)
                    _x_.decode(_o_, _n_);
                else {
                    for (; _n_ > 0; _n_--)
                        _x_.add(_o_.ReadLong(_t_));
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 32) {
            var _x_ = _list32;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                _t_ = _o_.ReadByte();
                int _n_ = _o_.ReadTagSize(_t_);
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.FLOAT)
                    _x_.decode(_o_, _n_);
                else {
                    for (; _n_ > 0; _n_--)
                        _x_.add(_o_.ReadFloat(_t_));
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 33) {
            var _x_ = _list33;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                _t_ = _o_.ReadByte();
                int _n_ = _o_.ReadTagSize(_t_);
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.VECTOR2)
                    _x_.decode(_o_, _n_);
                else {
                    for (; _n_ > 0; _n_--)
                        _x_.add(_o_.ReadVector2(_t_));
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 34) {
            var _x_ = _list34;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                _t_ = _o_.ReadByte();
                int _n_ = _o_.ReadTagSize(_t_);
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.VECTOR3)
                    _x_.decode(_o_, _n_);
                else {
                    for (; _n_ > 0; _n_--)
                        _x_.add(_o_.ReadVector3(_t_));
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 35) {
            var _x_ = _list35;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                _t_ = _o_.ReadByte();
                int _n_ = _o_.ReadTagSize(_t_);
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.VECTOR4)
                    _x_.decode(_o_, _n_);
                else {
                    for (; _n_ > 0; _n_--)
                        _x_.add(_o_.ReadVector4(_t_));
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 36) {
            var _x_ = _list36;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                _t_ = _o_.ReadByte();
                int _n_ = _o_.ReadTagSize(_t_);
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.VECTOR2INT)
                    _x_.decode(_o_, _n_);
                else {
                    for (; _n_ > 0; _n_--)
                        _x_.add(_o_.ReadVector2Int(_t_));
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 37) {
            var _x_ = _list37;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                _t_ = _o_.ReadByte();
                int _n_ = _o_.ReadTagSize(_t_);
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.VECTOR3INT)
                    _x_.decode(_o_, _n_);
                else {
                    for (; _n_ > 0; _n_--)
                        _x_.add(_o_.ReadVector3Int(_t_));
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 38) {
            var _x_ = _set38;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadInt(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 39) {
            var _x_ = _set39;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 40) {
            var _x_ = _map40;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadInt(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 41) {
            var _x_ = _map41;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new demo.Module1.BSimple.Data(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 42) {
            var _x_ = _map42Recursive;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadLong(_s_);
                    var _v_ = _o_.ReadBean(new demo.Module1.BValue.Data(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 43) {
            var _x_ = _list43;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--) {
                    var _e_ = new DynamicData_list43();
                    _o_.ReadDynamic(_e_, _t_);
                    _x_.add(_e_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 44) {
            _jsonObject = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 45) {
            _jsonArray = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 46) {
            _relationalMappingAlter = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 47) {
            var _x_ = _sortedmap1;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadInt(_t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 48) {
            var _x_ = _sortedmap2;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP) {
                int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                for (int _n_ = _o_.ReadUIntPositive(); _n_ > 0; _n_--) {
                    var _k_ = _o_.ReadInt(_s_);
                    var _v_ = _o_.ReadBean(new demo.Module1.BSimple.Data(), _t_);
                    _x_.put(_k_, _v_);
                }
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Map");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while ((_t_ & 0xff) > 1 && _i_ < 50) {
            _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 50) {
            _version = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while ((_t_ & 0xff) > 1 && _i_ < 99) {
            _o_.SkipUnknownField(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 99) {
            var _x_ = _LongList;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(@Nullable Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BValue.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BValue.Data)_o_;
        if (_int_1 != _b_._int_1)
            return false;
        if (_long2 != _b_._long2)
            return false;
        if (!_string3.equals(_b_._string3))
            return false;
        if (_bool4 != _b_._bool4)
            return false;
        if (_short5 != _b_._short5)
            return false;
        if (_float6 != _b_._float6)
            return false;
        if (_double7 != _b_._double7)
            return false;
        if (!_bytes8.equals(_b_._bytes8))
            return false;
        if (!_list9.equals(_b_._list9))
            return false;
        if (!_set10.equals(_b_._set10))
            return false;
        if (!_map11.equals(_b_._map11))
            return false;
        if (!_bean12.equals(_b_._bean12))
            return false;
        if (_byte13 != _b_._byte13)
            return false;
        if (!_dynamic14.equals(_b_._dynamic14))
            return false;
        if (!_map15.equals(_b_._map15))
            return false;
        if (!_map16.equals(_b_._map16))
            return false;
        if (!_vector2.equals(_b_._vector2))
            return false;
        if (!_vector2Int.equals(_b_._vector2Int))
            return false;
        if (!_vector3.equals(_b_._vector3))
            return false;
        if (!_vector3Int.equals(_b_._vector3Int))
            return false;
        if (!_vector4.equals(_b_._vector4))
            return false;
        if (!_quaternion.equals(_b_._quaternion))
            return false;
        if (!_dynamic23.equals(_b_._dynamic23))
            return false;
        if (!_listVector2Int.equals(_b_._listVector2Int))
            return false;
        if (!_map25.equals(_b_._map25))
            return false;
        if (!_map26.equals(_b_._map26))
            return false;
        if (!_dynamic27.equals(_b_._dynamic27))
            return false;
        if (!_key28.equals(_b_._key28))
            return false;
        if (!_array29.equals(_b_._array29))
            return false;
        if (!_list30.equals(_b_._list30))
            return false;
        if (!_list31.equals(_b_._list31))
            return false;
        if (!_list32.equals(_b_._list32))
            return false;
        if (!_list33.equals(_b_._list33))
            return false;
        if (!_list34.equals(_b_._list34))
            return false;
        if (!_list35.equals(_b_._list35))
            return false;
        if (!_list36.equals(_b_._list36))
            return false;
        if (!_list37.equals(_b_._list37))
            return false;
        if (!_set38.equals(_b_._set38))
            return false;
        if (!_set39.equals(_b_._set39))
            return false;
        if (!_map40.equals(_b_._map40))
            return false;
        if (!_map41.equals(_b_._map41))
            return false;
        if (!_map42Recursive.equals(_b_._map42Recursive))
            return false;
        if (!_list43.equals(_b_._list43))
            return false;
        if (!_jsonObject.equals(_b_._jsonObject))
            return false;
        if (!_jsonArray.equals(_b_._jsonArray))
            return false;
        if (_relationalMappingAlter != _b_._relationalMappingAlter)
            return false;
        if (!_sortedmap1.equals(_b_._sortedmap1))
            return false;
        if (!_sortedmap2.equals(_b_._sortedmap2))
            return false;
        if (!_LongList.equals(_b_._LongList))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + Integer.hashCode(_int_1);
        _h_ = _h_ * _p_ + Long.hashCode(_long2);
        _h_ = _h_ * _p_ + _string3.hashCode();
        _h_ = _h_ * _p_ + Boolean.hashCode(_bool4);
        _h_ = _h_ * _p_ + Short.hashCode(_short5);
        _h_ = _h_ * _p_ + Float.hashCode(_float6);
        _h_ = _h_ * _p_ + Double.hashCode(_double7);
        _h_ = _h_ * _p_ + _bytes8.hashCode();
        _h_ = _h_ * _p_ + _list9.hashCode();
        _h_ = _h_ * _p_ + _set10.hashCode();
        _h_ = _h_ * _p_ + _map11.hashCode();
        _h_ = _h_ * _p_ + _bean12.hashCode();
        _h_ = _h_ * _p_ + Byte.hashCode(_byte13);
        _h_ = _h_ * _p_ + _dynamic14.hashCode();
        _h_ = _h_ * _p_ + _map15.hashCode();
        _h_ = _h_ * _p_ + _map16.hashCode();
        _h_ = _h_ * _p_ + _vector2.hashCode();
        _h_ = _h_ * _p_ + _vector2Int.hashCode();
        _h_ = _h_ * _p_ + _vector3.hashCode();
        _h_ = _h_ * _p_ + _vector3Int.hashCode();
        _h_ = _h_ * _p_ + _vector4.hashCode();
        _h_ = _h_ * _p_ + _quaternion.hashCode();
        _h_ = _h_ * _p_ + _dynamic23.hashCode();
        _h_ = _h_ * _p_ + _listVector2Int.hashCode();
        _h_ = _h_ * _p_ + _map25.hashCode();
        _h_ = _h_ * _p_ + _map26.hashCode();
        _h_ = _h_ * _p_ + _dynamic27.hashCode();
        _h_ = _h_ * _p_ + _key28.hashCode();
        _h_ = _h_ * _p_ + _array29.hashCode();
        _h_ = _h_ * _p_ + _list30.hashCode();
        _h_ = _h_ * _p_ + _list31.hashCode();
        _h_ = _h_ * _p_ + _list32.hashCode();
        _h_ = _h_ * _p_ + _list33.hashCode();
        _h_ = _h_ * _p_ + _list34.hashCode();
        _h_ = _h_ * _p_ + _list35.hashCode();
        _h_ = _h_ * _p_ + _list36.hashCode();
        _h_ = _h_ * _p_ + _list37.hashCode();
        _h_ = _h_ * _p_ + _set38.hashCode();
        _h_ = _h_ * _p_ + _set39.hashCode();
        _h_ = _h_ * _p_ + _map40.hashCode();
        _h_ = _h_ * _p_ + _map41.hashCode();
        _h_ = _h_ * _p_ + _map42Recursive.hashCode();
        _h_ = _h_ * _p_ + _list43.hashCode();
        _h_ = _h_ * _p_ + _jsonObject.hashCode();
        _h_ = _h_ * _p_ + _jsonArray.hashCode();
        _h_ = _h_ * _p_ + Long.hashCode(_relationalMappingAlter);
        _h_ = _h_ * _p_ + _sortedmap1.hashCode();
        _h_ = _h_ * _p_ + _sortedmap2.hashCode();
        _h_ = _h_ * _p_ + _LongList.hashCode();
        return _h_;
    }
}
}
