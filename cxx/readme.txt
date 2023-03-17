
Net
	1. 现在网络接口很简单，可能未来需要写一个比较完整的版本。
	    【不要大用特用】
	2. Protocol 生命期
	    自行管理生命期，一般建议使用栈变量。【内部不管生命期】发完了事。
	3. Rpc 生命期
	    a) 同步模式
	    自行管理生命期，使用栈变量或new（自己delete）都可以。
	    等待到结果以后继续处理。例子：
	    Rpc1 rpc1;
	    rpc1.SendForWait(socket)->Wait();
	    rpc1.Result Process;
	    b) 异步模式
	    必须new出来，例子如下：
	    std::auto_ptr<Rpc1> guard(new Rpc1());
	    if (guard->Send(socket, []() { process async response ...; return 0; }))
		gurard.release(); // 【发送成功，内部开始接管生命期管理，马上释放所有权】

	4. 重载 Service DispatchProtocol DispatchRpcResponse 时对传入Protocol*p的生命期管理
	    【Dispatch时需要管理传入的*p】
	   例子：
	void Service::DispatchProtocol(Protocol* p, Service::ProtocolFactoryHandle& factoryHandle)
	{
		std::auto_ptr<Protocol> at(p);
		factoryHandle.Handle(p); // 马上处理，处理完，auto_ptr会释放内存，
		// 如果Dispatch把p传给其他地方而不是马上处理，那么接收的地方获得p的所有权。
		// 比如传给main线程的队列。main_thread_task_queue.add(at.release());
	}

	void Service::DispatchRpcResponse(Protocol* r, std::function<int(Protocol*)>& responseHandle, Service::ProtocolFactoryHandle& factoryHandle)
	{
		std::auto_ptr<Protocol> at(r);
		responseHandle(r);
		// 同 DispatchProtocol
	}

	* Protocol * Rpc * 处理函数一般不delete，基本规则是交给dispatch管理。
	   但是在实现dispatch时，自己也可以不管理，process的最后加一个delete。
	  【但是不建议这样】。
