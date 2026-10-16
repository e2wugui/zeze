// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

// table的注释
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class Table2 extends TableX<demo.Module1.Key, demo.Module1.BValue>
        implements TableReadOnly<demo.Module1.Key, demo.Module1.BValue, demo.Module1.BValueReadOnly> {
    public Table2() {
        super(991943441, "demo_Module1_Table2");
    }

    public Table2(String _s_) {
        super(991943441, "demo_Module1_Table2", _s_);
    }

    @Override
    public Class<demo.Module1.Key> getKeyClass() {
        return demo.Module1.Key.class;
    }

    @Override
    public Class<demo.Module1.BValue> getValueClass() {
        return demo.Module1.BValue.class;
    }

    @Override
    public boolean isRelationalMapping() {
        return true;
    }

    public static final int VAR_int_1 = 1;
    public static final int VAR_long2 = 2;
    public static final int VAR_string3 = 3;
    public static final int VAR_bool4 = 4;
    public static final int VAR_short5 = 5;
    public static final int VAR_float6 = 6;
    public static final int VAR_double7 = 7;
    public static final int VAR_bytes8 = 8;
    public static final int VAR_list9 = 9;
    public static final int VAR_set10 = 10;
    public static final int VAR_map11 = 11;
    public static final int VAR_bean12 = 12;
    public static final int VAR_byte13 = 13;
    public static final int VAR_dynamic14 = 14;
    public static final int VAR_map15 = 15;
    public static final int VAR_map16 = 16;
    public static final int VAR_vector2 = 17;
    public static final int VAR_vector2Int = 18;
    public static final int VAR_vector3 = 19;
    public static final int VAR_vector3Int = 20;
    public static final int VAR_vector4 = 21;
    public static final int VAR_quaternion = 22;
    public static final int VAR_dynamic23 = 23;
    public static final int VAR_listVector2Int = 24;
    public static final int VAR_map25 = 25;
    public static final int VAR_map26 = 26;
    public static final int VAR_dynamic27 = 27;
    public static final int VAR_key28 = 28;
    public static final int VAR_array29 = 29;
    public static final int VAR_list30 = 30;
    public static final int VAR_list31 = 31;
    public static final int VAR_list32 = 32;
    public static final int VAR_list33 = 33;
    public static final int VAR_list34 = 34;
    public static final int VAR_list35 = 35;
    public static final int VAR_list36 = 36;
    public static final int VAR_list37 = 37;
    public static final int VAR_set38 = 38;
    public static final int VAR_set39 = 39;
    public static final int VAR_map40 = 40;
    public static final int VAR_map41 = 41;
    public static final int VAR_map42Recursive = 42;
    public static final int VAR_list43 = 43;
    public static final int VAR_jsonObject = 44;
    public static final int VAR_jsonArray = 45;
    public static final int VAR_relationalMappingAlter = 46;
    public static final int VAR_sortedmap1 = 47;
    public static final int VAR_sortedmap2 = 48;
    public static final int VAR_version = 50;
    public static final int VAR_LongList = 99;

    @Override
    public demo.Module1.Key decodeKey(ByteBuffer _os_) {
        var _v_ = new demo.Module1.Key();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(demo.Module1.Key _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public demo.Module1.Key decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        var _p_ = new java.util.ArrayList<String>();
        var _v_ = new demo.Module1.Key();
        _p_.add("__key");
        _v_.decodeResultSet(_p_, _s_);
        _p_.removeLast();
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, demo.Module1.Key _v_) {
        var _p_ = new java.util.ArrayList<String>();
        _p_.add("__key");
        _v_.encodeSQLStatement(_p_, _s_);
        _p_.removeLast();
    }

    @Override
    public demo.Module1.BValue newValue() {
        return new demo.Module1.BValue();
    }

    @Override
    public @Nullable BValueReadOnly getReadOnly(demo.Module1.Key _k_) {
        return get(_k_);
    }
}
