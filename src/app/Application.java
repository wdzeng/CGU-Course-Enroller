package app;

import cgu.Course;
import notify.LoginFailException;
import notify.Result;
import swing.Area;
import swing.CounterLabel;
import swing.Padding;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * <p>Use this application to enroll the class you want to enroll.</p> <p>This class owns a member, a
 * unique {@link Task} object, which connects to CGU's server.
 * @author Parabola
 * @see Task
 * @see Request
 */
public class Application extends JFrame {

    private abstract class Button extends JButton {
        public Button(String text) {
            super(text);
            setPreferredSize(new Dimension(BTN_WIDTH, getPreferredSize().height));
            addActionListener(e -> click());
        }

        abstract void click();
    }

    private class Input extends JTextField {
        public Input() {
            setPreferredSize(new Dimension(IN_WIDTH, IN_HEIGHT));
        }
    }

    private class Label extends JLabel {

        public Label(String text) {
            super(text);
            setPreferredSize(new Dimension(LB_WIDTH, getPreferredSize().height));
        }

    }

    private class LoginButton extends Button {
        private boolean logged;

        public LoginButton(String text) {
            super(text);
        }

        @Override
        public void click() {
            if (logged) {
                logInMode(false);
                area.append("已登出。");
            }
            else {
                String studentId = inStuId.getText();
                @SuppressWarnings("deprecation") String password = inPswd.getText();
                if (studentId.length() != 0 && password.length() != 0) {
                    new Thread(() -> {
                        try {
                            inStuId.setEditable(false);
                            inPswd.setEditable(false);
                            setText("登入中");
                            setEnabled(false);
                            String username = client.login(studentId, password);
                            if (username == null) area.append("帳號或密碼錯誤。");
                            else {
                                area.append("已登入【" + username + "】");
                                logInMode(true);
                                return;
                            }
                        } catch (IOException | LoginFailException e) {
                            e.printStackTrace();
                            area.append("操作異常。");
                        }
                        logInMode(false);
                    }).start();
                }
            }
        }

        public void logInMode(boolean on) {
            if (on) {
                inStuId.setEditable(false);
                inPswd.setEditable(false);
                inClzId.setEditable(true);
                btnRun.setEnabled(true);
                logged = true;
                setText("登出");
            }
            else {
                client.close();
                client = new CguWebClient();
                logged = false;
                inStuId.setEditable(true);
                inPswd.setEditable(true);
                inClzId.setEditable(false);
                btnRun.setEnabled(false);
                setText("登入");
                System.gc();
            }
            setEnabled(true);
        }
    }

    private class PasswordInput extends JPasswordField {
        public PasswordInput() {
            setPreferredSize(new Dimension(IN_WIDTH, IN_HEIGHT));
        }
    }

    /**
     * This class is the inner class of {@link Application}. It is the class who does the task to send
     * requst to CGU's server. Actually, its outer class, <tt>Application</tt>, object deals with the GUI
     * and this class deals with the real jobs.
     */
    private class Task {

        /**
         * This object is used to refresh the {@link #counter}.
         */
        private class RefreshCounterTask extends TimerTask {
            @Override
            public void run() {
                counter.refresh();
            }
        }
        /**
         * The {@link Result} object from the procedure is put into this queue, and {@link #tMessage}
         * takes them out and cast the results to message and shows on GUI.
         */
        private final BlockingQueue<Result> queue = new LinkedBlockingDeque<>();
        /**
         * The <b>REALLY REAL</b> object that connects to CGU's server, sends request headers, reads the
         * response contents and cast them to {@link Result} obejcts.
         * @see Request
         */
        private Request submittion;
        /**
         * This thread shows the message on the GUI.
         * @see #area
         * @see CounterLabel
         * @see #counter
         */
        private Thread tMessage;
        /**
         * A very important flag. If this flag is set to <tt>false</tt>, the task will be shut down, or
         * ,the <tt>while</tt>loop will breaks. In this application, {@link Application#stop()
         * Application.stop()} is called to set it to <tt>false</tt>. It is <tt>volatile</tt> so that the
         * threads can ends ASAP if set to <tt>false</tt>.
         */
        private volatile boolean taskFlag;
        /**
         * This timer is used to refresh the {@link #counter}.
         */
        private Timer timer;
        /**
         * The <tt>Runnable</tt> target of {@link #tMessage}.
         */
        private final Runnable rMessage = () -> {
            try {
                Result result;
                while (taskFlag) {
                    result = queue.take();
                    if (!taskFlag) break;
                    if (result == Result.SUCCESS || result == Result.CONFLICT || result == Result
                            .GRADE_TOO_LOW || result == Result.REPEAT) {
                        taskFlag = false;
                        new Thread(Application.this::stop).start();
                        JOptionPane.showMessageDialog(null, result.toString(), "刷課機即將終止", JOptionPane
                                .INFORMATION_MESSAGE);
                    }
                    else if (result == Result.FULL || result == Result.TIME_INCORRECT) counter.full();
                    else counter.fail();
                }
            } catch (InterruptedException e) {
            }
        };
        public Task() {
        }

