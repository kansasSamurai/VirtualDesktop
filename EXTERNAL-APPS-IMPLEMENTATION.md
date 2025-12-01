# External Application Launching - Implementation Summary

## Overview

The VirtualDesktop project has been enhanced to support launching external applications (Windows executables, scripts, etc.) from desktop icons, in addition to the existing internal vapps (JInternalFrame-based applications).

## What Was Implemented

### 1. New Classes Created

#### `ExternalAppSpec.java`
- Extends `VirtualAppSpec` but launches external processes instead of creating JInternalFrames
- Uses `SystemCommandExecutor` for cross-platform process execution
- Supports Windows (via `cmd.exe /C`) and Unix/Linux (direct execution)
- Features:
  - Command line execution
  - Optional working directory
  - Optional wait-for-completion mode
  - Background execution by default

#### `ExternalAppAction.java`
- Similar to `DesktopAction` but for external applications
- Integrates with the existing Action framework
- Creates `ExternalAppSpec` instances and launches them via `DesktopManager`

#### `ExternalAppConfig.java`
- Java bean for JSON deserialization of individual app configurations
- Properties: name, command, icon, workingDir, desktopOnly, waitForCompletion

#### `ExternalAppsConfig.java`
- Container class for list of external app configurations
- Supports Jackson JSON deserialization

### 2. Modified Classes

#### `DesktopManager.java`
- Added detection for `ExternalAppSpec` instances in `createVApp(Object)`
- External apps are launched directly without creating JInternalFrames
- Line 76-93: New logic to check instance type and launch appropriately

#### `ActionFactory.java`
- Added `loadExternalApps()` method to read JSON configuration
- Added `registerExternalApp()` method to create actions for external apps
- Automatically loads `config/external-apps.json` on startup
- Gracefully handles missing configuration file

### 3. Configuration Files

#### `config/external-apps.json`
Example configuration with common Windows applications:
- Notepad
- Calculator
- Command Prompt
- Paint

#### `config/README-external-apps.md`
Complete documentation including:
- File structure and property descriptions
- Examples for various scenarios
- Platform-specific notes (Windows vs Linux/Mac)
- Available icon paths
- Troubleshooting guide

## How It Works

### Execution Flow

1. **Startup**: `ActionFactory.initDesktop()` calls `loadExternalApps()`
2. **Configuration Loading**: Reads `config/external-apps.json` using Jackson ObjectMapper
3. **Action Registration**: For each app config, creates `ExternalAppAction` and adds to action list
4. **Icon Creation**: Desktop icons are created in `App.java` from the action list
5. **User Interaction**: User double-clicks icon
6. **Action Execution**: `ExternalAppAction.actionPerformed()` → `run()`
7. **Spec Creation**: Creates `ExternalAppSpec` with command and settings
8. **Launch Detection**: `DesktopManager.createVApp()` detects `ExternalAppSpec` instance
9. **Process Launch**: `ExternalAppSpec.launch()` executes the command
10. **Platform Detection**: Detects OS and uses appropriate execution method
11. **Process Execution**:
    - Windows: Uses `SystemCommandExecutor` with `cmd.exe /C`
    - Unix/Linux: Uses `Runtime.exec()`

### Architecture Integration

The solution integrates seamlessly with existing architecture:

```
VShortcut (Desktop Icon)
    ↓
ExternalAppAction.actionPerformed()
    ↓
ExternalAppAction.run()
    ↓
Creates ExternalAppSpec
    ↓
DesktopManager.createVApp(ExternalAppSpec)
    ↓
Detects ExternalAppSpec → launches directly
    ↓
ExternalAppSpec.launch()
    ↓
SystemCommandExecutor or Runtime.exec()
```

Compared to internal vapps:

```
VShortcut (Desktop Icon)
    ↓
DesktopAction.actionPerformed()
    ↓
DesktopAction.run()
    ↓
Creates VirtualAppSpec via reflection
    ↓
DesktopManager.createVApp(VirtualAppSpec)
    ↓
Creates VirtualAppFrame (JInternalFrame)
```

