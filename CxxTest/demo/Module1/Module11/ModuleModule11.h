#pragma once

// ZEZE_FILE_CHUNK {{{ IMPORT GEN
#include "Gen/demo/Module1/Module11/AbstractModule.hpp"
// ZEZE_FILE_CHUNK }}} IMPORT GEN

namespace demo {
namespace Module1 {
namespace Module11 {

class ModuleModule11 : public AbstractModule {
public:
    void Start();
    void Stop();

    // ZEZE_FILE_CHUNK {{{ GEN MODULE
    ModuleModule11(demo::App* app);
    // ZEZE_FILE_CHUNK }}} GEN MODULE
};
}
}
}
