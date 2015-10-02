package com.tonycase.simplechime;

/**
 * Start and end time for the chime time range
 */
public class TimeRange {

    private final int start;
    private final int end;

    TimeRange(int start, int end) {
        if (start < 0 || start > 23 || end < 0 || end > 23)
            throw new IllegalArgumentException("start and end must in the hour range of 0-23");

        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    /**
     * Inverse mode means the user has set their start time to later than their end time.
     * @return
     */
    public boolean isInverseMode() {
        return start > end;
    }

    public boolean isAllDay() {
        return start == 0 && end == 23;
    }

    @Override
    public String toString() {
        return "TimeRange: start="+start+",end="+end+"]";
    }
}
