#pragma once

#include "zeze/cxx/Protocol.h"
#include "Gen/demo/Module2/BValue.hpp"

namespace demo {
namespace Module1 {
// protocol的注释
class Protocol3 : public Zeze::Net::ProtocolWithArgument<demo::Module2::BValue> {
public:
    static const int ModuleId_ = 1;
    static const int ProtocolId_ = -774467372; // 3520499924
    static const int64_t TypeId_ = Zeze::Net::Protocol::MakeTypeId(ModuleId_, ProtocolId_); // 7815467220

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
