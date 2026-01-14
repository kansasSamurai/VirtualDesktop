# VApps Configuration Guide

## Overview

The `config/vapps-config.json` file controls how virtual applications (vapps) are organized and displayed in the VirtualDesktop application. This configuration allows you to customize the menu structure, define desktop shortcuts, and organize applications to suit your workflow.

## Configuration Structure

```json
{
  "version": "1.0.0",
  "defaultIcon": "home156",
  "menuStructure": [ /* array of MenuGroup objects */ ],
  "desktopShortcuts": [ /* array of DesktopShortcut objects */ ]
}
```

### Top-Level Properties

- **version**: Configuration schema version (currently "1.0.0")
- **defaultIcon**: Default icon key to use when a vapp doesn't specify one
- **menuStructure**: Array of MenuGroup objects that define how vapps appear in the VApps menu
- **desktopShortcuts**: Array of shortcuts to display on the desktop

## Menu Types

The `menuStructure` supports three menu types, allowing flexible organization:

### 1. Inline Type

**Purpose**: Add vapps directly to the VApps menu without creating a submenu wrapper.

**Use case**: When you want quick access to frequently-used vapps without navigating submenus.

**Example**:
```json
{
  "type": "inline",
  "label": "",
  "mnemonic": "",
  "groups": [],
  "vapps": [
    {
      "class": "org.jwellman.virtualdesktop.vapps.SpecBeanShell",
      "title": "BeanShell Console",
      "icon": "laptop123",
      "enabled": true,
      "desktopOnly": false
    },
    {
      "class": "org.jwellman.virtualdesktop.vapps.SpecJCXConsole",
      "title": "JCX Console",
      "icon": "laptop123",
      "enabled": true,
      "desktopOnly": false
    }
  ]
}
```

**Result**:
```
VApps
├── BeanShell Console
└── JCX Console
```

**Note**: The `label` and `mnemonic` fields are ignored for inline types.

### 2. Flat Type

**Purpose**: Create a submenu under the VApps menu that contains vapps.

**Use case**: Organize related vapps into logical categories.

**Example**:
```json
{
  "type": "flat",
  "label": "Development Tools",
  "mnemonic": "D",
  "groups": [],
  "vapps": [
    {
      "class": "org.jwellman.virtualdesktop.vapps.SpecBeanShell",
      "title": "BeanShell Console",
      "icon": "laptop123",
      "enabled": true,
      "desktopOnly": false
    },
    {
      "class": "org.jwellman.virtualdesktop.vapps.SpecGroovyConsole",
      "title": "Groovy Console",
      "icon": "laptop123",
      "enabled": true,
      "desktopOnly": false
    }
  ]
}
```

**Result**:
```
VApps
└── Development Tools
    ├── BeanShell Console
    └── Groovy Console
```

### 3. Group Type

**Purpose**: Create hierarchical menus with nested subgroups.

**Use case**: Complex menu structures with multiple levels of organization.

**Example**:
```json
{
  "type": "group",
  "label": "Development",
  "mnemonic": "D",
  "groups": [
    {
      "type": "flat",
      "label": "Scripting",
      "mnemonic": "S",
      "groups": [],
      "vapps": [
        {
          "class": "org.jwellman.virtualdesktop.vapps.SpecBeanShell",
          "title": "BeanShell Console",
          "icon": "laptop123",
          "enabled": true,
          "desktopOnly": false
        },
        {
          "class": "org.jwellman.virtualdesktop.vapps.SpecGroovyConsole",
          "title": "Groovy Console",
          "icon": "laptop123",
          "enabled": true,
          "desktopOnly": false
        }
      ]
    },
    {
      "type": "flat",
      "label": "Terminals",
      "mnemonic": "T",
      "groups": [],
      "vapps": [
        {
          "class": "org.jwellman.virtualdesktop.vapps.SpecJCXConsole",
          "title": "JCX Console",
          "icon": "laptop123",
          "enabled": true,
          "desktopOnly": false
        },
        {
          "class": "org.jwellman.virtualdesktop.vapps.SpecJediTerm",
          "title": "JediTerm Terminal",
          "icon": "laptop123",
          "enabled": true,
          "desktopOnly": false
        }
      ]
    }
  ],
  "vapps": []
}
```

**Result**:
```
VApps
└── Development
    ├── Scripting
    │   ├── BeanShell Console
    │   └── Groovy Console
    └── Terminals
        ├── JCX Console
        └── JediTerm Terminal
```

**Note**: Group type can contain vapps at the top level AND nested groups.

## MenuGroup Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| type | string | Yes | Menu type: "inline", "flat", or "group" |
| label | string | No* | Display label for the menu (ignored for inline type) |
| mnemonic | string | No | Single character keyboard mnemonic (ignored for inline type) |
| groups | array | Yes | Array of nested MenuGroup objects (used only by group type) |
| vapps | array | Yes | Array of VappConfig objects to include in this menu |

*Required for flat and group types, ignored for inline type.

