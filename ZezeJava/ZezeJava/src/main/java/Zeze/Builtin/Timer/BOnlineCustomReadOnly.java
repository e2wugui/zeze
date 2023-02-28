// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// 保存真正的用户自定义数据
public interface BOnlineCustomReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BOnlineCustom copy();

    Zeze.Transaction.DynamicBeanReadOnly getCustomDataReadOnly();
}
