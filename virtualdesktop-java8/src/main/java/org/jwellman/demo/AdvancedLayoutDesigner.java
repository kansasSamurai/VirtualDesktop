package org.jwellman.demo;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

/**
 * Advanced Visual Layout Designer with snap-to-grid, alignment guides, and resize handles
 */
public class AdvancedLayoutDesigner extends JFrame {

    private JToolBar toolBar;
    private JCheckBox snapToGridCheck;
    private JCheckBox showGridCheck;
    private JCheckBox showGuidesCheck;
    private JSpinner gridSizeSpinner;
    private DesignCanvas canvas;

    private File layoutFile = new File("designer_layout.dat");

    private static final long serialVersionUID = 1L;

    public AdvancedLayoutDesigner() {
        setTitle("Advanced Layout Designer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);

        createToolBar();
        createCanvas();
        createMenuBar();

        setLocationRelativeTo(null);
    }

    private void createToolBar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // Add component buttons
        JButton addButtonBtn = new JButton("Add Button");
        addButtonBtn.addActionListener(e -> canvas.addDesignComponent(createButton()));
        toolBar.add(addButtonBtn);

        JButton addLabelBtn = new JButton("Add Label");
        addLabelBtn.addActionListener(e -> canvas.addDesignComponent(createLabel()));
        toolBar.add(addLabelBtn);

        JButton addTextFieldBtn = new JButton("Add TextField");
        addTextFieldBtn.addActionListener(e -> canvas.addDesignComponent(createTextField()));
        toolBar.add(addTextFieldBtn);

        toolBar.addSeparator();

        // Grid settings
        showGridCheck = new JCheckBox("Show Grid", true);
        showGridCheck.addActionListener(e -> canvas.setShowGrid(showGridCheck.isSelected()));
        toolBar.add(showGridCheck);
        
        snapToGridCheck = new JCheckBox("Snap to Grid", true);
        snapToGridCheck.addActionListener(e -> canvas.setSnapToGrid(snapToGridCheck.isSelected()));
        toolBar.add(snapToGridCheck);
        
        toolBar.add(new JLabel(" Grid Size:"));
        gridSizeSpinner = new JSpinner(new SpinnerNumberModel(10, 5, 50, 5));
        gridSizeSpinner.addChangeListener(e -> 
            canvas.setGridSize((Integer) gridSizeSpinner.getValue()));
        toolBar.add(gridSizeSpinner);
        
        toolBar.addSeparator();
        
        // Alignment guides
        showGuidesCheck = new JCheckBox("Show Alignment Guides", true);
        showGuidesCheck.addActionListener(e -> canvas.setShowAlignmentGuides(showGuidesCheck.isSelected()));
        toolBar.add(showGuidesCheck);
        
        add(toolBar, BorderLayout.NORTH);
    }
    
    private void createCanvas() {
        canvas = new DesignCanvas();
        JScrollPane scrollPane = new JScrollPane(canvas);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem saveItem = new JMenuItem("Save Layout");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> saveLayout());
        
        JMenuItem loadItem = new JMenuItem("Load Layout");
        loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        loadItem.addActionListener(e -> loadLayout());
        
        JMenuItem clearItem = new JMenuItem("Clear All");
        clearItem.addActionListener(e -> canvas.clearAll());
        
        fileMenu.add(saveItem);
        fileMenu.add(loadItem);
        fileMenu.addSeparator();
        fileMenu.add(clearItem);
        
        JMenu editMenu = new JMenu("Edit");
        
        JMenuItem deleteItem = new JMenuItem("Delete Selected");
        deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        deleteItem.addActionListener(e -> canvas.deleteSelected());
        
