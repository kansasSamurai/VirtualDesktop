# Feature Specification: `MemoryMonitorPanel`

## 1. Overview & Purpose
* **Class:** `org.apache.batik.util.gui.MemoryMonitorPanel` (extends `javax.swing.JPanel`, implements `ActionMap`)
* **Objective:** Provide an embeddable, zero-allocation, GPU-accelerated JVM heap telemetry widget for Java Swing applications.
* **Architectural Shift:** Replaces the legacy `JFrame`-based Apache Batik `MemoryMonitor` with a self-managing `JPanel` container that eliminates self-induced object allocation churn (preventing false sawtooth GC patterns during idle states) and leverages VRAM-backed `VolatileImage` offscreen caching.

---

## 2. Core Architecture & Hierarchy

### 2.1 Container & Lifecycle Management (`MemoryMonitorPanel`)
* **Layout:** `BorderLayout` containing a titled `MonitorContainerPanel` (`CENTER`) and a right-aligned `FlowLayout` control bar (`SOUTH`) housing a `CollectButton` (`System.gc()`).
* **Automated Thread Lifecycle:**
  * Overrides `addNotify()` to automatically start or resume the background `RepaintThread` when mounted to a visible Swing hierarchy.
  * Overrides `removeNotify()` to automatically suspend the `RepaintThread` when unmounted or hidden, eliminating manual `WindowListener` boilerplate and preventing zombie threads.
* **Configuration:** Supports default `1000ms` polling interval (`MemoryMonitorPanel()`) or custom millisecond intervals (`MemoryMonitorPanel(long time)`).

### 2.2 Sub-Component Layout (`MonitorContainerPanel`)
* Uses `GridBagLayout` with `5px` insets and lowered bevel borders:
  * **Left Pane (`weightx = 0.3`):** `Usage` component (segmented LED block meter).
  * **Right Pane (`weightx = 0.7`):** `History` component (scrolling time-series heap graph).

### 2.3 Rendering Engine (`AbstractVolatileImageComponent`)
* **VRAM Caching:** Allocates a hardware-accelerated `VolatileImage` via `GraphicsConfiguration.createCompatibleVolatileImage(...)` sized to physical device pixels (`Math.ceil(logicalDimension * scaleFactor)`).
* **Dirty-State Redrawing:** Re-rasterizes into the VRAM buffer *only* when `VolatileImage.validate()` returns `IMAGE_RESTORED`/`IMAGE_INCOMPATIBLE`, dimensions/DPI change, or `shouldRedrawBuffer()` returns `true`. Otherwise, performs a direct GPU blit (`g2d.drawImage(...)`).
* **Multi-Monitor & HiDPI Awareness:**
  * Reads `GraphicsConfiguration.getDefaultTransform()` to apply `bufG2d.scale(scaleX, scaleY)` so subclass drawing logic operates in logical coordinates while rasterizing at native physical pixel density.
  * Registers a `PropertyChangeListener` on `"graphicsConfiguration"` to immediately flush stale VRAM buffers (`offscreenBuffer.flush()`) and trigger `invalidateFontMetricsCache()` when dragged across displays with differing DPI scaling factors.
  * Cleans up listeners and VRAM buffers in `removeNotify()`.

---

## 3. Sub-Component Specifications

### 3.1 `Usage` (LED Block Indicator)
* **Dimensions:** Preferred `90x100` logical pixels.
* **Visual Elements:**
  * Renders 15 vertical discrete memory blocks (`BLOCKS = 15`), color-coded by threshold tier (`BLOCK_TYPE` indices `0` = high/red, `1` = medium/orange, `2` = low/green) with distinct active (`usedColors`) and inactive (`freeColors`) palettes.
  * Displays formatted `total` and `used` memory readouts in kilobytes (`totalMemory / 1024`) at top and bottom margins, respecting localization prefix/postfix rules (`POSTFIX`).
* **Optimization:** Sets `memoryChanged = true` only when `total` or `free` byte counts actually differ from the previous poll.

### 3.2 `History` (Time-Series Graph & Event Telemetry)
* **Dimensions:** Preferred `200x100` logical pixels, retaining a sliding window of `MAX_HISTORY = 190` data points.
* **Zero-Allocation Data Structures:**
  * Replaces `LinkedList<Long>` and per-tick `GeneralPath` instantiation with fixed-size parallel primitive circular buffers (`long[] usedMemoryBuffer`, `boolean[] gcEventBuffer`, `boolean[] spikeEventBuffer`) and a single reusable `GeneralPath` (`path.reset()`).
  * Pre-allocates all `Color` and `BasicStroke` instances as fields/constants to ensure zero young-generation heap churn during steady-state polling.
* **Diagnostic Event Detection:**
  * **GC Event Marker:** Triggered when `free > prevFree + 2MB`; renders a vertical cyan tick (`#00FFC8`) along the top edge (`y = 5..12`) at the corresponding X coordinate.
  * **Allocation Spike Marker:** Triggered when `used - prevUsed > 5MB`; renders a full-height red vertical indicator line (`#FF5050`) across the plot area.
* **Grid & Rendering:** Draws a scrolling vertical grid (`xShift` incremented modulo `10` once buffer reaches capacity), static horizontal grid lines, the yellow heap usage curve, and a green bounding border.

### 3.3 `RepaintThread` (Telemetry Poller)
* Daemon thread running at `Thread.MIN_PRIORITY`.
* Polls `Runtime.getRuntime().freeMemory()` and `totalMemory()` at the configured `timeout` interval.
* Coalesces UI updates onto the EDT via a reusable `UpdateRunnable` guarded by an `inEventQueue` boolean flag to prevent EventQueue flooding under UI thread contention.

---

## 4. Usage & Integration Contract

### Instantiation
```java
// Drop-in usage; no manual thread startup or window listeners required
MemoryMonitorPanel monitor = new MemoryMonitorPanel(1000);
parentContainer.add(monitor, BorderLayout.EAST);
```

### Invariants & Constraints for Extensions

1. **Zero-Allocation Paint Path:** Any modifications to `renderToBuffer(...)` or `rebuildPath()` must not allocate heap objects (`Color`, `Stroke`, `Shape`, boxed primitives) per frame.
2. **Thread Confinement:** `memoryStateChanged(long total, long free)` is invoked on the EDT via `UpdateRunnable` and synchronized on the component instance; buffer mutations and `repaint()` calls remain thread-safe.
3. **Resource Bundle Dependency:** Requires `org.apache.batik.util.gui.resources.MemoryMonitorMessages` and Batik `ResourceManager` / `ButtonFactory` utilities on the classpath.
