package controllers;

import play.*;
import play.mvc.*;

/**
 * User: divxer
 * Date: 2010-12-1
 * Time: 11:45:46
 */
@Check("admin")
@With(Secure.class)
public class Posts extends CRUD {
}
