
#include "Gen/demo/Bean1.hpp"
#include "Gen/demo/Module1/BValue.hpp"
#include "zeze/cxx/Net.h"
#include "demo/TestClient.h"
#include <cmath>

void TestByteBuffer();
void TestSocket();
void TestEncode();
void TestProtocol();
void TestFuture();
void TestEcho();

int main(char* args[])
{
	TestByteBuffer();
	TestFuture();
	int mills = 200;
	std::cout << std::ceil(mills / 1000.0) << std::endl;
	TestEncode();
	TestSocket();
	//TestEcho(); // telnet 127.0.0.1 9998 输入几个字符然后关闭就能退出这个测试。
	TestProtocol();
}

#include "Gen/demo/Module1/Protocol3.hpp"
#include "Gen/demo/Module1/Rpc1.hpp"
#include "Gen/demo/Module1/Rpc2.hpp"

class ProtocolServer : public Zeze::Net::Service
{
public:
	ProtocolServer()
	{
		AddProtocolFactory(demo::Module1::Protocol3::TypeId_, ProtocolFactoryHandle(
			[]()
			{
				return new demo::Module1::Protocol3();
			},
			[](Zeze::Net::Protocol* p)
			{
				std::cout << "Server ProcessProtocol3" << std::endl;
				p->Send(p->Sender.get());
				return 0;
			}
		));

		AddProtocolFactory(demo::Module1::Rpc1::TypeId_, ProtocolFactoryHandle(
			[]()
			{
				return new demo::Module1::Rpc1();
			},
			[](Zeze::Net::Protocol* p)
			{
				auto r = (demo::Module1::Rpc1*)p;
				std::cout << "Server ProcessRpc1 Never!" << std::endl;
				r->SendResult();

				return 0;
			}
		));

		AddProtocolFactory(demo::Module1::Rpc2::TypeId_, ProtocolFactoryHandle(
			[]()
			{
				return new demo::Module1::Rpc2();
			},
			[](Zeze::Net::Protocol* p)
			{
				auto r = (demo::Module1::Rpc2*)p;
				std::cout << "Server ProcessRpc2" << std::endl;
				r->SendResult();
				return 0;
			}
		));
	}
};

class ProtocolClient : public Zeze::Net::Service
{
public:
	ProtocolClient()
	{
		AddProtocolFactory(demo::Module1::Protocol3::TypeId_, ProtocolFactoryHandle(
			[]()
			{
				return new demo::Module1::Protocol3();
			},
			[](Zeze::Net::Protocol* p)
			{
				std::cout << "ProcessProtocol3" << std::endl;
				return 0;
			}
			));

		AddProtocolFactory(demo::Module1::Rpc1::TypeId_, ProtocolFactoryHandle(
			[]()
			{
				return new demo::Module1::Rpc1();
			},
			[](Zeze::Net::Protocol* p)
			{
				auto r = (demo::Module1::Rpc1*)p;
				std::cout << "ProcessRpc1 Never!" << std::endl;
				r->SendResult();
				(new demo::Module1::Rpc2())->SendAsync(p->Sender.get(), [](Zeze::Net::Protocol* p)
					{
						std::cout << "Rpc2 Async Response." << std::endl;
						return 0;
					});
				return 0;
			}
			));

		AddProtocolFactory(demo::Module1::Rpc2::TypeId_, ProtocolFactoryHandle(
			[]()
			{
				return new demo::Module1::Rpc2();
			},
			[](Zeze::Net::Protocol* p)
			{
				auto r = (demo::Module1::Rpc2*)p;
				std::cout << "ProcessRpc2" << std::endl;
				r->SendResult();
				return 0;
			}
			));
	}

	virtual void OnHandshakeDone(const std::shared_ptr<Zeze::Net::Socket>& sender) override
	{
		Service::OnHandshakeDone(sender);

		demo::Module1::Protocol3 p;
		p.Send(GetSocket().get());
		(new demo::Module1::Rpc1())->SendAsync(GetSocket().get(), [](Zeze::Net::Protocol* p)
			{
				std::cout << "Rpc1 Async Response." << std::endl;
				return 0;
			});

		(new demo::Module1::Rpc2())->SendAsync(GetSocket().get(), [](Zeze::Net::Protocol* p)
			{
				std::cout << "Rpc2 Async Response." << std::endl;
				return 0;
			});
		std::thread([this]()
			{
				demo::Module1::Rpc1 rpc1;
				rpc1.SendForWait(GetSocket().get())->Wait();
				std::cout << "Send Rpc1 Done." << std::endl;
				future.SetResult(1);
			}).detach();
	}
	Zeze::TaskCompletionSource<int> future;
};

#include "zeze/cxx/TaskCompletionSource.h"

void TestFuture()
{
	std::shared_ptr<Zeze::TaskCompletionSource<int>> future(new Zeze::TaskCompletionSource<int>());
	std::thread([future]
		{
			std::this_thread::sleep_for(std::chrono::milliseconds(2000));
			future->SetResult(1);
		}).detach();

	future->Wait();
	std::cout << "TaskCompletionSource Done -> " << future->Get() << std::endl;
}

