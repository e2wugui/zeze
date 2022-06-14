package Zege;

import Zeze.Builtin.LinkdBase.BReportError;

public class LinkdService extends LinkdServiceBase {
    public LinkdService(Zeze.Application zeze) throws Throwable {
        super(zeze);
    }

    @Override
    public void DispatchUnknownProtocol(Zeze.Net.AsyncSocket so, int moduleId, int protocolId, Zeze.Serialize.ByteBuffer data) {
        var linkSession = getAuthedSession(so);
        setStableLinkSid(linkSession, so, moduleId, protocolId, data);
        var dispatch = createDispatch(linkSession, so, moduleId, protocolId, data);
        switch (moduleId) {
        case 1:
            break;
        }
        if (findSend(linkSession, moduleId, dispatch))
            return;
        if (choiceBindSend(so, moduleId, dispatch))
            return;
        ReportError(so.getSessionId(), BReportError.FromLink, BReportError.CodeNoProvider, "no provider.");
    }
}
