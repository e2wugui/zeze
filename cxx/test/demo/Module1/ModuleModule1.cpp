
// ZEZE_FILE_CHUNK {{{ IMPORT GEN
#include "ModuleModule1.h"
#include "Gen/demo/App.h"
// ZEZE_FILE_CHUNK }}} IMPORT GEN

namespace demo {
namespace Module1 {

    void ModuleModule1::Start() {
    }

    void ModuleModule1::Stop() {
    }

    int64_t ModuleModule1::ProcessProtocol3(Zeze::Net::Protocol* _p) {
        return Zeze::ResultCode::NotImplement;
    }

    int64_t ModuleModule1::ProcessProtocol4(Zeze::Net::Protocol* _p) {
        return Zeze::ResultCode::NotImplement;
    }

    int64_t ModuleModule1::ProcessRpc2Request(Zeze::Net::Protocol* _r) {
        return Zeze::ResultCode::NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE
    ModuleModule1::ModuleModule1(demo::App* app) : AbstractModule(app) {
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE
}
}
