package jobs;

import controllers.GetEd2kLinkFromMldonkey;
import controllers.GetMoviezFromEastGame;
import controllers.GetMoviezFromMoviereleased;
import controllers.GetMoviezFromXtm;
import models.Category;
import models.Genre;
import models.Post;
import models.User;
import org.apache.http.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParamBean;
import org.apache.log4j.Level;
import org.cyberneko.html.parsers.DOMParser;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.DOMReader;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import play.Logger;
import play.jobs.Job;
import play.jobs.On;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 0day电影信息爬虫
 * User: divxer
 * Date: 10-12-22
 * Time: 上午10:36
 */

/**
 * Fire at every 5 minutes *
 */
@On("0 */5 * * * ?")
public class MovieInfoCrawler extends Job {
    private String movieTitle;
    private String moviezName;
    private String imdbCode;

    public void doJob() {
        // execute some application logic here ...
        Date timeNow = new Date();
//        System.out.println("output from job. 定时任务输出... 每5分钟。 time Now: " + timeNow.toString());
        Logger.info("output from job. time Now: " + timeNow.toString());

        long start = System.currentTimeMillis();

        //设置代理
//		String proxy = "219.129.216.23";
//		int port =22;
//		String userName ="lizong";
//		String passWord ="lizonghao";
//		initProxy(proxy,port,userName,passWord);

        //获取最新发布的0day电影介绍及豆瓣评论
        //XTM论坛
        try {
            GetMoviezFromXtm.getNewMovieIntro("http://xtmhd.com/forum-150-1.html");
        } catch (MalformedURLException e) {
            Logger.error(e, "MalformedURLException");
        } catch (SAXException e) {
            Logger.error(e, "SAXException");
        } catch (ParserConfigurationException e) {
            Logger.error(e, "ParserConfigurationException");
        }
        //eastgame论坛
        try {
            GetMoviezFromEastGame.getNewMovieIntro("http://www1.eastgame.org:8088/forumdisplay.php?fid=540");
        } catch (MalformedURLException e) {
            Logger.error(e, "MalformedURLException");
        } catch (SAXException e) {
            Logger.error(e, "SAXException");
        }
        //moviereleased博客
        try {
            GetMoviezFromMoviereleased.getNewMovieIntro("http://moviereleased.net/");
        } catch (SAXException e) {
            Logger.error(e,"SAXException");
        } catch (ParserConfigurationException e) {
            Logger.error(e, "ParserConfigurationException");
        } catch (MalformedURLException e) {
            Logger.error(e, "MalformedURLException");
        }


        long end = System.currentTimeMillis();
        Logger.info("output from job. job finished in: " + (end - start) + "ms ");
    }

    //初始化代理
    public static void initProxy(String host, int port, final String username,
                                 final String password) {
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username,
                        password.toCharArray());
            }
        });
        System.setProperty("http.proxyType", "4");
        System.setProperty("http.proxyPort", Integer.toString(port));
        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxySet", "true");
    }

    //通过IMDB代码或电影名称在豆瓣电影网站上查询电影相关信息
//		/html/body/div[2]/div[2]/div/div/table/tbody/tr/td[2]/div/a
    //豆瓣电影链接
//		/HTML/BODY/DIV[2]/DIV[2]/DIV/DIV/TABLE/TBODY/TR/TD[2]/DIV/A

    //		影评
    //*[@id="wt_0"]
