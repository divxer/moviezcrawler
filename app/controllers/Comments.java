package controllers;

import play.mvc.With;

/**
 * User: divxer
 * Date: 2010-12-1
 * Time: 11:52:00
 */
@Check("admin")
@With(Secure.class)
public class Comments extends CRUD {
}
