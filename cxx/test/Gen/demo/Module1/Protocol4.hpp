#pragma once

#include "zeze/cxx/Protocol.h"
#include "Gen/demo/Module1/BValue.hpp"

namespace demo {
namespace Module1 {
// protocol的注释
class Protocol4 : public Zeze::Net::ProtocolWithArgument<demo::Module1::BValue> {
public:
    static const int ModuleId_ = 1;
    static const int ProtocolId_ = 927897233;
    static const int64_t TypeId_ = Zeze::Net::Protocol::MakeTypeId(ModuleId_, ProtocolId_); // 5222864529

    virtual int ModuleId() const override {
        return ModuleId_;
    }

    virtual int ProtocolId() const override {
        return ProtocolId_;
    }

    int64_t TypeId() const {
        return TypeId_;
    }
};
}
}
