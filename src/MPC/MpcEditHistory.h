#pragma once

#include <functional>
#include <string>
#include <utility>
#include <vector>

namespace mpc {

class MpcEditHistory final {
public:
    struct Command final {
        std::string label;
        std::function<bool()> undo;
        std::function<bool()> redo;
    };

    bool record(Command command) {
        if (command.label.empty()
                || !command.undo
                || !command.redo) {
            return false;
        }
        undoStack_.push_back(std::move(command));
        redoStack_.clear();
        return true;
    }

    bool canUndo() const noexcept {
        return !undoStack_.empty();
    }

    bool canRedo() const noexcept {
        return !redoStack_.empty();
    }

    bool undo() {
        if (undoStack_.empty()) {
            return false;
        }

        auto command = std::move(undoStack_.back());
        undoStack_.pop_back();

        if (!command.undo()) {
            undoStack_.push_back(std::move(command));
            return false;
        }

        redoStack_.push_back(std::move(command));
        return true;
    }

    bool redo() {
        if (redoStack_.empty()) {
            return false;
        }

        auto command = std::move(redoStack_.back());
        redoStack_.pop_back();

        if (!command.redo()) {
            redoStack_.push_back(std::move(command));
            return false;
        }

        undoStack_.push_back(std::move(command));
        return true;
    }

    const std::string& nextUndoLabel() const noexcept {
        static const std::string empty;
        return undoStack_.empty() ? empty : undoStack_.back().label;
    }

    const std::string& nextRedoLabel() const noexcept {
        static const std::string empty;
        return redoStack_.empty() ? empty : redoStack_.back().label;
    }

    void clear() noexcept {
        undoStack_.clear();
        redoStack_.clear();
    }

private:
    std::vector<Command> undoStack_;
    std::vector<Command> redoStack_;
};

} // namespace mpc
