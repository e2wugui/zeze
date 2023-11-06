package Zeze.Services;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Zoker.AppendFile;
import Zeze.Builtin.Zoker.BListServiceResult;
import Zeze.Builtin.Zoker.BService;
import Zeze.Builtin.Zoker.CloseFile;
import Zeze.Builtin.Zoker.ListSerivce;
import Zeze.Builtin.Zoker.OpenFile;
import Zeze.Builtin.Zoker.StartService;
import Zeze.Builtin.Zoker.StopService;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Services.ZokerImpl.ZokerAgentService;
import org.jetbrains.annotations.NotNull;

public class ZokerAgent extends AbstractZokerAgent {
    private final ZokerAgentService clientWithAcceptor; // Zoker是server，但是Zoker主动连接client。
    private final ConcurrentHashMap<String, AsyncSocket> zokers = new ConcurrentHashMap<>();

    public ZokerAgent(Config config) {
        clientWithAcceptor = new ZokerAgentService(this, config);
        RegisterProtocols(clientWithAcceptor);
    }

    public void start() throws Exception {
        clientWithAcceptor.start();
    }

    public void stop() throws Exception {
        clientWithAcceptor.stop();
    }

    public ConcurrentHashMap<String, AsyncSocket> zokers() {
        return zokers;
    }

    @Override
    protected long ProcessRegisterRequest(Zeze.Builtin.Zoker.Register r) {
        if (null != zokers.putIfAbsent(r.Argument.getZokerName(), r.getSender()))
            return eDuplicateZoker;
        r.getSender().setUserState(r.Argument.getZokerName());
        r.SendResult();
        return 0;
    }

    private @NotNull AsyncSocket getZoker(String zokerName) {
        var zoker = zokers.get(zokerName);
        if (null == zoker)
            throw new RuntimeException("zoker not exist. " + zokerName);
        return zoker;
    }

    public long openFile(String zokerName, String fileName) {
        var zoker = getZoker(zokerName);
        var r = new OpenFile();
        r.Argument.setFileName(fileName);
        r.SendForWait(zoker).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("open file error. " + IModule.getErrorCode(r.getResultCode()));

        // 更多返回结果，修改 BOpenFileResult 并修改返回值。
        return r.Result.getOffset();
    }

    public void appendFile(String zokerName, String fileName, long offset, Binary data) {
        var zoker = getZoker(zokerName);
        var r = new AppendFile();
        r.Argument.setFileName(fileName);
        r.Argument.setOffset(offset);
        r.Argument.setChunk(data);
        r.SendForWait(zoker).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("append file error. " + IModule.getErrorCode(r.getResultCode()));
        // 需要返回结果，修改 BAppendFileResult
    }

    public void closeFile(String zokerName, String fileName, Binary md5) {
        var zoker = getZoker(zokerName);
        var r = new CloseFile();
        r.Argument.setFileName(fileName);
        r.Argument.setMd5(md5);
        r.SendForWait(zoker).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("close file error. " + IModule.getErrorCode(r.getResultCode()));
    }

    public BListServiceResult.Data listService(String zokerName) {
        var zoker = getZoker(zokerName);
        var r = new ListSerivce();
        r.SendForWait(zoker).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("list service error. " + IModule.getErrorCode(r.getResultCode()));
        return r.Result;
    }

    public BService.Data startService(String zokerName, String serviceName) {
        var zoker = getZoker(zokerName);
        var r = new StartService();
        r.Argument.setServiceName(serviceName);
        r.SendForWait(zoker).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("start service error. " + IModule.getErrorCode(r.getResultCode()));
        return r.Result;
    }

    public BService.Data stopService(String zokerName, String serviceName, boolean force) {
        var zoker = getZoker(zokerName);
        var r = new StopService();
        r.Argument.setServiceName(serviceName);
        r.Argument.setForce(force);
        r.SendForWait(zoker).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("stop service error. " + IModule.getErrorCode(r.getResultCode()));
        return r.Result;
    }
}
