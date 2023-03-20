#pragma once

// ZEZE_FILE_CHUNK {{{ IMPORT GEN
#include "Gen/TaskTest/TaskExt/AbstractModule.hpp"
// ZEZE_FILE_CHUNK }}} IMPORT GEN

namespace TaskTest {
namespace TaskExt {

class ModuleTaskExt : public AbstractModule {
public:
    void Start();
    void Stop();

    // ZEZE_FILE_CHUNK {{{ GEN MODULE
    ModuleTaskExt(demo::App* app);
    // ZEZE_FILE_CHUNK }}} GEN MODULE
};
}
}
