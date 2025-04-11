// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BTransmitAccountReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTransmitAccount copy();

    String getActionName();
    Zeze.Net.Binary getParameter();
    Zeze.Transaction.Collections.PSet1ReadOnly<Zeze.Builtin.ProviderDirect.BLoginKey> getTargetsReadOnly();
    String getSenderAccount();
    String getSenderClientId();
}
