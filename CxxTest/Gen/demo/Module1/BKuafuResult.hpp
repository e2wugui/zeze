#pragma once

#include "zeze/cxx/Bean.h"

namespace demo {
namespace Module1 {
class BKuafuResult : public Zeze::Bean {
public:
    static const int64_t TYPEID = -4375085639152583164LL;

    int64_t Money;

    BKuafuResult();
    BKuafuResult(int64_t Money_);
    virtual void Assign(const Zeze::Bean& other) override;
    void Assign(const BKuafuResult& other);
    BKuafuResult& operator=(const BKuafuResult& other);
    virtual int64_t TypeId() const override {
        return TYPEID;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override;
    void Decode(Zeze::ByteBuffer& _o_) override;
};
}
}
