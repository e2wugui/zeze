// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public interface BTransmitAccountReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTransmitAccount copy();

    public String getActionName();
    public Zeze.Net.Binary getParameter();
    public Zeze.Transaction.Collections.PSet1ReadOnly<String> getTargetAccountsReadOnly();
    public String getSenderAccount();
    public String getSenderClientId();
}
