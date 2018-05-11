package swing;
import javax.swing.*;
import java.awt.*;

public class Padding extends JPanel {

    public Padding(int pad, JComponent comp) {
       this(pad, pad, pad, pad, comp);
    }

    public Padding(int top, int left, int bottom, int right, JComponent comp) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        add(comp, BorderLayout.CENTER);
    }

}
