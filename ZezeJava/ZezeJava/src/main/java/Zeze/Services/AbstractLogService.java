// auto-generated @formatter:off
package Zeze.Services;

public abstract class AbstractLogService implements Zeze.IModule {
    public static final int ModuleId = 11035;
    public static final String ModuleName = "LogService";
    public static final String ModuleFullName = "Zeze.Services.LogService";

    @Override public int getId() { return ModuleId; }
    @Override public String getName() { return ModuleName; }
    @Override public String getFullName() { return ModuleFullName; }
    @Override public boolean isBuiltin() { return true; }

    public void RegisterProtocols(Zeze.Net.Service service) {
        var _reflect = new Zeze.Util.Reflect(getClass());
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LogService.BrowseRegex.class, Zeze.Builtin.LogService.BrowseRegex.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LogService.BrowseRegex::new;
            factoryHandle.Handle = this::ProcessBrowseRegexRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBrowseRegexRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessBrowseRegexRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47398183984017L, factoryHandle); // 11035, -1075094639
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LogService.BrowseWords.class, Zeze.Builtin.LogService.BrowseWords.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LogService.BrowseWords::new;
            factoryHandle.Handle = this::ProcessBrowseWordsRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessBrowseWordsRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessBrowseWordsRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47396025451615L, factoryHandle); // 11035, 1061340255
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LogService.SearchRegex.class, Zeze.Builtin.LogService.SearchRegex.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LogService.SearchRegex::new;
            factoryHandle.Handle = this::ProcessSearchRegexRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSearchRegexRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSearchRegexRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47397581277245L, factoryHandle); // 11035, -1677801411
        }
        {
            var factoryHandle = new Zeze.Net.Service.ProtocolFactoryHandle<>(Zeze.Builtin.LogService.SearchWords.class, Zeze.Builtin.LogService.SearchWords.TypeId_);
            factoryHandle.Factory = Zeze.Builtin.LogService.SearchWords::new;
            factoryHandle.Handle = this::ProcessSearchWordsRequest;
            factoryHandle.Level = _reflect.getTransactionLevel("ProcessSearchWordsRequest", Zeze.Transaction.TransactionLevel.None);
            factoryHandle.Mode = _reflect.getDispatchMode("ProcessSearchWordsRequest", Zeze.Transaction.DispatchMode.Normal);
            service.AddFactoryHandle(47395407458876L, factoryHandle); // 11035, 443347516
        }
    }

    public static void UnRegisterProtocols(Zeze.Net.Service service) {
        service.getFactorys().remove(47398183984017L);
        service.getFactorys().remove(47396025451615L);
        service.getFactorys().remove(47397581277245L);
        service.getFactorys().remove(47395407458876L);
    }

    public void RegisterZezeTables(Zeze.Application zeze) {
    }

    public void UnRegisterZezeTables(Zeze.Application zeze) {
    }

    public static void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks) {
    }

    protected abstract long ProcessBrowseRegexRequest(Zeze.Builtin.LogService.BrowseRegex r) throws Exception;
    protected abstract long ProcessBrowseWordsRequest(Zeze.Builtin.LogService.BrowseWords r) throws Exception;
    protected abstract long ProcessSearchRegexRequest(Zeze.Builtin.LogService.SearchRegex r) throws Exception;
    protected abstract long ProcessSearchWordsRequest(Zeze.Builtin.LogService.SearchWords r) throws Exception;
}
