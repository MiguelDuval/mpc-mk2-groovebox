package com.miguelduval.mpcmk2groovebox;

/** Keeps Step Edit focus aligned with the visible 16-step hardware/page bank. */
final class MpcSequenceStepPagePolicy {
    private MpcSequenceStepPagePolicy() {}

    static int selectedStepForPageChange(
            int selectedStep,
            int previousPage,
            int nextPage,
            int totalSteps,
            int pageSize) {
        if (previousPage == nextPage) return selectedStep;

        final int safePageSize = Math.max(1, pageSize);
        final int safeTotalSteps = Math.max(1, totalSteps);
        final int relativeColumn = selectedStep >= 0
                        && selectedStep / safePageSize == previousPage
                ? selectedStep % safePageSize
                : 0;
        final long nextPageFirstStep = (long) Math.max(0, nextPage) * safePageSize;
        return (int) Math.max(
                0L,
                Math.min(
                        (long) safeTotalSteps - 1L,
                        nextPageFirstStep + relativeColumn));
    }
}
