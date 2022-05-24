package Zege.Message;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Util.TaskCompletionSource;

public class ModuleMessage extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    public TaskCompletionSource<EmptyBean> send(String target, String message) {
        if (target.endsWith("@group")) {
            var req = new SendDepartmentMessage();
            req.Argument.setGroup(target);
            var bMsg = new BTextMessage();
            bMsg.setMessage(message);
            req.Argument.getMessage().setSecureMessage(new Binary(ByteBuffer.Encode(bMsg)));
            return req.SendForWait(App.Connector.GetReadySocket());
        } else {
            var req = new SendMessage();
            req.Argument.setFriend(target);
            var bMsg = new BTextMessage();
            bMsg.setMessage(message);
            req.Argument.getMessage().setSecureMessage(new Binary(ByteBuffer.Encode(bMsg)));
            return req.SendForWait(App.Connector.GetReadySocket());
        }
    }

    @Override
    protected long ProcessNotifyMessageRequest(Zege.Message.NotifyMessage r) {
        Program.Instance.process(r);
        return Procedure.Success;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleMessage(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
