package controllers;

import models.User;

/**
 * User: divxer
 * Date: 2010-12-1
 * Time: 16:03:39
 */
public class Security extends Secure.Security {

    static boolean authenticate(String username, String password) {
        return User.connect(username, password) != null;
    }

    static void onAuthenticated() {
        Admin.index();
    }

    static void onDisconnected() {
        Application.index();
    }

    static boolean check(String profile) {
        if ("admin".equals(profile)) {
            return User.find("byEmail", connected()).<User>first().isAdmin;
        }
        return false;
    }

}
