package cgu;
import java.net.URL;

/**
 * This class represents a class, especially to general coarse. All of the field in this class is static
 * and cannot be modified once it is assigned.
 */
public class Course {

    /**
     * The day this class is.
     */
    public final WeekDay day;
    /**
     * The ID of this class.
     */
    public final String id;
    /**
     * The name of this class.
     */
    public final String name;
    /**
     * The teacher who gives lectures to this class.
     */
    public final String teacher;
    /**
     * A url of the web page to enroll this class.
     */
    public final URL url;

    public Course(String name, URL url, String id, String teacher, WeekDay day) {
        this.name = name;
        this.url = url;
        this.id = id;
        this.teacher = teacher;
        this.day = day;
    }

    @Override
    public String toString() {
        if (day == null) return "【" + teacher + "】" + name;
        return "【" + day.toString() + "／" + teacher + "】" + name;
    }

}
