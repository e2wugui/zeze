// auto-generated @formatter:off
package Zeze.Builtin.AutoKey;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tAutoKeys extends TableX<Zeze.Builtin.AutoKey.BSeedKey, Zeze.Builtin.AutoKey.BAutoKey>
        implements TableReadOnly<Zeze.Builtin.AutoKey.BSeedKey, Zeze.Builtin.AutoKey.BAutoKey, Zeze.Builtin.AutoKey.BAutoKeyReadOnly> {
    public tAutoKeys() {
        super(-716529252, "Zeze_Builtin_AutoKey_tAutoKeys");
    }

    public tAutoKeys(String _s_) {
        super(-716529252, "Zeze_Builtin_AutoKey_tAutoKeys", _s_);
    }

    @Override
    public Class<Zeze.Builtin.AutoKey.BSeedKey> getKeyClass() {
        return Zeze.Builtin.AutoKey.BSeedKey.class;
    }

    @Override
    public Class<Zeze.Builtin.AutoKey.BAutoKey> getValueClass() {
        return Zeze.Builtin.AutoKey.BAutoKey.class;
    }

    public static final int VAR_NextId = 1;

    @Override
    public Zeze.Builtin.AutoKey.BSeedKey decodeKey(ByteBuffer _os_) {
        var _v_ = new Zeze.Builtin.AutoKey.BSeedKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.AutoKey.BSeedKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.AutoKey.BSeedKey decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        var _p_ = new java.util.ArrayList<String>();
        var _v_ = new Zeze.Builtin.AutoKey.BSeedKey();
        _p_.add("__key");
        _v_.decodeResultSet(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Zeze.Builtin.AutoKey.BSeedKey _v_) {
        var _p_ = new java.util.ArrayList<String>();
        _p_.add("__key");
        _v_.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public Zeze.Builtin.AutoKey.BAutoKey newValue() {
        return new Zeze.Builtin.AutoKey.BAutoKey();
    }

    @Override
    public Zeze.Builtin.AutoKey.BAutoKeyReadOnly getReadOnly(Zeze.Builtin.AutoKey.BSeedKey _k_) {
        return get(_k_);
    }
}
