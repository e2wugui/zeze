// auto-generated
package Zezex.Provider;

public abstract class AbstractModule extends Zeze.IModule {
    public String getFullName() { return "Zezex.Provider"; }
    public String getName() { return "Provider"; }
    public int getId() { return 10001; }

    public abstract int ProcessAnnounceLinkInfo(Zeze.Net.Protocol _p);

    public abstract int ProcessDispatch(Zeze.Net.Protocol _p);

    public abstract int ProcessLinkBroken(Zeze.Net.Protocol _p);

    public abstract int ProcessModuleRedirectRequest(Zeze.Net.Protocol _p);

    public abstract int ProcessModuleRedirectAllRequest(Zeze.Net.Protocol _p);

    public abstract int ProcessModuleRedirectAllResult(Zeze.Net.Protocol _p);

    public abstract int ProcessSendConfirm(Zeze.Net.Protocol _p);

    public abstract int ProcessTransmit(Zeze.Net.Protocol _p);

}
