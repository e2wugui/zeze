#include "AbstractModule.hpp"
#include "Gen/demo/App.h"

namespace demo {
namespace Module1 {
    const char* AbstractModule::ModuleName = "Module1";
    const char* AbstractModule::ModuleFullName = "demo.Module1";

    AbstractModule::AbstractModule(demo::App* app)
    {
        App = app;
        // register protocol factory and handles
        {
            Zeze::Net::Service::ProtocolFactoryHandle factoryHandle;
            factoryHandle.Factory = []() { return new demo::Module1::Protocol3(); };
            factoryHandle.Handle = std::bind(&AbstractModule::ProcessProtocol3, this, std::placeholders::_1);
            App->TestClient->AddProtocolFactory(7815467220LL, factoryHandle); // 1, -774467372
        }
        {
            Zeze::Net::Service::ProtocolFactoryHandle factoryHandle;
            factoryHandle.Factory = []() { return new demo::Module1::Protocol4(); };
            factoryHandle.Handle = std::bind(&AbstractModule::ProcessProtocol4, this, std::placeholders::_1);
            App->TestClient->AddProtocolFactory(5222864529LL, factoryHandle); // 1, 927897233
        }
        {
            Zeze::Net::Service::ProtocolFactoryHandle factoryHandle;
            factoryHandle.Factory = []() { return new demo::Module1::Rpc1(); };
            App->TestClient->AddProtocolFactory(5635082623LL, factoryHandle); // 1, 1340115327
        }
        {
            Zeze::Net::Service::ProtocolFactoryHandle factoryHandle;
            factoryHandle.Factory = []() { return new demo::Module1::Rpc2(); };
            factoryHandle.Handle = std::bind(&AbstractModule::ProcessRpc2Request, this, std::placeholders::_1);
            App->TestClient->AddProtocolFactory(7854078040LL, factoryHandle); // 1, -735856552
        }
        {
            Zeze::Net::Service::ProtocolFactoryHandle factoryHandle;
            factoryHandle.Factory = []() { return new demo::Module1::RpcOnlyData(); };
            App->TestClient->AddProtocolFactory(7336162357LL, factoryHandle); // 1, -1253772235
        }
        {
            Zeze::Net::Service::ProtocolFactoryHandle factoryHandle;
            factoryHandle.Factory = []() { return new demo::Module1::RpcOnlyUseData(); };
            App->TestClient->AddProtocolFactory(7938190671LL, factoryHandle); // 1, -651743921
        }
    }

    void AbstractModule::UnRegister()
    {
    }
}
}
