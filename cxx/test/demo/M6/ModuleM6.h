#pragma once

// ZEZE_FILE_CHUNK {{{ IMPORT GEN
#include "Gen/demo/M6/AbstractModule.hpp"
// ZEZE_FILE_CHUNK }}} IMPORT GEN

namespace demo {
namespace M6 {

class ModuleM6 : public AbstractModule {
public:
    void Start();
    void Stop();

    // ZEZE_FILE_CHUNK {{{ GEN MODULE
    ModuleM6(demo::App* app);
    // ZEZE_FILE_CHUNK }}} GEN MODULE
};
}
}
