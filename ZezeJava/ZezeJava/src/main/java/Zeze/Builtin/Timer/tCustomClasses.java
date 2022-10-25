// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// key is 1, only one record
@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tCustomClasses extends TableX<Integer, Zeze.Builtin.Timer.BCustomClasses>
        implements TableReadOnly<Integer, Zeze.Builtin.Timer.BCustomClasses, Zeze.Builtin.Timer.BCustomClassesReadOnly> {
    public tCustomClasses() {
        super("Zeze_Builtin_Timer_tCustomClasses");
    }

    @Override
    public int getId() {
        return -1904799209;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_CustomClasses = 1;

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
    public Zeze.Builtin.Timer.BCustomClasses newValue() {
        return new Zeze.Builtin.Timer.BCustomClasses();
    }

    @Override
    public Zeze.Builtin.Timer.BCustomClassesReadOnly getReadOnly(Integer key) {
        return get(key);
    }
}
