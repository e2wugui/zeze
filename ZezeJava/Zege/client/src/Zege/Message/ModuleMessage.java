package Zege.Message;

import Zege.Program;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import Zeze.Util.TaskCompletionSource;

public class ModuleMessage extends AbstractModule {
    public void Start(Zege.App app) throws Exception {
    }

    public void Stop(Zege.App app) throws Exception {
    }

    public TaskCompletionSource<?> send(String target, String message, long departmentId) {
        if (target.endsWith("@group")) {
            var req = new SendDepartmentMessage();
            req.Argument.setGroup(target);
            req.Argument.setDepartmentId(departmentId);
            if (null != message) {
                var bMsg = new BTextMessage();
                bMsg.setMessage(message);
                req.Argument.getMessage().setSecureMessage(new Binary(ByteBuffer.encode(bMsg)));
            }
            Program.counters.increment("SendGroupMessage:" + req.Argument.getGroup() + "#" + req.Argument.getDepartmentId());
            return req.SendForWait(App.Connector.TryGetReadySocket());
        }
        var req = new SendMessage();
        req.Argument.setFriend(target);
        if (null != message) {
            var bMsg = new BTextMessage();
            bMsg.setMessage(message);
            req.Argument.getMessage().setSecureKeyIndex(-1);
            req.Argument.getMessage().setSecureMessage(new Binary(ByteBuffer.encode(bMsg)));
        }
        Program.counters.increment("SendFriendMessage");
        return req.SendForWait(App.Connector.TryGetReadySocket());
    }

    @Override
    protected long ProcessNotifyMessageRequest(Zege.Message.NotifyMessage r) throws Exception {
        Zege.Program.Instance.OnMessage(r);
        return Procedure.Success;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleMessage(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
