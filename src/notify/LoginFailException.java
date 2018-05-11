package notify;

/**
 * This exception is thrown if login process fails for a unknown reason.
 * @author Parabola
 */
public class LoginFailException extends Exception {

    public LoginFailException() {super(); }

    public LoginFailException(String mess) {super(mess);}

    public LoginFailException(Throwable e) {super(e); }
}
