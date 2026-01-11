## The Flow

  1. ActionFactory.registerExternalApp() (lines 145-155):
    - Creates both large (48x48) and small (16x16) icons
    - Sets them on the ExternalAppAction using standard Action keys:
    action.putValue(Action.LARGE_ICON_KEY, icon);  // 48x48
  action.putValue(Action.SMALL_ICON, icon);       // 16x16
  2. ExternalAppAction.run() (lines 58-60):
    - Creates the ExternalAppSpec
    - Only passes the LARGE icon to the spec:
    if (getValue(LARGE_ICON_KEY) != null) {
      spec.setIcon((javax.swing.Icon) getValue(LARGE_ICON_KEY));  // ⚠️ Using 48x48!
  }
  3. VirtualAppSpec (line 29):
    - Has only ONE icon property: private Icon icon;
    - No separate large/small icon support
  4. DesktopManager.createVApp() (lines 164, 173):
    - Gets the single icon from spec and sets it on the frame:
    Icon icon = spec.getIcon();
  frame.setFrameIcon(icon);
  5. VAppListCellRenderer (line 28):
    - Displays whatever icon is on the frame:
    c.setIcon(myvalue.getFrameIcon());  // This is the 48x48 icon!

  The Root Cause

  ExternalAppAction:59 is setting the LARGE_ICON_KEY (48x48) when it should be setting the SMALL_ICON (16x16), because VirtualAppSpec only has one icon property that eventually gets displaye        ed in the JList.
  