import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.imageio.ImageIO;

public class SecureSteganography extends JFrame {

    // ---------------- CRYPTOGRAPHY SETTINGS ----------------

    private static final int SALT_SIZE = 16;
    private static final int NONCE_SIZE = 12;
    private static final int TAG_BITS = 128;
    private static final int ITERATIONS = 300000;
    private static final int KEY_SIZE = 256;

    private static final byte[] MAGIC = {'S', 'T', 'E', 'G'};
    private static final SecureRandom RANDOM = new SecureRandom();

    // ---------------- GUI COMPONENTS ----------------

    private JTextArea messageArea;
    private JPasswordField passwordField;
    private JCheckBox showPasswordCheck;

    private JLabel characterCountLabel;
    private JLabel passwordStrengthLabel;
    private JProgressBar passwordStrengthBar;

    private JLabel imagePreview;
    private JLabel imageInfo;
    private JLabel capacityLabel;
    private JProgressBar capacityBar;

    private JLabel statusLabel;
    private String coverImagePath;

    private final Color BACKGROUND = new Color(15, 23, 42);
    private final Color CARD = new Color(30, 41, 59);
    private final Color BORDER = new Color(51, 65, 85);
    private final Color TEAL = new Color(20, 184, 166);
    private final Color TEXT = new Color(241, 245, 249);
    private final Color MUTED = new Color(148, 163, 184);

