// Import packages
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;

// Class responsible for loading and managing retro-style fonts
public class FontManager {
    // Static font variables accessible throughout the program
    public static Font RETRO_FONT;
    public static Font RETRO_FONT_SMALL;
    public static Font RETRO_FONT_MEDIUM;
    public static Font RETRO_FONT_LARGE;

    // Static block runs once when the class is first loaded
    static {
        try {
            // Attempt to load a TrueType font from the "fonts" directory
            RETRO_FONT = Font.createFont(
                Font.TRUETYPE_FONT, 
                new File("fonts/PressStart2P.ttf") // Path to the font file
            );

            // Register the loaded font with the system's graphics environment
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(RETRO_FONT);

            // Create different font sizes for convenience
            RETRO_FONT_SMALL = RETRO_FONT.deriveFont(8f);   // Small size (8 pt)
            RETRO_FONT_MEDIUM = RETRO_FONT.deriveFont(12f); // Medium size (12 pt)
            RETRO_FONT_LARGE = RETRO_FONT.deriveFont(24f);  // Large size (24 pt)

        } catch (IOException | FontFormatException e) {
            // If the custom font fails to load, print an error message
            System.err.println("Error loading font: " + e.getMessage());

            // Fall back to a default monospaced font in different sizes
            RETRO_FONT = new Font("Monospaced", Font.BOLD, 12);
            RETRO_FONT_SMALL = new Font("Monospaced", Font.BOLD, 8);
            RETRO_FONT_MEDIUM = new Font("Monospaced", Font.BOLD, 12);
            RETRO_FONT_LARGE = new Font("Monospaced", Font.BOLD, 24);
        }
    }
}
