// auto-generated @formatter:off
package Zeze.Builtin.AutoKeyOld;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tAutoKeys extends TableX<Zeze.Builtin.AutoKeyOld.BSeedKey, Zeze.Builtin.AutoKeyOld.BAutoKey>
        implements TableReadOnly<Zeze.Builtin.AutoKeyOld.BSeedKey, Zeze.Builtin.AutoKeyOld.BAutoKey, Zeze.Builtin.AutoKeyOld.BAutoKeyReadOnly> {
    public tAutoKeys() {
        super("Zeze_Builtin_AutoKeyOld_tAutoKeys");
    }

    @Override
    public int getId() {
        return 739941246;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
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
    public Zeze.Builtin.AutoKeyOld.BAutoKey newValue() {
        return new Zeze.Builtin.AutoKeyOld.BAutoKey();
    }

    @Override
    public Zeze.Builtin.AutoKeyOld.BAutoKeyReadOnly getReadOnly(Zeze.Builtin.AutoKeyOld.BSeedKey key) {
        return get(key);
    }
}
