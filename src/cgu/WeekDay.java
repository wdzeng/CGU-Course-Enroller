package cgu;

/**
 * This class represents the week day of a class.
 */
public enum WeekDay {

    /**
     * Monday
     */
    MON, /**
     * Tueday
     */
    TUE, /**
     * Wednesday
     */
    WED, /**
     * Thursday
     */
    THU, /**
     * Friday
     */
    FRI, /**
     * Saturday
     */
    SAT, /**
     * Sunday
     */
    SUN;

    @Override
    public String toString() {
        switch (this) {
        case MON:
            return "週一";
        case TUE:
            return "週二";
        case WED:
            return "週三";
        case THU:
            return "週四";
        case FRI:
            return "週五";
        case SAT:
            return "週六";
        case SUN:
            return "週日";
        default:
            throw new Error(); //Never happens.
        }
    }
}
