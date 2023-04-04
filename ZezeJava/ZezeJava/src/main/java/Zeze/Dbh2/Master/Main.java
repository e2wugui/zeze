package Zeze.Dbh2.Master;

import Zeze.Config;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import org.rocksdb.RocksDBException;

public class Main {
	private final MasterService service;
	private final Master master;

	public Main() throws RocksDBException {
		var config = Config.load();
		service = new MasterService(config);
		master = new Master("master");
	}

	public void start() throws Exception {
		master.RegisterProtocols(service);
		service.start();

		ShutdownHook.add(this, this::stop);
	}

	public void stop() throws Exception {
		ShutdownHook.remove(this);

		service.stop();
		master.close();
	}

	public static void main(String[] args) throws Exception {
		Task.tryInitThreadPool(null, null, null);

		var main = new Main();
		main.start();

		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
