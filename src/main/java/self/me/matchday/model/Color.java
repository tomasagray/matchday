package self.me.matchday.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;

/**
 * Wrap Java AWT Color class for storage and serialization
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class Color {

    private int red;
    private int green;
    private int blue;
    private int alpha;

    public Color(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }
}
