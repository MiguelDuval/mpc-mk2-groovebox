#include "MPC/Sequencer/MpcPatternScheduler.h"

#include <array>
#include <cassert>

int main() {
    mpc::domain::Pattern pattern;
    pattern.lengthTicks = 1000;
    pattern.notes = {
            {50, 30, 36, 100, 0, 1},
            {100, 120, 37, 110, 127, 1},
            {800, 240, 38, 90, 127, 4}};

    std::array<mpc::sequencer::ScheduledMidiEvent, 4> output{};

    const auto first = mpc::sequencer::schedulePatternWindow(
            pattern,
            mpc::sequencer::TickWindow{0, 500, 0},
            1234u,
            output);

    assert(first.written == 1);
    assert(first.eligible == 1);
    assert(!first.truncated);
    assert(output[0].offsetTicks == 100);
    assert(output[0].patternTick == 100);
    assert(output[0].durationTicks == 120);
    assert(output[0].note == 37);
    assert(output[0].velocity == 110);
    assert(output[0].ratchetCount == 1);
    assert(output[0].sourceNoteIndex == 1);

    const auto wrapped = mpc::sequencer::schedulePatternWindow(
            pattern,
            mpc::sequencer::TickWindow{700, 200, 1},
            1234u,
            output);

    assert(wrapped.written == 2);
    assert(wrapped.eligible == 2);
    assert(!wrapped.truncated);

    assert(output[0].offsetTicks == 100);
    assert(output[0].patternTick == 800);
    assert(output[0].ratchetCount == 4);
    assert(output[0].sourceNoteIndex == 2);

    assert(output[1].offsetTicks == 400);
    assert(output[1].patternTick == 100);
    assert(output[1].ratchetCount == 1);
    assert(output[1].sourceNoteIndex == 1);

    mpc::domain::Pattern nudgePattern;
    nudgePattern.lengthTicks = 1000;
    nudgePattern.notes = {{100, 30, 40, 100, 127, 1, 40}};
    const auto nudged = mpc::sequencer::schedulePatternWindow(
            nudgePattern,
            mpc::sequencer::TickWindow{0, 200, 0},
            1234u,
            output);
    assert(nudged.written == 1);
    assert(output[0].offsetTicks == 140);
    assert(output[0].patternTick == 100);

    mpc::domain::Pattern nudgeWrapPattern;
    nudgeWrapPattern.lengthTicks = 1000;
    nudgeWrapPattern.notes = {{0, 30, 41, 100, 127, 1, -20}};
    const auto nudgedAcrossLoop = mpc::sequencer::schedulePatternWindow(
            nudgeWrapPattern,
            mpc::sequencer::TickWindow{900, 100, 1},
            1234u,
            output);
    assert(nudgedAcrossLoop.written == 1);
    assert(output[0].offsetTicks == 80);
    assert(output[0].patternTick == 0);

    const auto fullCycle = mpc::sequencer::schedulePatternWindow(
            pattern,
            mpc::sequencer::TickWindow{0, 0, 1},
            1234u,
            output);

    assert(fullCycle.written == 2);
    assert(fullCycle.eligible == 2);
    assert(output[0].offsetTicks == 100);
    assert(output[1].offsetTicks == 800);

    std::array<mpc::sequencer::ScheduledMidiEvent, 1> tinyOutput{};
    const auto truncated = mpc::sequencer::schedulePatternWindow(
            pattern,
            mpc::sequencer::TickWindow{0, 999, 0},
            1234u,
            tinyOutput);

    assert(truncated.written == 1);
    assert(truncated.eligible == 2);
    assert(truncated.truncated);

    const auto deterministicAgain =
            mpc::sequencer::schedulePatternWindow(
                    pattern,
                    mpc::sequencer::TickWindow{700, 200, 1},
                    1234u,
                    output);
    assert(deterministicAgain.written == wrapped.written);
    assert(output[0].offsetTicks == 100);
    assert(output[1].offsetTicks == 400);

    assert(mpc::sequencer::schedulePatternWindow(
                   pattern,
                   mpc::sequencer::TickWindow{-1, 10, 0},
                   1234u,
                   output)
                   .written == 0);

    mpc::domain::Pattern rangedPattern;
    rangedPattern.lengthTicks = 3840;
    rangedPattern.notes = {
            {0, 30, 36, 100, 127, 1},
            {960, 30, 37, 110, 127, 1},
            {1920, 30, 38, 120, 127, 1},
            {2880, 30, 39, 90, 127, 1}};

    const auto ranged = mpc::sequencer::schedulePatternWindowInRange(
            rangedPattern,
            mpc::sequencer::TickWindow{960, 960, 1},
            960,
            2880,
            1234u,
            output);
    assert(ranged.written == 2);
    assert(ranged.eligible == 2);
    assert(output[0].patternTick == 960);
    assert(output[0].offsetTicks == 0);
    assert(output[1].patternTick == 1920);
    assert(output[1].offsetTicks == 960);

    // Nudge is applied after loop membership is established. An event exactly
    // at loopEnd is excluded, while an in-range event may micro-shift across
    // the loop boundary and wrap safely.
    mpc::domain::Pattern rangeNudgePattern;
    rangeNudgePattern.lengthTicks = 3840;
    rangeNudgePattern.notes = {
            {960, 30, 42, 100, 127, 1, -120},
            {2880, 30, 43, 100, 127, 1, 0}};
    const auto rangeNudged = mpc::sequencer::schedulePatternWindowInRange(
            rangeNudgePattern,
            mpc::sequencer::TickWindow{2700, 2800, 0},
            960,
            2880,
            1234u,
            output);
    assert(rangeNudged.written == 1);
    assert(output[0].patternTick == 960);
    assert(output[0].offsetTicks == 60);

    pattern.lengthTicks = 0;
    assert(mpc::sequencer::schedulePatternWindow(
                   pattern,
                   mpc::sequencer::TickWindow{0, 0, 0},
                   1234u,
                   output)
                   .written == 0);

    return 0;
}
