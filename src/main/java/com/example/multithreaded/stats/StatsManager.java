package com.example.multithreaded.stats;

import com.example.multithreaded.io.LogWriter;

import java.text.MessageFormat;

/**
 * Aggregates the stats from LogWriter into a string to report
 */
public class StatsManager {

    private final static String REPORT_MSG = "Received {0} new unique numbers, {1} new duplicates. Unique total: {2}, Duplicate total: {3}";

    private final LogWriter writer;
    private long previousCount = 0;
    private long previousDupes = 0;

    public StatsManager(LogWriter writer) {
        this.writer = writer;
    }

    /**
     * Snapshots current values then reports the deltas, totals, etc. as called
     * @return the formatted string to report
     */
    public String reportStats() {
        long curDupes = writer.getDupeCount();
        long curCount = writer.getCount();

        long dupeDelta = curDupes - previousDupes;
        long uniqueDelta = curCount - previousCount;

        previousDupes = curDupes;
        previousCount = curCount;

        return MessageFormat.format(REPORT_MSG, uniqueDelta, dupeDelta, curCount, curDupes);
    }


}
