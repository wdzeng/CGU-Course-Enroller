package swing;
import javax.swing.*;
import java.awt.*;

public class CounterLabel extends JLabel {

    public final String title;

    public CounterLabel(String title) {
        this.title = title;
        setText(0);
        setHorizontalAlignment(RIGHT);
        setPreferredSize(new Dimension(150, getPreferredSize().height));
    }

    public void setText(long n) {
        super.setText(title + " " + n + " 次");
    }
}
