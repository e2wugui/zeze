#pragma once

#include "zeze/cxx/Bean.h"

namespace demo {
namespace Module1 {
class BTestSchemas : public Zeze::Bean {
public:
    static const int64_t TYPEID = -3970310617082156489LL;

    int64_t Var1;
    int64_t Var2;

    BTestSchemas();
    BTestSchemas(int64_t Var1_, int64_t Var2_);
    virtual void Assign(const Zeze::Bean& other) override;
    void Assign(const BTestSchemas& other);
    BTestSchemas& operator=(const BTestSchemas& other);
    virtual int64_t TypeId() const override {
        return TYPEID;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override;
    void Decode(Zeze::ByteBuffer& _o_) override;
};
}
}