        /**
         * Once this method is called, it starts a new thread and connects to CGU's server and send
         * requests repeatly (by <tt>while</tt> loop). Call this method twice or three or more times if
         * higher efficincy is needed. To interrupt the thread, the only way is setting {@link #taskFlag}
         * to <tt>false</tt> (In this application, {@link Application#stop() Application.stop()} is
         * called to set it to <tt>false</tt>). After a thread is dead, resetting <tt>taskFlag</tt> to
         * <tt>true</tt> is useless. Call this method again.
         */
        public void execute() {
            new Thread(() -> {
                while (taskFlag) {
                    try {
                        queue.put(Request.getResult(submittion.submit()));
                    } catch (InterruptedException e) {
                    } catch (IOException e) {
                        try {
                            queue.put(Result.FAIL);
                        } catch (InterruptedException neverHappen) { //Never happens
                        }
                    }
                }
            }).start();
        }

        /**
         * Start the task. On the other hand, a <tt>stop</tt> method is not provided bt this class. Call
         * {@link Application#stop() Application.stop()} instead.
         * @param client the <tt>CguWebClient</tt> object which provides the students data
         * @param course the course to enroll
         */
        public void start(CguWebClient client, Course course) {
            try {
                //啟動刷頻
                submittion = new Request(client, course);
                //計數器歸零
                counter.clear();
                //初始化訊息監聽器
                tMessage = new Thread(rMessage);
                //初始化計數器設置
                timer = new Timer();
                //訊息監聽器啟動
                tMessage.start();
                //計數器啟動
                timer.scheduleAtFixedRate(new RefreshCounterTask(), 0, 60);
                //刷課機啟動
                taskFlag = true;
                int nCore = Runtime.getRuntime().availableProcessors();
                //依該電腦的CPU數量決定所開啟的執行緒數目
                for (int n = 0; n < nCore; n++) {
                    execute();
                }
                area.append("初始化完成。開始刷課。");
            } catch (IOException e) {
                e.printStackTrace();
                area.append("初始化失敗！");
            }
        }
    }

    private class TaskButton extends Button {

        public TaskButton(String text) {
            super(text);
        }

        @Override
        public void click() {
            if (task.taskFlag) stop();
            else start();
        }
    }

    private static final int AREA_HEIGHT = 150;
    private static final int BTN_WIDTH = 120;
    private static final int IN_HEIGHT = 30;
    private static final int IN_WIDTH = 360;
    private static final int LB_WIDTH = 60;
    private static final int PADDING = 6;
    private final Area area = new Area();
    private final JLabel author = new JLabel("作者：雙曲線");
    private final LoginButton btnLogin = new LoginButton("登入");
    private final TaskButton btnRun = new TaskButton("開始刷課");
    private final CounterLabel cnFail = new CounterLabel("錯誤次數");
    private final CounterLabel cnFull = new CounterLabel("嘗試次數");
    private final Counter counter = new Counter(cnFull, cnFail);
    private final Input inClzId = new Input();
    private final Input inClzName = new Input();
    private final PasswordInput inPswd = new PasswordInput();
    private final Input inStuId = new Input();
    private final Label lbClzId = new Label("開課序號");
    private final Label lbClzName = new Label("課堂名稱");
    private final Label lbPswd = new Label("密碼");
    private final Label lbUser = new Label("學號");
    private final Task task = new Task();
    private CguWebClient client = new CguWebClient();

    private Application() {
        super("長庚通識無限刷");
        initSwings();
        showWarningText();
    }

    /**
     * Start the application.
     * @param args arguments given by JVM.
     */
    public static void main(String[] args) {
        new Application();
    }

