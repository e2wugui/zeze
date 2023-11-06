// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

// 服务：查询，启动，关闭
public interface BServiceReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BService copy();

    String getServiceName();
    String getState();
    String getPs();
}
