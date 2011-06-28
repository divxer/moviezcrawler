package controllers;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.List;

/**
 * User: divxer
 * Date: 11-2-2
 * Time: 上午9:03
 */
public class GetEd2kLinkFromMldonkey {
    public static String getLink(String moviezName) {
        String linkString = null;

        WebClient webClient = new WebClient(BrowserVersion.FIREFOX_3);

		try {
			DefaultCredentialsProvider dcp = new DefaultCredentialsProvider();
			dcp.addCredentials("admin", "qinhu6383");
			webClient.setCredentialsProvider(dcp);
			HtmlPage mainPage = (HtmlPage) webClient.getPage("http://javamad.3322.org:4080");

			final HtmlPage commandsPage = (HtmlPage) mainPage.getFrameByName("commands").getEnclosedPage();

			//http://htmlunit.sourceforge.net/faq.html#AJAXDoesNotWork
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());


            HtmlTableDataCell searchBtn = (HtmlTableDataCell)((commandsPage).getFirstByXPath("/HTML/BODY/FORM/TABLE/TBODY/TR/TD[2]/TABLE/TBODY/TR/TD"));
            if (searchBtn != null) {
                System.out.println("click Search");
                HtmlPage fstatusPage = searchBtn.click();

                HtmlTableDataCell extendSearchBtn = (HtmlTableDataCell)((fstatusPage).getFirstByXPath("/HTML/BODY/DIV/TABLE/TBODY/TR/TD[1]"));

                if (extendSearchBtn != null) {
                	System.out.println("click Extend Search");
                	HtmlPage outputPage = extendSearchBtn.click();

                	final HtmlTextInput keywordsField = outputPage.getFirstByXPath("/HTML/BODY/FORM/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD[2]/INPUT");
                	final HtmlTextInput minSizeField = outputPage.getFirstByXPath("/HTML/BODY/FORM/TABLE/TBODY/TR[2]/TD/TABLE/TBODY/TR[2]/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD[2]/INPUT");
                	final HtmlSubmitInput submitBtn = outputPage.getFirstByXPath("/HTML/BODY/FORM/INPUT[2]");

                	keywordsField.setValueAttribute(moviezName);
                	minSizeField.setValueAttribute("600");

                	System.out.println("click Search submit");

                	HtmlPage searchSubmitPage = submitBtn.click();
                	System.out.println("submit success? " + searchSubmitPage.asText().indexOf("Sending query !!!"));

                    if (searchSubmitPage.asText().indexOf("Sending query !!!") != -1) {
                        Thread.sleep(3000);
                        HtmlPage searchResultsPage = webClient.getPage("http://javamad.3322.org:4080/submit?q=vr");

                        final List<HtmlAnchor> ed2kLinkList = (List<HtmlAnchor>) searchResultsPage.getByXPath("/HTML/BODY/DIV[2]/TABLE/TBODY/TR/TD[1]/A");
                        for (HtmlAnchor ed2kLink : ed2kLinkList) {
                            if (linkString != null) {
                                linkString = linkString + ed2kLink.getAttribute("href");
                            } else {
                                linkString = ed2kLink.getAttribute("href");
                            }
                        }
                        System.out.println(linkString);
                        return linkString;
                    }
                }
            }

		} catch (Exception e) {
			e.printStackTrace();
		}
        return null;
    }
}
