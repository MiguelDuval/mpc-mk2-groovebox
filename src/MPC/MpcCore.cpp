#include "MPC/MpcCore.h"

namespace mpc {

MpcCore::MpcCore()
        : projectState_(),
          audio_(projectState_) {
}

MpcCore& MpcCore::instance() {
    static MpcCore core;
    return core;
}

} // namespace mpc
