// Import packages
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// Main class
public class PacmanGame extends JFrame {
    // Make CELL_SIZE, MAZE_WIDTH, and MAZE_HEIGHT constants accessible
    public static final int CELL_SIZE = 25;
    public static final int MAZE_WIDTH = 28;
    public static final int MAZE_HEIGHT = 31;
    
    // Game state management
    private boolean gameStarted = false;
    private int selectedDifficulty = 1; // 1=Easy, 2=Medium, 3=Hard
    private JPanel difficultyScreen;
    private BufferedImage pacmanLogo;
    private JButton easyButton;
    private JButton mediumButton;
    private JButton hardButton;
    
    public PacmanGame() {
        setLayout(new BorderLayout());
        setTitle("PAC-MAN");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        // Load the Pacman logo
        try {
            pacmanLogo = ImageIO.read(new File("images/other/pacman-logo.png"));
        } catch (IOException e) {
            // Logo not found - will create text alternative
        }

        // Create game board (will be added when game starts)
        gameBoard = new GameBoard();
        
        // Create a stats panel at the bottom
        statsPanel = new StatsPanel();
        
        // Initialize the difficulty selection screen
        createDifficultyScreen();
        
        // Initially add the difficulty screen
        add(difficultyScreen, BorderLayout.CENTER);

        // Setup key listener for game controls - adding to the frame
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameStarted) {
                    gameBoard.handleKeyPress(e.getKeyCode());
                }
            }
        });

        setFocusable(true);
        requestFocus();
        pack();
        setLocationRelativeTo(null);
    }
    
    // Create the difficulty selection screen
    private void createDifficultyScreen() {
        difficultyScreen = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw a black background
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
                
                // If logo is available, draw it at the top
                if (pacmanLogo != null) {
                    int logoWidth = Math.min(400, getWidth() - 100);
                    float aspectRatio = (float)pacmanLogo.getHeight() / pacmanLogo.getWidth();
                    int logoHeight = (int)(logoWidth * aspectRatio);
                    
                    g.drawImage(pacmanLogo, 
                        (getWidth() - logoWidth) / 2, 
                        50, 
                        logoWidth, 
                        logoHeight, 
                        null);
                } else {
                    // Draw text logo as alternative
                    g.setColor(Color.YELLOW);
                    g.setFont(FontManager.RETRO_FONT.deriveFont(48f));
                    String title = "PAC-MAN";
                    FontMetrics fm = g.getFontMetrics();
                    int titleWidth = fm.stringWidth(title);
                    g.drawString(title, (getWidth() - titleWidth) / 2, 120);
                }
            }
        };
        difficultyScreen.setPreferredSize(new Dimension(MAZE_WIDTH * CELL_SIZE, MAZE_HEIGHT * CELL_SIZE));
        
        // Create the panel to hold the buttons
        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 0, 20));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(180, 50, 50, 50));
        
        // Create title label
        JLabel titleLabel = new JLabel("SELECT DIFFICULTY", JLabel.CENTER);
        titleLabel.setFont(FontManager.RETRO_FONT.deriveFont(24f));
        titleLabel.setForeground(Color.WHITE);
        
        // Create difficulty buttons with uniform style
        easyButton = createStyledButton("EASY - MOSTLY RANDOM GHOSTS", 1);
        mediumButton = createStyledButton("MEDIUM - SMARTER GHOSTS", 2);
        hardButton = createStyledButton("HARD - AGGRESSIVE GHOSTS", 3);
        
        // Add components to button panel
        buttonPanel.add(titleLabel);
        buttonPanel.add(easyButton);
        buttonPanel.add(mediumButton);
        buttonPanel.add(hardButton);
        
        // Add the button panel to the center of the difficulty screen
        difficultyScreen.add(buttonPanel, BorderLayout.CENTER);
        
        // Add a description label at the bottom with updated description
        JLabel descriptionLabel = new JLabel(
            "<html><center>Choose your challenge level!<br>" +
            "Higher difficulties make ghosts follow their classic behaviors more aggressively.</center></html>",
            JLabel.CENTER
        );
        descriptionLabel.setFont(FontManager.RETRO_FONT.deriveFont(12f));
        descriptionLabel.setForeground(Color.CYAN);
        descriptionLabel.setBorder(new EmptyBorder(0, 20, 30, 20));
        
        difficultyScreen.add(descriptionLabel, BorderLayout.SOUTH);
    }
    
    // Helper method to create consistently styled buttons
    private JButton createStyledButton(String text, final int difficulty) {
        JButton button = new JButton(text);
        button.setFont(FontManager.RETRO_FONT.deriveFont(18f));
        button.setBackground(Color.BLACK);
        button.setForeground(Color.YELLOW);
        button.setFocusPainted(false);
        button.setFocusable(false); // Prevent buttons from stealing focus
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLUE, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(0, 0, 100));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Color.BLACK);
            }
        });
        
        // Click action
        button.addActionListener(e -> {
            selectedDifficulty = difficulty;
            startGame();
        });
        
        return button;
    }
    
    // Start the game with the selected difficulty
    private void startGame() {
        gameStarted = true;
        
        // Remove the difficulty screen
        remove(difficultyScreen);
        
        // Add the game components
        add(gameBoard, BorderLayout.CENTER);
        add(statsPanel, BorderLayout.SOUTH);
        
        // Set the selected difficulty on the game board
        gameBoard.setDifficulty(selectedDifficulty);
        
        // Pass the stats panel to the GameBoard to update score
        gameBoard.setStatsPanel(statsPanel);
        
        // Request focus for keyboard input and update the UI
        requestFocusInWindow();
        requestFocus();
        revalidate();
        repaint();
        pack();
    }

    private GameBoard gameBoard;
    private StatsPanel statsPanel;

    // Inner class for the stats panel at the bottom
    class StatsPanel extends JPanel {
        private int score = 0;
        private int lives = 3;
        private int level = 1;
        
        public StatsPanel() {
            // Reduce height from 60 to 40 pixels for a smaller stats panel
            setPreferredSize(new Dimension(MAZE_WIDTH * CELL_SIZE, 40));
            setBackground(Color.BLACK);
        }
        
        public void updateStats(int score, int lives, int level) {
            this.score = score;
            this.lives = lives;
            this.level = level;
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw dividing line
            g2d.setColor(new Color(32, 34, 211));
            g2d.fillRect(0, 0, getWidth(), 2);
            
            // Draw level
            g2d.setColor(Color.WHITE);
            g2d.setFont(FontManager.RETRO_FONT.deriveFont(16f));
            String levelText = "LEVEL: " + level;
            g2d.drawString(levelText, 30, 25);
            
            // Draw score
            String scoreText = "SCORE: " + score;
            FontMetrics fm = g2d.getFontMetrics();
            int scoreWidth = fm.stringWidth(scoreText);
            g2d.drawString(scoreText, (getWidth() - scoreWidth) / 2, 25);
            
            // Draw difficulty
            String difficultyText = "";
            switch(selectedDifficulty) {
                case 1: difficultyText = "EASY"; break;
                case 2: difficultyText = "MEDIUM"; break;
                case 3: difficultyText = "HARD"; break;
            }
            
            // Draw lives as Pacman icons
            int pacmanSize = 20;
            int pacmanSpacing = 25;
            int rightMargin = 30;
            
            // Draw "LIVES:" text
            String livesText = "LIVES:";
            g2d.drawString(livesText, getWidth() - rightMargin - (lives * pacmanSpacing) - 80, 25);
            
            for (int i = 0; i < lives; i++) {
                g2d.setColor(Color.YELLOW);
                g2d.fillArc(getWidth() - rightMargin - ((lives - i) * pacmanSpacing), 
                           8, pacmanSize, pacmanSize, 30, 300);
            }
        }
    }

    public static void main(String[] args) {
        // Ensure the font is loaded before creating the UI
        FontManager.RETRO_FONT.getFamily(); // This forces the static block to execute
        
        EventQueue.invokeLater(() -> {
            try {
                // Set UI look and feel to the system default
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            PacmanGame game = new PacmanGame();
            game.setVisible(true);
        });
    }
}
