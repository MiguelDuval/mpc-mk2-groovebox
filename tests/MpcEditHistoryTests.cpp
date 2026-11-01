#include "MPC/MpcEditHistory.h"

#include <cassert>

int main() {
    mpc::MpcEditHistory history;
    int value = 0;

    assert(!history.canUndo());
    assert(!history.canRedo());

    assert(history.record({
            "Set 1",
            [&value]() { value = 0; return true; },
            [&value]() { value = 1; return true; }}));
    value = 1;
    assert(history.canUndo());
    assert(!history.canRedo());
    assert(history.nextUndoLabel() == "Set 1");

    assert(history.undo());
    assert(value == 0);
    assert(!history.canUndo());
    assert(history.canRedo());
    assert(history.nextRedoLabel() == "Set 1");

    assert(history.redo());
    assert(value == 1);
    assert(history.canUndo());
    assert(!history.canRedo());

    assert(history.record({
            "Set 2",
            [&value]() { value = 1; return true; },
            [&value]() { value = 2; return true; }}));
    value = 2;
    assert(history.undo());
    assert(value == 1);
    assert(history.redo());
    assert(value == 2);

    assert(history.record({
            "Set 3",
            [&value]() { value = 2; return true; },
            [&value]() { value = 3; return true; }}));
    value = 3;
    assert(history.undo());
    assert(history.canRedo());

    assert(history.record({
            "Set 4",
            [&value]() { value = 1; return true; },
            [&value]() { value = 4; return true; }}));
    value = 4;
    assert(!history.canRedo());
    assert(history.canUndo());

    return 0;
}
