// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BTransmitReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTransmit copy();

    public String getActionName();
    public Zeze.Transaction.Collections.PSet1ReadOnly<Long> getRolesReadOnly();
    public long getSender();
    public Zeze.Net.Binary getParameter();
}
