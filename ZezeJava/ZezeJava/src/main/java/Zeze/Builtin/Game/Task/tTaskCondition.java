// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tTaskCondition extends TableX<Zeze.Builtin.Game.Task.BTaskConditionKey, Zeze.Builtin.Game.Task.BTaskCondition>
        implements TableReadOnly<Zeze.Builtin.Game.Task.BTaskConditionKey, Zeze.Builtin.Game.Task.BTaskCondition, Zeze.Builtin.Game.Task.BTaskConditionReadOnly> {
    public tTaskCondition() {
        super("Zeze_Builtin_Game_Task_tTaskCondition");
    }

    @Override
    public int getId() {
        return 1082052680;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_TaskConditionId = 1;
    public static final int VAR_TaskConditionName = 2;
    public static final int VAR_TaskConditionCustomData = 3;

    @Override
    public Zeze.Builtin.Game.Task.BTaskConditionKey decodeKey(ByteBuffer _os_) {
        Zeze.Builtin.Game.Task.BTaskConditionKey _v_ = new Zeze.Builtin.Game.Task.BTaskConditionKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Game.Task.BTaskConditionKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Game.Task.BTaskCondition newValue() {
        return new Zeze.Builtin.Game.Task.BTaskCondition();
    }

    @Override
    public Zeze.Builtin.Game.Task.BTaskConditionReadOnly getReadOnly(Zeze.Builtin.Game.Task.BTaskConditionKey key) {
        return get(key);
    }
}
