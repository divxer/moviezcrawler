package controllers;

/**
 * User: divxer
 * Date: 11-2-2
 * Time: 上午9:00
 */
public class GetEd2kLink {
    public String geted2kLink() {
        return GetEd2kLinkFromMldonkey.getLink(null);
    }
}
