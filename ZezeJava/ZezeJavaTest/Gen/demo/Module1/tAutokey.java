// auto-generated @formatter:off
package demo.Module1;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

// table的注释
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tAutokey extends TableX<demo.Module1.AutoKey, demo.Module1.BAutoValue>
        implements TableReadOnly<demo.Module1.AutoKey, demo.Module1.BAutoValue, demo.Module1.BAutoValueReadOnly> {
    public tAutokey() {
        super(-1591505893, "demo_Module1_tAutokey");
    }

    public tAutokey(String _s_) {
        super(-1591505893, "demo_Module1_tAutokey", _s_);
    }

    @Override
    public Class<demo.Module1.AutoKey> getKeyClass() {
        return demo.Module1.AutoKey.class;
    }

    @Override
    public Class<demo.Module1.BAutoValue> getValueClass() {
        return demo.Module1.BAutoValue.class;
    }

    public static final int VAR_Current = 1;
    public static final int VAR_Name = 2;
    public static final int VAR_LocalId = 3;

    @Override
    public demo.Module1.AutoKey decodeKey(ByteBuffer _os_) {
        var _v_ = new demo.Module1.AutoKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(demo.Module1.AutoKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public demo.Module1.AutoKey decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        var _p_ = new java.util.ArrayList<String>();
        var _v_ = new demo.Module1.AutoKey();
        _p_.add("__key");
        _v_.decodeResultSet(_p_, _s_);
        _p_.removeLast();
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, demo.Module1.AutoKey _v_) {
        var _p_ = new java.util.ArrayList<String>();
        _p_.add("__key");
        _v_.encodeSQLStatement(_p_, _s_);
        _p_.removeLast();
    }

    @Override
    public demo.Module1.BAutoValue newValue() {
        return new demo.Module1.BAutoValue();
    }

    @Override
    public @Nullable BAutoValueReadOnly getReadOnly(demo.Module1.AutoKey _k_) {
        return get(_k_);
    }
}
