#pragma once

#include "zeze/cxx/Protocol.h"

namespace demo {
namespace Module1 {
// protocol的注释
class ProtocolNoProcedure : public Zeze::Net::ProtocolWithArgument<Zeze::EmptyBean> {
public:
    static const int ModuleId_ = 1;
    static const int ProtocolId_ = 1350939829;
    static const int64_t TypeId_ = Zeze::Net::Protocol::MakeTypeId(ModuleId_, ProtocolId_); // 5645907125

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
