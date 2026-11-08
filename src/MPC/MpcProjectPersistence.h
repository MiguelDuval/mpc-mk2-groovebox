#pragma once

#include "MPC/Domain/MpcDomain.h"

#include <string>
#include <string_view>

namespace mpc::persistence {

// Version 4 embeds unique referenced PCM assets in binary blocks. Versions 1-3
// remain readable; legacy files without PCM retain explicit missing-asset state.
inline constexpr int kCurrentProjectFormatVersion = 4;

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
