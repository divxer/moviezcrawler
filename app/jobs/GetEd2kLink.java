package jobs;

import controllers.GetEd2kLinkFromMldonkey;
import models.Post;
import play.jobs.Job;
import play.jobs.On;

import java.util.List;

/**
 * User: divxer
 * Date: 11-2-7
 * Time: 上午10:57
 */
/** Fire at every 47 minutes **/
@On("0 */47 * * * ?")
public class GetEd2kLink extends Job {
    public void doJob() {
        List<Post> list = Post.findAll();
        for (Post p : list) {
            if (p.tryCount == null) p.tryCount = 0L;
            if (p.moviezName == null || p.tryCount > 10) continue;
            p.tryCount+=1;
            String ed2kLink = GetEd2kLinkFromMldonkey.getLink(p.moviezName);
            if (ed2kLink != null) {
                p.ed2kLink = ed2kLink;
            }
            p.save();
        }
    }
}
