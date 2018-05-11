package swing;
import javax.swing.*;
import java.awt.*;

public class Area extends JTextArea {
    public Area() {
        super();
        setLineWrap(true);
        setWrapStyleWord(true);
        setEditable(false);
        setFont(new Font(getFont().getFontName(), Font.PLAIN, 16));
    }

    @Override
    public synchronized void append(String mess) {
        super.append(mess);
        super.append("\n");
        setCaretPosition(getText().length());
    }

    public synchronized void append() {
        super.append("\n");
        setCaretPosition(getText().length());
    }
}
