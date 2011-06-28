package models;

import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.Entity;

/**
 * User: divxer
 * Date: 10-12-23
 * Time: 上午9:22
 */
@Entity
public class Genre extends Model implements Comparable<Genre> {
    @Required
    public String name;

    public Genre(String name) {
        this.name = name;
    }

    public static Genre findOrCreateByName(String name) {
        Genre genre = Genre.find("byName", name).first();
        if (genre == null) {
            genre = new Genre(name);
        }
        return genre;
    }

    @Override
    public String toString() {
        return "Genre{" +
                "name='" + name + '\'' +
                '}';
    }

    public int compareTo(Genre otherGenre) {
        return name.compareTo(otherGenre.name);
    }
}
