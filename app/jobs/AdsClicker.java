package jobs;

/**
 * User: divxer
 * Date: 10-12-28
 * Time: 下午3:54
 */

import play.jobs.On;

/** Fire at every 5 minutes **/
@On("0 * 8 * * ?")
public class AdsClicker {
}
