// auto-generated @formatter:off
package Zeze.Builtin.HistoryModule;

public interface BLogChangesReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLogChanges copy();

    long getGlobalSerialId();
    Zeze.Net.Binary getProtocolArgument();
}
