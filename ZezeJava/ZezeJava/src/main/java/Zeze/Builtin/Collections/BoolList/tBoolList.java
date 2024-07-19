// auto-generated @formatter:off
package Zeze.Builtin.Collections.BoolList;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tBoolList extends TableX<Zeze.Builtin.Collections.BoolList.BKey, Zeze.Builtin.Collections.BoolList.BValue>
        implements TableReadOnly<Zeze.Builtin.Collections.BoolList.BKey, Zeze.Builtin.Collections.BoolList.BValue, Zeze.Builtin.Collections.BoolList.BValueReadOnly> {
    public tBoolList() {
        super(1535934961, "Zeze_Builtin_Collections_BoolList_tBoolList");
    }

    public tBoolList(String _s_) {
        super(1535934961, "Zeze_Builtin_Collections_BoolList_tBoolList", _s_);
    }

    @Override
    public Class<Zeze.Builtin.Collections.BoolList.BKey> getKeyClass() {
        return Zeze.Builtin.Collections.BoolList.BKey.class;
    }

    @Override
    public Class<Zeze.Builtin.Collections.BoolList.BValue> getValueClass() {
        return Zeze.Builtin.Collections.BoolList.BValue.class;
    }

    public static final int VAR_Item0 = 1;
    public static final int VAR_Item1 = 2;
    public static final int VAR_Item2 = 3;
    public static final int VAR_Item3 = 4;
    public static final int VAR_Item4 = 5;
    public static final int VAR_Item5 = 6;
    public static final int VAR_Item6 = 7;
    public static final int VAR_Item7 = 8;

    @Override
    public Zeze.Builtin.Collections.BoolList.BKey decodeKey(ByteBuffer _os_) {
        var _v_ = new Zeze.Builtin.Collections.BoolList.BKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Collections.BoolList.BKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Collections.BoolList.BKey decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        var _p_ = new java.util.ArrayList<String>();
        var _v_ = new Zeze.Builtin.Collections.BoolList.BKey();
        _p_.add("__key");
        _v_.decodeResultSet(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Zeze.Builtin.Collections.BoolList.BKey _v_) {
        var _p_ = new java.util.ArrayList<String>();
        _p_.add("__key");
        _v_.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public Zeze.Builtin.Collections.BoolList.BValue newValue() {
        return new Zeze.Builtin.Collections.BoolList.BValue();
    }

    @Override
    public Zeze.Builtin.Collections.BoolList.BValueReadOnly getReadOnly(Zeze.Builtin.Collections.BoolList.BKey _k_) {
        return get(_k_);
    }
}
