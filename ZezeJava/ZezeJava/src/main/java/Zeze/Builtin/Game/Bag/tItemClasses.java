// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tItemClasses extends Zeze.Transaction.TableX<Integer, Zeze.Builtin.Game.Bag.BItemClasses> {
    public tItemClasses() {
        super("Zeze_Builtin_Game_Bag_tItemClasses");
    }

    @Override
    public int getId() {
        return 1057953754;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_ItemClasses = 1;

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
    public Zeze.Builtin.Game.Bag.BItemClasses newValue() {
        return new Zeze.Builtin.Game.Bag.BItemClasses();
    }
}
