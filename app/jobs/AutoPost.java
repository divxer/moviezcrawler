package jobs;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import models.Comment;
import models.Post;
import org.apache.http.client.params.CookiePolicy;
import play.Logger;
import play.jobs.Job;
import play.jobs.On;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.List;

/**
 * 自动发表帖子
 * User: divxer
 * Date: 10-12-24
 * Time: 下午11:31
 */
/** Fire at every 30 minutes **/
@On("30 */30 * * * ?")
public class AutoPost extends Job {
    public void doJob() {
        Logger.info("Auto post comments begin");

        System.setProperty("apache.commons.httpclient.cookiespec", CookiePolicy.BROWSER_COMPATIBILITY);

        final WebClient webClient = new WebClient(BrowserVersion.FIREFOX_3_6);
		//设置代理
//		final WebClient webClient = new WebClient(BrowserVersion.FIREFOX_3_6, "219.129.216.23", 22);
//		final DefaultCredentialsProvider credentialsProvider = (DefaultCredentialsProvider) webClient.getCredentialsProvider();
//	    credentialsProvider.addCredentials("lizong", "lizonghao");

        // Get the first page
        HtmlPage page1 = null;
        try {
            page1 = webClient.getPage("http://wadianying.com/");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get the form that we are dealing with and within that form,
        // find the submit button and the field that we want to change.
        //get div which has a 'name' attribute of 'John'
        assert page1 != null;
        List<HtmlDivision> divList = (List<HtmlDivision>) page1.getByXPath("//div[@id='instance-loginwidget-2']");
        if (divList != null && divList.size() > 0) return;
        final HtmlDivision div = divList.get(0);
        final HtmlForm form = div.getFirstByXPath("//form[@action='http://wadianying.com/wp-login.php']");

        final HtmlTextInput userField = form.getInputByName("log");
        final HtmlPasswordInput pwdField = form.getInputByName("pwd");
        final HtmlSubmitInput submitBtn = (HtmlSubmitInput) form.getElementsByAttribute("input","class", "button").get(0);

        // Change the value of the text field
        userField.setValueAttribute("divxer");
        pwdField.setValueAttribute("waqwerdianying");

        // Now submit the form by clicking the button and get back the second page.
        try {
            final HtmlPage page2 = submitBtn.click();
            final String pageAsText = page2.asText();
            if (pageAsText.contains("控制板")) {
                Logger.info("Login successfully. Begin to post comments.");
            	postComment(webClient);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Logger.info("Post comments finished.");

        webClient.closeAllWindows();
    }

    public void postComment(WebClient webClient) throws IOException, MalformedURLException {
//        final WebClient webClient = new WebClient(BrowserVersion.FIREFOX_3_6);
        //设置代理
//		final WebClient webClient = new WebClient(BrowserVersion.FIREFOX_3_6, "219.129.216.23", 22);
//		final DefaultCredentialsProvider credentialsProvider = (DefaultCredentialsProvider) webClient.getCredentialsProvider();
//	    credentialsProvider.addCredentials("lizong", "lizonghao");

//        System.setProperty("apache.commons.httpclient.cookiespec", CookiePolicy.BROWSER_COMPATIBILITY);

        webClient.setJavaScriptEnabled(true);
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());
		webClient.waitForBackgroundJavaScript(10000);
//		webClient.waitForBackgroundJavaScriptStartingBefore(10000);
		webClient.setCssEnabled(false);
		webClient.setThrowExceptionOnScriptError(false);
		webClient.setThrowExceptionOnFailingStatusCode(false);
		webClient.setRedirectEnabled(true);
		webClient.setPrintContentOnFailingStatusCode(false);
//		webClient.setJavaScriptTimeout(1000000);

        List<Post> movieList = Post.find("order by postedAt desc").from(0).fetch(10);

        for (Post p : movieList) {
            if (p.comments.size() > 0) {
                String movieTitle = p.title;
                String encodedUrl = URLEncoder.encode(movieTitle, "UTF-8");
                final HtmlPage page3 = webClient.getPage("http://wadianying.com/?s=" + encodedUrl);
//            	final HtmlDivision searchResultDiv = (HtmlDivision) page3.getByXPath("//*[@id='primary-content']").get(0);
//            	final HtmlDivision blocksDiv = (HtmlDivision) searchResultDiv.getByXPath("//DIV[@class='blocks']").get(0);
                final List<HtmlAnchor> titleList = (List<HtmlAnchor>) page3.getByXPath("//H2[@class='title']/A");
                for (HtmlAnchor title : titleList) {
                    int postCount = 0;
                    String movieUrl = title.getAttribute("href");
                    final HtmlPage page4 = webClient.getPage(movieUrl);
                    if (page4.asText().contains("没有评论")) {
//                    没有评论，则插入豆瓣评论
                        final HtmlForm commentForm = page4.getFirstByXPath("//*[@id='commentform']");
                        final HtmlTextArea commentArea = page4.getFirstByXPath("//*[@id='comment']");
                        final HtmlSubmitInput commentSubmitBtn = (HtmlSubmitInput) page4.getHtmlElementById("submit");
                        for (Comment comment : p.comments) {
                            commentArea.setText(comment.content);
                            final HtmlPage page5 = commentSubmitBtn.click();
                            if (page5.asText().contains("Your comment was added.")) {
                                postCount++;
                                Logger.info("Posted comments for movie: " + page5.getTitleText());
                            }
                        }
                        Logger.info("Posted " + postCount + " comments for movie: " + movieTitle);
                    }
                }
            }
        }
    }
}
