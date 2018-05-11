package app;
import cgu.Course;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import notify.Result;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * This class is rearded to the final step to enroll the class. This class creates a real connection to
 * CGU's server and reads the response contents.
 */
public class Request {

    private static final String DP1 = "__EVENTTARGET=&__EVENTARGUMENT=&__VIEWSTATE=";
    private static final String DP2 = "&__VIEWSTATEGENERATOR=C2962417&_ctl1%3AtermsList=45&_ctl1" +
            "%3AdepartmentsList=-1&_ctl1%3AcallID=&_ctl1%3AcourseID=";
    private static final String DP3 = "&_ctl1%3AInstructorName=&_ctl1%3AcourseName=&_ctl1%3AweekDay=-1" +
            "" + "" + "&_ctl1%3AbeginSection=-1&_ctl1%3AendSection=-1&_ctl1%3AclassID=-1&_ctl1" +
            "%3AfieldsList" + "=-1" + "&_ctl2%3AmyGrid%3A_ctl2%3AaddTaking=%E5%8A%A0%E9%81%B8%E8%AA%B2" +
            "%E7%A8%8B%0AADD";
    private final String cookies;
    private final Course course;
    private final String param;

    /**
     * Create a <tt>Request</tt> object. This constuctor does not create any connection. A {@link
     * CguWebClient} object and a {@link Course} object is needed to get proper request contents.
     * @param client a client that has logged in the student system.
     * @param course a course who wants to enroll.
     */
    public Request(CguWebClient client, Course course) throws IOException {
        try {
            this.course = course;
            StringBuilder sb = new StringBuilder(500);
            boolean first = true;
            for (Cookie c : client.client.getCookies(new URL("https://www.is.cgu.edu.tw/"))) {
                if (first) first = false;
                else sb.append("; ");
                sb.append(c.getName()).append("=").append(c.getValue());
            }
            cookies = sb.toString();
            HtmlPage page = client.client.getPage(course.url);
            HtmlInput viewState = page.getElementByName("__VIEWSTATE");
            this.param = DP1 + URLEncoder.encode(viewState.getValueAttribute(), "UTF-8") + DP2 +
                    course.id + DP3;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Get the final result of the submittion.
     * @param in An inputstream of a connection that has connected to CGU's server.
     * @return the final result of the submittion
     */
    public static Result getResult(InputStream in) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, "utf-8"))) {
            String readed;
            while ((readed = br.readLine()) != null) {
                //System.out.println(readed);
                if (readed.contains("span id=\"_ctl2_result\"")) {
                    if (readed.contains("人數上限")) return Result.FULL;
                    if (readed.contains("目前未開放")) return Result.TIME_INCORRECT;
                    if (readed.contains("衝堂") || readed.contains("不可重複選修科目代號")) return Result.CONFLICT;
                    if (readed.contains("你已經")) return Result.SUCCESS;
                    if (readed.contains("不可重複修讀已修畢之科目")) return Result.REPEAT;
                    if (readed.contains("學生所屬年級必須等於或高於課程開設年級") || readed.contains("不開放大學部學生選修"))
                        return Result.GRADE_TOO_LOW;
                }
            }
            return Result.FAIL;
        }
    }

    /**
     * This method creates a real connection to CGU's server and sends the request headers, and then
     * returns the inputstream of the connection socket. Call {@link #getResult(InputStream)} to get the
     * final result of the submittion.
     * @return the inputstream of the connection socket
     * @see #getResult(InputStream)
     */
    public InputStream submit() throws IOException {
        URLConnection conn = course.url.openConnection();
        conn.setRequestProperty("Cookie", cookies);
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9," +
                "image/webp,ima" + "ge/apng,*/*;q=0.8");
        conn.setRequestProperty("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KH" + "TML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        try (PrintWriter out = new PrintWriter(conn.getOutputStream())) {
            out.print(param);
            out.flush();
        }
        return conn.getInputStream();
    }

}
