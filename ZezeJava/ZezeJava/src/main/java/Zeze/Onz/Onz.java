package Zeze.Onz;

import Zeze.Serialize.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Transaction.Bean;
import Zeze.Transaction.RelativeRecordSet;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;

public class Onz extends AbstractOnz {
    private final ConcurrentHashMap<String, OnzProcedureStub<?, ?>> procedureStubs = new ConcurrentHashMap<>();
    private final LongConcurrentHashMap<RelativeRecordSet> onzRrs = new LongConcurrentHashMap<>();
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

    public <A extends Bean, R extends Bean> void registerSaga(
            Application zeze,
            String name, OnzFuncSaga<A, R> func, OnzFuncSagaCancel funcCancel,
            Class<A> argumentClass, Class<R> resultClass) {

        if (null != procedureStubs.putIfAbsent(name,
                new OnzSagaStub<>(zeze, name, func, argumentClass, resultClass, funcCancel)))
            throw new RuntimeException("duplicate Onz Procedure Name=" + name);
    }

    public void markRrs(RelativeRecordSet rrs, OnzProcedure onzProcedure) {
        if (null != onzProcedure) {
            rrs.addOnzProcedures(onzProcedure);
            // todo 这里标记并记录rrs相关信息，
            //  用来在任意一个zeze集群发起rrs.flush的时候，
            //  能通知到相关onz，onz能追溯到这个onz事务相关的其他zeze集群，
            //  最终让其他相关的zeze集群也同步开始flush。
            //  因为不同的zeze集群的rrs.flush时机是没有统一控制的。
            //  这个追溯流程有点复杂，看看实现上有没有更好的方法。
            // todo 这里的代码随便写的。
            // todo onzRrs 的删除：准备采取不严格定时扫描检测删除，严格生命期需要侵入到原来zeze的代码，怕有遗漏。
            for (var onz : rrs.getOnzProcedures()) {
                onzRrs.put(onz.getOnzTid(), rrs);
            }
        }
    }

    @Override
    protected long ProcessFuncProcedureRequest(Zeze.Builtin.Onz.FuncProcedure r) throws Exception {
        var stub = procedureStubs.get(r.Argument.getFuncName());
        if (stub == null)
            return errorCode(eProcedureNotFound);
        var buffer = ByteBuffer.Wrap(r.Argument.getFuncArgument().bytesUnsafe());
        var procedure = stub.newProcedure(r.Argument.getOnzTid(), buffer);
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
        var procedure = stub.newProcedure(r.Argument.getOnzTid(), buffer);
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
    protected long ProcessFuncSagaCancelRequest(Zeze.Builtin.Onz.FuncSagaCancel r) {
        var context = sagas.remove(r.Argument.getOnzTid());
        if (context == null)
            return errorCode(eSagaNotFound);

        // 没有设置cancel标志时，表示事务正常结束，用来删除sagas上下文。
        if (r.Argument.isCancel()) {
            var rc = Task.call(context.getStub().getZeze().newProcedure(context, context.getName()));
            if (rc != 0)
                return rc;
        }

        r.SendResult();
        return 0;
    }
}
