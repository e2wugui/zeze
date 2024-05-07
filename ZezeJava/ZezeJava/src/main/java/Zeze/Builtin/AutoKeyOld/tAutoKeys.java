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

    public tAutoKeys(String suffix) {
        super(739941246, "Zeze_Builtin_AutoKeyOld_tAutoKeys", suffix);
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
        Zeze.Builtin.AutoKeyOld.BSeedKey _v_ = new Zeze.Builtin.AutoKeyOld.BSeedKey();
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
    public Zeze.Builtin.AutoKeyOld.BSeedKey decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        var parents = new java.util.ArrayList<String>();
        Zeze.Builtin.AutoKeyOld.BSeedKey _v_ = new Zeze.Builtin.AutoKeyOld.BSeedKey();
        parents.add("__key");
        _v_.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Zeze.Builtin.AutoKeyOld.BSeedKey _v_) {
        var parents = new java.util.ArrayList<String>();
        parents.add("__key");
        _v_.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }

    @Override
    public Zeze.Builtin.AutoKeyOld.BAutoKey newValue() {
        return new Zeze.Builtin.AutoKeyOld.BAutoKey();
    }

    @Override
    public Zeze.Builtin.AutoKeyOld.BAutoKeyReadOnly getReadOnly(Zeze.Builtin.AutoKeyOld.BSeedKey key) {
        return get(key);
    }
}
