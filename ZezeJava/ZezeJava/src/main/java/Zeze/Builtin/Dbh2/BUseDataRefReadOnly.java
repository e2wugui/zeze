// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

// 用来生成Data数据结构，没有实际功能
public interface BUseDataRefReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BUseDataRef copy();

    Zeze.Builtin.Dbh2.BLogBeginTransactionReadOnly getRef1ReadOnly();
}
