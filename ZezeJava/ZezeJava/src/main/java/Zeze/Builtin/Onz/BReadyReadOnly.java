// auto-generated @formatter:off
package Zeze.Builtin.Onz;

// 2段提交相关控制协议
public interface BReadyReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BReady copy();

    long getOnzTid();
}