## VappConfig Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| class | string | Yes | Fully qualified class name of the vapp |
| title | string | No | Display name in menu (defaults to class simple name) |
| icon | string | No | Icon key (uses defaultIcon if not specified) |
| enabled | boolean | Yes | Whether to load this vapp |
| desktopOnly | boolean | Yes | If true, vapp appears only in desktop shortcuts, not menus |

## Desktop Shortcuts

Desktop shortcuts provide quick access to vapps directly from the desktop.

**Example**:
```json
"desktopShortcuts": [
  {
    "class": "org.jwellman.virtualdesktop.vapps.SpecJCXConsole",
    "label": "Home",
    "icon": "home156",
    "enabled": true
  },
  {
    "class": "org.jwellman.virtualdesktop.vapps.SpecBeanShell",
    "label": "BeanShell",
    "icon": "laptop123",
    "enabled": true
  }
]
```

### DesktopShortcut Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| class | string | Yes | Fully qualified class name of the vapp |
| label | string | Yes | Display label for the desktop icon |
| icon | string | No | Icon key (uses defaultIcon if not specified) |
| enabled | boolean | Yes | Whether to show this shortcut |

## Complete Example

Here's a complete configuration demonstrating all three menu types:

```json
{
  "version": "1.0.0",
  "defaultIcon": "home156",
  "menuStructure": [
    {
      "type": "inline",
      "label": "",
      "mnemonic": "",
      "groups": [],
      "vapps": [
        {
          "class": "org.jwellman.virtualdesktop.vapps.SpecJCXConsole",
          "title": "JCX Console",
          "icon": "laptop123",
          "enabled": true,
          "desktopOnly": false
        }
      ]
    },
    {
      "type": "flat",
      "label": "Database",
      "mnemonic": "B",
      "groups": [],
      "vapps": [
        {
          "class": "org.jwellman.virtualdesktop.vapps.SpecHyperSQL",
          "title": "HyperSQL Manager",
          "icon": "download171",
          "enabled": true,
          "desktopOnly": false
        },
        {
          "class": "org.jwellman.virtualdesktop.vapps.SpecHyperSQLClient",
          "title": "HyperSQL Client",
          "icon": "download171",
          "enabled": true,
          "desktopOnly": false
        }
      ]
    },
    {
      "type": "group",
      "label": "Development",
      "mnemonic": "D",
      "groups": [
        {
          "type": "flat",
          "label": "Scripting",
          "mnemonic": "S",
          "groups": [],
          "vapps": [
            {
              "class": "org.jwellman.virtualdesktop.vapps.SpecBeanShell",
              "title": "BeanShell Console",
              "icon": "laptop123",
              "enabled": true,
              "desktopOnly": false
            },
            {
              "class": "org.jwellman.virtualdesktop.vapps.SpecGroovyConsole",
              "title": "Groovy Console",
              "icon": "laptop123",
              "enabled": true,
              "desktopOnly": false
            }
          ]
        }
      ],
      "vapps": []
    }
  ],
  "desktopShortcuts": [
    {
      "class": "org.jwellman.virtualdesktop.vapps.SpecJCXConsole",
      "label": "Home",
      "icon": "home156",
      "enabled": true
    }
  ]
}
```

**Result**:
```
VApps
├── JCX Console (inline - no submenu)
├── Database
│   ├── HyperSQL Manager
│   └── HyperSQL Client
└── Development
    └── Scripting
        ├── BeanShell Console
        └── Groovy Console
```

## Icon Keys

Icon keys reference icons in the icon theme system. Common icon keys include:

- `home156` - Home/house icon
- `laptop123` - Laptop/console icon
- `download171` - Database/download icon
- `stats9` - Chart/statistics icon
- `calendar168` - Calendar icon
- `briefcase58` - File/briefcase icon
- `palette7` - Paint palette icon
- `signal49` - Signal/bars icon
- `add196` - Add/plus icon

See the icon theme configuration for available icons in your installation.

## Tips and Best Practices

1. **Testing Workflow**: Use inline type during development/testing to minimize submenu navigation
2. **Production Layout**: Use flat or group types to organize vapps into logical categories
3. **Mixed Approach**: Combine inline (for frequently-used tools) with flat/group (for organized categories)
4. **Keyboard Access**: Always specify mnemonics for flat/group types to enable keyboard navigation
5. **Disabled Vapps**: Set `enabled: false` to temporarily remove a vapp without deleting its configuration
6. **Desktop Only**: Use `desktopOnly: true` for vapps you only want as desktop shortcuts

## Troubleshooting

**Problem**: Vapp doesn't appear in menu
**Solution**: Check that `enabled: true` and `desktopOnly: false`

**Problem**: Icon not displaying
**Solution**: Verify the icon key exists in your icon theme, or use the defaultIcon

**Problem**: Menu structure not updating
**Solution**: Restart the application to reload the configuration

**Problem**: Configuration not loading
**Solution**: Validate JSON syntax using a JSON validator tool
