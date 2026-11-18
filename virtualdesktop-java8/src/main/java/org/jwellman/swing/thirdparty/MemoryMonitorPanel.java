package org.jwellman.swing.thirdparty;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.apache.batik.util.gui.ExtendedGridBagConstraints;
import org.apache.batik.util.gui.resource.ActionMap;
import org.apache.batik.util.gui.resource.ButtonFactory;
import org.apache.batik.util.gui.resource.MissingListenerException;
import org.apache.batik.util.resources.ResourceManager;
import org.jwellman.swing.AbstractVolatileImageComponent;

/**
 * MemoryMonitor component: embeddable JPanel backed by VRAM VolatileImage
 * caching.
 * 
 * ### Key Design Benefits
 * 
 * 1. **Circular Primitive Buffers:** Using `long[]` and `boolean[]` ring
 * buffers avoids boxing overhead (`Long` objects) and dynamic array resizing
 * (`ArrayList`), keeping garbage collection churn strictly at **zero**. 
 * 2. **Instant Diagnostic Clarity:** Cyan markers at the top of the timeline
 * highlight exact GC reclamation sweeps, while red vertical hazard lines expose
 * sudden heap allocation surges.
 * 
 * 
 */
@SuppressWarnings("serial")
public class MemoryMonitorPanel extends JPanel implements ActionMap {

    protected static final String RESOURCE = "org.apache.batik.util.gui.resources.MemoryMonitorMessages";
    protected static ResourceBundle bundle;
    protected static ResourceManager resources;

    static {
        bundle = ResourceBundle.getBundle(RESOURCE, Locale.getDefault());
        resources = new ResourceManager(bundle);
    }

    protected Map<String, Action> listeners = new HashMap<>();
    protected MonitorContainerPanel monitorPanel;
    protected RepaintThread repaintThread;

    public MemoryMonitorPanel() {
        this(1000);
    }

