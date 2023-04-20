// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// key is 1, only one record
@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tEventClasses extends TableX<Integer, Zeze.Builtin.Game.TaskBase.BEventClasses>
        implements TableReadOnly<Integer, Zeze.Builtin.Game.TaskBase.BEventClasses, Zeze.Builtin.Game.TaskBase.BEventClassesReadOnly> {
    public tEventClasses() {
        super("Zeze_Builtin_Game_TaskBase_tEventClasses");
    }

    public tEventClasses(String suffix) {
        super("Zeze_Builtin_Game_TaskBase_tEventClasses" + suffix);
    }

    public String getOriginName() {
        return "Zeze_Builtin_Game_TaskBase_tEventClasses";
    }

    @Override
    public int getId() {
        return -1631158308;
    }

    public static final int VAR_EventClasses = 1;

    @Override
    public Integer decodeKey(ByteBuffer _os_) {
        int _v_;
        _v_ = _os_.ReadInt();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Integer _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteInt(_v_);
        return _os_;
    }

    @Override
    public Integer decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        int _v_;
        _v_ = rs.getInt("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Integer _v_) {
        st.appendInt("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BEventClasses newValue() {
        return new Zeze.Builtin.Game.TaskBase.BEventClasses();
    }

    @Override
    public Zeze.Builtin.Game.TaskBase.BEventClassesReadOnly getReadOnly(Integer key) {
        return get(key);
    }
}
