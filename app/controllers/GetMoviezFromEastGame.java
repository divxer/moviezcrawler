package controllers;

import jobs.MovieInfoCrawler;
import models.Post;
import org.cyberneko.html.parsers.DOMParser;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.DOMReader;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import play.Logger;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: divxer
 * Date: 11-1-6
 * Time: 下午4:12
 */
public class GetMoviezFromEastGame {
    //从eastgame论坛中得到电影的标题和链接
	@SuppressWarnings("unchecked")
	public static void getNewMovieIntro(String siteUrlString) throws SAXException, MalformedURLException {
		// 指定url
		URL url = new URL(siteUrlString);
		String webSite = url.getProtocol()+"://"+url.getHost()+":"+url.getPort();

        org.dom4j.Document doc = null;
        doc = MovieInfoCrawler.getDom4jDoc(siteUrlString);

        if (doc == null) return;

        List<Node> list = doc.selectNodes("/HTML/BODY/DIV[6]/DIV/DIV/DIV[3]/FORM/TABLE/TBODY");

        Logger.info("output from job. find " + list.size() + " movies from eastgame forum. ");

		org.dom4j.Element node = null;

        for (Node aList : list) {
            node = (Element) aList;
            String idAttrib = node.attributeValue("id");
            if (idAttrib!=null&&idAttrib.contains("normalthread_")) {
            	//生成一个Document
                org.dom4j.Document document = null;
                try {
                    document = DocumentHelper.parseText(node.asXML());
                } catch (DocumentException e) {
                    e.printStackTrace();
                }
                Element newMovieNode = null;
                if (document != null) {
                    newMovieNode = (Element) document.selectSingleNode("/TBODY/TR/TH/SPAN/A");
                }
                if (Post.find("byTitle", node.getText()).fetch().size() == 0) {
                    String movieName = newMovieNode.getText();
                    System.out.println(movieName);
                    getDetail(webSite + "/" + newMovieNode.attributeValue("href"));
                }
            }
        }
	}

    //得到eastgame论坛中电影的详细介绍
	public static void getDetail(String siteUrlString) throws SAXException, MalformedURLException {

		System.out.println(siteUrlString);

        org.dom4j.Document doc = null;
        doc = MovieInfoCrawler.getDom4jDoc(siteUrlString);

        if (doc == null) return;

        org.dom4j.Node movieTitleNode = doc.selectSingleNode("/HTML/BODY/DIV[6]/DIV[2]/DIV/TABLE/TBODY/TR/TD[2]/DIV[3]/DIV[4]/DIV/H1");

		org.dom4j.Node node = doc.selectSingleNode("/HTML/BODY/DIV[6]/DIV[2]/DIV/TABLE/TBODY/TR/TD[2]/DIV[3]/DIV[4]/DIV[@class='t_msgfontfix']/TABLE/TBODY/TR/TD[@class='t_msgfont']");

		org.dom4j.Element imdbLink = (Element) doc.selectSingleNode("/HTML/BODY/DIV[6]/DIV[2]/DIV/TABLE/TBODY/TR/TD[2]/DIV[3]/DIV[4]/DIV[2]/TABLE/TBODY/TR/TD[@class='t_msgfont']/A");

		if (node!=null&&node.hasContent()) {
			//输出电影介绍
            //保存电影详细介绍到数据库
            String movieTitle = movieTitleNode.getText();
            String[] splitName = movieTitle.split("[0-9]{2}\\.[0-9]{2}\\.[0-9]{2}\\.");
            String moviezName = splitName[splitName.length-1].split(" ")[0];

            //IMDB链接
            String imdbCode = null;
            if (imdbLink != null && imdbLink.hasContent()) {
                URL imdbUrl = new URL(imdbLink.attributeValue("href"));
                String[] urlSplit = imdbUrl.getPath().split("/");
                imdbCode = urlSplit[urlSplit.length - 1];
            }

            //去除quote中的内容
            org.dom4j.Element  quoteNode = (Element) node.selectSingleNode("//DIV[@class='quote']");
            if (quoteNode!=null&&quoteNode.hasContent()) {
                quoteNode.detach();
            }

            //去除DIV标记
            StringBuilder result = new StringBuilder();
            String patternStrs = "(^<TD class=.+?>)(.+?)(</TD>$)";
            //匹配换行
//            String patternStrs="(^<DIV class=.+?>)((.|[\r\n])+?)(</DIV>$)";
            Pattern pattern = Pattern.compile(patternStrs, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(node.asXML());
            while (matcher.find()) {
                result.append(matcher.group(2) + "\n");
            }

//            System.out.println("movieTitle: "+movieTitle+"\nresult: "+node.getText().toString().trim()+"\nimdbCode:"+imdbCode);

            MovieInfoCrawler.saveMoviezInfo(movieTitle, moviezName,new String[0], result.toString().trim(), "eastgame.org", imdbCode);
        }
	}
}
