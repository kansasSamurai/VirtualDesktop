# External Applications Configuration

This directory contains configuration for external applications that can be launched from VirtualDesktop icons.

## Configuration File

The `external-apps.json` file defines which external applications appear as desktop icons.

### File Structure

```json
{
  "externalApps": [
    {
      "name": "Application Name",
      "command": "command to execute",
      "icon": "path/to/icon",
      "workingDir": "optional/working/directory",
      "desktopOnly": true,
      "waitForCompletion": false
    }
  ]
}
```

### Properties

- **name** (required): Display name shown under the desktop icon
- **command** (required): Command line to execute
  - For Windows: Can be executable name (e.g., "notepad.exe") or full path
  - For Linux/Mac: Full command including any arguments
- **icon** (optional): Path to SVG icon resource
  - If omitted, uses default icon
  - Path is relative to resources (e.g., "org/jwellman/virtualdesktop/images/global_ui/document176")
- **workingDir** (optional): Working directory for the process
  - If omitted, uses current directory
- **desktopOnly** (optional): Whether icon appears only on desktop (default: true)
- **waitForCompletion** (optional): Whether to wait for process to exit (default: false)

## Examples

### Simple Application

```json
{
  "name": "Notepad",
  "command": "notepad.exe",
  "icon": "org/jwellman/virtualdesktop/images/global_ui/document176"
}
```

### Application with Working Directory

```json
{
  "name": "My Project",
  "command": "code .",
  "workingDir": "C:/Projects/MyProject",
  "icon": "org/jwellman/virtualdesktop/images/global_ui/folder197"
}
```

### Application with Full Path (Windows)

```json
{
  "name": "Firefox",
  "command": "C:\\Program Files\\Mozilla Firefox\\firefox.exe",
  "icon": "org/jwellman/virtualdesktop/images/global_ui/internet39"
}
```

### Command with Arguments

```json
{
  "name": "Edit Config",
  "command": "notepad.exe C:\\config\\app.properties",
  "icon": "org/jwellman/virtualdesktop/images/global_ui/settings78"
}
```

## Platform Differences

### Windows
- Commands are executed via `cmd.exe /C`
- Can use simple executable names (searches PATH)
- Use double backslashes in paths: `C:\\Program Files\\...`

### Linux/Mac
- Commands are executed directly via shell
- Usually need full path to executable
- Use forward slashes in paths: `/usr/bin/...`

## Available Icons

Common icon paths in the VirtualDesktop resources:

- Documents: `org/jwellman/virtualdesktop/images/global_ui/document176`
- Folder: `org/jwellman/virtualdesktop/images/global_ui/folder197`
- Calculator: `org/jwellman/virtualdesktop/images/global_ui/calculator29`
- Console: `org/jwellman/virtualdesktop/images/global_ui/console1`
- Settings: `org/jwellman/virtualdesktop/images/global_ui/settings78`
- Internet: `org/jwellman/virtualdesktop/images/global_ui/internet39`
- Paintbrush: `org/jwellman/virtualdesktop/images/global_ui/paintbrush9`
- Calendar: `org/jwellman/virtualdesktop/images/global_ui/calendar168`
- Home: `org/jwellman/virtualdesktop/images/global_ui/home156`
- Trash: `org/jwellman/virtualdesktop/images/global_ui/rubbish1`

Browse the `src/main/resources/org/jwellman/virtualdesktop/images/global_ui/` directory for more icons.

## Troubleshooting

### Icon doesn't appear
- Check that the configuration file is valid JSON
- Verify the command is accessible from command line
- Check console output for error messages

### Application doesn't launch
- Test the command manually in command prompt/terminal
- Check file paths (use absolute paths if relative don't work)
- Verify working directory exists
- Check console output for error messages

### Icon shows but with default image
- Verify icon path is correct
- Check that the icon file exists in resources
- Omit `.svg` extension from icon path

## Reloading Configuration

Changes to `external-apps.json` require restarting VirtualDesktop to take effect.