    public SecureSteganography() {
        setTitle("CRYPTOVault | Secure Image Messaging");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 850);
        setMinimumSize(new Dimension(760, 700));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(15, 15));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(20, 25, 15, 25));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BACKGROUND);

        JPanel heading = new JPanel();
        heading.setBackground(BACKGROUND);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("CRYPTOVault");
        title.setFont(new Font("Arial", Font.BOLD, 30));
        title.setForeground(new Color(94, 234, 212));

        JLabel subtitle = new JLabel(
                "Secure Image Messaging  |  AES-256-GCM + LSB");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitle.setForeground(MUTED);

        heading.add(title);
        heading.add(Box.createVerticalStrut(5));
        heading.add(subtitle);

        JLabel badge = new JLabel(" AES-256-GCM ");
        badge.setOpaque(true);
        badge.setBackground(new Color(6, 78, 59));
        badge.setForeground(new Color(167, 243, 208));
        badge.setFont(new Font("Arial", Font.BOLD, 13));
        badge.setBorder(new EmptyBorder(8, 8, 8, 8));

        header.add(heading, BorderLayout.WEST);
        header.add(badge, BorderLayout.EAST);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Arial", Font.BOLD, 15));
        tabs.addTab("  Hide & Encrypt  ", createHideTab());
        tabs.addTab("  Extract & Decrypt  ", createExtractTab());

        statusLabel = new JLabel("  System Ready | Select a cover image to begin");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(94, 234, 212));

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(BACKGROUND);
        statusPanel.add(statusLabel, BorderLayout.CENTER);

        root.add(header, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(statusPanel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ---------------- HIDE & ENCRYPT TAB ----------------

    private JPanel createHideTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Message card
        JPanel messageCard = createCard();
        messageCard.add(centerLabel("Enter Secret Message", 18));
        messageCard.add(Box.createVerticalStrut(10));

        messageArea = new JTextArea(5, 40);
        messageArea.setFont(new Font("Arial", Font.PLAIN, 15));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBackground(BACKGROUND);
        messageArea.setForeground(TEXT);
        messageArea.setCaretColor(Color.WHITE);
        messageArea.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane messageScroll = new JScrollPane(messageArea);
        messageScroll.setPreferredSize(new Dimension(600, 115));
        messageScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        messageCard.add(messageScroll);

        characterCountLabel = new JLabel("Characters: 0");
        characterCountLabel.setForeground(MUTED);
        characterCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageCard.add(Box.createVerticalStrut(5));
        messageCard.add(characterCountLabel);

        messageArea.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    public void insertUpdate(javax.swing.event.DocumentEvent e) {
                        updateCharacterCount();
                    }

                    public void removeUpdate(javax.swing.event.DocumentEvent e) {
                        updateCharacterCount();
                    }

                    public void changedUpdate(javax.swing.event.DocumentEvent e) {
                        updateCharacterCount();
                    }
                });

        // Password card
        JPanel passwordCard = createCard();
        passwordCard.add(centerLabel("Enter Secret Password", 18));
        passwordCard.add(Box.createVerticalStrut(10));

        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 15));
        passwordField.setMaximumSize(new Dimension(280, 35));
        passwordField.setBackground(BACKGROUND);
        passwordField.setForeground(TEXT);
        passwordField.setCaretColor(Color.WHITE);
        passwordField.setEchoChar('*');
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordCard.add(passwordField);

        showPasswordCheck = new JCheckBox("Show Password");
        showPasswordCheck.setBackground(CARD);
        showPasswordCheck.setForeground(TEXT);
        showPasswordCheck.setAlignmentX(Component.CENTER_ALIGNMENT);
        showPasswordCheck.addActionListener(e -> {
            passwordField.setEchoChar(
                    showPasswordCheck.isSelected() ? (char) 0 : '*');
        });
        passwordCard.add(Box.createVerticalStrut(6));
        passwordCard.add(showPasswordCheck);

        passwordStrengthBar = new JProgressBar(0, 100);
        passwordStrengthBar.setPreferredSize(new Dimension(280, 12));
        passwordStrengthBar.setMaximumSize(new Dimension(280, 12));
        passwordStrengthBar.setStringPainted(false);
        passwordStrengthBar.setForeground(new Color(100, 116, 139));
        passwordStrengthBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        passwordStrengthLabel = new JLabel("Password strength: Empty");
        passwordStrengthLabel.setForeground(MUTED);
        passwordStrengthLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        passwordCard.add(Box.createVerticalStrut(8));
        passwordCard.add(passwordStrengthBar);
        passwordCard.add(Box.createVerticalStrut(5));
        passwordCard.add(passwordStrengthLabel);

        passwordField.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    public void insertUpdate(javax.swing.event.DocumentEvent e) {
                        updatePasswordStrength();
                    }

                    public void removeUpdate(javax.swing.event.DocumentEvent e) {
                        updatePasswordStrength();
                    }

                    public void changedUpdate(javax.swing.event.DocumentEvent e) {
                        updatePasswordStrength();
                    }
                });

        // Image card
        JPanel imageCard = createCard();
        imageCard.add(centerLabel("Cover Image", 18));
        imageCard.add(Box.createVerticalStrut(10));

        imagePreview = new JLabel("No image selected", SwingConstants.CENTER);
        imagePreview.setPreferredSize(new Dimension(220, 130));
        imagePreview.setMinimumSize(new Dimension(220, 130));
        imagePreview.setMaximumSize(new Dimension(220, 130));
        imagePreview.setForeground(MUTED);
        imagePreview.setBorder(BorderFactory.createLineBorder(BORDER));
        imagePreview.setAlignmentX(Component.CENTER_ALIGNMENT);
        imageCard.add(imagePreview);

        imageInfo = new JLabel("Select a PNG or BMP image");
        imageInfo.setForeground(TEXT);
        imageInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

        capacityLabel = new JLabel("Available capacity: --");
        capacityLabel.setForeground(new Color(94, 234, 212));
        capacityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        capacityBar = new JProgressBar(0, 100);
        capacityBar.setStringPainted(true);
        capacityBar.setString("0%");
        capacityBar.setMaximumSize(new Dimension(450, 20));

        JButton chooseImageButton =
                createButton("Choose Cover Image", new Color(37, 99, 235));
        chooseImageButton.addActionListener(e -> chooseCoverImage());

        imageCard.add(Box.createVerticalStrut(8));
        imageCard.add(imageInfo);
        imageCard.add(Box.createVerticalStrut(5));
        imageCard.add(capacityLabel);
        imageCard.add(Box.createVerticalStrut(5));
        imageCard.add(capacityBar);
        imageCard.add(Box.createVerticalStrut(10));
        imageCard.add(chooseImageButton);

        // Main action
        JButton hideButton =
                createButton("ENCRYPT & HIDE MESSAGE", new Color(5, 150, 105));
        hideButton.setFont(new Font("Arial", Font.BOLD, 17));
        hideButton.setMaximumSize(new Dimension(400, 48));
        hideButton.addActionListener(e -> hideMessage());

        panel.add(messageCard);
        panel.add(Box.createVerticalStrut(12));
        panel.add(passwordCard);
        panel.add(Box.createVerticalStrut(12));
        panel.add(imageCard);
        panel.add(Box.createVerticalStrut(15));
        panel.add(hideButton);

        return panel;
    }

    // ---------------- EXTRACT & DECRYPT TAB ----------------

    private JPanel createExtractTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(35, 30, 30, 30));

        JLabel title = centerLabel("Recover Your Secret Message", 22);

        JLabel description = new JLabel(
                "<html><div style='text-align:center;'>"
                + "Select the received stego image and enter the original "
                + "password to recover the message."
                + "</div></html>");
        description.setForeground(MUTED);
        description.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton extractButton =
                createButton("SELECT IMAGE & DECRYPT",
                        new Color(37, 99, 235));
        extractButton.setFont(new Font("Arial", Font.BOLD, 17));
        extractButton.setMaximumSize(new Dimension(400, 50));
        extractButton.addActionListener(e -> extractMessage());

        JLabel note = new JLabel(
                "AES-GCM authentication checks whether data was altered.");
        note.setForeground(new Color(94, 234, 212));
        note.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalStrut(40));
        panel.add(title);
        panel.add(Box.createVerticalStrut(20));
        panel.add(description);
        panel.add(Box.createVerticalStrut(40));
        panel.add(extractButton);
        panel.add(Box.createVerticalStrut(20));
        panel.add(note);

        return panel;
    }

    // ---------------- GUI HELPERS ----------------

    private JPanel createCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(12, 16, 12, 16)));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));
        return card;
    }

    private JLabel centerLabel(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, size));
        label.setForeground(TEXT);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    