## Usage

### Adding External Applications

1. Edit `config/external-apps.json`
2. Add new application entry:
   ```json
   {
     "name": "My Application",
     "command": "path/to/executable",
     "icon": "org/jwellman/virtualdesktop/images/global_ui/icon_name",
     "desktopOnly": true
   }
   ```
3. Restart VirtualDesktop
4. New icon appears on desktop

### Example Configurations

**Simple executable:**
```json
{
  "name": "Notepad",
  "command": "notepad.exe"
}
```

**With working directory:**
```json
{
  "name": "My Project",
  "command": "code .",
  "workingDir": "C:/Projects/MyProject"
}
```

**Full path with arguments:**
```json
{
  "name": "Edit Config",
  "command": "notepad.exe C:\\config\\app.properties"
}
```

## Features

### Supported
- ✅ Desktop icon creation
- ✅ SVG icon support
- ✅ Windows command execution
- ✅ Linux/Mac command execution
- ✅ Working directory specification
- ✅ Background execution (default)
- ✅ Wait-for-completion mode
- ✅ JSON configuration
- ✅ Graceful error handling
- ✅ Console logging

### Not Supported (Future Enhancements)
- ❌ Menu bar integration (only desktop icons currently)
- ❌ Configuration file hot-reload
- ❌ Process monitoring UI
- ❌ Standard output capture display
- ❌ Process termination from UI

## Testing

To test the implementation:

1. Verify the example configuration works:
   - Launch VirtualDesktop
   - Check console for "Loaded 4 external app(s) from configuration"
   - Look for new icons on desktop (Notepad, Calculator, etc.)
   - Double-click an icon to launch

2. Add your own application:
   - Edit `config/external-apps.json`
   - Add your application configuration
   - Restart VirtualDesktop
   - Test launching

3. Test error handling:
   - Try invalid command path
   - Check console for error messages
   - Try missing configuration file
   - Verify graceful degradation

## Benefits

1. **User-Configurable**: No code changes needed to add new external apps
2. **Platform-Aware**: Automatically handles Windows vs Unix differences
3. **Backward Compatible**: Existing internal vapps continue to work unchanged
4. **Consistent UI**: External apps use same icon system as internal vapps
5. **Extensible**: Easy to add new properties to configuration
6. **Leverages Existing Code**: Uses proven `SystemCommandExecutor` utility

## Future Enhancements

Potential improvements for future releases:

1. **Menu Integration**: Add external apps to menu bar, not just desktop
2. **Hot Reload**: Reload configuration without restart
3. **Process Manager**: UI to view/manage running external processes
4. **Output Viewer**: Display stdout/stderr in JInternalFrame
5. **Environment Variables**: Support for custom environment variables
6. **Conditional Loading**: Platform-specific app definitions
7. **Icon Management**: GUI tool to browse and select icons
8. **Validation**: JSON schema validation with helpful error messages

## Files Changed/Created

### Created:
- `src/main/java/org/jwellman/virtualdesktop/vapps/ExternalAppSpec.java`
- `src/main/java/org/jwellman/virtualdesktop/vapps/ExternalAppAction.java`
- `src/main/java/org/jwellman/virtualdesktop/vapps/ExternalAppConfig.java`
- `src/main/java/org/jwellman/virtualdesktop/vapps/ExternalAppsConfig.java`
- `config/external-apps.json`
- `config/README-external-apps.md`
- `EXTERNAL-APPS-IMPLEMENTATION.md` (this file)

### Modified:
- `src/main/java/org/jwellman/virtualdesktop/DesktopManager.java` (lines 76-93)
- `src/main/java/org/jwellman/virtualdesktop/vapps/ActionFactory.java` (added import, methods)

## Conclusion

The external application launching feature is fully implemented and ready for use. It provides a clean, extensible architecture that integrates seamlessly with the existing VirtualDesktop framework while maintaining backward compatibility with all internal vapps.
