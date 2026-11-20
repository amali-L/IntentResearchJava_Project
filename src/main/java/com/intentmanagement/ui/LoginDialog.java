package com.intentmanagement.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Red/black, full-screen authentication flow for the local research prototype. */
public final class LoginDialog {
    private static final Color BG = new Color(10, 10, 13);
    private static final Color PANEL = new Color(24, 24, 31);
    private static final Color FIELD = new Color(33, 35, 44);
    private static final Color RED = new Color(220, 38, 55);
    private static final Color RED_BRIGHT = new Color(255, 70, 85);
    private static final Color TEXT = new Color(245, 247, 252);
    private static final Color MUTED = new Color(190, 200, 215);

    private final AuthService auth = new AuthService();
    private final Map<String, String> resetCodes = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final JFrame frame = new JFrame("IntentOps | Secure Access");
    private Runnable onAuthenticated = () -> {};
    private boolean authenticated;

    /** Show the authentication window; the callback runs only after a successful sign-in. */
    public void show(Runnable onAuthenticated) {
        this.onAuthenticated = onAuthenticated == null ? () -> {} : onAuthenticated;
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1000, 700));
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);
        showLogin();
        frame.setVisible(true);
    }

    private void shell(String title, String subtitle, JPanel form) {
        JPanel root = new JPanel(new BorderLayout(24, 28));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(34, 48, 38, 48));

        JLabel brand = new JLabel("INTENTOPS  /  ACCESS CONTROL");
        brand.setForeground(RED_BRIGHT);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 16));
        JLabel heading = new JLabel(title);
        heading.setForeground(TEXT);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 34));
        JLabel sub = new JLabel(subtitle);
        sub.setForeground(MUTED);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 18));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(brand);
        header.add(Box.createVerticalStrut(18));
        header.add(heading);
        header.add(Box.createVerticalStrut(8));
        header.add(sub);
        root.add(header, BorderLayout.NORTH);

        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(10, 0, 10, 0));

        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(75, 45, 54), 1),
                new EmptyBorder(24, 28, 24, 28)));
        card.add(form, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(760, Math.max(390, form.getComponentCount() * 66 + 90)));
        card.setMaximumSize(new Dimension(820, 760));

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        center.add(card, gbc);
        root.add(center, BorderLayout.CENTER);

        JLabel footer = new JLabel("LOCAL PROTOTYPE  •  PBKDF2 PASSWORD HASHING  •  INTENTOPS RESEARCH");
        footer.setForeground(new Color(125, 135, 150));
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        root.add(footer, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.revalidate();
        frame.repaint();
    }

    private JPanel fieldGroup(String label) {
        JPanel group = new JPanel();
        group.setOpaque(false);
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        JLabel caption = new JLabel(label);
        caption.setForeground(TEXT);
        caption.setFont(new Font("Segoe UI", Font.BOLD, 16));
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(caption);
        group.add(Box.createVerticalStrut(8));
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        return group;
    }

    private JTextField field(JPanel parent, String label) {
        JPanel group = fieldGroup(label);
        JTextField input = new JTextField();
        styleField(input);
        group.add(input);
        group.add(Box.createVerticalStrut(14));
        parent.add(group);
        return input;
    }

    private JPasswordField password(JPanel parent, String label) {
        JPanel group = fieldGroup(label);
        JPasswordField input = new JPasswordField();
        styleField(input);
        group.add(input);
        group.add(Box.createVerticalStrut(14));
        parent.add(group);
        return input;
    }

    private void styleField(JTextField input) {
        input.setBackground(FIELD);
        input.setForeground(TEXT);
        input.setCaretColor(RED_BRIGHT);
        input.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 58, 68)),
                new EmptyBorder(11, 13, 11, 13)));
        input.setPreferredSize(new Dimension(680, 50));
        input.setMinimumSize(new Dimension(300, 48));
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        input.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private JButton button(String text, boolean primary, Runnable action) {
        JButton button = new JButton(text);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBackground(primary ? RED : new Color(38, 40, 50));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? RED_BRIGHT : new Color(85, 90, 105)),
                new EmptyBorder(13, 16, 13, 16)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        button.addActionListener(e -> action.run());
        return button;
    }

    private void gap(JPanel p, int height) { p.add(Box.createVerticalStrut(height)); }

    private void showLogin() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        JTextField email = field(form, "EMAIL ADDRESS");
        JPasswordField pass = password(form, "PASSWORD");
        gap(form, 4);
        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        actions.setOpaque(false);
        actions.add(button("SIGN IN", true, () -> {
            if (auth.login(email.getText(), pass.getPassword())) {
                authenticated = true;
                frame.dispose();
                onAuthenticated.run();
            } else {
                JOptionPane.showMessageDialog(frame,
                        "Email or password is incorrect. If you have not registered after installing this version, create an account first.",
                        "Sign-in failed", JOptionPane.ERROR_MESSAGE);
            }
        }));
        actions.add(button("CREATE ACCOUNT", false, this::showSignup));
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        form.add(actions);
        gap(form, 12);
        JButton forgot = button("FORGOT PASSWORD?", false, this::showForgot);
        form.add(forgot);
        shell("Welcome back", "Sign in to open the IntentOps research console", form);
    }

    private void showSignup() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        JTextField name = field(form, "FULL NAME");
        JTextField email = field(form, "EMAIL ADDRESS");
        JPasswordField p1 = password(form, "PASSWORD (MINIMUM 8 CHARACTERS)");
        JPasswordField p2 = password(form, "CONFIRM PASSWORD");
        form.add(button("CREATE ACCOUNT", true, () -> {
            char[] first = p1.getPassword();
            char[] second = p2.getPassword();
            if (!java.util.Arrays.equals(first, second)) {
                JOptionPane.showMessageDialog(frame, "Passwords do not match.", "Check password", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!auth.signup(name.getText(), email.getText(), first)) {
                JOptionPane.showMessageDialog(frame,
                        "Account was not created. Check the name, email format, password length (8+ characters), and whether the email is already registered.",
                        "Sign-up failed", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(frame, "Account created successfully. Sign in using the same email and password.");
            showLogin();
        }));
        gap(form, 10);
        form.add(button("BACK TO SIGN IN", false, this::showLogin));
        shell("Create your account", "Local prototype account • salted PBKDF2 password hashing", form);
    }

    private void showForgot() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        JTextField email = field(form, "REGISTERED EMAIL ADDRESS");
        form.add(button("GENERATE RESET CODE", true, () -> {
            String normalized = email.getText().trim().toLowerCase(Locale.ROOT);
            if (!auth.exists(normalized)) {
                JOptionPane.showMessageDialog(frame, "No account was found for this email. Create an account first.");
                return;
            }
            String code = String.format("%06d", random.nextInt(1_000_000));
            resetCodes.put(normalized, code);
            JOptionPane.showMessageDialog(frame,
                    "Prototype reset code: " + code + "\nEmail delivery is not configured in this local prototype.",
                    "Password reset code", JOptionPane.INFORMATION_MESSAGE);
            showReset(normalized);
        }));
        gap(form, 10);
        form.add(button("BACK TO SIGN IN", false, this::showLogin));
        shell("Recover access", "Generate a one-time local prototype code to reset your password", form);
    }

    private void showReset(String email) {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        JTextField code = field(form, "RESET CODE");
        JPasswordField p1 = password(form, "NEW PASSWORD (MINIMUM 8 CHARACTERS)");
        JPasswordField p2 = password(form, "CONFIRM NEW PASSWORD");
        form.add(button("RESET PASSWORD", true, () -> {
            String expected = resetCodes.get(email);
            if (expected == null || !expected.equals(code.getText().trim())) {
                JOptionPane.showMessageDialog(frame, "The reset code is invalid. Generate a new code and try again.");
                return;
            }
            char[] first = p1.getPassword();
            char[] second = p2.getPassword();
            if (!java.util.Arrays.equals(first, second) || first.length < 8) {
                JOptionPane.showMessageDialog(frame, "Passwords must match and contain at least 8 characters.");
                return;
            }
            if (auth.reset(email, first)) {
                resetCodes.remove(email);
                JOptionPane.showMessageDialog(frame, "Password changed successfully. Sign in with the new password.");
                showLogin();
            } else {
                JOptionPane.showMessageDialog(frame, "Password could not be reset. Please try again.");
            }
        }));
        gap(form, 10);
        form.add(button("BACK TO SIGN IN", false, this::showLogin));
        shell("Reset password", "Choose a new password for your account", form);
    }
}