private JButton createButton(String text, Color color) {
    JButton button = new JButton(text);
    button.setFont(new Font("Arial", Font.BOLD, 14));
    button.setBackground(color);
    button.setForeground(Color.WHITE);
    button.setOpaque(true);
    button.setContentAreaFilled(true);
    button.setBorderPainted(false);
    button.setFocusPainted(false);
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    button.setAlignmentX(Component.CENTER_ALIGNMENT);
    button.setBorder(new EmptyBorder(10, 18, 10, 18));
    return button;
}

    private void updateCharacterCount() {
        int count = messageArea.getText().length();
        characterCountLabel.setText("Characters: " + count);
        updateCapacity();
    }

    private void updatePasswordStrength() {
        char[] password = passwordField.getPassword();

        if (password.length == 0) {
            passwordStrengthBar.setValue(0);
            passwordStrengthBar.setForeground(new Color(100, 116, 139));
            passwordStrengthLabel.setText("Password strength: Empty");
            Arrays.fill(password, '\0');
            return;
        }

        int score = 0;
        if (password.length >= 8) score++;
        if (password.length >= 12) score++;

        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;

        for (char c : password) {
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isLowerCase(c)) lower = true;
            else if (Character.isDigit(c)) digit = true;
            else special = true;
        }

        if (upper) score++;
        if (lower) score++;
        if (digit) score++;
        if (special) score++;

        int value = Math.min(100, score * 20);
        passwordStrengthBar.setValue(value);

        if (score <= 2) {
            passwordStrengthLabel.setText("Password strength: Weak");
            passwordStrengthBar.setForeground(new Color(239, 68, 68));
        } else if (score <= 4) {
            passwordStrengthLabel.setText("Password strength: Medium");
            passwordStrengthBar.setForeground(new Color(234, 179, 8));
        } else {
            passwordStrengthLabel.setText("Password strength: Strong");
            passwordStrengthBar.setForeground(new Color(34, 197, 94));
        }

        Arrays.fill(password, '\0');
    }

    private void chooseCoverImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose Cover Image");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "PNG and BMP Images", "png", "bmp"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new IOException("Unsupported or invalid image.");
            }

            coverImagePath = file.getAbsolutePath();

            Image scaled = image.getScaledInstance(
                    220, 130, Image.SCALE_SMOOTH);
            imagePreview.setIcon(new ImageIcon(scaled));
            imagePreview.setText("");

            imageInfo.setText(file.getName() + " | "
                    + image.getWidth() + " x " + image.getHeight());

            long rawCapacity = (long) image.getWidth()
                    * image.getHeight() * 3 / 8;

            long overhead = 4L + MAGIC.length + SALT_SIZE
                    + NONCE_SIZE + 16;

            long usableBytes = Math.max(0, rawCapacity - overhead);

            capacityLabel.setText(
                    "Approx. maximum message: " + usableBytes + " bytes");

            updateCapacity();
            statusLabel.setText("  Cover image selected successfully");

        } catch (Exception ex) {
            coverImagePath = null;
            JOptionPane.showMessageDialog(this,
                    "Unable to load image: " + ex.getMessage(),
                    "Image Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCapacity() {
        if (coverImagePath == null || capacityBar == null
                || messageArea == null) {
            return;
        }

        try {
            BufferedImage image = ImageIO.read(new File(coverImagePath));
            if (image == null) return;

            long totalCapacity = (long) image.getWidth()
                    * image.getHeight() * 3 / 8;

            long overhead = 4L + MAGIC.length + SALT_SIZE
                    + NONCE_SIZE + 16;

            long usable = Math.max(1, totalCapacity - overhead);

            int messageBytes = messageArea.getText()
                    .getBytes(StandardCharsets.UTF_8).length;

            long estimatedPayload = messageBytes + overhead;
            int percentage = (int) Math.min(100,
                    estimatedPayload * 100 / Math.max(1, totalCapacity));

            capacityBar.setValue(percentage);
            capacityBar.setString(percentage + "%");

            if (estimatedPayload > totalCapacity) {
                capacityLabel.setText("Message exceeds image capacity!");
                capacityLabel.setForeground(new Color(248, 113, 113));
            } else {
                capacityLabel.setText(
                        "Approx. available: " + usable + " bytes");
                capacityLabel.setForeground(new Color(94, 234, 212));
            }
        } catch (Exception ex) {
            capacityBar.setValue(0);
            capacityBar.setString("0%");
        }
    }

    // ---------------- KEY GENERATION ----------------

    private static SecretKey generateKey(char[] password, byte[] salt)
            throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
                password, salt, ITERATIONS, KEY_SIZE);

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            try {
                return new SecretKeySpec(keyBytes, "AES");
            } finally {
                Arrays.fill(keyBytes, (byte) 0);
            }
        } finally {
            spec.clearPassword();
        }
    }

    // ---------------- AES ENCRYPTION ----------------

    private static byte[] encryptMessage(String message, char[] password)
            throws Exception {
        byte[] salt = new byte[SALT_SIZE];
        byte[] nonce = new byte[NONCE_SIZE];
        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(nonce);

        SecretKey key = generateKey(password, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key,
                new GCMParameterSpec(TAG_BITS, nonce));

        byte[] ciphertext = cipher.doFinal(
                message.getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(MAGIC);
        output.write(salt);
        output.write(nonce);
        output.write(ciphertext);

        return output.toByteArray();
    }

    // ---------------- AES DECRYPTION ----------------

    private static String decryptMessage(byte[] data, char[] password)
            throws Exception {
        int minimumSize = MAGIC.length + SALT_SIZE + NONCE_SIZE + 16;

        if (data.length < minimumSize) {
            throw new Exception("Invalid or incomplete hidden data.");
        }

        byte[] magic = Arrays.copyOfRange(data, 0, MAGIC.length);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new Exception("No valid hidden message found.");
        }

        int position = MAGIC.length;

        byte[] salt = Arrays.copyOfRange(
                data, position, position + SALT_SIZE);
        position += SALT_SIZE;

        byte[] nonce = Arrays.copyOfRange(
                data, position, position + NONCE_SIZE);
        position += NONCE_SIZE;

        byte[] ciphertext = Arrays.copyOfRange(
                data, position, data.length);

        SecretKey key = generateKey(password, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key,
                new GCMParameterSpec(TAG_BITS, nonce));

        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    // ---------------- STEGANOGRAPHY ----------------

    private static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    private static void hideData(String inputPath, byte[] secretData,
                                 String outputPath) throws Exception {
        BufferedImage image = ImageIO.read(new File(inputPath));
        if (image == null) {
            throw new IOException("Invalid cover image.");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(intToBytes(secretData.length));
        output.write(secretData);
        byte[] allData = output.toByteArray();

        long requiredBits = (long) allData.length * 8;
        long capacityBits = (long) width * height * 3;

        if (requiredBits > capacityBits) {
            throw new Exception(
                    "Message is too large for this image. "
                    + "Choose a larger image or shorten the message.");
        }

        int bitIndex = 0;

        for (int y = 0; y < height && bitIndex < requiredBits; y++) {
            for (int x = 0; x < width && bitIndex < requiredBits; x++) {
                int rgb = image.getRGB(x, y);

                int alpha = (rgb >>> 24) & 255;
                int[] channels = {
                        (rgb >>> 16) & 255,
                        (rgb >>> 8) & 255,
                        rgb & 255
                };

                for (int c = 0; c < 3 && bitIndex < requiredBits; c++) {
                    int byteIndex = bitIndex / 8;
                    int bitPosition = 7 - (bitIndex % 8);
                    int bit = ((allData[byteIndex] & 255)
                            >>> bitPosition) & 1;

                    channels[c] = (channels[c] & 0xFE) | bit;
                    bitIndex++;
                }

                int newRgb = (alpha << 24)
                        | (channels[0] << 16)
                        | (channels[1] << 8)
                        | channels[2];

                image.setRGB(x, y, newRgb);
            }
        }

        if (!ImageIO.write(image, "png", new File(outputPath))) {
            throw new IOException("Could not save output as PNG.");
        }
    }

    private static byte[] extractData(String imagePath) throws Exception {
        BufferedImage image = ImageIO.read(new File(imagePath));
        if (image == null) {
            throw new IOException("Invalid image file.");
        }

        int width = image.getWidth();
        int height = image.getHeight();
        long capacityBits = (long) width * height * 3;

        if (capacityBits < 32) {
            throw new Exception("Image is too small.");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int currentByte = 0;
        int bitCount = 0;
        int totalBytesNeeded = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                int[] channels = {
                        (rgb >>> 16) & 255,
                        (rgb >>> 8) & 255,
                        rgb & 255
                };

                for (int channel : channels) {
                    currentByte = (currentByte << 1) | (channel & 1);
                    bitCount++;

                    if (bitCount == 8) {
                        output.write(currentByte);
                        currentByte = 0;
                        bitCount = 0;

                        byte[] extracted = output.toByteArray();

                        if (extracted.length == 4) {
                            int dataLength = ByteBuffer.wrap(extracted).getInt();

                            if (dataLength < MAGIC.length + SALT_SIZE
                                    + NONCE_SIZE + 16
                                    || (long) dataLength * 8 + 32
                                    > capacityBits) {
                                throw new Exception(
                                        "No valid hidden data found.");
                            }

                            totalBytesNeeded = 4 + dataLength;
                        }

                        if (totalBytesNeeded > 0
                                && extracted.length == totalBytesNeeded) {
                            return Arrays.copyOfRange(
                                    extracted, 4, extracted.length);
                        }
                    }
                }
            }
        }

        throw new Exception("Incomplete or missing hidden data.");
    }

    // ---------------- HIDE MESSAGE ACTION ----------------

    private void hideMessage() {
        String message = messageArea.getText().trim();
        char[] password = passwordField.getPassword();

        if (message.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a secret message.");
            Arrays.fill(password, '\0');
            return;
        }

        if (password.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a password.");
            return;
        }

        if (coverImagePath == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a cover image first.");
            Arrays.fill(password, '\0');
            return;
        }

        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setDialogTitle("Save Stego Image");
        saveChooser.setSelectedFile(new File("stego_image.png"));
        saveChooser.setFileFilter(new FileNameExtensionFilter(
                "PNG Image", "png"));

        try {
            if (saveChooser.showSaveDialog(this)
                    != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File outputFile = saveChooser.getSelectedFile();
            String outputPath = outputFile.getAbsolutePath();

            if (!outputPath.toLowerCase().endsWith(".png")) {
                outputPath += ".png";
            }

            byte[] encryptedData = encryptMessage(message, password);
            hideData(coverImagePath, encryptedData, outputPath);

            statusLabel.setText("  Message encrypted and hidden successfully");
            JOptionPane.showMessageDialog(this,
                    "Message encrypted and hidden successfully!\n\n"
                    + "Saved to:\n" + outputPath,
                    "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error: " + ex.getMessage(),
                    "Operation Failed", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    // ---------------- EXTRACT MESSAGE ACTION ----------------

    
private void extractMessage() {
    // Ask for the original password
    JPasswordField passwordInput = new JPasswordField();

    Object[] prompt = {
        "Enter the original password:",
        passwordInput
    };

    int choice = JOptionPane.showConfirmDialog(
        this,
        prompt,
        "Password Required",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE
    );

    // Stop if the user cancels
    if (choice != JOptionPane.OK_OPTION) {
        return;
    }

    char[] password = passwordInput.getPassword();

    if (password.length == 0) {
        JOptionPane.showMessageDialog(
            this, "Please enter the password.");
        Arrays.fill(password, '\0');
        return;
    }

    try {
        // Select the image containing the hidden message
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Stego Image");
        chooser.setFileFilter(
            new FileNameExtensionFilter(
                "PNG and BMP Images", "png", "bmp"));

        if (chooser.showOpenDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File imageFile = chooser.getSelectedFile();

        byte[] encryptedData =
            extractData(imageFile.getAbsolutePath());

        // Decrypt only if the password is correct
        String originalMessage =
            decryptMessage(encryptedData, password);

        statusLabel.setText(
            "  Message extracted successfully");

        JTextArea resultArea =
            new JTextArea(originalMessage, 8, 35);

        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setEditable(false);
        resultArea.setFont(
            new Font("Arial", Font.PLAIN, 15));

        JScrollPane resultScroll =
            new JScrollPane(resultArea);

        JOptionPane.showMessageDialog(
            this,
            resultScroll,
            "Decrypted Message",
            JOptionPane.INFORMATION_MESSAGE);

    } catch (Exception ex) {
        statusLabel.setText("  Decryption failed");

        JOptionPane.showMessageDialog(
            this,
            "Incorrect password or invalid image. "
                + "The hidden message could not be recovered.",
            "Decryption Failed",
            JOptionPane.ERROR_MESSAGE);

    } finally {
        Arrays.fill(password, '\0');
    }
}

    // ---------------- MAIN ----------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Use default Swing look and feel.
            }

            SecureSteganography app = new SecureSteganography();
            app.setVisible(true);
        });
    }
}
