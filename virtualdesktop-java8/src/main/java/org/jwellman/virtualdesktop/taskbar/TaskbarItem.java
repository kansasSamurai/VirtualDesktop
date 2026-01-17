package org.jwellman.virtualdesktop.taskbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import org.jwellman.virtualdesktop.state.model.FrameState;

/**
 * View model representing an item in the taskbar.
 *
 * Can represent either a single tool or a group of tools (when grouping is enabled).
 * This decouples the view from the Redux state model.
 *
 * @author rwellman
 */
public class TaskbarItem {

    private final String id;
    private final String title;
    private final String toolType;
    private final Icon icon;
    private final FrameState frameState;
    private final boolean isGroup;
    private final List<TaskbarItem> groupedItems;

    /**
     * Create a single tool item.
     */
    public TaskbarItem(String id, String title, String toolType, Icon icon, FrameState frameState) {
        this.id = id;
        this.title = title;
        this.toolType = toolType;
        this.icon = icon;
        this.frameState = frameState;
        this.isGroup = false;
        this.groupedItems = Collections.emptyList();
    }

    /**
     * Create a group item containing multiple tools.
     */
    public TaskbarItem(String toolType, Icon icon, List<TaskbarItem> items) {
        this.id = "group:" + toolType;
        this.title = toolType + " (" + items.size() + ")";
        this.toolType = toolType;
        this.icon = icon;
        this.frameState = FrameState.NORMAL;
        this.isGroup = true;
        this.groupedItems = new ArrayList<>(items);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getToolType() {
        return toolType;
    }

    public Icon getIcon() {
        return icon;
    }

    public FrameState getFrameState() {
        return frameState;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public List<TaskbarItem> getGroupedItems() {
        return Collections.unmodifiableList(groupedItems);
    }

    public int getGroupSize() {
        return isGroup ? groupedItems.size() : 1;
    }

    @Override
    public String toString() {
        return title;
    }

}
