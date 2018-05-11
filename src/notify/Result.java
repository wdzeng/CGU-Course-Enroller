package notify;

/**
 * This class represents the result status of the enrolling class action.
 * @author Parabola
 */
public enum Result {
    /**
     * Success.
     */
    SUCCESS, /**
     * Conflict with another class.
     */
    CONFLICT, /**
     * The count of students enrolling this class is full.
     */
    FULL, /**
     * Unable to enroll a class currently.
     */
    TIME_INCORRECT, /**
     * An exception occurs when downloading contents.
     */
    FAIL, /**
     * Someone has enrolled this class.
     */
    REPEAT,

    GRADE_TOO_LOW;

    @Override
    public String toString() {
        switch (this) {
        case SUCCESS:
            return "刷課成功！";
        case CONFLICT:
            return "課表衝堂！";
        case FULL:
            return "人數已滿。";
        case TIME_INCORRECT:
            return "目前非選課時間。";
        case FAIL:
            return "操作異常！";
        case REPEAT:
            return "不能重複修同一門課。";
        case GRADE_TOO_LOW:
            return "不能跨年級或跨部選修。";
        }
        throw new InternalError(); //Never happens;
    }
}
