package Zeze.Onz;

import Zeze.Serialize.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Transaction.Bean;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;

public class Onz extends AbstractOnz {
    private final ConcurrentHashMap<String, OnzProcedureStub<?, ?>> procedureStubs = new ConcurrentHashMap<>();
    private final LongConcurrentHashMap<OnzSaga> sagas = new LongConcurrentHashMap<>();
    private final OnzService service;

    public static class OnzService extends Service {
        public static final String eName = "Zeze.Onz.Server";
        public OnzService(Application zeze) {
            super(eName, zeze);
        }
    }

    public Onz(Application zeze) {
        if (null != zeze.getConfig().getServiceConf(OnzService.eName)) {
            service = new OnzService(zeze);
            RegisterProtocols(service);
        } else {
            service = null;
        }
    }

    public void start() throws Exception {
        if (null != service)
            service.start();
    }

    public void stop() throws Exception {
        if (null != service)
            service.stop();
    }

    public <A extends Bean, R extends Bean> void register(
            Application zeze,
            String name, OnzFuncProcedure<A, R> func,
            Class<A> argumentClass, Class<R> resultClass) {

        if (null != procedureStubs.putIfAbsent(name,
                new OnzProcedureStub<>(zeze, name, func, argumentClass, resultClass)))
            throw new RuntimeException("duplicate Onz Procedure Name=" + name);
    }

    public <A extends Bean, R extends Bean, T extends Bean> void registerSaga(
            Application zeze,
            String name, OnzFuncSaga<A, R> func, OnzFuncSagaEnd<T> funcCancel,
            Class<A> argumentClass, Class<R> resultClass, Class<T> cancelClass) {

        if (null != procedureStubs.putIfAbsent(name,
                new OnzSagaStub<>(zeze, name, func, argumentClass, resultClass, funcCancel, cancelClass)))
            throw new RuntimeException("duplicate Onz Procedure Name=" + name);
    }

    @Override
    protected long ProcessFuncProcedureRequest(Zeze.Builtin.Onz.FuncProcedure r) throws Exception {
        var stub = procedureStubs.get(r.Argument.getFuncName());
        if (stub == null)
            return errorCode(eProcedureNotFound);
        var buffer = ByteBuffer.Wrap(r.Argument.getFuncArgument().bytesUnsafe());
        var procedure = stub.newProcedure(r.getSender(), r.Argument, buffer);
        var rc = Task.call(stub.getZeze().newProcedure(procedure, procedure.getName()));
        if (rc != 0)
            return rc;

        var bbResult = ByteBuffer.Allocate();
        procedure.getResult().encode(bbResult);
        r.Result.setFuncResult(new Binary(bbResult));
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessFuncSagaRequest(Zeze.Builtin.Onz.FuncSaga r) throws Exception {
        var stub = procedureStubs.get(r.Argument.getFuncName());
        if (stub == null)
            return errorCode(eProcedureNotFound);

        var buffer = ByteBuffer.Wrap(r.Argument.getFuncArgument().bytesUnsafe());
        var procedure = stub.newProcedure(r.getSender(), r.Argument, buffer);
        if (null != sagas.putIfAbsent(r.Argument.getOnzTid(), (OnzSaga)procedure))
            return errorCode(eSagaTidExist);

        var rc = Task.call(stub.getZeze().newProcedure(procedure, procedure.getName()));
        if (rc != 0)
            return rc;

        var bbResult = ByteBuffer.Allocate();
        procedure.getResult().encode(bbResult);
        r.Result.setFuncResult(new Binary(bbResult));
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessFuncSagaEndRequest(Zeze.Builtin.Onz.FuncSagaEnd r) throws Exception {
        var context = sagas.remove(r.Argument.getOnzTid());
        if (context == null)
            return errorCode(eSagaNotFound);

        // 没有设置cancel标志时，表示事务正常结束，用来删除sagas上下文。
        if (r.Argument.isCancel()) {
            var stub = (OnzSagaStub<?, ?, ?>)context.getStub();
            var cancelArgument = stub.decodeCancelArgument(r.Argument.getFuncArgument());
            var rc = Task.call(stub.getZeze().newProcedure(() -> stub.end(context, cancelArgument), context.getName()));
            if (rc != 0)
                return rc;
        }
        context.setEnd();

        r.SendResult();
        return 0;
    }
}
