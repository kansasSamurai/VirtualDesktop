package org.jwellman.swing.grid;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * A pooled SmartGrid row panel for displaying raw log data (plain text, JSON,
 * or abbreviated stack traces) with targeted syntax highlighting and live search
 * term highlighting.  Designed for use with SmartGrid's dark theme.
 *
 * Usage: register via grid.registerRowRenderer("log-row", () -> new LogRowPanel(...))
 * and tag every GridRow with .setTag("fnd-type", "log-row").
 */
public class LogRowPanel extends JPanel implements Recyclable, Selectable {

    // -------------------------------------------------------------------------
    // Color palette — class-level constants, computed once
    // -------------------------------------------------------------------------

    private static final Color BG_EVEN       = new Color(30, 30, 30);
    private static final Color BG_ODD        = new Color(38, 38, 38);
    private static final Color BG_SELECTED   = new Color(0x2D5A8E);
    private static final Color TEXT_DEFAULT  = new Color(212, 212, 212);
    private static final Color BORDER_COLOR  = new Color(55, 55, 55);

    private static final Color SEV_DEBUG     = new Color(120, 120, 120);
    private static final Color SEV_INFO      = new Color(78, 201, 176);
    private static final Color SEV_WARN      = new Color(220, 180, 50);
    private static final Color SEV_ERROR     = new Color(215, 80, 80);

    private static final Color JSON_BRACKET  = new Color(220, 185, 100);
    private static final Color JSON_KEY      = new Color(130, 190, 230);
    private static final Color JSON_STR_VAL  = new Color(200, 140, 120);
    private static final Color JSON_NUMBER   = new Color(180, 210, 140);

    private static final Color STACK_EXC     = new Color(200, 100, 100);
    private static final Color STACK_FRAME   = new Color(140, 140, 160);
    private static final Color SEARCH_HL     = new Color(255, 255, 0, 60);

    // -------------------------------------------------------------------------
    // Instance fields
    // -------------------------------------------------------------------------

    private final int[]              columnWidths;
    private final String[]           searchHolder;
    private final ListSelectionModel selectionModel;

    private final JLabel             severityLabel;
    private final JTextPane          contentPane;

    private final SimpleAttributeSet defaultAttr;
    private final SimpleAttributeSet bracketAttr;
    private final SimpleAttributeSet keyAttr;
    private final SimpleAttributeSet stringValAttr;
    private final SimpleAttributeSet numberAttr;
    private final SimpleAttributeSet stackExcAttr;
    private final SimpleAttributeSet stackFrameAttr;
    private final DefaultHighlighter.DefaultHighlightPainter searchPainter;

    private MouseAdapter rowListener;
    private int          lastRowIndex;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public LogRowPanel(int[] columnWidths, String[] searchHolder, ListSelectionModel selectionModel) {
        this.columnWidths  = columnWidths;
        this.searchHolder  = searchHolder;
        this.selectionModel = selectionModel;

        setLayout(null);
        setOpaque(true);
        setBackground(BG_EVEN);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        severityLabel = new JLabel("", JLabel.CENTER);
        severityLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        severityLabel.setOpaque(false);
        add(severityLabel);

        contentPane = new JTextPane();
        contentPane.setEditable(false);
        contentPane.setOpaque(true);
        contentPane.setBackground(BG_EVEN);
        contentPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        contentPane.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        add(contentPane);

        defaultAttr = makeAttr(TEXT_DEFAULT, false);
        bracketAttr = makeAttr(JSON_BRACKET, false);
        keyAttr     = makeAttr(JSON_KEY,     true);
        stringValAttr = makeAttr(JSON_STR_VAL, false);
        numberAttr  = makeAttr(JSON_NUMBER,  false);
        stackExcAttr    = makeAttr(STACK_EXC,   false);
        stackFrameAttr  = makeAttr(STACK_FRAME, false);

        searchPainter = new DefaultHighlighter.DefaultHighlightPainter(SEARCH_HL);
    }

    // -------------------------------------------------------------------------
    // Layout — make this panel a validate root so that JTextPane's internal
    // revalidate() calls (fired on every document change) stop here and do not
    // cascade up to SmartGrid's VirtualCanvas, which would corrupt the virtual
    // scroll engine during bind() and on viewport resize.
    // -------------------------------------------------------------------------

    @Override
    public boolean isValidateRoot() {
        return true;
    }

    @Override
    public void doLayout() {
        int h = getHeight();
        if (h == 0) {
            return;
        }
        int w0 = columnWidths.length > 0 ? columnWidths[0] : 80;
        int w1 = Math.max(0, getWidth() - w0);
        severityLabel.setBounds(0, 0, w0, h);
        // h - 1: Swing paints children AFTER the border, so an opaque child whose
        // bounds reach the panel's bottom pixel will paint its background over the
        // 1px bottom border declared in the constructor (createMatteBorder 0,0,1,0).
        // Stopping the content pane 1px short leaves the border pixel uncovered.
        // severityLabel is setOpaque(false) so its full height is fine — the panel's
        // border shows through underneath it naturally.
        // If the border width in the constructor ever changes, update this offset too.
        contentPane.setBounds(w0, 0, w1, h - 1);
    }