        editMenu.add(deleteItem);
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        setJMenuBar(menuBar);
    }
    
    private JComponent createButton() {
        JButton button = new JButton("Button");
        button.setName("Button_" + System.currentTimeMillis());
        return button;
    }
    
    private JComponent createLabel() {
        JLabel label = new JLabel("Label");
        label.setName("Label_" + System.currentTimeMillis());
        return label;
    }
    
    private JComponent createTextField() {
        JTextField textField = new JTextField("Text Field");
        textField.setName("TextField_" + System.currentTimeMillis());
        return textField;
    }
    
    private void saveLayout() {
        try {
            canvas.saveLayout(layoutFile);
            JOptionPane.showMessageDialog(this, "Layout saved successfully!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadLayout() {
        try {
            canvas.loadLayout(layoutFile);
            JOptionPane.showMessageDialog(this, "Layout loaded successfully!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AdvancedLayoutDesigner().setVisible(true);
        });
    }
}

/**
 * Design canvas with drag, resize, snap-to-grid, and alignment features
 */
class DesignCanvas extends JPanel {
    private List<DesignComponent> components = new ArrayList<>();
    private DesignComponent selectedComponent;
    private boolean showGrid = true;
    private boolean snapToGrid = true;
    private boolean showAlignmentGuides = true;
    private int gridSize = 10;
    private List<AlignmentGuide> activeGuides = new ArrayList<>();
    private static final int GUIDE_THRESHOLD = 5;

    private static final long serialVersionUID = 1L;

    public DesignCanvas() {
        setLayout(null);
        setPreferredSize(new Dimension(2000, 2000));
        setBackground(Color.WHITE);
        
        // Click on empty space to deselect
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getComponent() == DesignCanvas.this) {
                    deselectAll();
                }
            }
        });
    }
    
    public void addDesignComponent(JComponent component) {
        // Position new components in center of visible area
        Rectangle visible = getVisibleRect();
        int x = visible.x + visible.width / 2 - 50;
        int y = visible.y + visible.height / 2 - 15;
        
        if (snapToGrid) {
            x = snapToGrid(x);
            y = snapToGrid(y);
        }
        
        component.setBounds(x, y, 100, 30);
        DesignComponent dc = new DesignComponent(component);
        components.add(dc);
        add(component);
        
        selectComponent(dc);
        revalidate();
        repaint();
    }
    
    public void deleteSelected() {
        if (selectedComponent != null) {
            components.remove(selectedComponent);
            remove(selectedComponent.component);
            selectedComponent = null;
            revalidate();
            repaint();
        }
    }
    
    public void clearAll() {
        components.clear();
        removeAll();
        selectedComponent = null;
        revalidate();
        repaint();
    }
    
    private void selectComponent(DesignComponent dc) {
        deselectAll();
        selectedComponent = dc;
        dc.setSelected(true);
        repaint();
    }
    
    private void deselectAll() {
        if (selectedComponent != null) {
            selectedComponent.setSelected(false);
        }
        selectedComponent = null;
        repaint();
    }
    
    public void setShowGrid(boolean show) {
        this.showGrid = show;
        repaint();
    }
    
    public void setSnapToGrid(boolean snap) {
        this.snapToGrid = snap;
    }
    
    public void setShowAlignmentGuides(boolean show) {
        this.showAlignmentGuides = show;
        repaint();
    }
    
    public void setGridSize(int size) {
        this.gridSize = size;
        repaint();
    }
    
    private int snapToGrid(int value) {
        return Math.round((float) value / gridSize) * gridSize;
    }
    
    private List<AlignmentGuide> findAlignmentGuides(DesignComponent moving) {
        List<AlignmentGuide> guides = new ArrayList<>();
        Rectangle movingBounds = moving.component.getBounds();
        
        for (DesignComponent dc : components) {
            if (dc == moving) continue;
            
            Rectangle bounds = dc.component.getBounds();
            
            // Left edge alignment
            if (Math.abs(movingBounds.x - bounds.x) < GUIDE_THRESHOLD) {
                guides.add(new AlignmentGuide(bounds.x, AlignmentGuide.Type.VERTICAL));
            }
            
            // Right edge alignment
            if (Math.abs(movingBounds.x + movingBounds.width - (bounds.x + bounds.width)) < GUIDE_THRESHOLD) {
                guides.add(new AlignmentGuide(bounds.x + bounds.width, AlignmentGuide.Type.VERTICAL));
            }
            
            // Center vertical alignment
            int movingCenterX = movingBounds.x + movingBounds.width / 2;
            int centerX = bounds.x + bounds.width / 2;
            if (Math.abs(movingCenterX - centerX) < GUIDE_THRESHOLD) {
                guides.add(new AlignmentGuide(centerX, AlignmentGuide.Type.VERTICAL));
            }
            
            // Top edge alignment
            if (Math.abs(movingBounds.y - bounds.y) < GUIDE_THRESHOLD) {
                guides.add(new AlignmentGuide(bounds.y, AlignmentGuide.Type.HORIZONTAL));
            }
            
            // Bottom edge alignment
            if (Math.abs(movingBounds.y + movingBounds.height - (bounds.y + bounds.height)) < GUIDE_THRESHOLD) {
                guides.add(new AlignmentGuide(bounds.y + bounds.height, AlignmentGuide.Type.HORIZONTAL));
            }
            
            // Center horizontal alignment
            int movingCenterY = movingBounds.y + movingBounds.height / 2;
            int centerY = bounds.y + bounds.height / 2;
            if (Math.abs(movingCenterY - centerY) < GUIDE_THRESHOLD) {
                guides.add(new AlignmentGuide(centerY, AlignmentGuide.Type.HORIZONTAL));
            }
        }
        
        return guides;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        // Draw grid
        if (showGrid) {
            drawGrid(g2d);
        }

        // Draw alignment guides
        if (showAlignmentGuides && !activeGuides.isEmpty()) {
            drawAlignmentGuides(g2d);
        }
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(230, 230, 230));
        
        int width = getWidth();
        int height = getHeight();
        
        // Vertical lines
        for (int x = 0; x < width; x += gridSize) {
            g2d.drawLine(x, 0, x, height);
        }
        
        // Horizontal lines
        for (int y = 0; y < height; y += gridSize) {
            g2d.drawLine(0, y, width, y);
        }
    }
    
    private void drawAlignmentGuides(Graphics2D g2d) {
        g2d.setColor(new Color(255, 0, 255, 150));
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 
                                     0, new float[]{5, 5}, 0));
        
        for (AlignmentGuide guide : activeGuides) {
            if (guide.type == AlignmentGuide.Type.VERTICAL) {
                g2d.drawLine(guide.position, 0, guide.position, getHeight());
            } else {
                g2d.drawLine(0, guide.position, getWidth(), guide.position);
            }
        }
        
        g2d.setStroke(oldStroke);
    }
    
    public void saveLayout(File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            List<ComponentData> data = new ArrayList<>();
            for (DesignComponent dc : components) {
                data.add(new ComponentData(dc));
            }
            oos.writeObject(data);
        }
    }
    
    @SuppressWarnings("unchecked")
    public void loadLayout(File file) throws IOException, ClassNotFoundException {
        clearAll();
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            List<ComponentData> data = (List<ComponentData>) ois.readObject();
            for (ComponentData cd : data) {
                JComponent component = cd.recreateComponent();
                if (component != null) {
                    DesignComponent dc = new DesignComponent(component);
                    components.add(dc);
                    add(component);
                }
            }
        }
        
        revalidate();
        repaint();
    }
    
    /**
     * Wrapper for design components with selection and resize handles
     */
    class DesignComponent {
        JComponent component;
        private boolean selected;
        private SelectionBorder selectionBorder;
        private ComponentDragHandler dragHandler;

        public DesignComponent(JComponent component) {
            this.component = component;
            this.selectionBorder = new SelectionBorder(this);
            this.dragHandler = new ComponentDragHandler(this);

            component.addMouseListener(dragHandler);
            component.addMouseMotionListener(dragHandler);
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            if (selected) {
                component.setBorder(selectionBorder);
            } else {
                component.setBorder(null);
            }
        }

        public boolean isSelected() {
            return selected;
        }
    }

    /**
     * Custom border that draws selection handles
     */
    class SelectionBorder extends LineBorder {

        @SuppressWarnings("unused")
        private DesignComponent designComponent;

        private static final int HANDLE_SIZE = 6;

        private static final long serialVersionUID = 1L;

        public SelectionBorder(DesignComponent dc) {
            super(Color.BLUE, 2);
            this.designComponent = dc;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            super.paintBorder(c, g, x, y, width, height);

            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(Color.BLUE);
            g2d.fillRect(x - HANDLE_SIZE / 2, y - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
            g2d.fillRect(x + width - HANDLE_SIZE / 2, y - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
            g2d.fillRect(x - HANDLE_SIZE / 2, y + height - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
            g2d.fillRect(x + width - HANDLE_SIZE / 2, y + height - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);

            // Mid-point handles
            g2d.fillRect(x + width / 2 - HANDLE_SIZE / 2, y - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
            g2d.fillRect(x + width / 2 - HANDLE_SIZE / 2, y + height - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
            g2d.fillRect(x - HANDLE_SIZE / 2, y + height / 2 - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
            g2d.fillRect(x + width - HANDLE_SIZE / 2, y + height / 2 - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(3, 3, 3, 3);
        }
    }

    /**
     * Handles dragging and resizing of components
     */
    class ComponentDragHandler extends MouseAdapter {

        private Point pressPoint;
        private Rectangle startBounds;
        private ResizeHandle activeHandle;
        private DesignComponent designComponent;

        public ComponentDragHandler(DesignComponent dc) {
            this.designComponent = dc;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            selectComponent(designComponent);
            pressPoint = e.getPoint();
            startBounds = designComponent.component.getBounds();
            activeHandle = getResizeHandle(e.getPoint(), startBounds);
            System.out.println("Handle: " + activeHandle);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Point currentPoint = e.getPoint();

            // Calculate offset from press point
            final int dx = currentPoint.x - pressPoint.x;
            final int dy = currentPoint.y - pressPoint.y;

            Rectangle currentBounds = designComponent.component.getBounds();
            System.out.print("Drag, width start: ");
            System.out.print(currentBounds.width);
            System.out.print(", dx: ");
            System.out.print(dx);

            if (activeHandle == ResizeHandle.NONE) {
                // Move component

                int newX = currentBounds.x + dx; // startBounds
                int newY = currentBounds.y + dy;
                if (snapToGrid) {
                    newX = snapToGrid(newX);
                    newY = snapToGrid(newY);
                }

                Rectangle newBounds = new Rectangle(
                    newX, //currentBounds.x + dx,
                    newY, //currentBounds.y + dy,
                    currentBounds.width,
                    currentBounds.height
                );
                designComponent.component.setBounds(newBounds);

                // Find alignment guides
                if (showAlignmentGuides) {
                    activeGuides = findAlignmentGuides(designComponent);
                    // repaint(); // this was original code but pretty sure it is redundant with call below
                }

            } else {
                // Resize component (currentBounds or startBounds or does it depend?)
                Rectangle x = currentBounds;
                switch(activeHandle) {
                case NONE:
                case E:
                case S:
                case SE:
                    x = startBounds;
                    break;
                case N:
                case NE:
                case NW:
                case SW:
                case W:
                default:
                    break;
                }

                Rectangle newBounds = calculateResizedBounds(activeHandle, x, dx, dy);
                if (snapToGrid) {
                    newBounds.x = snapToGrid(newBounds.x);
                    newBounds.y = snapToGrid(newBounds.y);
                    newBounds.width = snapToGrid(newBounds.width);
                    newBounds.height = snapToGrid(newBounds.height);
                }

                // Enforce minimum size
//                if (newBounds.width < 20)
//                    newBounds.width = 20;
//                if (newBounds.height < 20)
//                    newBounds.height = 20;

                designComponent.component.setBounds(newBounds);
                System.out.print(", width end: ");
                System.out.println(newBounds.width);
            }

            revalidate();
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // Snap on release (AFTER drag is complete)
//            if (snapToGrid && activeHandle == ResizeHandle.NONE) {
//                Rectangle bounds = designComponent.component.getBounds();
//                bounds.x = snapToGrid(bounds.x);
//                bounds.y = snapToGrid(bounds.y);
//                designComponent.component.setBounds(bounds);
//            } else if (snapToGrid) {
//                // Snap resize too
//                Rectangle bounds = designComponent.component.getBounds();
//                bounds.x = snapToGrid(bounds.x);
//                bounds.y = snapToGrid(bounds.y);
//                bounds.width = snapToGrid(bounds.width);
//                bounds.height = snapToGrid(bounds.height);
//                designComponent.component.setBounds(bounds);
//            }

            activeGuides.clear();

            revalidate();
            repaint();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (designComponent.isSelected()) {
                ResizeHandle handle = getResizeHandle(e.getPoint(), designComponent.component.getBounds());
                setCursorForHandle(handle);
            }
        }

        private ResizeHandle getResizeHandle(Point p, Rectangle bounds) {
            int hs = 6; // handle size

            // Check corners first
            if (isNear(p, 0, 0, hs))
                return ResizeHandle.NW;
            if (isNear(p, bounds.width, 0, hs))
                return ResizeHandle.NE;
            if (isNear(p, 0, bounds.height, hs))
                return ResizeHandle.SW;
            if (isNear(p, bounds.width, bounds.height, hs))
                return ResizeHandle.SE;

            // Check edges
            if (isNear(p, bounds.width / 2, 0, hs)) return ResizeHandle.N;
            if (isNear(p, bounds.width / 2, bounds.height, hs)) return ResizeHandle.S;
            if (isNear(p, 0, bounds.height / 2, hs)) return ResizeHandle.W;
            if (isNear(p, bounds.width, bounds.height / 2, hs)) return ResizeHandle.E;
            
            return ResizeHandle.NONE;
        }
        
        private boolean isNear(Point p, int x, int y, int threshold) {
            return Math.abs(p.x - x) <= threshold && Math.abs(p.y - y) <= threshold;
        }
        
        private void setCursorForHandle(ResizeHandle handle) {
            Cursor cursor = Cursor.getDefaultCursor();
            
            switch (handle) {
                case NW: case SE: cursor = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR); break;
                case NE: case SW: cursor = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR); break;
                case N: case S: cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR); break;
                case E: case W: cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR); break;
                case NONE: cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR); break;
            }
            
            designComponent.component.setCursor(cursor);
        }
        
        private Rectangle calculateResizedBounds(ResizeHandle handle, Rectangle start, int dx, int dy) {
            // TODO this should be a flyweight 
            Rectangle bounds = new Rectangle(start);

            switch (handle) {
            case NW:
                bounds.x += dx;
                bounds.y += dy;
                bounds.width -= dx;
                bounds.height -= dy;
                break;
            case N:
                bounds.y += dy;
                bounds.height -= dy;
                break;
            case NE:
                bounds.y += dy;
                bounds.width += dx;
                bounds.height -= dy;
                break;
            case E: // works with startBounds
                bounds.width += dx;
                break;
            case SE: // works with startBounds
                bounds.width += dx;
                bounds.height += dy;
                break;
            case S: // works with startBounds
                bounds.height += dy;
                break;
            case SW:
                bounds.x += dx;
                bounds.width -= dx;
                bounds.height += dy;
                break;
            case W:
                bounds.x += dx; // dx is actually negative so it moves left
                bounds.width -= dx; // dx is actually negative so width increases
                break;
            case NONE:
                break;
            default:
                break;
            }

            return bounds;
        }
    }
}

/**
 * Alignment guide data structure
 */
class AlignmentGuide {
    enum Type { VERTICAL, HORIZONTAL }
    
    int position;
    Type type;
    
    public AlignmentGuide(int position, Type type) {
        this.position = position;
        this.type = type;
    }
}

/**
 * Serializable component data for save/load
 */
class ComponentData implements Serializable {

    String name;
    String className;
    String text;
    int x, y, width, height;

    private static final long serialVersionUID = 1L;

    public ComponentData(DesignCanvas.DesignComponent dc) {
        JComponent comp = dc.component;
        this.name = comp.getName();
        this.className = comp.getClass().getName();

        Rectangle bounds = comp.getBounds();
        this.x = bounds.x;
        this.y = bounds.y;
        this.width = bounds.width;
        this.height = bounds.height;

        // Extract text based on component type
        if (comp instanceof JButton) {
            this.text = ((JButton) comp).getText();
        } else if (comp instanceof JLabel) {
            this.text = ((JLabel) comp).getText();
        } else if (comp instanceof JTextField) {
            this.text = ((JTextField) comp).getText();
        }
    }

    public JComponent recreateComponent() {
        JComponent component = null;
        
        if (className.endsWith("JButton")) {
            component = new JButton(text);
        } else if (className.endsWith("JLabel")) {
            component = new JLabel(text);
        } else if (className.endsWith("JTextField")) {
            component = new JTextField(text);
        }
        
        if (component != null) {
            component.setName(name);
            component.setBounds(x, y, width, height);
        }
        
        return component;
    }

}
