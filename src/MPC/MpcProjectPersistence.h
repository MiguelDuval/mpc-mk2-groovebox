#pragma once

#include "MPC/Domain/MpcDomain.h"

#include <string>
#include <string_view>

namespace mpc::persistence {

// Version 2 adds explicit Pattern-placement records. Version 3 adds per-note
// transient pitch offsets; versions 1 and 2 remain readable with a zero offset.
inline constexpr int kCurrentProjectFormatVersion = 3;

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
