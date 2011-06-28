package controllers;

import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;
import groovy.lang.Newify;
import models.Category;
import models.Genre;
import models.Post;
import org.apache.commons.lang.StringEscapeUtils;
import org.jdom.CDATA;
import play.Logger;
import play.mvc.Controller;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * User: divxer
 * Date: 10-12-23
 * Time: 上午9:51
 */
public class Rss extends Controller {
    private static final String MIME_TYPE = "application/xml; charset=UTF-8";

    // Rome中RSS的可选标准
    // rss_0.90, rss_0.91, rss_0.92, rss_0.93, rss_0.94, rss_1.0, rss_2.0, atom_0.3
    private static final String RSS_TYPE = "rss_2.0";
    private static final String WEBSITE_LINK = "http://wadianying.com";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    public static void newsFeed() throws FeedException {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
        DateFormat dateParser = new SimpleDateFormat(DATE_FORMAT);

        Date publishDate = new Date( System.currentTimeMillis() );

        SyndFeed feed = new SyndFeedImpl();
        feed.setPublishedDate(publishDate);
        feed.setFeedType(RSS_TYPE);
        feed.setLink(WEBSITE_LINK);
        feed.setTitle("0day电影资料库");
        feed.setDescription("最新电影介绍");

        List<Post> latestMovies = getLatestMovies();
        List<SyndEntry> entries = new ArrayList<SyndEntry>();
        for (Post item : latestMovies) {
            StringBuffer itemContent = new StringBuffer(item.content);
//            String itemContent = item.content.replace(System.getProperty("line.separator"), "<BR/>");
            if (item.ed2kLink != null && (!item.ed2kLink.equals(""))) {
                 itemContent.append("\n\n");
                itemContent.append("<!--loginview start-->\n");
                itemContent.append("[ed2k]\n");
                itemContent.append(item.ed2kLink).append("\n");
                itemContent.append("[/ed2k]\n");
                itemContent.append("<!--loginview end-->\n");
            }

            SyndEntry entry = new SyndEntryImpl(); // create a feed entry
            entry.setTitle(item.title);
            entry.setPublishedDate(item.postedAt);
            entry.setAuthor(item.author.fullname);

            SyndContent content = new SyndContentImpl(); // create the content of your entry
            content.setType( "text/html" );
            content.setValue(itemContent.toString());
            entry.setDescription(content);

            //分类
            List<SyndCategory> categories = new ArrayList<SyndCategory>();
            List<Category> categoryList = Category.all().fetch();
            for (Category c : categoryList) {
                if (item.title.toLowerCase().contains(c.name.toLowerCase())) {
                    SyndCategory category = new SyndCategoryImpl();
                    category.setName(c.name);
                    categories.add(category);
                 }
            }

            //分类
            for (Genre c : item.genres) {
                    SyndCategory tag = new SyndCategoryImpl();
                    tag.setName(c.name);
                    categories.add(tag);
            }

            //Tags

            entry.setCategories(categories);
            entries.add( entry );
        }
        feed.setEntries(entries);

        SyndFeedOutput output = new SyndFeedOutput();
        renderXml(output.outputW3CDom(feed));
    }

    public static List<Post> getLatestMovies() {
        return Post.find("order by postedAt desc").from(0).fetch(10);
    }
}
