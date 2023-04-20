// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BTransmitReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTransmit copy();

    String getActionName();
    Zeze.Transaction.Collections.PSet1ReadOnly<Long> getRolesReadOnly();
    long getSender();
    Zeze.Net.Binary getParameter();
    String getOnlineSetName();
}
