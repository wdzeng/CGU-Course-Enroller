package app;

import swing.CounterLabel;

import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is used to count the times a request has been sent. It is thread-safe.
 */
public class Counter {

    /**
     * A <tt>JLabel</tt> used to show the text of {@link #nFail}.
     * @see CounterLabel
     */
    private final CounterLabel lbFail;
    /**
     * A <tt>JLabel</tt> used to show the text of {@link #nFull}.
     * @see CounterLabel
     */
    private final CounterLabel lbFull;
    /**
     * Indicates the times of failure.
     */
    private final AtomicLong nFail = new AtomicLong(0);
    /**
     * Indicates the times of "This course is full of students".
     */
    private final AtomicLong nFull = new AtomicLong(0);

    /**
     * Create a Counter. The initial count of <tt>nFail</tt> and <tt>nFull</tt> are both 0.
     * @param lbFull a <tt>JLabel</tt> used to show the text of <tt>nFull</tt>
     * @param lbFail a <tt>JLabel</tt> used to show the text of <tt>nFail</tt>
     */
    public Counter(CounterLabel lbFull, CounterLabel lbFail) {
        this.lbFull = lbFull;
        this.lbFail = lbFail;
    }

    /**
     * Set the times of fail and full to 0.
     */
    public void clear() {
        nFail.set(0);
        nFull.set(0);
        refresh();
    }

    /**
     * Times of fail +1.
     */
    public void fail() {
        nFail.addAndGet(1);
    }

    /**
     * Times of full +1.
     */
    public void full() {
        nFull.addAndGet(1);
    }

    /**
     * show the latest counts.
     */
    public void refresh() {
        lbFull.setText(nFull.get());
        lbFail.setText(nFail.get());
    }
}
