// auto-generated @formatter:off
package Zeze.Builtin.Onz;

// 2段提交相关控制协议
public interface BCommitReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BCommit copy();

    long getOnzTid();
}
