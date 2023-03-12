package self.me.matchday.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;import javax.persistence.Embeddable;

/**
 * Wrap Java AWT Color class for storage and serialization
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class Color {

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int red = 0;
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int green = 0;
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int blue = 0;
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int alpha = 0;

    public Color(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }
}
