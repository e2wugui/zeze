// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// key is 1, only one record
@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tEventClasses extends TableX<Integer, Zeze.Builtin.Game.Task.BEventClasses>
        implements TableReadOnly<Integer, Zeze.Builtin.Game.Task.BEventClasses, Zeze.Builtin.Game.Task.BEventClassesReadOnly> {
    public tEventClasses() {
        super("Zeze_Builtin_Game_Task_tEventClasses");
    }

    @Override
    public int getId() {
        return -1411967206;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
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
    public Zeze.Builtin.Game.Task.BEventClasses newValue() {
        return new Zeze.Builtin.Game.Task.BEventClasses();
    }

    @Override
    public Zeze.Builtin.Game.Task.BEventClassesReadOnly getReadOnly(Integer key) {
        return get(key);
    }
}
