package org.jwellman.console.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

import org.jwellman.console.CommandHistory;
import org.jwellman.console.CompletionProvider;
import org.jwellman.console.ConsoleTheme;
import org.jwellman.console.InterpreterAdapter;
import org.jwellman.console.impl.DefaultCommandHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic console/REPL component that works with any InterpreterAdapter.
 *
 * <p>Provides a JTextPane-based console with:</p>
 * <ul>
 *   <li>Command history (up/down arrows)</li>
 *   <li>Tab completion</li>
 *   <li>Cut/copy/paste with protected output area</li>
 *   <li>Customizable themes</li>
 *   <li>Piped I/O streams for interpreter integration</li>
 * </ul>
 *
 * <p>Based on the proven JConsole implementation but abstracted
 * to work with any interpreter, not just BeanShell.</p>
 *
 * @author Rick Wellman
 */
public class GenericConsole extends JScrollPane implements
        Runnable,
        KeyListener,
        MouseListener,
        ActionListener,
        PropertyChangeListener {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(GenericConsole.class);

    private static final String CUT = "Cut";
    private static final String COPY = "Copy";
    private static final String PASTE = "Paste";
    private static final int SHOW_AMBIG_MAX = 10;

    // UI Components
    private ConsoleTextPane text;
    private JPopupMenu menu;

    // Interpreter and completion
    private InterpreterAdapter interpreter;
    private CompletionProvider completionProvider;

    // History management
    private CommandHistory history;
    private String startedLine;

    // Theme
    private ConsoleTheme theme;

    // I/O streams
    private InputStream in;
    private PrintStream out;
    private InputStream inPipe;
    private OutputStream outPipe;

    // State
    private int cmdStart = 0;
    private boolean gotUp = true;  // Hack to prevent key repeat issues
    private volatile boolean running = true;

    // Flush synchronization: sentinel travels through the same pipe as output,
    // guaranteeing it is ordered after all preceding command output.
    private static final String FLUSH_SENTINEL = "^F.L.U.S.H.$";
    private volatile CountDownLatch flushLatch = null;

    /**
     * Create a console with default theme.
     */
    public GenericConsole() {
        this(null, ConsoleTheme.classic());
    }

    /**
     * Create a console with specified interpreter adapter.
     *
     * @param interpreter the interpreter adapter
     */
    public GenericConsole(InterpreterAdapter interpreter) {
        this(interpreter, ConsoleTheme.classic());
    }

    /**
     * Create a console with specified theme.
     *
     * @param interpreter the interpreter adapter (or null for I/O only mode)
     * @param theme the visual theme
     */
    public GenericConsole(InterpreterAdapter interpreter, ConsoleTheme theme) {
        super();

        this.interpreter = interpreter;
        this.theme = theme != null ? theme : ConsoleTheme.classic();
        this.history = new DefaultCommandHistory();

        if (interpreter != null) {
            this.completionProvider = interpreter.getCompletionProvider();
        }

        initializeUI();
        initializeStreams(null, null);
        startInputWatcher();
    }

    /**
     * Create a console with existing I/O streams.
     *
     * @param cin input stream
     * @param cout output stream
     * @param theme the visual theme
     */
    public GenericConsole(InputStream cin, OutputStream cout, ConsoleTheme theme) {
        super();

        this.interpreter = null;
        this.theme = theme != null ? theme : ConsoleTheme.classic();
        this.history = new DefaultCommandHistory();

        initializeUI();
        initializeStreams(cin, cout);
        startInputWatcher();
    }

    private void initializeUI() {
        // Create the text pane with callbacks
        text = new ConsoleTextPane();
        text.setCommandStartProvider(new ConsoleTextPane.CommandStartProvider() {
            @Override
            public int getCommandStart() {
                return cmdStart;
            }
        });
        text.setForceCaretToEnd(new Runnable() {
            @Override
            public void run() {
                forceCaretMoveToEnd();
            }
        });

        // Apply theme
        text.applyTheme(theme);

        // Add listeners
        text.addKeyListener(this);
        text.addMouseListener(this);

        setViewportView(text);

        // Create popup menu
        menu = new JPopupMenu("Console Menu");
        menu.add(new JMenuItem(CUT)).addActionListener(this);
        menu.add(new JMenuItem(COPY)).addActionListener(this);
        menu.add(new JMenuItem(PASTE)).addActionListener(this);

        // Track Look & Feel changes
        UIManager.addPropertyChangeListener(this);
    }

    private void initializeStreams(InputStream cin, OutputStream cout) {
        outPipe = cout;
        if (outPipe == null) {
            outPipe = new PipedOutputStream();
            try {
                in = new PipedInputStream((PipedOutputStream) outPipe, 64 * 1024);
            } catch (IOException e) {
                print("Console internal error (output pipe)...", theme.getErrorColor());
            }
        }

        inPipe = cin;
        if (inPipe == null) {
            PipedOutputStream pout = new PipedOutputStream();
            try {
                out = new PrintStream(pout, true, "UTF-8");
                inPipe = new BlockingPipedInputStream(pout);
            } catch (IOException e) {
                print("Console internal error (input pipe): " + e, theme.getErrorColor());
            }
        }
    }

    private void startInputWatcher() {
        Thread watcherThread = new Thread(this, "Console-InputWatcher");
        watcherThread.setDaemon(true);
        watcherThread.start();

        requestFocus();
    }

    // ========== Public API ==========

    /**
     * Set the interpreter adapter.
     *
     * @param interpreter the interpreter
     */
    public void setInterpreter(InterpreterAdapter interpreter) {
        this.interpreter = interpreter;
        if (interpreter != null) {
            this.completionProvider = interpreter.getCompletionProvider();
        }
    }

    /**
     * Get the interpreter adapter.
     *
     * @return the interpreter, or null
     */
    public InterpreterAdapter getInterpreter() {
        return interpreter;
    }

    /**
     * Set the completion provider.
     *
     * @param provider the completion provider
     */
    public void setCompletionProvider(CompletionProvider provider) {
        this.completionProvider = provider;
    }

    /**
     * Set the command history.
     *
     * @param history the history implementation
     */
    public void setHistory(CommandHistory history) {
        this.history = history;
    }

    /**
     * Apply a new theme.
     *
     * @param theme the theme to apply
     */
    public void setTheme(ConsoleTheme theme) {
        this.theme = theme;
        text.applyTheme(theme);
    }

    /**
     * Get the input stream for reading commands.
     *
     * @return the input stream
     */
    public InputStream getInputStream() {
        return in;
    }

    /**
     * Get the output stream for writing output.
     *
     * @return the print stream
     */
    public PrintStream getOut() {
        return out;
    }

    /**
     * Get the error stream (same as output in this implementation).
     *
     * @return the print stream
     */
    public PrintStream getErr() {
        return out;
    }

    /**
     * Get the text pane for direct manipulation.
     *
     * @return the text pane
     */
    public ConsoleTextPane getTextPane() {
        return text;
    }

    /**
     * Clear the console.
     */
    public void clear() {
        invokeAndWait(new Runnable() {
            @Override
            public void run() {
                text.setText("");
                cmdStart = 0;
            }
        });
    }

    /**
     * Block until all output previously written to {@code out} has been
     * displayed by the InputWatcher thread.
     *
     * <p>A sentinel string is written into the same pipe that command output
     * travels through. Because the pipe is FIFO, the sentinel cannot overtake
     * any output written before it. When the InputWatcher finds and removes the
     * sentinel, it signals this method that all preceding output is on-screen.</p>
     *
     * <p>Must not be called from the EDT: the InputWatcher posts to the EDT via
     * invokeAndWait, so blocking the EDT here would deadlock.</p>
     */
    private void flushOutput() {
        if (SwingUtilities.isEventDispatchThread()) {
            return;
        }
        if (out == null) {
            return;
        }
        final CountDownLatch latch = new CountDownLatch(1);
        // Volatile write BEFORE writing to the pipe. JMM volatile happens-before
        // guarantees the InputWatcher sees this non-null value when it reads
        // flushLatch after finding the sentinel in the pipe.
        flushLatch = latch;
        out.print(FLUSH_SENTINEL);
        try {
            boolean signalled = latch.await(2, TimeUnit.SECONDS);
            if (!signalled) {
                logger.warn("flushOutput() timed out; sentinel may have been lost");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            flushLatch = null;
        }
    }

    /**
     * Print the prompt.
     */
    public void printPrompt() {
        if (interpreter != null) {
            flushOutput();
            print(interpreter.getPrompt(), theme.getPromptColor());
        }
    }

    /**
     * Shutdown the console.
     */
    public void shutdown() {
        running = false;
        if (interpreter != null) {
            interpreter.shutdown();
        }
        try {
            if (in != null) in.close();
            if (inPipe != null) inPipe.close();
            if (out != null) out.close();
            if (outPipe != null) outPipe.close();
        } catch (IOException e) {
            // Ignore shutdown errors
        }
    }

    // ========== Output Methods ==========

    /**
     * Print text with default color.
     *
     * @param o the object to print
     */
    public void print(final Object o) {
        invokeAndWait(new Runnable() {
            @Override
            public void run() {
                append(String.valueOf(o));
                resetCommandStart();
                text.setCaretPosition(cmdStart);
            }
        });
    }

    /**
     * Print text with newline.
     *
     * @param o the object to print
     */
    public void println(Object o) {
        print(String.valueOf(o) + "\n");
        text.repaint();
    }

    /**
     * Print a newline.
     */
    public void println() {
        print("\n");
        text.repaint();
    }

    /**
     * Print text with specified color.
     *
     * @param o the object to print
     * @param color the color
     */
    public void print(final Object o, final Color color) {
        invokeAndWait(new Runnable() {
            @Override
            public void run() {
                AttributeSet old = text.getCurrentStyle();
                text.setTextStyle(color);
                append(String.valueOf(o));
                resetCommandStart();
                text.setCaretPosition(cmdStart);
                text.restoreStyle(old);
            }
        });
    }

    /**
     * Print text with specified font and color.
     *
     * @param o the object to print
     * @param font the font
     * @param color the color
     */
    public void print(final Object o, final Font font, final Color color) {
        invokeAndWait(new Runnable() {
            @Override
            public void run() {
                AttributeSet old = text.getCurrentStyle();
                text.setTextStyle(font, color);
                append(String.valueOf(o));
                resetCommandStart();
                text.setCaretPosition(cmdStart);
                text.restoreStyle(old);
            }
        });
    }

    /**
     * Print an error message.
     *
     * @param o the error message
     */
    public void error(Object o) {
        print(o, theme.getErrorColor());
    }

    /**
     * Print an icon.
     *
     * @param icon the icon
     */
    public void print(final Icon icon) {
        if (icon == null) return;

        invokeAndWait(new Runnable() {
            @Override
            public void run() {
                text.insertIcon(icon);
                resetCommandStart();
                text.setCaretPosition(cmdStart);
            }
        });
    }

    /**
     * Set wait cursor feedback.
     *
     * @param on true to show wait cursor
     */
    public void setWaitFeedback(boolean on) {
        if (on) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    // ========== KeyListener ==========

    @Override
    public void keyPressed(KeyEvent e) {
        type(e);
        gotUp = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        type(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        gotUp = true;
        type(e);
    }

    private synchronized void type(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ENTER:
                if (e.getID() == KeyEvent.KEY_PRESSED && gotUp) {
                    enter();
                    resetCommandStart();
                    text.setCaretPosition(cmdStart);
                }
                e.consume();
                text.repaint();
                break;

            case KeyEvent.VK_UP:
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    historyUp();
                }
                e.consume();
                break;

            case KeyEvent.VK_DOWN:
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    historyDown();
                }
                e.consume();
                break;

            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_BACK_SPACE:
            case KeyEvent.VK_DELETE:
                if (text.getCaretPosition() <= cmdStart) {
                    e.consume();
                }
                break;

            case KeyEvent.VK_RIGHT:
                forceCaretMoveToStart();
                break;

            case KeyEvent.VK_HOME:
                text.setCaretPosition(cmdStart);
                e.consume();
                break;

            case KeyEvent.VK_U:  // Ctrl-U: clear line
                if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) > 0) {
                    replaceRange("", cmdStart, textLength());
                    if (history instanceof DefaultCommandHistory) {
                        ((DefaultCommandHistory) history).saveCurrentLine("");
                    }
                    history.reset();
                    e.consume();
                }
                break;

            case KeyEvent.VK_ALT:
            case KeyEvent.VK_CAPS_LOCK:
            case KeyEvent.VK_CONTROL:
            case KeyEvent.VK_META:
            case KeyEvent.VK_SHIFT:
            case KeyEvent.VK_PRINTSCREEN:
            case KeyEvent.VK_SCROLL_LOCK:
            case KeyEvent.VK_PAUSE:
            case KeyEvent.VK_INSERT:
            case KeyEvent.VK_F1:
            case KeyEvent.VK_F2:
            case KeyEvent.VK_F3:
            case KeyEvent.VK_F4:
            case KeyEvent.VK_F5:
            case KeyEvent.VK_F6:
            case KeyEvent.VK_F7:
            case KeyEvent.VK_F8:
            case KeyEvent.VK_F9:
            case KeyEvent.VK_F10:
            case KeyEvent.VK_F11:
            case KeyEvent.VK_F12:
            case KeyEvent.VK_ESCAPE:
                // Modifier keys only - ignore
                break;

            case KeyEvent.VK_C:  // Ctrl-C
                if (text.getSelectedText() == null) {
                    if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) > 0
                            && e.getID() == KeyEvent.KEY_PRESSED) {
                        append("^C");
                    }
                    e.consume();
                }
                break;

            case KeyEvent.VK_TAB:
                if (e.getID() == KeyEvent.KEY_RELEASED) {
                    String fullText = text.getText();
                    if (fullText != null) {
                        doCommandCompletion(fullText.substring(cmdStart));
                    }
                }
                e.consume();
                break;

            default:
                if ((e.getModifiersEx() &
                        (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK)) == 0) {
                    forceCaretMoveToEnd();
                }

                // Handle backspace that slips through
                if (e.paramString().contains("Backspace") && text.getCaretPosition() <= cmdStart) {
                    e.consume();
                }
                break;
        }
    }

    // ========== Command Processing ==========

    private void enter() {
        String cmd = getCmd();

        if (cmd.length() == 0) {
            cmd = ";\n";
        } else {
            history.add(cmd);
            cmd = cmd + "\n";
        }

        append("\n");
        history.reset();
        acceptLine(cmd);
        text.repaint();
    }

    private String getCmd() {
        try {
            String cmd = text.getText(cmdStart, textLength() - cmdStart);
            // Support ".." shorthand for method invocation
            if (cmd.endsWith("..")) {
                cmd = cmd.substring(0, cmd.length() - 2) + "();";
            }
            return cmd;
        } catch (BadLocationException e) {
            logger.error("Internal GenericConsole Error", e);
            return "";
        }
    }

    private void acceptLine(String line) {
        // Handle Unicode characters
        final String ZEROS = "000";
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c > 127) {
                String val = Integer.toString(c, 16);
                val = ZEROS.substring(0, 4 - val.length()) + val;
                buf.append("\\u").append(val);
            } else {
                buf.append(c);
            }
        }
        line = buf.toString();

        if (outPipe == null) {
            print("Console internal error: cannot output...", theme.getErrorColor());
        } else {
            try {
                outPipe.write(line.getBytes(StandardCharsets.UTF_8));
                outPipe.flush();
            } catch (IOException e) {
                outPipe = null;
                throw new RuntimeException("Console pipe broken...");
            }
        }
    }

    // ========== History Navigation ==========

    private void historyUp() {
        if (history.size() == 0) return;

        // Save current line on first navigation
        if (history instanceof DefaultCommandHistory) {
            DefaultCommandHistory dh = (DefaultCommandHistory) history;
            String currentCmd = getCmd();
            dh.saveCurrentLine(currentCmd);
        } else {
            startedLine = getCmd();
        }

        String prev = history.getPrevious();
        if (prev != null) {
            showHistoryLine(prev);
        }
    }

    private void historyDown() {
        String next = history.getNext();
        if (next != null) {
            showHistoryLine(next);
        } else if (startedLine != null) {
            showHistoryLine(startedLine);
        }
    }

    private void showHistoryLine(String line) {
        replaceRange(line, cmdStart, textLength());
        text.setCaretPosition(textLength());
        text.repaint();
    }

    // ========== Tab Completion ==========

    private void doCommandCompletion(String part) {
        if (completionProvider == null) {
            return;
        }

        // Extract the identifier part to complete
        int i = part.length() - 1;
        while (i >= 0) {
            char c = part.charAt(i);
            if (Character.isJavaIdentifierPart(c) || c == '.') {
                i--;
            } else {
                break;
            }
        }
        part = part.substring(i + 1);

        if (part.length() < 2) {
            return;  // Need minimum length
        }

        String[] completions = completionProvider.complete(part);
        if (completions == null || completions.length == 0) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            return;
        }

        // Single completion
        if (completions.length == 1 && !completions[0].equals(part)) {
            String toAppend = completions[0].substring(part.length());
            append(toAppend);
            return;
        }

        // Multiple completions - show them
        String fullText = text.getText();
        if (fullText == null) fullText = "";
        final String command = fullText.substring(cmdStart);

        // Find prompt
        int promptStart = cmdStart;
        for (i = cmdStart; i > 0 && fullText.charAt(i) != '\n'; i--) {
            promptStart = i;
        }
        if (promptStart > 0) promptStart++;
        final String prompt = fullText.substring(promptStart, cmdStart);

        // Show ambiguous completions
        StringBuilder sb = new StringBuilder("\n");
        for (i = 0; i < completions.length && i < SHOW_AMBIG_MAX; i++) {
            sb.append(completions[i]).append("\n");
        }
        if (i == SHOW_AMBIG_MAX) {
            sb.append("...\n");
        }

        print(sb, theme.getOutputColor());
        print(prompt);
        append(command);
    }

    // ========== Input Watcher Thread ==========

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[256];
            int read;
            StringBuilder acc = new StringBuilder();
            while (running && (read = inPipe.read(buffer)) != -1) {
                acc.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                processBuffer(acc);
            }
            if (acc.length() > 0) {
                print(acc.toString());
            }
        } catch (IOException e) {
            if (running) {
                print("Console: I/O Error: " + e + "\n", theme.getErrorColor());
            }
        }
        println("Console: Input closed...");
    }

    /**
     * Drain any complete flush sentinels from {@code acc}, signalling the
     * waiting {@code flushOutput()} caller for each one found, then print
     * all content that cannot be the start of a partial sentinel at the tail.
     *
     * <p>Rather than blindly withholding the last N chars (where N = sentinel
     * length - 1), we only hold back the tail that is actually a prefix of
     * {@link #FLUSH_SENTINEL}.  Normal output characters that don't begin the
     * sentinel sequence are printed immediately, so short results like
     * {@code "hello\n"} are never delayed until the next read.</p>
     */
    private void processBuffer(StringBuilder acc) {
        while (true) {
            int idx = acc.indexOf(FLUSH_SENTINEL);
            if (idx == -1) {
                break;
            }
            if (idx > 0) {
                print(acc.substring(0, idx));
            }
            // Read flushLatch once to avoid TOCTOU with flushOutput()'s finally block.
            CountDownLatch latch = flushLatch;
            if (latch != null) {
                latch.countDown();
            }
            acc.delete(0, idx + FLUSH_SENTINEL.length());
        }
        // Hold back only the tail that is a genuine prefix of the sentinel,
        // so a sentinel straddling two reads is still detected correctly.
        int hold = sentinelPrefixTailLength(acc);
        int safeLength = acc.length() - hold;
        if (safeLength > 0) {
            print(acc.substring(0, safeLength));
            acc.delete(0, safeLength);
        }
    }

    /**
     * Returns the length of the longest suffix of {@code acc} that is also a
     * non-empty proper prefix of {@link #FLUSH_SENTINEL}.  That suffix must be
     * kept in the accumulator so that, if the next read delivers the remainder
     * of the sentinel, the two pieces can be joined and recognised.
     */
    private static int sentinelPrefixTailLength(StringBuilder acc) {
        int maxLen = Math.min(acc.length(), FLUSH_SENTINEL.length() - 1);
        for (int len = maxLen; len > 0; len--) {
            if (FLUSH_SENTINEL.startsWith(acc.substring(acc.length() - len))) {
                return len;
            }
        }
        return 0;
    }

    // ========== Helper Methods ==========

    private void resetCommandStart() {
        cmdStart = textLength();
    }

    private void append(String string) {
        int len = textLength();
        text.select(len, len);
        text.replaceSelection(string);
    }

    private void replaceRange(String s, int start, int end) {
        text.select(start, end);
        text.replaceSelection(s);
    }

    private void forceCaretMoveToEnd() {
        if (text.getCaretPosition() < cmdStart) {
            text.setCaretPosition(textLength());
        }
        text.repaint();
    }

    private void forceCaretMoveToStart() {
        text.repaint();
    }

    private int textLength() {
        return text.getDocument().getLength();
    }

    private void invokeAndWait(Runnable run) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(run);
            } catch (InterruptedException | InvocationTargetException e) {
                logger.error("Error in invokeAndWait", e);
            }
        } else {
            run.run();
        }
    }

    // ========== MouseListener ==========

    @Override
    public void mousePressed(MouseEvent event) {
        if (event.isPopupTrigger()) {
            menu.show((Component) event.getSource(), event.getX(), event.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (event.isPopupTrigger()) {
            menu.show((Component) event.getSource(), event.getX(), event.getY());
        }
        text.repaint();
    }

    @Override
    public void mouseEntered(MouseEvent event) {}

    @Override
    public void mouseExited(MouseEvent event) {}

    @Override
    public void mouseClicked(MouseEvent event) {}

    // ========== PropertyChangeListener ==========

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if ("lookAndFeel".equals(event.getPropertyName())) {
            SwingUtilities.updateComponentTreeUI(menu);
        }
    }

    // ========== ActionListener ==========

    @Override
    public void actionPerformed(ActionEvent event) {
        String cmd = event.getActionCommand();
        if (CUT.equals(cmd)) {
            text.cut();
        } else if (COPY.equals(cmd)) {
            text.copy();
        } else if (PASTE.equals(cmd)) {
            text.paste();
        }
    }

    // ========== Focus ==========

    @Override
    public void requestFocus() {
        super.requestFocus();
        text.requestFocus();
    }

    @Override
    public String toString() {
        if (interpreter != null) {
            return interpreter.getName() + " Console";
        }
        return "Generic Console";
    }

}
