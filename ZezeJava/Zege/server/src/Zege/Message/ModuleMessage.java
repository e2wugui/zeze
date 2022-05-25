package Zege.Message;

import Zeze.Arch.ProviderUserSession;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.TransactionLevelAnnotation;

public class ModuleMessage extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    @Override
    @TransactionLevelAnnotation(Level=TransactionLevel.None)
    protected long ProcessSendDepartmentMessageRequest(Zege.Message.SendDepartmentMessage r) {
        var session = ProviderUserSession.get(r);
        var group = App.Zege_Friend.getDepartmentTree(r.Argument.getGroup());

        r.Argument.getMessage().setFrom(session.getAccount());
        r.Argument.getMessage().setGroup(r.Argument.getGroup());

        var notify = new NotifyMessage();
        notify.Argument = r.Argument.getMessage();
        if (0 == r.Argument.getDepartmentId()) {
            // group root
            group.getMembers().walk((node, member) -> {
                App.Provider.Online.sendWhileCommit(member.getAccount(), "PC", notify);
                return true;
            });
        } else {
            // department
            group.getDepartmentMembers(r.Argument.getDepartmentId()).walk((node, member) -> {
                App.Provider.Online.sendWhileCommit(member.getAccount(), "PC", notify);
                return true;
            });
        }
        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessSendMessageRequest(Zege.Message.SendMessage r) {
        var session = ProviderUserSession.get(r);
        var friends = App.Zege_Friend.getFriends(session.getAccount());
        var friend = friends.get(r.Argument.getFriend());

        if (null == friend)
            return Procedure.LogicError;

        r.Argument.getMessage().setFrom(session.getAccount());
        var notify = new NotifyMessage();
        notify.Argument = r.Argument.getMessage();
        App.Provider.Online.sendWhileCommit(r.Argument.getFriend(), "PC", notify);

        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleMessage(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
