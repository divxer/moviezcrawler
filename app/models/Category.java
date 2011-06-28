package models;

import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.Entity;

/**
 * User: divxer
 * Date: 10-12-23
 * Time: 下午4:18
 */
@Entity
public class Category extends Model implements Comparable<Category> {
    @Required
    public String name;

    public Category(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Category{" +
                "name='" + name + '\'' +
                '}';
    }

    public int compareTo(Category otherCategory) {
        return name.compareTo(otherCategory.name);
    }
}