void TestProtocol()
{
	Zeze::Net::Startup();
	ProtocolClient client;
	ProtocolServer server;
	server.SetHandshakeOptions(Zeze::Net::eEncryptTypeAes, Zeze::Net::eCompressTypeMppc, Zeze::Net::eCompressTypeMppc);
	server.Listen("127.0.0.1", 7777);
	client.Connect("127.0.0.1", 7777);
	client.future.Wait();
	Zeze::Net::Cleanup();
}

class Client : public Zeze::Net::Service
{
public:
	void OnSocketProcessInputBuffer(const std::shared_ptr<Zeze::Net::Socket>& sender, Zeze::ByteBuffer& input) override
	{
		std::cout << std::string((char*)input.Bytes, input.ReadIndex, input.Size()) << std::endl;
		input.ReadIndex = input.WriteIndex;
	}

	void OnSocketConnected(const std::shared_ptr<Zeze::Net::Socket>& sender)
	{
		Service::OnSocketConnected(sender);
		std::string req("HEAD / HTTP/1.0\r\n\r\n");
		sender->Send(req.data(), (int)req.size());
	}

	void OnSocketClose(const std::shared_ptr<Zeze::Net::Socket>& sender, const std::exception* e)
	{
		Service::OnSocketClose(sender, e);
		future.SetResult(1);
	}
	Zeze::TaskCompletionSource<int> future;
};

void TestSocket()
{
	Zeze::Net::Startup();
	Client client;
	client.Connect("www.163.com", 80);
	client.future.Wait();
	Zeze::Net::Cleanup();
}

class EchoServer : public Zeze::Net::Service
{
public:
	void OnSocketProcessInputBuffer(const std::shared_ptr<Zeze::Net::Socket>& sender, Zeze::ByteBuffer& input) override
	{
		//std::cout << std::string((char*)input.Bytes, input.ReadIndex, input.Size()) << std::endl;
		sender->Send((const char*)input.Bytes, input.ReadIndex, input.Size());
		input.ReadIndex = input.WriteIndex;
	}

	void OnSocketAccept(const std::shared_ptr<Zeze::Net::Socket>& sender)
	{
		AddSocket(sender);
		//Service::OnSocketAccept(sender); // 默认的实现是handshake协议加密。
		//std::cout << "on accept." << std::endl;
	}

	void OnSocketClose(const std::shared_ptr<Zeze::Net::Socket> & sender, const std::exception * e)
	{
		Service::OnSocketClose(sender, e);
		future.SetResult(1);
	}
	Zeze::TaskCompletionSource<int> future;
};

void TestEcho()
{
	Zeze::Net::Startup();
	EchoServer server;
	server.Listen("::", 9998);
	server.future.Wait();
	Zeze::Net::Cleanup();
}

void TestEncode()
{
	Zeze::ByteBuffer bb(16);
	demo::Module1::BValue bValue;
	bValue.Int_1 = 1;
	bValue.Long2 = 2;
	bValue.String3 = "3";
	bValue.Bool4 = true;
	bValue.Short5 = 5;
	bValue.Float6 = 6;
	bValue.Double7 = 7;
	bValue.Bytes8 = "8";
	bValue.List9.push_back(demo::Bean1());
	bValue.Set10.insert(10);
	bValue.Map11[11] = demo::Module2::BValue();
	bValue.Bean12.Int_1 = 12;
	bValue.Byte13 = 13;
	bValue.Dynamic14.SetBean(new demo::Bean1());
	bValue.Dynamic14.SetBean(new demo::Module1::BSimple()); // set again
	bValue.Map15[15] = 15;
	demo::Module1::Key key(16, "abc");
	bValue.Map16[key] = demo::Module1::BSimple();
	bValue.Vector2.x = 17;
	bValue.Vector2Int.x = 18;
	bValue.Vector3.x = 19;
	bValue.Vector4.x = 20;
	bValue.Quaternion.x = 21;
	Zeze::Vector2Int v2i(22, 22);
//	bValue.MapVector2Int[v2i] = v2i;
	bValue.ListVector2Int.push_back(v2i);
	bValue.Map25[key] = demo::Module1::BSimple();
	bValue.Map26[key] = demo::Module1::BValue::constructDynamicBean_Map26();
	bValue.Map26[key].SetBean(new demo::Module1::BSimple());
	bValue.Dynamic27.SetBean(new demo::Module1::BSimple());
	bValue.Key28.S = 28;
	bValue.Key28.Assign(demo::Module1::Key(28, "abc"));
	bValue.Array29.push_back(29);
	bValue.LongList.push_back(30);
	bValue.Encode(bb);

	Zeze::ByteBuffer bb2(bb.Bytes, 0, bb.WriteIndex);
	demo::Module1::BValue bValueDecoded;
	bValueDecoded.Decode(bb2);
	bValue.Assign(bValueDecoded);
}
