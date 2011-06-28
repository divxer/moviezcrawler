package controllers;

import play.mvc.With;

/**
 * User: divxer
 * Date: 2010-12-1
 * Time: 11:50:53
 */
@Check("admin")
@With(Secure.class)
public class Users extends CRUD {
}
