package app;
import cgu.Course;
import cgu.WeekDay;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import notify.LoginFailException;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * <p>This class represents a client used to deal with the website of CGU, such as logging in the student
 * system or enrolling a class. Use this class to enroll a class violently.</p> <p>Use the same instance
 * when logging and enrolling a class. Otherwise, different instances represents different client, which
 * means that it is unable to enroll any classes.</p> <p>Remember to close the client if it is not used.
 * Call {@link #close()} method.</p>
 * @author Parabola
 */
public class CguWebClient implements Closeable {

    private static final URL SEARCH_CLASS;
    private static final URL STUDENT_SYSTEM;

    static {
        try {
            STUDENT_SYSTEM = new URL("https://www.is.cgu.edu.tw/portal/DesktopDefault.aspx");
            SEARCH_CLASS = new URL("http://www.is.cgu.edu.tw/portal/DesktopDefault" + "" + "" +
                    ".aspx?tabindex=1&tabid=61");
        } catch (MalformedURLException neverHappen) { //Never happens
            throw new InternalError(neverHappen);
        }
    }

    /**
     * The real client that connects to CGU's website. It is only package-visible and final.
     */
    final WebClient client;

    /**
     * To get a new client with default timeout. The timeout will be 4000 ms.
     */
    public CguWebClient() {
        this(4000);
    }

    /**
     * To get a new client with given timeout.
     */
    public CguWebClient(int timeout) {
        super();
        client = new WebClient(BrowserVersion.INTERNET_EXPLORER);
        client.getOptions().setJavaScriptEnabled(true);
        client.getOptions().setRedirectEnabled(true);
        client.getOptions().setThrowExceptionOnScriptError(false);
        client.getOptions().setCssEnabled(false);
        client.getOptions().setTimeout(timeout);
    }

    /**
     * Close the client.
     */
    @Override
    public void close() {
        client.close();
    }

    /**
     * Get the url address of a given course ID.
     * @param courseID the ID of a class
     * @return the url address of a given course ID, or <tt>null</tt> if that class does not exist.
     */
    public Course getCourse(final String courseID) throws IOException {
        try {
            HtmlPage searchClassPage = client.getPage(SEARCH_CLASS);
            ((HtmlTextInput) searchClassPage.getElementByName("_ctl1:courseID")).setValueAttribute
                    (courseID);
            final HtmlPage searchResult = searchClassPage.getElementByName("_ctl1:newSearch").click();
            final String html = searchResult.getWebResponse().getContentAsString();
            if (html.contains("<td nowrap=\"nowrap\" align=\"Center\" valign=\"Middle\">\r\n" +
                    courseID + "<BR>")) {
                final HtmlTableRow row = ((HtmlTable) searchResult.getElementById("_ctl2_myGrid"))
                        .getBodies().get(0).getRows().get(1);
                final String dayCell = row.getCell(8).asText();
                final WeekDay day;
                if (dayCell.contains("Mon")) day = WeekDay.MON;
                else if (dayCell.contains("Tue")) day = WeekDay.TUE;
                else if (dayCell.contains("Wed")) day = WeekDay.WED;
                else if (dayCell.contains("Thu")) day = WeekDay.THU;
                else if (dayCell.contains("Fri")) day = WeekDay.FRI;
                else if (dayCell.contains("Say")) day = WeekDay.SAT;
                else if (dayCell.contains("Sun")) day = WeekDay.SUN;
                else day = null;
                final String clzName = row.getCell(5).asText().split("\r\n")[0];
                final String teacher = row.getCell(6).asText();
                return new Course(clzName, searchResult.getUrl(), courseID, teacher, day);
            }
            return null;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Get the name of the student who logs in in current.
     * @return the student's name, or <tt>null</tt> if no one is logging in.
     */
    public String getStudentName() throws IOException {
        StringBuilder html = new StringBuilder(client.getPage(STUDENT_SYSTEM).getWebResponse()
                .getContentAsString());
        int beginIndex = html.indexOf("Hello");
        if (beginIndex == -1) {
            return null;
        }
        int endIndex = html.lastIndexOf("  <span class=Accent>");
        return html.substring(beginIndex + 7, endIndex);
    }

    /**
     * Check if a class exists.
     * @param classID the ID of a class
     * @return <tt>true</tt> if a class exists
     */
    public boolean hasCourse(String classID) throws IOException {
        return getCourse(classID) != null;
    }

    /**
     * Call this method to log in student system.
     * @param studentId a student's ID
     * @param password  a student's password to access her or his user system
     * @return the student's name, or <tt>null</tt> if password is wrong
     * @throws LoginFailException if something unknown happens like user's IP address is banned by
     *                            school's internet administator because of visiting a site too
     *                            frequently
     */
    public String login(String studentId, String password) throws IOException, LoginFailException {
        HtmlPage loginPage = null;
        try {
            // Log in process.
            loginPage = ((HtmlPage) client.getPage(STUDENT_SYSTEM)).getAnchorByHref("/portal/Login" +
                    ".aspx").click();
            ((HtmlTextInput) loginPage.getElementByName("Ecom_User_ID")).setValueAttribute(studentId);
            ((HtmlPasswordInput) loginPage.getElementByName("Ecom_Password")).setValueAttribute
                    (password);
            HtmlPage relocatePage = loginPage.getElementsByTagName("button").get(0).click();
            client.waitForBackgroundJavaScript(30 * 1000);
            StringBuilder html = new StringBuilder(relocatePage.getWebResponse().getContentAsString());

            if (!relocatePage.getUrl().equals(STUDENT_SYSTEM)) {
                relocatePage = client.getPage(STUDENT_SYSTEM);
            }
            // Check log in status.
            int beginIndex = html.indexOf("Hello");
            // Log in success.
            if (beginIndex != -1) {
                int endIndex = html.lastIndexOf("  <span class=Accent>");
                return html.substring(beginIndex + 7, endIndex);
            }
            // Wrong password.
            if (html.indexOf("Login failed, please try again.") != -1) return null;
            // Unknown status.
            throw new LoginFailException(relocatePage.asText());
        } catch (IOException | LoginFailException e) {
            throw e;
        } catch (Exception e) {
            //Unknown status.
            if (loginPage != null) {
                System.err.println("Exception occurs when visiting url: " + loginPage.getUrl());
                System.err.println(loginPage.getWebResponse().getContentAsString());
            }
            else {
                System.err.println("Arg loginPage is null.");
            }
            throw new LoginFailException(e);
        }
    }

    /**
     * Check if this client has logged in the student system.
     * @return <tt>true</tt> if the client has logged in the student system.
     */
    public boolean login() throws IOException {
        return getStudentName() != null;
    }

}

