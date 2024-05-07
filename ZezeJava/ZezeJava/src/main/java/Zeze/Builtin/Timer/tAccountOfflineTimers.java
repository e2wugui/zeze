// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tAccountOfflineTimers extends TableX<Zeze.Builtin.Timer.BAccountClientId, Zeze.Builtin.Timer.BOfflineTimers>
        implements TableReadOnly<Zeze.Builtin.Timer.BAccountClientId, Zeze.Builtin.Timer.BOfflineTimers, Zeze.Builtin.Timer.BOfflineTimersReadOnly> {
    public tAccountOfflineTimers() {
        super(-865861330, "Zeze_Builtin_Timer_tAccountOfflineTimers");
    }

    public tAccountOfflineTimers(String suffix) {
        super(-865861330, "Zeze_Builtin_Timer_tAccountOfflineTimers", suffix);
    }

    @Override
    public Class<Zeze.Builtin.Timer.BAccountClientId> getKeyClass() {
        return Zeze.Builtin.Timer.BAccountClientId.class;
    }

    @Override
    public Class<Zeze.Builtin.Timer.BOfflineTimers> getValueClass() {
        return Zeze.Builtin.Timer.BOfflineTimers.class;
    }

    public static final int VAR_OfflineTimers = 1;

    @Override
    public Zeze.Builtin.Timer.BAccountClientId decodeKey(ByteBuffer _os_) {
        Zeze.Builtin.Timer.BAccountClientId _v_ = new Zeze.Builtin.Timer.BAccountClientId();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Timer.BAccountClientId _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Timer.BAccountClientId decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        var parents = new java.util.ArrayList<String>();
        Zeze.Builtin.Timer.BAccountClientId _v_ = new Zeze.Builtin.Timer.BAccountClientId();
        parents.add("__key");
        _v_.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Zeze.Builtin.Timer.BAccountClientId _v_) {
        var parents = new java.util.ArrayList<String>();
        parents.add("__key");
        _v_.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
    }

    @Override
    public Zeze.Builtin.Timer.BOfflineTimers newValue() {
        return new Zeze.Builtin.Timer.BOfflineTimers();
    }

    @Override
    public Zeze.Builtin.Timer.BOfflineTimersReadOnly getReadOnly(Zeze.Builtin.Timer.BAccountClientId key) {
        return get(key);
    }
}
