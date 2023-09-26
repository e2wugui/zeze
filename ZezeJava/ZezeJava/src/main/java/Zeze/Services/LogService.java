package Zeze.Services;

import Zeze.Builtin.LogService.Browse;
import Zeze.Builtin.LogService.CloseSession;
import Zeze.Builtin.LogService.NewSessionRegex;
import Zeze.Builtin.LogService.NewSessionWords;
import Zeze.Builtin.LogService.Search;

public class LogService extends AbstractLogService {
    @Override
    protected long ProcessCloseSessionRequest(CloseSession r) throws Exception {
        return 0;
    }

    @Override
    protected long ProcessNewSessionRegexRequest(NewSessionRegex r) throws Exception {
        return 0;
    }

    @Override
    protected long ProcessNewSessionWordsRequest(NewSessionWords r) throws Exception {
        return 0;
    }

    @Override
    protected long ProcessBrowseRequest(Browse r) throws Exception {
        return 0;
    }

    @Override
    protected long ProcessSearchRequest(Search r) throws Exception {
        return 0;
    }
}
