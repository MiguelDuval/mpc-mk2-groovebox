#pragma once

#include "MPC/Domain/MpcDomain.h"

#include <string>
#include <string_view>

namespace mpc::persistence {

// Version 2 adds explicit Pattern-placement records. Version 1 is accepted
// for projects whose Tracks contain Patterns but no explicit placements.
inline constexpr int kCurrentProjectFormatVersion = 2;

std::string serializeProject(const domain::Project& project, std::string* error = nullptr);
bool deserializeProject(
        std::string_view bytes,
        domain::Project& project,
        std::string* error = nullptr);

bool saveProjectFile(
        const domain::Project& project,
        const std::string& path,
        std::string* error = nullptr);
bool loadProjectFile(
        const std::string& path,
        domain::Project& project,
        std::string* error = nullptr);

} // namespace mpc::persistence