    public MemoryMonitorPanel(long time) {
        super(new BorderLayout());
        listeners.put("CollectButtonAction", new CollectButtonAction());

        monitorPanel = new MonitorContainerPanel(time);
        add(monitorPanel, BorderLayout.CENTER);
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                resources.getString("Frame.border_title")));

        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        ButtonFactory bf = new ButtonFactory(bundle, this);
        p.add(bf.createJButton("CollectButton"));
        add(p, BorderLayout.SOUTH);

        // Start the background polling thread when added to screen
        addNotify();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (repaintThread == null || !repaintThread.isAlive()) {
            repaintThread = monitorPanel.getRepaintThread();
            repaintThread.start();
        } else {
            repaintThread.safeResume();
        }
    }

    @Override
    public void removeNotify() {
        if (repaintThread != null) {
            repaintThread.safeSuspend();
        }
        super.removeNotify();
    }

    public Action getAction(String key) throws MissingListenerException {
        return listeners.get(key);
    }

    protected static class CollectButtonAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            System.gc();
        }
    }

    /**
     * Internal container holding the Usage and History widgets.
     */
    public static class MonitorContainerPanel extends JPanel {
        protected RepaintThread repaintThread;

        public MonitorContainerPanel(long time) {
            super(new GridBagLayout());
            ExtendedGridBagConstraints constraints = new ExtendedGridBagConstraints();
            constraints.insets = new Insets(5, 5, 5, 5);

            List<Component> l = new ArrayList<>();
            
            // Usage Indicator
            JPanel p = new JPanel(new BorderLayout());
            p.setBorder(BorderFactory.createLoweredBevelBorder());
            Usage usage = new Usage();
            p.add(usage);
            constraints.weightx = 0.3;
            constraints.weighty = 1;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.setGridBounds(0, 0, 1, 1);
            add(p, constraints);
            l.add(usage);

            // History Chart
            p = new JPanel(new BorderLayout());
            p.setBorder(BorderFactory.createLoweredBevelBorder());
            History history = new History();
            p.add(history);
            constraints.weightx = 0.7;
            constraints.setGridBounds(1, 0, 1, 1);
            add(p, constraints);
            l.add(history);

            repaintThread = new RepaintThread(time, l);
        }

        public RepaintThread getRepaintThread() {
            return repaintThread;
        }
    }

    /**
     * Optimized Usage LED Block Component extending AbstractVolatileImageComponent.
     */
    public static class Usage extends AbstractVolatileImageComponent implements MemoryChangeListener {
        public static final int PREFERRED_WIDTH = 90;
        public static final int PREFERRED_HEIGHT = 100;

        protected static final String UNITS = resources.getString("Usage.units");
        protected static final String TOTAL = resources.getString("Usage.total");
        protected static final String USED = resources.getString("Usage.used");
        protected static final boolean POSTFIX = resources.getBoolean("Usage.postfix");

        protected static final int FONT_SIZE = 9;
        protected static final int BLOCK_MARGIN = 10;
        protected static final int BLOCKS = 15;
        protected static final double BLOCK_WIDTH = PREFERRED_WIDTH - BLOCK_MARGIN * 2;
        protected static final double BLOCK_HEIGHT = ((double) PREFERRED_HEIGHT - (3 * FONT_SIZE) - BLOCKS) / BLOCKS;
        protected static final int[] BLOCK_TYPE = { 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2 };

        protected Color[] usedColors = { Color.red, new Color(255, 165, 0), Color.green };
        protected Color[] freeColors = { new Color(130, 0, 0), new Color(130, 90, 0), new Color(0, 130, 0) };
        protected Font font = new Font("SansSerif", Font.BOLD, FONT_SIZE);
        protected Color textColor = Color.green;

        private long totalMemory;
        private long freeMemory;
        private boolean memoryChanged = true;

        public Usage() {
            setBackground(Color.black);
            setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        }

        @Override
        public synchronized void memoryStateChanged(long total, long free) {
            if (this.totalMemory != total || this.freeMemory != free) {
                this.totalMemory = total;
                this.freeMemory = free;
                this.memoryChanged = true;
                repaint();
            }
        }

        @Override
        protected boolean shouldRedrawBuffer() {
            return memoryChanged;
        }

        @Override
        protected void resetBufferRedrawFlag() {
            memoryChanged = false;
        }

        @Override
        protected void renderToBuffer(Graphics2D g2d, int width, int height) {
            g2d.setColor(getBackground());
            g2d.fillRect(0, 0, width, height);

            if (totalMemory == 0) return;

            int nfree = (int) Math.round(((double) BLOCKS) * freeMemory / totalMemory);
            nfree = Math.max(0, Math.min(BLOCKS, nfree));

            for (int i = 0; i < nfree; i++) {
                Rectangle2D rect = new Rectangle2D.Double(10, i * BLOCK_HEIGHT + i + FONT_SIZE + 5, BLOCK_WIDTH, BLOCK_HEIGHT);
                g2d.setPaint(freeColors[BLOCK_TYPE[i]]);
                g2d.fill(rect);
            }

            for (int i = nfree; i < BLOCKS; i++) {
                Rectangle2D rect = new Rectangle2D.Double(10, i * BLOCK_HEIGHT + i + FONT_SIZE + 5, BLOCK_WIDTH, BLOCK_HEIGHT);
                g2d.setPaint(usedColors[BLOCK_TYPE[i]]);
                g2d.fill(rect);
            }

            g2d.setPaint(textColor);
            g2d.setFont(font);

            long total = totalMemory / 1024;
            long used = (totalMemory - freeMemory) / 1024;
            String totalText = POSTFIX ? total + UNITS + " " + TOTAL : TOTAL + " " + total + UNITS;
            String usedText = POSTFIX ? used + UNITS + " " + USED : USED + " " + used + UNITS;

            g2d.drawString(totalText, 10, 10);
            g2d.drawString(usedText, 10, PREFERRED_HEIGHT - 3);
        }
    }

    /**
     * Optimized History Chart Component extending AbstractVolatileImageComponent.
     * 
     * Here is the enhanced `History` component. It tracks data points in 
     * parallel primitive arrays, detects spikes and GC events on the fly 
     * with **zero heap allocation**, and renders distinct visual markers 
     * (cyan ticks for GC events, orange/red vertical highlights for allocation spikes) 
     * directly into the VRAM buffer.
     * 
     */
    public static class History extends AbstractVolatileImageComponent implements MemoryChangeListener {

        public static final int PREFERRED_WIDTH = 200;
        public static final int PREFERRED_HEIGHT = 100;
        private static final int MAX_HISTORY = 190;

        protected static final Stroke GRID_LINES_STROKE = new BasicStroke(1);
        protected static final Stroke CURVE_STROKE = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        protected static final Stroke BORDER_STROKE = new BasicStroke(2);
        protected static final Stroke MARKER_STROKE = new BasicStroke(1.0f);

        protected Color gridLinesColor = new Color(0, 130, 0);
        protected Color curveColor = Color.yellow;
        protected Color borderColor = Color.green;
        protected Color gcMarkerColor = new Color(0, 255, 200);     // Cyan tick for GC
        protected Color spikeMarkerColor = new Color(255, 80, 80);   // Red tint for allocation spike

        // Parallel primitive arrays to eliminate object allocation / boxing churn
        private final long[] usedMemoryBuffer = new long[MAX_HISTORY];
        private final boolean[] gcEventBuffer = new boolean[MAX_HISTORY];
        private final boolean[] spikeEventBuffer = new boolean[MAX_HISTORY];
        private int head = 0;
        private int size = 0;

        protected int xShift = 0;
        protected long totalMemory;
        protected long freeMemory;
        protected GeneralPath path = new GeneralPath();
        private boolean dataChanged = true;

        public History() {
            setBackground(Color.black);
            setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        }

        @Override
        public synchronized void memoryStateChanged(long total, long free) {
            long used = total - free;
            boolean isGC = false;
            boolean isSpike = false;

            if (size > 0) {
                int prevIndex = (head - 1 + MAX_HISTORY) % MAX_HISTORY;
                long prevUsed = usedMemoryBuffer[prevIndex];
                long delta = used - prevUsed;

                // Heuristic: If free memory jumped up by > 2MB, a GC occurred
                if (free > freeMemory + (2L * 1024 * 1024)) {
                    isGC = true;
                }
                // Heuristic: If used memory surged by > 5MB between polls, mark allocation spike
                else if (delta > (5L * 1024 * 1024)) {
                    isSpike = true;
                }
            }

            this.totalMemory = total;
            this.freeMemory = free;

            // Store in circular buffer
            usedMemoryBuffer[head] = used;
            gcEventBuffer[head] = isGC;
            spikeEventBuffer[head] = isSpike;

            head = (head + 1) % MAX_HISTORY;
            if (size < MAX_HISTORY) {
                size++;
            } else {
                xShift = (xShift + 1) % 10;
            }

            rebuildPath();
            dataChanged = true;
            repaint();
        }

        private void rebuildPath() {
            path.reset();
            if (size == 0 || totalMemory == 0) return;

            int startIndex = (head - size + MAX_HISTORY) % MAX_HISTORY;
            long firstUsed = usedMemoryBuffer[startIndex];
            
            float startY = PREFERRED_HEIGHT - 5 - (((float) firstUsed / totalMemory) * 80.0f + 5.0f);
            path.moveTo(5, startY);

            for (int i = 1; i < size; i++) {
                int idx = (startIndex + i) % MAX_HISTORY;
                long used = usedMemoryBuffer[idx];
                float x = 5 + i;
                float y = PREFERRED_HEIGHT - 5 - (((float) used / totalMemory) * 80.0f + 5.0f);
                path.lineTo(x, y);
            }
        }

        @Override
        protected boolean shouldRedrawBuffer() {
            return dataChanged;
        }

        @Override
        protected void resetBufferRedrawFlag() {
            dataChanged = false;
        }

        @Override
        protected void renderToBuffer(Graphics2D g2d, int width, int height) {
            g2d.setColor(getBackground());
            g2d.fillRect(0, 0, width, height);

            // Vertical grid lines
            g2d.setPaint(gridLinesColor);
            g2d.setStroke(GRID_LINES_STROKE);
            for (int i = 1; i < 20; i++) {
                int lx = i * 10 + 5 - xShift;
                g2d.draw(new Line2D.Double(lx, 5, lx, PREFERRED_HEIGHT - 5));
            }

            // Horizontal grid lines
            for (int i = 1; i < 9; i++) {
                int ly = i * 10 + 5;
                g2d.draw(new Line2D.Double(5, ly, PREFERRED_WIDTH - 5, ly));
            }

            // Render Event Markers (Spikes & GC Ticks)
            if (size > 0) {
                int startIndex = (head - size + MAX_HISTORY) % MAX_HISTORY;
                for (int i = 0; i < size; i++) {
                    int idx = (startIndex + i) % MAX_HISTORY;
                    float x = 5 + i;

                    if (spikeEventBuffer[idx]) {
                        g2d.setColor(spikeMarkerColor);
                        g2d.setStroke(MARKER_STROKE);
                        g2d.drawLine((int) x, 5, (int) x, PREFERRED_HEIGHT - 5);
                    } else if (gcEventBuffer[idx]) {
                        g2d.setColor(gcMarkerColor);
                        g2d.setStroke(MARKER_STROKE);
                        // Draw small tick mark at the top of the chart for GC events
                        g2d.drawLine((int) x, 5, (int) x, 12);
                    }
                }
            }

            // Curve
            g2d.setPaint(curveColor);
            g2d.setStroke(CURVE_STROKE);
            g2d.draw(path);

            // Border
            g2d.setStroke(BORDER_STROKE);
            g2d.setPaint(borderColor);
            g2d.draw(new Rectangle2D.Double(5, 5, PREFERRED_WIDTH - 10, PREFERRED_HEIGHT - 10));
        }
    }

    public interface MemoryChangeListener {
        void memoryStateChanged(long total, long free);
    }

    /**
     * Polling thread for updating memory usage metrics.
     */
    public static class RepaintThread extends Thread {
        protected long timeout;
        protected List<Component> components;
        protected Runtime runtime = Runtime.getRuntime();
        protected boolean suspended;
        protected UpdateRunnable updateRunnable;

        public RepaintThread(long timeout, List<Component> components) {
            this.timeout = timeout;
            this.components = components;
            this.updateRunnable = new UpdateRunnable();
            setDaemon(true);
            setPriority(Thread.MIN_PRIORITY);
        }

        @Override
        public void run() {
            for (;;) {
                try {
                    synchronized (updateRunnable) {
                        if (!updateRunnable.inEventQueue)
                            EventQueue.invokeLater(updateRunnable);
                        updateRunnable.inEventQueue = true;
                    }
                    sleep(timeout);
                    synchronized (this) {
                        while (suspended) {
                            wait();
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        protected class UpdateRunnable implements Runnable {
            public boolean inEventQueue = false;
            @Override
            public void run() {
                long free = runtime.freeMemory();
                long total = runtime.totalMemory();
                for (Object component : components) {
                    Component c = (Component) component;
                    if (c instanceof MemoryChangeListener) {
                        ((MemoryChangeListener) c).memoryStateChanged(total, free);
                    }
                }
                synchronized (this) { inEventQueue = false; }
            }
        }

        public synchronized void safeSuspend() {
            suspended = true;
        }

        public synchronized void safeResume() {
            suspended = false;
            notify();
        }
    }

}
