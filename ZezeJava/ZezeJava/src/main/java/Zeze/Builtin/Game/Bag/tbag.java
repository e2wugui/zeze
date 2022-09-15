// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tbag extends Zeze.Transaction.TableX<String, Zeze.Builtin.Game.Bag.BBag> {
    public tbag() {
        super("Zeze_Builtin_Game_Bag_tbag");
    }

    @Override
    public int getId() {
        return 863603985;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_Capacity = 1;
    public static final int VAR_Items = 2;

    @Override
    public String decodeKey(ByteBuffer _os_) {
        String _v_;
        _v_ = _os_.ReadString();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(String _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteString(_v_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Game.Bag.BBag newValue() {
        return new Zeze.Builtin.Game.Bag.BBag();
    }
}
