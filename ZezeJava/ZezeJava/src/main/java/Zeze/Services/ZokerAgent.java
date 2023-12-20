package Zeze.Services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Zoker.AppendFile;
import Zeze.Builtin.Zoker.BListServiceResult;
import Zeze.Builtin.Zoker.BService;
import Zeze.Builtin.Zoker.CloseFile;
import Zeze.Builtin.Zoker.CommitService;
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

    public void appendFile(String zokerName, String fileName, long offset, byte[] data) {
        appendFile(zokerName, fileName, offset, data, 0, data.length);
    }

    public void appendFile(String zokerName, String fileName, long offset, byte[] data, int dataOffset, int dataLength) {
        var zoker = getZoker(zokerName);
        var r = new AppendFile();
        r.Argument.setFileName(fileName);
        r.Argument.setOffset(offset);
        r.Argument.setChunk(new Binary(data, dataOffset, dataLength));
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

    /**
     * 发布服务到zoker上。
     * @param zokerName zoker name
     * @param localServiceHome local service home 服务文件目录所在的文件夹
     * @param serviceName service name
     * @param versionNo version no 将被当作文件路径名，不能包含文件路径不支持的字符。
     */
    public void distributeService(String zokerName, File localServiceHome, String serviceName, String versionNo) throws Exception {
        var serviceDir = new File(localServiceHome, serviceName);
        if (!serviceDir.isDirectory() || !serviceDir.exists())
            throw new RuntimeException("service dir error.");
        distributeService(zokerName, localServiceHome.toPath(), serviceDir);
        var r = new CommitService();
        r.Argument.setServiceName(serviceName);
        r.Argument.setVersionNo(versionNo);
        r.SendForWait(getZoker(zokerName)).await();
        if (r.getResultCode() != 0)
            throw new RuntimeException("commit service error=" + IModule.getErrorCode(r.getResultCode()));
    }

    private static void md5To(MessageDigest md5, BufferedInputStream bis, long toOffset) throws IOException {
        var buffer = new byte[16 * 1024];
        while (toOffset > 0) {
            var tryReadLen = (int)(toOffset > buffer.length ? buffer.length : toOffset);
            var rc = bis.read(buffer, 0, tryReadLen);
            if (rc >= 0) {
                toOffset -= rc;
                md5.update(buffer, 0, rc);
                continue;
            }
            throw new RuntimeException("md5To not enough data");
        }
    }

    private void distributeService(String zokerName, Path localServiceHome, File serviceDir) throws Exception {
        var files = serviceDir.listFiles();
        if (null == files)
            return;

        for (var file : files) {
            if (file.isDirectory()) {
                distributeService(zokerName, localServiceHome, file);
                continue;
            }
            var fileRelativeName = localServiceHome.relativize(file.toPath()).toString().replace("\\", "/");
            var md5 = MessageDigest.getInstance("MD5");
            var fileOffset = openFile(zokerName, fileRelativeName);
            try (var bis = new BufferedInputStream(new FileInputStream(file))) {
                md5To(md5, bis, fileOffset);
                var buffer = new byte[16 * 1024];
                var rc = 0;
                while ((rc = bis.read(buffer)) >= 0) {
                    md5.update(buffer, 0, rc);
                    appendFile(zokerName, fileRelativeName, fileOffset, buffer, 0, rc);
                    fileOffset += rc;
                }
            } finally {
                closeFile(zokerName, fileRelativeName, new Binary(md5.digest()));
            }
        }
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
