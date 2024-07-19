// auto-generated @formatter:off
package Zeze.Builtin.AutoKeyOld;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tAutoKeys extends TableX<Zeze.Builtin.AutoKeyOld.BSeedKey, Zeze.Builtin.AutoKeyOld.BAutoKey>
        implements TableReadOnly<Zeze.Builtin.AutoKeyOld.BSeedKey, Zeze.Builtin.AutoKeyOld.BAutoKey, Zeze.Builtin.AutoKeyOld.BAutoKeyReadOnly> {
    public tAutoKeys() {
        super(739941246, "Zeze_Builtin_AutoKeyOld_tAutoKeys");
    }

    public tAutoKeys(String _s_) {
        super(739941246, "Zeze_Builtin_AutoKeyOld_tAutoKeys", _s_);
    }

    @Override
    public Class<Zeze.Builtin.AutoKeyOld.BSeedKey> getKeyClass() {
        return Zeze.Builtin.AutoKeyOld.BSeedKey.class;
    }

    @Override
    public Class<Zeze.Builtin.AutoKeyOld.BAutoKey> getValueClass() {
        return Zeze.Builtin.AutoKeyOld.BAutoKey.class;
    }

    public static final int VAR_NextId = 1;

    @Override
    public Zeze.Builtin.AutoKeyOld.BSeedKey decodeKey(ByteBuffer _os_) {
        var _v_ = new Zeze.Builtin.AutoKeyOld.BSeedKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.AutoKeyOld.BSeedKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.AutoKeyOld.BSeedKey decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        var _p_ = new java.util.ArrayList<String>();
        var _v_ = new Zeze.Builtin.AutoKeyOld.BSeedKey();
        _p_.add("__key");
        _v_.decodeResultSet(_p_, _s_);
        _p_.remove(_p_.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Zeze.Builtin.AutoKeyOld.BSeedKey _v_) {
        var _p_ = new java.util.ArrayList<String>();
        _p_.add("__key");
        _v_.encodeSQLStatement(_p_, _s_);
        _p_.remove(_p_.size() - 1);
    }

    @Override
    public Zeze.Builtin.AutoKeyOld.BAutoKey newValue() {
        return new Zeze.Builtin.AutoKeyOld.BAutoKey();
    }

    @Override
    public Zeze.Builtin.AutoKeyOld.BAutoKeyReadOnly getReadOnly(Zeze.Builtin.AutoKeyOld.BSeedKey _k_) {
        return get(_k_);
    }
}
