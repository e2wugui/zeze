// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

public interface BJobReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BJob copy();

    String getJobHandleName();
    Zeze.Net.Binary getJobState();
}
