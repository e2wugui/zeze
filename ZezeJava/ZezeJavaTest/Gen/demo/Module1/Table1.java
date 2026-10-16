// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

import demo.Module1.BValueReadOnly;
// table的注释
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class Table1 extends TableX<Long, demo.Module1.BValue>
        implements TableReadOnly<Long, demo.Module1.BValue, demo.Module1.BValueReadOnly> {
    public Table1() {
        super(-315275145, "demo_Module1_Table1");
    }

    public Table1(String _s_) {
        super(-315275145, "demo_Module1_Table1", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<demo.Module1.BValue> getValueClass() {
        return demo.Module1.BValue.class;
    }

    @Override
    public boolean isAutoKey() {
        return true;
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

    public long insert(demo.Module1.BValue _v_) {
        //noinspection DataFlowIssue
        long _k_ = getAutoKey().next();
        insert(_k_, _v_);
        return _k_;
    }

    @Override
    public Long decodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(_v_));
        _os_.WriteLong(_v_);
        return _os_;
    }

    @Override
    public Long decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        long _v_;
        _v_ = _s_.getLong("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Long _v_) {
        _s_.appendLong("__key", _v_);
    }

    @Override
    public demo.Module1.BValue newValue() {
        return new demo.Module1.BValue();
    }

    @Override
    public @Nullable BValueReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
