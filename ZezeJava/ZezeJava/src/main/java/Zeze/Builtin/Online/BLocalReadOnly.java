// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BLocalReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLocal copy();

    long getLoginVersion();
    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Online.BAny, Zeze.Builtin.Online.BAnyReadOnly> getDatasReadOnly();
}
