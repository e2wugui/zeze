
package demo;

// ZEZE_FILE_CHUNK {{{ IMPORT GEN
import java.util.*;
// ZEZE_FILE_CHUNK }}} IMPORT GEN


public class App {

    public static App Instance = new App();
    public static App getInstance() {
        return Instance;
    }

    public void Start() {
        Create();
        Zeze.Start(); // 启动数据库
        StartModules(); // 启动模块，装载配置什么的。
        StartService(); // 启动网络
    }

    public void Stop() {
        StopService(); // 关闭网络
        StopModules(); // 关闭模块,，卸载配置什么的。
        Zeze.Stop(); // 关闭数据库
        Destroy();
    }

    // ZEZE_FILE_CHUNK {{{ GEN APP
    public Zeze.Application Zeze;
    public HashMap<String, Zeze.IModule> Modules = new HashMap<>();

    public demo.Module1.ModuleModule1 demo_Module1;

    public demo.Module1.Module11.ModuleModule11 demo_Module1_Module11;

    public demo.Server Server;

    public Zeze.IModule ReplaceModuleInstance(Zeze.IModule module) {
        return module;
    }

    public void Create() {
        Create(null);
    }

    public void Create(Zeze.Config config) {
        synchronized (this) {
            if (null != Zeze)
                return;

            Zeze = new Zeze.Application("demo", config);

            Server = new demo.Server(Zeze);

            demo_Module1 = new demo.Module1.ModuleModule1(this);
            demo_Module1 = (demo.Module1.ModuleModule1)ReplaceModuleInstance(demo_Module1);
            if (null != Modules.put(demo_Module1.getName(), demo_Module1)) {
                throw new RuntimeException("duplicate module name: demo_Module1");
            }
            demo_Module1_Module11 = new demo.Module1.Module11.ModuleModule11(this);
            demo_Module1_Module11 = (demo.Module1.Module11.ModuleModule11)ReplaceModuleInstance(demo_Module1_Module11);
            if (null != Modules.put(demo_Module1_Module11.getName(), demo_Module1_Module11)) {
                throw new RuntimeException("duplicate module name: demo_Module1_Module11");
            }

            Zeze.setSchemas(new demo.Schemas());
        }
    }

    public void Destroy() {
        synchronized(this) {
            demo_Module1 = null;
            demo_Module1_Module11 = null;
            Modules.clear();
            Server = null;
            Zeze = null;
        }
    }

    public void StartModules() {
        synchronized(this) {
            demo_Module1.Start(this);
            demo_Module1_Module11.Start(this);

        }
    }

    public void StopModules() {
        synchronized(this) {
            demo_Module1.Stop(this);
            demo_Module1_Module11.Stop(this);
        }
    }

    public void StartService() {
        synchronized(this) {
            Server.Start();
        }
    }

    public void StopService() {
        synchronized(this) {
            Server.Stop();
        }
    }

	public Zeze.Application getZeze() {
		return Zeze;
	}

}