    private void initSwings() {
        inClzId.setEditable(false);
        inClzName.setEditable(false);
        btnRun.setEnabled(false);

        JPanel pnUser = new JPanel(new BorderLayout());
        pnUser.add(new Padding(PADDING, lbUser), BorderLayout.CENTER);
        pnUser.add(new Padding(PADDING, 0, PADDING, PADDING, inStuId), BorderLayout.EAST);
        JPanel pnPswd = new JPanel(new BorderLayout());
        pnPswd.add(new Padding(0, PADDING, PADDING, PADDING, lbPswd), BorderLayout.CENTER);
        pnPswd.add(new Padding(0, 0, PADDING, PADDING, inPswd), BorderLayout.EAST);
        JPanel pnUsPw = new JPanel(new BorderLayout());
        pnUsPw.add(pnUser, BorderLayout.CENTER);
        pnUsPw.add(pnPswd, BorderLayout.SOUTH);
        JPanel pnNorth = new JPanel(new BorderLayout());
        pnNorth.add(pnUsPw, BorderLayout.CENTER);
        pnNorth.add(new Padding(PADDING, 0, PADDING, PADDING, btnLogin), BorderLayout.EAST);

        JPanel pnClzId = new JPanel(new BorderLayout());
        pnClzId.add(new Padding(PADDING, lbClzId), BorderLayout.CENTER);
        pnClzId.add(new Padding(PADDING, 0, PADDING, PADDING, inClzId), BorderLayout.EAST);
        JPanel pnClzData = new JPanel(new BorderLayout());
        pnClzData.add(new Padding(0, PADDING, PADDING, PADDING, lbClzName), BorderLayout.CENTER);
        pnClzData.add(new Padding(0, 0, PADDING, PADDING, inClzName), BorderLayout.EAST);
        JPanel pnClz = new JPanel(new BorderLayout());
        pnClz.add(pnClzId, BorderLayout.CENTER);
        pnClz.add(pnClzData, BorderLayout.SOUTH);
        JPanel pnClzRun = new JPanel(new BorderLayout());
        pnClzRun.add(pnClz, BorderLayout.CENTER);
        pnClzRun.add(new Padding(PADDING, 0, PADDING, PADDING, btnRun), BorderLayout.EAST);

        JPanel pnCenter = new JPanel(new BorderLayout());
        pnCenter.add(pnClzRun, BorderLayout.NORTH);
        JScrollPane scrArea = new JScrollPane(area);
        scrArea.setPreferredSize(new Dimension(scrArea.getPreferredSize().width, AREA_HEIGHT));
        scrArea.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        pnCenter.add(new Padding(0, PADDING, PADDING, PADDING, scrArea), BorderLayout.CENTER);

        JPanel pnCounter = new JPanel(new BorderLayout());
        pnCounter.add(new Padding(0, 20, PADDING, 20, cnFull), BorderLayout.CENTER);
        pnCounter.add(new Padding(0, 20, PADDING, PADDING, cnFail), BorderLayout.EAST);
        JPanel pnSouth = new JPanel(new BorderLayout());
        pnSouth.add(new Padding(0, PADDING, PADDING, 20, author), BorderLayout.CENTER);
        pnSouth.add(pnCounter, BorderLayout.EAST);

        add(pnCenter, BorderLayout.CENTER);
        add(pnNorth, BorderLayout.NORTH);
        add(pnSouth, BorderLayout.SOUTH);
        pack();
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
    }

    private void showWarningText() {
        area.append("邪惡的刷課機已經準備好了。先登入帳號密碼，然後再輸入開課序號，就可以開始刷課了。" +
                "如果你是用長庚的爛宿網的話，要小心刷課的行為可能會讓你的宿網被學校系統封鎖，所以可以的話還" + "是用自己的網路比較好。");
        area.append();
        area.append("如果你是用長庚的爛宿網的話，要小心刷課的行為可能會讓你的宿網被學校系統封鎖，所以可以的" +
                "話還是用自己的網路比較好。不知道開課序號的話，到校務資訊系統去查。就算是同樣老師開的同樣的" +
                "課，星期一班跟星期二班的序號卻是不一樣的，要記得確認要選的課是在星期幾。");
        area.append();
        area.append("當刷課機得到衝堂、重複選修或選課成功的結果之後，刷課系統會自動停止並告知使用者。此外，視窗" +
                "下面有兩個指標嘗試次數和錯誤次數。「嘗試」是指當刷課處理器得到「人數已滿」或「尚未" +
                "開放選課」的結果。「錯誤」是指刷課機執行異常得到無法理解的結果，例如IP被封、連線逾" +
                "時等等。");
        area.append();
    }

    /**
     * Start the prosedure.
     * @return <tt>true</tt> if prosedure starts
     */
    private synchronized boolean start() {
        String courseID = inClzId.getText();
        if (courseID.length() == 0) return false;
        final Course course;
        try {
            course = client.getCourse(courseID);
        } catch (IOException e) {
            area.append("操作異常。");
            e.printStackTrace();
            return false;
        }
        if (course == null) {
            inClzName.setForeground(Color.RED);
            inClzName.setText("開課序號錯誤");
            return false;
        }

        // Start to enrolling process;
        btnLogin.setEnabled(false);
        inClzId.setEditable(false);
        inClzName.setForeground(null);
        inClzName.setText(course.toString());
        btnRun.setText("終止刷課");
        task.start(client, course);
        return true;
    }

    /**
     * Stop the prosedure.
     * @return always <tt>true</tt>
     */
    private synchronized boolean stop() {
        task.taskFlag = false;
        //終止計數器Timer
        task.timer.cancel();
        //清空監聽器訊息
        task.queue.clear();
        //swing恢復設置
        inClzId.setEditable(true);
        btnLogin.setEnabled(true);
        btnRun.setText("開始刷課");
        area.append("刷課機已停止。");
        //清空記憶體垃圾
        System.gc();
        return true;
    }

}

