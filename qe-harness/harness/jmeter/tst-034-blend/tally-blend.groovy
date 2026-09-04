// TST-034 per-journey tally (Wave 17). Shared by all four Journey Load Thread
// Groups' own JSR223PostProcessor via the same env-var-path convention
// assert-blend.groovy already established (filename=${__groovy(...ASSERT_SCRIPT_PATH...)}).
// Extracted here, rather than kept as four byte-identical inline copies, after
// a real drift risk surfaced during code review: an earlier version of this
// plan needed the same min-window-tracking bug fixed identically in all four
// inline copies, and a future fix could easily miss one of them. One file,
// referenced four times, cannot drift from itself.
//
// Tags every sample by vars.get("tst034_journey") (set by this journey's
// own JSR223PreProcessor above) into props -- the cross-thread aggregation
// pattern tst-031-ratelimit's assert-ratelimit.groovy established, extended
// here to four concurrent journeys instead of one metric. Discards every
// sample before tst034_warmup_ms has elapsed, for every tally below -- see
// README.md's I5.
String name = vars.get("tst034_journey")
long runStartMs = Long.parseLong(props.getProperty("tst034_run_start_ms"))
long warmupMs = Long.parseLong(props.getProperty("tst034_warmup_ms"))
long subWindowMillis = Long.parseLong(props.getProperty("tst034_sub_window_millis"))
final int RESERVOIR_SIZE = 300

long elapsedMs = Math.max(0L, prev.getTimeStamp() - runStartMs)
String code = prev.getResponseCode()
boolean ok = code != null && code.startsWith("2")

synchronized (props) {
    if (elapsedMs < warmupMs) {
        // Still warming up: I5 requires measurement not begin here --
        // every tally below is skipped entirely for this sample, not just
        // its throughput contribution.
        return
    }

    long measuredElapsedMs = elapsedMs - warmupMs
    int windowIdx = (int) Math.floorDiv(measuredElapsedMs, subWindowMillis)

    int maxWindowSeen = Integer.parseInt(props.getProperty("tst034_max_window_idx_seen", "-1"))
    if (windowIdx > maxWindowSeen) {
        props.put("tst034_max_window_idx_seen", String.valueOf(windowIdx))
    }

    long total = Long.parseLong(props.getProperty("tst034_total_samples", "0"))
    props.put("tst034_total_samples", String.valueOf(total + 1))

    long count = Long.parseLong(props.getProperty("tst034_" + name + "_count", "0"))
    props.put("tst034_" + name + "_count", String.valueOf(count + 1))

    if (!ok) {
        long errors = Long.parseLong(props.getProperty("tst034_" + name + "_errors", "0"))
        props.put("tst034_" + name + "_errors", String.valueOf(errors + 1))
    }

    // O(1) props keys per journey for the per-sub-window minimum (I3),
    // rather than one key per window index: with a 100ms sub-window over
    // an 18s measured smoke run that is 180+ never-reused keys per
    // journey, and empirically (confirmed by an early version of this
    // plan) enough churn on the one shared, synchronized "props" Hashtable
    // to trigger internal resizes that stall EVERY thread sharing it for
    // 100ms+ at a time -- indistinguishable, without this fix, from a
    // real defect. Tracks only the CURRENT window's own running count and
    // a running MINIMUM across every window already closed; the last,
    // still-open window at the moment the run ends is therefore never
    // folded into the minimum, which is also exactly the "exclude the
    // trailing partial window" property this plan needs regardless.
    int maxJourneyWindow = Integer.parseInt(props.getProperty("tst034_" + name + "_max_window_idx", "0"))
    if (windowIdx > maxJourneyWindow) {
        props.put("tst034_" + name + "_max_window_idx", String.valueOf(windowIdx))
    }
    int curWindowIdx = Integer.parseInt(props.getProperty("tst034_" + name + "_cur_window_idx", "-1"))
    long curWindowCount = Long.parseLong(props.getProperty("tst034_" + name + "_cur_window_count", "0"))
    if (windowIdx > curWindowIdx) {
        if (curWindowIdx >= 0) {
            long minSoFar = Long.parseLong(props.getProperty("tst034_" + name + "_min_window_count", String.valueOf(Long.MAX_VALUE)))
            long newMin = Math.min(minSoFar, curWindowCount)
            // If windowIdx jumped by more than 1, one or more whole
            // sub-windows between the previous one and this one got ZERO
            // samples -- a gap this incremental design would otherwise
            // silently skip over (it only ever finalizes the window it was
            // last tracking, never the ones in between). Missing this was
            // a real bug in an earlier version of this design: it made I3
            // structurally unable to ever observe starvation, since a
            // fully-empty window never got recorded as anything at all.
            if (windowIdx - curWindowIdx > 1) {
                newMin = 0L
            }
            props.put("tst034_" + name + "_min_window_count", String.valueOf(newMin))
        }
        props.put("tst034_" + name + "_cur_window_idx", String.valueOf(windowIdx))
        props.put("tst034_" + name + "_cur_window_count", "1")
    } else {
        // Same window, or (rare, with multiple threads in one journey's
        // Thread Group) a slightly late arrival for an already-closed
        // window -- folded into the current window's count either way,
        // since an exact per-window count is not the point; a
        // representative minimum is.
        props.put("tst034_" + name + "_cur_window_count", String.valueOf(curWindowCount + 1))
    }

    // Bounded reservoir: the last RESERVOIR_SIZE latencies for this journey,
    // overwritten round-robin -- Finalize Blend Tallies (TearDown) sorts
    // this once, at the end, to derive p95, rather than this PostProcessor
    // re-sorting a growing list on every single sample.
    long latencyN = Long.parseLong(props.getProperty("tst034_" + name + "_latency_n", "0"))
    int reservoirSlot = (int) (latencyN % RESERVOIR_SIZE)
    props.put("tst034_" + name + "_latency_" + reservoirSlot, String.valueOf(prev.getTime()))
    props.put("tst034_" + name + "_latency_n", String.valueOf(latencyN + 1))
}
