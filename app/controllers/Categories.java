package controllers;

import models.Category;
import play.mvc.With;
import controllers.CRUD;

/**
 * User: divxer
 * Date: 10-12-24
 * Time: 下午3:03
 */
@Check("admin")
@With(Secure.class)
@CRUD.For(Category.class)
public class Categories extends CRUD {
}
