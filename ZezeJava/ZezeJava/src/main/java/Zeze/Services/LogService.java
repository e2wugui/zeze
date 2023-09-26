package Zeze.Services;

public class LogService extends AbstractLogService {
    @Override
    protected long ProcessBrowseRegexRequest(Zeze.Builtin.LogService.BrowseRegex r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessBrowseWordsRequest(Zeze.Builtin.LogService.BrowseWords r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessSearchRegexRequest(Zeze.Builtin.LogService.SearchRegex r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessSearchWordsRequest(Zeze.Builtin.LogService.SearchWords r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
