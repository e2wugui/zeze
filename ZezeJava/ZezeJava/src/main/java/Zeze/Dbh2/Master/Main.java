package Zeze.Dbh2.Master;

import Zeze.Config;

public class Main {
	private final MasterService service;
	private final Master master;

	public Main() {
		var config = Config.load();
		service = new MasterService(config);
		master = new Master();
	}

	public void start() throws Exception {
		master.RegisterProtocols(service);
		service.start();
	}

	public static void main(String [] args) throws Exception {
		var main = new Main();
		main.start();
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
