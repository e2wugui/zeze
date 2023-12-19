// auto-generated @formatter:off
package Zeze.Builtin.Onz;

// Flush阶段控制协议
public interface BFlushReadyReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BFlushReady copy();

    long getOnzTid();
}
