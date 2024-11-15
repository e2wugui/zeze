package Zeze.MQ;

public class MQManager extends AbstractMQManager {
    @Override
    protected long ProcessSendMessageRequest(Zeze.Builtin.MQ.SendMessage r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessSubscribeRequest(Zeze.Builtin.MQ.Subscribe r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }
}
