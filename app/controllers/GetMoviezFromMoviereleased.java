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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: divxer
 * Date: 11-1-6
 * Time: 下午5:11
 */
public class GetMoviezFromMoviereleased {
    //从XTM论坛中得到电影的标题和链接
	@SuppressWarnings("unchecked")
	public static void getNewMovieIntro(String siteUrlString) throws SAXException, ParserConfigurationException, MalformedURLException {
		// 指定url
		URL url = new URL(siteUrlString);
		String webSite = url.getProtocol()+"://"+url.getHost();

        org.dom4j.Document doc = null;
        doc = MovieInfoCrawler.getDom4jDoc(siteUrlString);

        if (doc == null) return;

//        /HTML/BODY/DIV/DIV[@id=\"main\"]/DIV[@id=\"container\"]/DIV[@id=\"content\"]/DIV/H2/A
//        /HTML/BODY/DIV/DIV[4]/DIV/DIV/DIV[2]/H2/A
        List<Node> list = doc.selectNodes("/HTML/BODY/DIV/DIV/DIV/DIV/DIV/H2/A");

        Logger.info("output from job. find " + list.size() + " movies from moviereleased.net. ");

		org.dom4j.Element node = null;
        for (Node aList : list) {
            node = (Element) aList;
            String moviezName =  node.getText().split(" ")[0];
            if (Post.find("byMoviezName", moviezName).fetch().size() == 0) {
                getDetail(node.attributeValue("href"));
            }
        }
	}

    //得到XTM论坛中电影的详细介绍
	public static void getDetail(String siteUrlString) throws SAXException, ParserConfigurationException, MalformedURLException {

        org.dom4j.Document doc = null;
        doc = MovieInfoCrawler.getDom4jDoc(siteUrlString);

        if (doc == null) return;

        // XPath: /HTML/BODY/DIV[3]/FORM/DIV/TABLE/TBODY/TR/TD[2]/DIV[3]/DIV[@class='t_msgfont']
//        /HTML/BODY/DIV/DIV[2]/DIV/DIV[2]/DIV/H1
//        /HTML/BODY/DIV/DIV[4]/DIV/DIV/DIV/H2/A    (2011.02.04)
        org.dom4j.Node movieTitleNode = doc.selectSingleNode("/HTML/BODY/DIV/DIV[4]/DIV/DIV/DIV/H2/A");

//        //*[@class="entry-content"]
//		org.dom4j.Node node = doc.selectSingleNode("//*[@class=\"entry-content\"]");
//        //*[@class="postcontent"]    (2011.02.04)
        org.dom4j.Node node = doc.selectSingleNode("//*[@class=\"postcontent\"]");

//        /html/body/div/div[2]/div/div[2]/div/div/span
//        org.dom4j.Node entryDateNode = doc.selectSingleNode("//*[@class=\"entry-date\"]/A");

//		org.dom4j.Element imdbLink = (Element) doc.selectSingleNode("/HTML/BODY/DIV[3]/FORM/DIV/TABLE/TBODY/TR/TD[2]/DIV[3]/DIV[@class='t_msgfont']/A");

		if (node!=null&&node.hasContent()) {
			//输出电影介绍
            //保存电影详细介绍到数据库
            String movieTitle = movieTitleNode.getText();
//            String[] splitName = movieTitle.split("\\[.*\\][0-9]{2}\\.[0-9]{2}\\.[0-9]{2}\\.");
            String moviezName = movieTitle.split(" ")[0];

            //IMDB链接
//            String imdbCode = null;
//            if (imdbLink != null && imdbLink.hasContent()) {
//                URL imdbUrl = new URL(imdbLink.attributeValue("href"));
//                String[] urlSplit = imdbUrl.getPath().split("/");
//                imdbCode = urlSplit[urlSplit.length - 1];
//            }

            //去除wumii-hook中的内容
            org.dom4j.Element  quoteNode = (Element) node.selectSingleNode("//DIV[@class='wumii-hook']");
            if (quoteNode!=null&&quoteNode.hasContent()) {
                quoteNode.detach();
            }
            //去除javascript
            org.dom4j.Element  scriptNode = (Element) node.selectSingleNode("//P[@style='margin:0;padding:0;height:1px;']");
            if (scriptNode!=null&&scriptNode.hasContent()) {
                scriptNode.detach();
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

            //提取IMDB链接
//            【IMDB链接】http://www.imdb.com/title/tt0466399<BR/>
            String imdbLink = null;
            String imdbCode = null;
            String imdbPatternStrs = "(【IMDB链接】)(.+?)(<BR/>)";
            Pattern imdbPattern = Pattern.compile(imdbPatternStrs, Pattern.DOTALL);
            Matcher imdbMatcher = imdbPattern.matcher(node.asXML());
            while (imdbMatcher.find()) {
                imdbLink = imdbMatcher.group(2);
                if (!imdbLink.matches("http://www.imdb.com/title/tt[0-9]+")) imdbLink = null;
            }
            if (imdbLink != null && imdbLink.trim() != null) {
                URL imdbUrl = new URL(imdbLink);
                String[] urlSplit = imdbUrl.getPath().split("/");
                imdbCode = urlSplit[urlSplit.length - 1];
            }

            //发布日期
//            [0-9]{2,}
//            String postDate = null;
//            String postDatePatternStrs = "[0-9]{2,}";
//            Pattern postDatePattern = Pattern.compile(postDatePatternStrs, Pattern.DOTALL);
//            Matcher postDateMatcher = postDatePattern.matcher(entryDateNode.getText());
//            StringBuffer buffer = new StringBuffer();
//            String yearStr = null;
//            while (postDateMatcher.find()) {
//                String t = postDateMatcher.group();
//                if (t.length()==4) {
//                    yearStr = postDateMatcher.group().substring(2);
//                } else {
//                    if (buffer.length() != 0) buffer.append(".");
//                    buffer.append(postDateMatcher.group());
//                }
//            }
//            if (buffer.length() != 0) buffer.append(".");
//            buffer.append(yearStr);
//            movieTitle = buffer + "." + movieTitle;
            //(2011.02.04)
            java.text.DateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");
            movieTitle = dateFormat.format(new Date()) + "." + movieTitle;

            //提取国家
//            【国　　家】澳大利亚<BR/
            String country = null;
            String countryPatternStrs = "(【国.*家】)(.+?)(<BR/>)";
            Pattern countryPattern = Pattern.compile(countryPatternStrs, Pattern.DOTALL);
            Matcher countryMatcher = countryPattern.matcher(node.asXML());
            while (countryMatcher.find()) {
                country = countryMatcher.group(2);
                System.out.println("国家: " + countryMatcher.group(2));
            }
            if (country != null) {
                movieTitle = "[" + country + "]" + movieTitle;
            }

            //提取类别
//            【类　　别】剧情/冒险/战争/喜剧/历史<BR/>
            String genre = null;
            String genrePatternStrs = "(【类.*型】)(.+?)(<BR/>)";
            Pattern genrePattern = Pattern.compile(genrePatternStrs, Pattern.DOTALL);
            Matcher genreMatcher = genrePattern.matcher(node.asXML());
            while (genreMatcher.find()) {
                genre = genreMatcher.group(2);
            }
            String[] genres = new String[0];
            if (genre != null) {
                genres = genre.split("\\|");
            }

            MovieInfoCrawler.saveMoviezInfo(movieTitle, moviezName, genres, result.toString().trim(), "moviereleased.net", imdbCode);
        }
	}
}
