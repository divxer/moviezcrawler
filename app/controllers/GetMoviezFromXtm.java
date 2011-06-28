package controllers;

import jobs.MovieInfoCrawler;
import models.Post;
import org.dom4j.Element;
import org.dom4j.Node;
import org.xml.sax.SAXException;
import play.Logger;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: divxer
 * Date: 11-1-6
 * Time: 下午5:04
 */
public class GetMoviezFromXtm {
    //从XTM论坛中得到电影的标题和链接
    @SuppressWarnings("unchecked")
    public static void getNewMovieIntro(String siteUrlString) throws SAXException, ParserConfigurationException, MalformedURLException {
        // 指定url
        URL url = new URL(siteUrlString);
        String webSite = url.getProtocol() + "://" + url.getHost();

        org.dom4j.Document doc = null;
        doc = MovieInfoCrawler.getDom4jDoc(siteUrlString);

        if (doc == null) return;

        List<Node> list = doc.selectNodes("/HTML/BODY/DIV[3]/DIV[8]/FORM/TABLE[2]/TBODY/TR/TH/SPAN/A");

        Logger.info("output from job. find " + list.size() + " movies. ");

        org.dom4j.Element node = null;
        for (Node aList : list) {
            node = (Element) aList;
            String[] splitName = node.getText().split("\\[.*\\][0-9]{2}\\.[0-9]{2}\\.[0-9]{2}\\.");
            String moviezName = splitName[splitName.length - 1].split(" ")[0];
            if (Post.find("byMoviezName", moviezName).fetch().size() == 0) {
                getDetail(webSite + "/" + node.attributeValue("href"));
            }
        }
    }

    //得到XTM论坛中电影的详细介绍
    public static void getDetail(String siteUrlString) throws SAXException, ParserConfigurationException, MalformedURLException {

        org.dom4j.Document doc = null;
        doc = MovieInfoCrawler.getDom4jDoc(siteUrlString);

        if (doc == null) return;

        // XPath: /HTML/BODY/DIV[3]/FORM/DIV/TABLE/TBODY/TR/TD[2]/DIV[3]/DIV[@class='t_msgfont']

        org.dom4j.Node movieTitleNode = doc.selectSingleNode("/HTML/BODY/DIV[3]/FORM/DIV/TABLE/TBODY/TR/TD[2]/DIV[3]/H2");

        org.dom4j.Node node = doc.selectSingleNode("/HTML/BODY/DIV[3]/FORM/DIV/TABLE/TBODY/TR/TD[2]/DIV[3]/DIV[@class='t_msgfont']");

        org.dom4j.Element imdbLink = (Element) doc.selectSingleNode("/HTML/BODY/DIV[3]/FORM/DIV/TABLE/TBODY/TR/TD[2]/DIV[3]/DIV[@class='t_msgfont']/A");

        if (movieTitleNode != null && node != null && node.hasContent()) {
            //输出电影介绍
            //保存电影详细介绍到数据库
            String movieTitle = movieTitleNode.getText();
            String[] splitName = movieTitle.split("\\[.*\\][0-9]{2}\\.[0-9]{2}\\.[0-9]{2}\\.");
            String moviezName = splitName[splitName.length - 1].split(" ")[0];

            //IMDB链接
            String imdbCode = null;
            if (imdbLink != null && imdbLink.hasContent()) {
                URL imdbUrl = new URL(imdbLink.attributeValue("href"));
                String[] urlSplit = imdbUrl.getPath().split("/");
                imdbCode = urlSplit[urlSplit.length - 1];
                //获取豆瓣信息
//                getInfoFromDouban(null, imdbCode);
            }

            //去除quote中的内容
            org.dom4j.Element quoteNode = (Element) node.selectSingleNode("//DIV[@class='quote']");
            if (quoteNode != null && quoteNode.hasContent()) {
                quoteNode.detach();
            }

            //去除DIV标记
            StringBuilder result = new StringBuilder();
            String patternStrs = "(^<DIV class=.+?>)(.+?)(</DIV>$)";
            //匹配换行
//            String patternStrs="(^<DIV class=.+?>)((.|[\r\n])+?)(</DIV>$)";
            Pattern pattern = Pattern.compile(patternStrs, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(node.asXML());
            while (matcher.find()) {
                result.append(matcher.group(2) + "\n");
            }

            //提取类别
//            <FONT color="blue">◎类　　别　</FONT>喜剧/爱情<BR/>
            String genre = null;
            String genrePatternStrs = "(<FONT color=\"blue\">◎类.+?别.+?</FONT>)(.+?)(<BR/>)";
            Pattern genrePattern = Pattern.compile(genrePatternStrs, Pattern.DOTALL);
            Matcher genreMatcher = genrePattern.matcher(node.asXML());
            while (genreMatcher.find()) {
                genre = genreMatcher.group(2);
            }
            String[] genres = new String[0];
            if (genre != null) {
                genres = genre.split("/");
            }

            MovieInfoCrawler.saveMoviezInfo(movieTitle, moviezName, genres, result.toString().trim(), "xtmhd.com", imdbCode);
        }
    }
}