//		/html/body/div[2]/div[2]/div/div/div[3]/div[5]/div
//		/HTML/BODY/DIV[2]/DIV[2]/DIV/DIV/DIV[3]/DIV[5]/DIV
//		/html/body/div[2]/div[2]/div/div/div[3]/div[5]/div/ul/li/h3/a
//		/HTML/BODY/DIV[2]/DIV[2]/DIV/DIV/DIV[3]/DIV[5]/DIV/UL/LI/H3/A
    public static void getInfoFromDouban(String movieName, String imdbCode, Post savedPost) throws SAXException {

        String searchText;
        if (imdbCode != null) {
            searchText = imdbCode;
        } else {
            String[] splitName = movieName.split(" ");
            searchText = splitName[splitName.length - 1].split("/")[0];
        }
        if (searchText != null) {
            //豆瓣查询链接
            //http://movie.douban.com/subject_search?search_text=%E6%88%91%E7%9A%84%E9%87%8E%E8%9B%AE%E5%A5%B3%E5%8F%8B2&cat=1002
            String searchUrlString = "http://movie.douban.com/subject_search?cat=1002&search_text="
                    + searchText;

            org.dom4j.Document doc = null;
            doc = getDom4jDoc(searchUrlString);

            if (doc == null) return;

            org.dom4j.Element doubanMovieLink = (Element) doc.selectSingleNode("/HTML/BODY/DIV[2]/DIV[2]/DIV/DIV/TABLE/TBODY/TR/TD[2]/DIV/A");
            if (doubanMovieLink != null && doubanMovieLink.hasContent()) {
                //获取豆瓣评论
                getCommentFromDouban(doubanMovieLink.attributeValue("href"), savedPost);
            }
        }
    }

    //通过电影名称在豆瓣电影网站上查询电影相关信息
    public static void getInfoFromDouban(String movieName) throws IOException, SAXException {
        getInfoFromDouban(movieName, null, null);
    }

    //获取豆瓣评论
    @SuppressWarnings("unchecked")
    public static void getCommentFromDouban(String doubanMovieLink, Post savedPost) throws SAXException {

        org.dom4j.Document doc = null;
        doc = getDom4jDoc(doubanMovieLink);

        if (doc == null) return;

        List<org.dom4j.Node> list = doc.selectNodes("/HTML/BODY/DIV[2]/DIV[2]/DIV/DIV/DIV[3]/DIV[@id='wt_0']/DIV/UL/LI/H3/A");

//		/html/body/div[2]/div[2]/div/div/div[3]/div[5]/div[5]/ul/li[3]/span/span[2]
        List<org.dom4j.Node> movieLevelList = doc.selectNodes("/HTML/BODY/DIV[2]/DIV[2]/DIV/DIV/DIV[3]/DIV[5]/DIV[5]/UL/LI[3]/SPAN/SPAN[2]");

        org.dom4j.Element node = null;
        org.dom4j.Element movieLevelNode = null;
        for (Node aList : list) {
            node = (Element) aList;
//        	movieLevelNode = (Element) movieLevelList.get(i);

            getCommentDetailFromDouban(node.attributeValue("href"), savedPost);
        }

        Logger.info("output from job. Added " + list.size() + " comments to" + savedPost.title);
    }

    //获取豆瓣评论的详细信息
    public static void getCommentDetailFromDouban(String commentUrlString, Post savedPost) throws SAXException {

        org.dom4j.Document doc = null;
        doc = getDom4jDoc(commentUrlString);

        if (doc == null) return;

        org.dom4j.Node node = doc.selectSingleNode("/HTML/BODY/DIV[2]/DIV[2]/DIV/DIV/DIV/DIV[2]/SPAN[3]");
        org.dom4j.Node commentTitle = doc.selectSingleNode("/HTML/BODY/DIV[2]/H1/SPAN");

        if (node != null && node.hasContent()) {
            String commentDetail = commentTitle.getText() + "---来自\"豆瓣影评\"\n\n" + node.getText();

            //保存评论详细信息到数据库
            savedPost.addComment("from 豆瓣电影", commentDetail);
        }
    }

    /**
     * 获取页面DOM树
     * @param toFetchURL
     * @return
     */
    public static org.dom4j.Document getDom4jDoc(String toFetchURL) {
        HttpGet get = null;
        HttpEntity entity = null;

        HttpParams params = new BasicHttpParams();
		HttpProtocolParamBean paramsBean = new HttpProtocolParamBean(params);
		paramsBean.setVersion(HttpVersion.HTTP_1_1);
		paramsBean.setContentCharset("UTF-8");
		paramsBean.setUseExpectContinue(false);

		params.setParameter("http.useragent",
				"moviezCrawler (https://github.com/divxer/moviezcrawler)");

		params.setIntParameter("http.socket.timeout", 20000);

		params.setIntParameter("http.connection.timeout", 30000);

		params.setBooleanParameter("http.protocol.handle-redirects", false);

		ConnPerRouteBean connPerRouteBean = new ConnPerRouteBean();
		connPerRouteBean.setDefaultMaxPerRoute(100);
		ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRouteBean);
		ConnManagerParams.setMaxTotalConnections(params, 100);

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

		ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager(params, schemeRegistry);
		DefaultHttpClient httpclient = new DefaultHttpClient(connectionManager, params);

        try {
            get = new HttpGet(toFetchURL);
            HttpResponse response = httpclient.execute(get);
            entity = response.getEntity();

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                return null;
            }

            if (entity != null) {
                long size = entity.getContentLength();
                if (size == -1) {
                    Header length = response.getLastHeader("Content-Length");
                    if (length == null) {
                        length = response.getLastHeader("Content-length");
                    }
                    if (length != null) {
                        size = Integer.parseInt(length.getValue());
                    } else {
                        size = -1;
                    }
                }

                // parse content
                DOMParser parser = new DOMParser();
                parser.setProperty(
                        "http://cyberneko.org/html/properties/default-encoding",
                        "utf-8");
                parser.setFeature("http://xml.org/sax/features/namespaces", false);

                parser.parse(new InputSource(entity.getContent()));
                Document document = parser.getDocument();
                DOMReader reader = new DOMReader();

                return reader.read(document);
            } else {
                get.abort();
            }
        } catch (IOException e) {
            Logger.error("Fatal transport error: " + e.getMessage() + " while fetching " + toFetchURL);
            return null;
        } catch (IllegalStateException e) {
            // ignoring exceptions that occur because of not registering https
            // and other schemes
        } catch (Exception e) {
            if (e.getMessage() == null) {
                Logger.error("Error while fetching " + toFetchURL);
            } else {
                Logger.error(e.getMessage() + " while fetching " + toFetchURL);
            }
        } finally {
            try {
                if (entity != null) {
                    entity.consumeContent();
                } else if (get != null) {
                    get.abort();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //保存电影信息到数据库
    public static void saveMoviezInfo(String title, String moviezName, String[] genres, String content, String source, String imdbCode) throws SAXException {
        // Create post
        User author = User.all().first();
        if (Post.find("byMoviezName", moviezName).fetch().size() == 0) {
            Post post = new Post(author, title, content);
            //0day名称
            post.moviezName = moviezName;
            //电驴链接
//            post.ed2kLink = GetEd2kLinkFromMldonkey.getLink(moviezName);
            //
            for (String g : genres) {
                post.genres.add(Genre.findOrCreateByName(g.trim()));
            }
            //来源
            post.source = source;
            //分类
            List<Category> categoryList = Category.all().fetch();
            for (Category c : categoryList) {
                if (post.title.toLowerCase().contains(c.name.toLowerCase())) {
                    post.categories.add(c);
                    Logger.info("category name: " + c.name);
                }
            }
            //电影类别
//            StringBuilder result = new StringBuilder();
//            String patternStrs = "(^<DIV class=.+?>)(.+?)(</DIV>$)";
//            Pattern pattern = Pattern.compile(patternStrs, Pattern.DOTALL);
//            Matcher matcher = pattern.matcher(content);
//            while (matcher.find()) {
//                result.append(matcher.group(2) + "\n");
//            }
            // Save
            Post savedPost = post.save();

            Logger.info("output from job. Saved movie: " + title + " to database.");

//            从豆瓣电影网获取评论详细信息
            getInfoFromDouban(title, imdbCode, savedPost);
        }
    }
}