    // -------------------------------------------------------------------------
    // Recyclable
    // -------------------------------------------------------------------------

    @Override
    public void prepareForReuse() {
        if (rowListener != null) {
            removeMouseListener(rowListener);
            rowListener = null;
        }
        severityLabel.setText("");
        severityLabel.setForeground(TEXT_DEFAULT);
        contentPane.getHighlighter().removeAllHighlights();
        contentPane.setDocument(new DefaultStyledDocument());
        setBackground(BG_EVEN);
        contentPane.setBackground(BG_EVEN);
    }

    @Override
    public void bind(GridRow row, int rowIndex) {
        if (rowListener != null) {
            removeMouseListener(rowListener);
            rowListener = null;
        }

        lastRowIndex = rowIndex;
        Color rowBg = rowIndex % 2 == 0 ? BG_EVEN : BG_ODD;
        setBackground(rowBg);
        contentPane.setBackground(rowBg);

        int h  = Math.max(getHeight(), 64);
        int w0 = columnWidths.length > 0 ? columnWidths[0] : 80;
        int w1 = Math.max(0, getWidth() - w0);

        String level   = strVal(row.get("level"));
        String content = strVal(row.get("content"));

        severityLabel.setBounds(0, 0, w0, h);
        severityLabel.setText(level);
        severityLabel.setForeground(severityColor(level));

        contentPane.setBounds(w0, 0, w1, h - 1); // see doLayout() for why h - 1

        hydrateLog(content, searchHolder[0]);

        final int capturedIdx = rowIndex;
        final ListSelectionModel sm = selectionModel;
        rowListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (sm == null) {
                    return;
                }
                if (e.isControlDown() || e.isMetaDown()) {
                    if (sm.isSelectedIndex(capturedIdx)) {
                        sm.removeSelectionInterval(capturedIdx, capturedIdx);
                    } else {
                        sm.addSelectionInterval(capturedIdx, capturedIdx);
                    }
                } else if (e.isShiftDown()) {
                    int anchor = sm.getAnchorSelectionIndex();
                    if (anchor < 0) {
                        anchor = capturedIdx;
                    }
                    sm.setSelectionInterval(anchor, capturedIdx);
                } else {
                    boolean sole = sm.isSelectedIndex(capturedIdx)
                            && sm.getMinSelectionIndex() == capturedIdx
                            && sm.getMaxSelectionIndex() == capturedIdx;
                    if (sole) {
                        sm.clearSelection();
                    } else {
                        sm.setSelectionInterval(capturedIdx, capturedIdx);
                    }
                }
            }
        };
        addMouseListener(rowListener);
    }

    // -------------------------------------------------------------------------
    // Selectable
    // -------------------------------------------------------------------------

    @Override
    public void setSelected(boolean selected) {
        Color bg = selected ? BG_SELECTED : (lastRowIndex % 2 == 0 ? BG_EVEN : BG_ODD);
        setBackground(bg);
        contentPane.setBackground(bg);
    }

    // -------------------------------------------------------------------------
    // Log content rendering
    // -------------------------------------------------------------------------

    private void hydrateLog(String content, String searchTerm) {
        // Build the document while it is DETACHED from the JTextPane.
        // No BasicTextUI listener means no View-tree rebuild on each insertString()
        // call — the many micro-inserts in insertJsonStyled are pure data ops.
        // setDocument() below installs the finished document in one shot, triggering
        // exactly one View rebuild instead of one per character-group transition.
        DefaultStyledDocument newDoc = new DefaultStyledDocument();

        if (content != null && !content.isEmpty()) {
            String trimmed = content.trim();
            try {
                if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                    insertJsonStyled(newDoc, trimmed);
                } else if (trimmed.contains("\n  at ") || isStackTrace(trimmed)) {
                    insertStackTraceStyled(newDoc, trimmed);
                } else {
                    newDoc.insertString(0, content, defaultAttr);
                }
            } catch (BadLocationException e) {
                try {
                    newDoc.insertString(0, content, defaultAttr);
                } catch (BadLocationException ignored) {}
            }
        }

        // One event, one View rebuild.
        contentPane.setDocument(newDoc);

        // Highlighter lives on the JTextPane UI, not the document; clear stale
        // positions left over from before setDocument() and add fresh ones.
        contentPane.getHighlighter().removeAllHighlights();
        if (searchTerm != null && !searchTerm.isEmpty()) {
            applySearchHighlights(searchTerm.toLowerCase());
        }
    }

    private static boolean isStackTrace(String text) {
        return text.contains("at ") && text.contains("(") && text.contains(".java:");
    }

    private void insertJsonStyled(StyledDocument doc, String json) throws BadLocationException {
        int len = json.length();
        StringBuilder buf = new StringBuilder();
        // state: 0=neutral, 1=in key string, 2=in string value, 3=in number/bool/null
        int state = 0;
        boolean expectValue = false;
        int i = 0;

        while (i < len) {
            char c = json.charAt(i);

            if (c == '{' || c == '}' || c == '[' || c == ']') {
                flushBuf(doc, buf, state);
                state = 0;
                expectValue = false;
                doc.insertString(doc.getLength(), String.valueOf(c), bracketAttr);

            } else if (c == ':') {
                flushBuf(doc, buf, state);
                state = 0;
                expectValue = true;
                doc.insertString(doc.getLength(), ": ", defaultAttr);
                // skip an optional space that may follow the colon
                if (i + 1 < len && json.charAt(i + 1) == ' ') {
                    i++;
                }

            } else if (c == ',') {
                flushBuf(doc, buf, state);
                state = 0;
                expectValue = false;
                doc.insertString(doc.getLength(), ", ", defaultAttr);
                if (i + 1 < len && json.charAt(i + 1) == ' ') {
                    i++;
                }

            } else if (c == '"') {
                flushBuf(doc, buf, state);
                state = expectValue ? 2 : 1;
                buf.append(c);
                i++;
                while (i < len) {
                    char nc = json.charAt(i);
                    buf.append(nc);
                    i++;
                    if (nc == '"' && (buf.length() < 2 || buf.charAt(buf.length() - 2) != '\\')) {
                        break;
                    }
                }
                flushBuf(doc, buf, state);
                state = 0;
                continue;

            } else if (expectValue && state == 0
                    && (Character.isDigit(c) || c == '-'
                        || c == 't' || c == 'f' || c == 'n')) {
                flushBuf(doc, buf, state);
                state = 3;
                buf.append(c);

            } else if (state == 3 && (c == ',' || c == '}' || c == ']' || c == ' ' || c == '\n')) {
                flushBuf(doc, buf, state);
                state = 0;
                expectValue = false;
                buf.append(c);

            } else {
                buf.append(c);
            }
            i++;
        }
        flushBuf(doc, buf, state);
    }

    private void flushBuf(StyledDocument doc, StringBuilder buf, int state) throws BadLocationException {
        if (buf.length() == 0) {
            return;
        }
        String text = buf.toString();
        buf.setLength(0);
        SimpleAttributeSet attr;
        switch (state) {
            case 1:  attr = keyAttr;       break;
            case 2:  attr = stringValAttr; break;
            case 3:  attr = numberAttr;    break;
            default: attr = defaultAttr;   break;
        }
        doc.insertString(doc.getLength(), text, attr);
    }

    private void insertStackTraceStyled(StyledDocument doc, String text) throws BadLocationException {
        String[] lines = text.split("\n", -1);
        for (int li = 0; li < lines.length; li++) {
            String line = lines[li];
            SimpleAttributeSet attr;
            String trimmed = line.trim();
            if (trimmed.startsWith("at ") || trimmed.startsWith("... ")) {
                attr = stackFrameAttr;
            } else if (li == 0) {
                attr = stackExcAttr;
            } else {
                attr = defaultAttr;
            }
            doc.insertString(doc.getLength(), line, attr);
            if (li < lines.length - 1) {
                doc.insertString(doc.getLength(), "\n", defaultAttr);
            }
        }
    }

    private void applySearchHighlights(String lowerTerm) {
        try {
            StyledDocument doc = contentPane.getStyledDocument();
            String full = doc.getText(0, doc.getLength()).toLowerCase();
            int idx = 0;
            while ((idx = full.indexOf(lowerTerm, idx)) >= 0) {
                contentPane.getHighlighter().addHighlight(idx, idx + lowerTerm.length(), searchPainter);
                idx += lowerTerm.length();
            }
        } catch (BadLocationException ignored) {}
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Color severityColor(String level) {
        if (level == null) {
            return TEXT_DEFAULT;
        }
        switch (level.toUpperCase()) {
            case "DEBUG": return SEV_DEBUG;
            case "INFO":  return SEV_INFO;
            case "WARN":  return SEV_WARN;
            case "ERROR": return SEV_ERROR;
            default:      return TEXT_DEFAULT;
        }
    }

    private static String strVal(Object v) {
        return v != null ? v.toString() : "";
    }

    private static SimpleAttributeSet makeAttr(Color fg, boolean bold) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, fg);
        StyleConstants.setFontFamily(a, Font.MONOSPACED);
        StyleConstants.setFontSize(a, 12);
        if (bold) {
            StyleConstants.setBold(a, true);
        }
        return a;
    }

}
