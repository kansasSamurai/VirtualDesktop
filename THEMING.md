  Let me explain what's fundamentally different:

  Current System vs Themed System

  Current Problem

  You write code like this:
  Icon icon = DSP.Icons.getIcon("home156-16");

  If a user wants all icons bigger (for accessibility, preference, high-DPI display), they're stuck. You'd have to:
  1. Find every "-16" in the codebase
  2. Change to "-24"
  3. Find every "-48"
  4. Change to "-64"
  5. Recompile and redistribute

  With Theming (The Key Feature)

  You write code like this:
  Icon icon = DSP.Icons.getIcon("home156", IconContext.LIST);  // or just pass an enum

  Now the user can select in preferences:
  - "Default Icons" (16px for lists, 48px for desktop)
  - "Large Icons" (24px for lists, 64px for desktop)
  - "Extra Large Icons" (32px for lists, 96px for desktop)

  No code changes needed. The theme controls the size mapping.

  The Real Question

  Before I design further - what features do YOU want users to have?

  1. Theme Selection? - Pick from preset themes (Default, Large Icons, High Contrast)
  2. Custom Sizes? - "I want list icons to be 20px" (user config)
  3. Color Coordination? - Dark theme = different icon colors
  4. Per-App Preferences? - "This app uses large icons, that one uses small"
  5. Runtime Switching? - Change theme without restart
  
-------------------------

Ultimately, I want to support 
features 1, 2, 3, and 5.  I do not want to support per app preferences and I doubt I will      
change my mind later on that.  I will say that I want everything to be configured using        
human readable config file and .json is my desired format.  Another thing I want to nail       
down is the building of the colors/palette (because ultimately I want to build tools to        
help edit all this information). I want your feedback on "standard practices" but:  it         
seems like there are a few steps in building a theme palette.  1) choose actual color 
palette with each color getting a symbolic name, 2) assign those symbolic color names to       
symbolic theme names like "desktopBackground", "iconForeground", "iconBackground", "info",     
"warning", "success", "failure", etc. [those are just example for discussion to be vetted      
later].  Finally, the color palette would have to be "married" with the icons (also 
recognizing that some icons may be "fixed" in their color scheme and not modifiable at         
runtime). 

● Excellent! Now we're having the right design conversation. Let me break down standard practices for each feature and get your feedback:

  1. Color Palette Architecture

  Your multi-step approach is exactly what modern design systems do (Material Design, Apple HIG, Microsoft Fluent). Here's the standard pattern:

  Step 1: Base Color Palette (Named Colors)

  {
    "palette": {
      "blue100": "#E3F2FD",
      "blue500": "#2196F3",
      "blue900": "#0D47A1",
      "gray100": "#F5F5F5",
      "gray500": "#9E9E9E",
      "gray900": "#212121",
      "red500": "#F44336",
      "green500": "#4CAF50",
      "amber500": "#FFC107"
    }
  }

  Why this works: You can swap entire palettes (light → dark) without touching semantic mappings.

  Step 2: Semantic Color Roles (UI Purpose)

  {
    "colors": {
      "background": {
        "primary": "gray100",      // References palette
        "secondary": "gray50",
        "desktop": "blue900"
      },
      "foreground": {
        "primary": "gray900",
        "secondary": "gray500",
        "disabled": "gray300"
      },
      "icon": {
        "default": "gray500",
        "active": "blue500",
        "disabled": "gray300"
      },
      "status": {
        "info": "blue500",
        "success": "green500",
        "warning": "amber500",
        "error": "red500"
      }
    }
  }

  Step 3: Component-Specific Mappings (Optional)

  {
    "components": {
      "menuIcon": {
        "foreground": "icon.default",
        "background": "transparent"
      },
      "desktopIcon": {
        "foreground": "icon.active",
        "background": "background.desktop"
      }
    }
  }

  Question for you: Do you want Step 3 (component-specific), or is Step 2 (semantic roles) enough?

  ---
  2. Icon-Color Marriage (The Tricky Part)

  You're right that some icons are "fixed" (like PNG photos) and some are "themeable" (SVG with CSS variables, icon fonts). Here's the standard approach:

  Icon Metadata (Part of theme or separate?)

  {
    "icons": {
      "home156": {
        "colorable": true,           // Can be tinted
        "type": "svg",               // or "font" or "raster"
        "defaultForeground": "icon.default",
        "defaultBackground": "transparent"
      },
      "photo_background": {
        "colorable": false,          // Fixed colors (like a JPG)
        "type": "raster"
      }
    }
  }

  Standard Practice: Most systems have:
  - Symbolic icons (colorable): SVG with single color, icon fonts
  - Full-color icons (non-colorable): Detailed SVG, PNG/JPG

  Questions:
  1. Should icon metadata be in the theme JSON or separate icon catalog?
  2. Do you want to support SVG color replacement (e.g., find all fill="#000000" and replace with theme color)?

  ---
  3. Theme Selection (Feature 1)

  Standard Practice: Themes are JSON files in a directory:

  config/themes/
    ├── default.json
    ├── dark.json
    ├── high-contrast.json
    └── user-custom.json

  Each theme file is complete (has all required fields). User selects via:
  - UI preference dialog
  - Command-line flag: --theme=dark
  - Config file: config/preferences.json → { "theme": "dark" }

  Question: Where do you want to store the user's theme selection?

  ---
  4. Custom Sizes (Feature 2)

  Two Approaches:

  A. Per-Theme Custom Sizes (Most Common)

  User creates config/themes/my-custom.json:
  {
    "name": "My Custom Theme",
    "extends": "default",           // Inherit from default
    "iconSizes": {
      "small": 20,                  // Override just this
      "large": 60                   // Override just this
    }
  }

  B. Global Size Multiplier (Simpler)

  User sets in preferences:
  {
    "theme": "default",
    "iconSizeScale": 1.25          // 25% larger than theme default
  }
  System multiplies all icon sizes: 16 → 20, 48 → 60

  Question: Which approach do you prefer? Or both?

  ---
  5. Runtime Switching (Feature 5)

  Standard Practice: Use observer pattern or event bus:

  ThemeManager.getInstance().setTheme("dark");
  // Fires ThemeChangedEvent
  // UI components listen and refresh themselves

  Challenges:
  1. Icons are cached - Need to invalidate DSP.Icons cache
  2. Swing components don't auto-refresh - Need to call updateUI() or repaint()
  3. Some things need restart - Look and Feel (LAF) changes

  Question:
  - Full hot-swap (complex, some things may not work)?
  - Or "Preview and restart" (simpler, always works)?

  ---
  6. JSON Schema Proposal

  Based on your requirements, here's a complete theme file structure:

  {
    "name": "Default",
    "description": "Default VirtualDesktop theme",
    "version": "1.0",

    // STEP 1: Color Palette (Named Colors)
    "palette": {
      "blue100": "#E3F2FD",
      "blue500": "#2196F3",
      "blue900": "#0D47A1",
      "gray100": "#F5F5F5",
      "gray500": "#9E9E9E",
      "gray900": "#212121"
    },

    // STEP 2: Semantic Color Mappings
    "colors": {
      "background": {
        "desktop": "blue900",
        "window": "gray100",
        "panel": "gray100"
      },
      "foreground": {
        "primary": "gray900",
        "secondary": "gray500"
      },
      "icon": {
        "default": "gray500",
        "menu": "gray500",
        "toolbar": "gray500",
        "desktop": "gray100"
      },
      "status": {
        "info": "blue500",
        "success": "#4CAF50",      // Can also use direct color
        "warning": "#FFC107",
        "error": "#F44336"
      }
    },

    // Icon Sizes (Semantic → Pixels)
    "iconSizes": {
      "small": 16,
      "menu": 18,
      "toolbar": 24,
      "large": 48,
      "xlarge": 64
    },

    // Fonts
    "fonts": {
      "base": "Dialog-PLAIN-12",
      "title": "Dialog-BOLD-14",
      "menu": "Dialog-PLAIN-12"
    }
  }

  Question: Missing anything? Too much?

  ---
  7. Tool Building (Your Mention)

  You said you want to build tools to edit this. What kind of tool?

  Options:
  1. Visual Theme Editor - GUI app with color pickers, live preview
  2. JSON Editor with Validation - Schema-based editor
  3. Theme Generator - "Pick 3 colors, generate full theme"
  4. Theme Tester - See how theme looks on sample UI

  Which matters most to you first?

  ---
  My Recommendations (Based on Industry Standards)

  1. ✅ Use 2-step color system (palette → semantic) like you described
  2. ✅ JSON theme files in config/themes/ directory
  3. ✅ Theme extends/inheritance for custom themes
  4. ✅ Separate icon metadata from theme (icon catalog is reusable across themes)
  5. ⚠️ Preview-and-restart for theme switching (easier than full hot-swap)
  6. ✅ Start with preset themes, add custom size override later
  
  
-------------------------------------------

I like your recommendations except I want hotswap theme switching. When it comes time to     
  implement that, I think I have an idea how to implement (and, yes, we may have to make       
  changes to DSP).  Let me also answer some of your other questions:  A) I do want step        
  3/component specific theming options, B) I want icon metadata to be in the theme JSON, C)    
  Ultimately I do want SVG color replacement but it is a lower priority as I'm guessing that   
  even if it is lengthy to implement, it would be abstracted easily in the API and be a        
  "focused" change, D) user theme selection can be in a "global" config file, E) I want        
  Per-Theme Custom Sizes, F) this theming will not include look and feel considerations - for  
  the foreseaable future, look and feel is completely separate, G) I like the JSON Schema      
  Proposal - at least a cursory review looks like what I had in mind. H) Lets defer the        
  tooling conversation until we have made, tested, and mostly vetted the underlying
  implementation. 
  
-------------------------

1/11/2026

 Desktop Theming System - Feature Design

 Vision Statement

 Create a comprehensive, user-configurable theming system that allows users to customize the   
 visual appearance of VirtualDesktop applications through JSON configuration files, without    
 requiring code changes or recompilation.

 Core Problems Being Solved

 1. Hard-coded Sizes - Currently icon sizes are pixel values scattered throughout code
 2. No Theme Selection - Users cannot choose visual themes (dark, high contrast, large icons)  
 3. No Color Coordination - Colors are hard-coded, cannot be coordinated across the system     
 4. No Accessibility - Users with visual impairments cannot increase icon sizes
 5. No Customization - Developers cannot easily create themed variations of apps
 6. Scattered Configuration - No central place to define visual appearance

 Target Features (Agreed Upon)

 ✅ Feature 1: Theme Selection

 - Users can select from preset themes (Default, Dark, High Contrast, Large Icons, etc.)       
 - Themes are defined in JSON files: config/themes/*.json
 - User's choice stored in: config/preferences.json
 - Hot-swap themes at runtime without restart

 ✅ Feature 2: Custom Icon Sizes

 - Each theme defines its own icon size mappings
 - Users can create custom themes with different sizes
 - Uses inheritance: custom theme extends base theme, overrides only size values
 - Example: "My Large Icons" theme extends "Default", changes sizes from 16→24, 48→64

 ✅ Feature 3: Color Coordination

 - Three-level color system:
   a. Color Palette - Named colors (blue500, gray900, etc.)
   b. Semantic Roles - Purpose-based mappings (background.desktop, icon.menu, etc.)
   c. Component Mappings - Per-component color assignments
 - Dark theme swaps palette, all semantic mappings automatically adjust
 - Status colors (info, warning, success, error) configurable

 ✅ Feature 4: (Future) SVG Color Replacement

 - Lower priority but planned
 - Replace SVG fill colors at runtime based on theme
 - Allows monochrome SVG icons to match theme colors

 ❌ Feature NOT Supported: Per-App Preferences

 - No per-application theme overrides
 - System-wide theme applies to all VirtualDesktop apps

 ---
 Architectural Decisions

 Configuration Format: JSON

 - Human-readable and editable
 - Industry standard for configuration
 - Easy tooling support (validation, editors, etc.)
 - Location: config/themes/*.json

 Color Architecture: 3-Level System

 Following industry best practices (Material Design, Fluent, Apple HIG):

 Level 1 - Palette (Named colors, reusable)
 "palette": {
   "blue500": "#2196F3",
   "gray900": "#212121"
 }

 Level 2 - Semantic Roles (UI purpose)
 "colors": {
   "background": { "desktop": "blue900" },
   "icon": { "default": "gray500" },
   "status": { "error": "red500" }
 }

 Level 3 - Component Mappings (Specific components)
 "components": {
   "menuIcon": {
     "foreground": "icon.default",
     "background": "transparent"
   }
 }

 Why this matters:
 - Swap Level 1 (palette) → entire theme changes color scheme
 - Level 2 stays same → consistent semantic meaning
 - Level 3 fine-tunes → specific component needs

 Icon-Color Integration

 - Icon metadata stored IN theme JSON
 - Each icon declares if it's colorable (SVG/font) or fixed (raster)
 - Colorable icons reference theme colors
 - Non-colorable icons use original colors

 Icon Size System: Semantic Names

 Replace pixel values with contextual names:
 - SMALL - Lists, trees, small UI elements (default: 16px)
 - MENU - Menu items (default: 18px)
 - TOOLBAR - Toolbar buttons (default: 24px)
 - LARGE - Desktop shortcuts, prominent icons (default: 48px)
 - XLARGE - Special purposes (default: 64px)

 Themes can map these to different pixel values.

 Theme Inheritance

 Custom themes extend base themes:
 {
   "name": "My Custom",
   "extends": "default",
   "iconSizes": {
     "small": 20    // Override just this, inherit rest
   }
 }

 Hot-Swap Support

 When theme changes at runtime:
 1. ThemeManager fires ThemeChangedEvent
 2. UI components listen and refresh
 3. DSP.Icons cache invalidated and rebuilt
 4. Components repaint with new theme

 Note: Look and Feel (LAF) is separate, not part of theming.

 ---
 System Architecture

 Component 1: IconSize Enum

 Purpose: Semantic names for icon sizes instead of pixel values

 Key Concept: Code requests icons by context (SMALL, MENU, TOOLBAR, LARGE) not pixels

 Before (Bad):
 Icon icon = DSP.Icons.getIcon("home156-16");  // Hard-coded pixel size

 After (Good):
 Icon icon = ThemeManager.getIcon("home156", IconSize.SMALL);  // Semantic size

 Why: User can switch to "Large Icons" theme and SMALL becomes 24px instead of 16px

 Component 2: Theme Interface

 Purpose: Define the contract for what a theme provides

 Responsibilities:
 - Map semantic sizes (SMALL, LARGE) → pixel values (16, 48)
 - Provide color palette and semantic color mappings
 - Define component-specific color overrides
 - Provide font configuration
 - Declare icon metadata (which icons are colorable)

 Key Methods:
 - getIconSize(IconSize) → Returns pixel size for semantic size
 - getColor(String path) → Returns color by semantic path ("background.desktop", "icon.menu")  
 - getComponentColor(String component, String property) → Returns component-specific color     
 - isIconColorable(String iconName) → Whether icon can be themed

 Component 3: JSON-Based Theme Implementation

 Purpose: Load theme from JSON file, provide Theme interface

 Key Concept: Theme = JSON file parsed into Java object implementing Theme interface

 Supports:
 - Loading from config/themes/*.json
 - Theme inheritance ("extends": "default")
 - Validation (ensure all required fields present)
 - Defaults for missing optional fields

 Component 4: ThemeManager (Singleton Service)

 Purpose: Central service for theme management

 Responsibilities:
 - Discover available themes from config/themes/
 - Load and cache themes
 - Track current active theme
 - Fire ThemeChangedEvent when theme switches
 - Provide convenience methods for UI code

 Key Operations:
 - setTheme(String name) → Switch theme, fire events, invalidate caches
 - getCurrentTheme() → Get active theme
 - getAvailableThemes() → List all discovered themes
 - getIcon(String name, IconSize size) → Convenience wrapper around DSP.Icons
 - getColor(String semanticPath) → Get themed color

 Component 5: Hot-Swap Support (ThemeChangeListener)

 Purpose: Allow UI components to react to theme changes at runtime without restart

 Key Concept: When user switches from "Default" to "Large Icons" theme, all UI components must 
  immediately reflect the new icon sizes and colors without restarting the application.        

 Mechanism:
 - Observer pattern using PropertyChangeSupport
 - Components register as listeners
 - When theme changes, listeners notified
 - Components refresh their appearance

 Example Usage:
 // In a UI component's initialization
 ThemeManager.addThemeChangeListener(event -> {
     // Re-fetch icons with new theme
     homeIcon = ThemeManager.getIcon("home156", IconSize.LARGE);

     // Update colors
     setBackground(ThemeManager.getColor("background.window"));

     // Repaint
     revalidate();
     repaint();
 });

 Hot-Swap Sequence (When User Selects New Theme):

 1. User Action → ThemeManager.setTheme("large-icons")
 2. ThemeManager loads new theme:
   - Reads config/themes/large-icons.json
   - Resolves inheritance (extends "default")
   - Validates theme structure
   - Stores as current theme
 3. ThemeManager invalidates DSP.Icons cache:
   - DSP.Icons.clearCache() - removes all cached Icon instances
   - This forces components to re-fetch icons on next access
 4. ThemeManager re-registers all icons with new sizes:
 // Before (default theme): home156-small → 16px icon
 // After (large-icons theme): home156-small → 24px icon
 DirectoryIconProvider provider = DSP.Icons.getProvider("Directory");
 provider.discoverAndRegisterIcons("Directory",
     theme.getIconSize(IconSize.SMALL),    // Now 24 instead of 16
     theme.getIconSize(IconSize.LARGE));   // Now 64 instead of 48
 5. ThemeManager fires ThemeChangedEvent:
   - All registered listeners notified via PropertyChangeSupport
   - Event contains: oldTheme, newTheme
 6. Components respond to event:
   - VirtualAppFrame (JInternalFrame instances):
       - Re-fetch frame icon: setFrameIcon(ThemeManager.getIcon(iconName, IconSize.SMALL))     
     - Update title bar colors
     - Repaint
   - VShortcut (Desktop icons):
       - Re-fetch large icon: icon = ThemeManager.getIcon(iconName, IconSize.LARGE)
     - Update label colors from theme
     - Repaint
   - ActionFactory actions:
       - Re-fetch menu icons: Action.SMALL_ICON
     - Re-fetch toolbar icons: Action.LARGE_ICON_KEY
   - JList renderers (e.g., VAppListCellRenderer):
       - Re-fetch list icons
     - Update colors
     - Fire list data changed event to force re-render
 7. ThemeManager updates preferences:
   - Writes config/preferences.json with new theme name
   - Persists for next application startup

 DSP.Icons Integration Changes Required:

 Current state: DSP.Icons has no cache invalidation mechanism.

 Required additions to org.jwellman.dsp.DSP.Icons:
 public static void clearCache() {
     // Clear internal icon cache
     // Force re-creation on next getIcon() call
 }

 public static void reregisterAll(Theme theme) {
     // Re-run icon discovery and registration with new theme sizes
     // Called by ThemeManager during theme switch
 }

 Component Refresh Patterns:

 Pattern 1: Store icon name, re-fetch on theme change
 class VShortcut extends JLabel {
     private String iconName;  // Store name, not Icon instance
     private IconSize iconSize;

     public VShortcut(String iconName) {
         this.iconName = iconName;
         this.iconSize = IconSize.LARGE;
         updateIcon();

         ThemeManager.addThemeChangeListener(e -> updateIcon());
     }

     private void updateIcon() {
         setIcon(ThemeManager.getIcon(iconName, iconSize));
     }
 }

 Pattern 2: Actions re-create icons on demand
 // Actions already use getValue(SMALL_ICON) to fetch icons
 // When theme changes, ActionFactory recreates icon and calls:
 action.putValue(Action.SMALL_ICON, ThemeManager.getIcon("home156", IconSize.SMALL));

 Pattern 3: Global repaint after theme change
 // After all listeners notified, ThemeManager forces global repaint:
 SwingUtilities.invokeLater(() -> {
     JFrame mainFrame = App.getMainFrame();
     mainFrame.revalidate();
     mainFrame.repaint();
 });

 Challenges and Solutions:

 Challenge 1: Components holding Icon references become stale
 - Solution: Components must store icon NAME, not Icon instance, and re-fetch on theme change  

 Challenge 2: Actions created once at startup
 - Solution: ActionFactory listens to theme changes and updates all registered actions

 Challenge 3: Components may not re-query icon after cache clear
 - Solution: Components must register as theme change listeners and explicitly re-fetch        

 Challenge 4: Avoiding flicker during theme switch
 - Solution: Use SwingUtilities.invokeLater() to batch all updates on EDT

 Component 6: User Preferences

 Purpose: Persist user's theme selection

 File: config/preferences.json
 {
   "theme": "default",
   "lastModified": "2026-01-10T12:00:00Z"
 }

 Behavior:
 - On startup, ThemeManager reads preferences.json
 - Loads selected theme
 - If theme not found, falls back to "default"
 - When user changes theme via UI, preferences.json updated

 ---
 Migration Strategy

 Phase 1: Create Theme Infrastructure (This PR)

 1. Create org.jwellman.virtualdesktop.theme package
 2. Implement IconSize enum
 3. Implement Theme interface
 4. Implement DefaultTheme
 5. Implement ThemeManager singleton

 Phase 2: Update Icon Registration

 1. Modify DirectoryIconProvider.discoverAndRegisterIcons() to accept IconSize...
 2. Update App.createTheme() to use semantic sizes
 3. Keep backward compatibility by also registering pixel-suffixed keys temporarily

 Phase 3: Update Icon Retrieval

 1. Update ActionFactory to use ThemeManager.buildIconKey()
 2. Update all DSP.Icons.getIcon() calls throughout codebase
 3. Remove pixel-suffixed registrations once migration complete

 Phase 4: Additional Themes

 1. Implement LargeIconsTheme
 2. Implement DarkTheme
 3. Implement HighContrastTheme
 4. Add theme selection UI

 Phase 5: Configuration

 1. Add theme configuration file (properties or JSON)
 2. Support user-defined themes
 3. Persist theme selection

 ---
 Icon Key Naming Convention

 Current (Pixel-based)

 - "home156-16" - home156 icon at 16 pixels
 - "home156-48" - home156 icon at 48 pixels

 Proposed (Semantic)

 - "home156-small" - home156 icon at SMALL size (16px in default theme, 24px in large icons    
 theme)
 - "home156-large" - home156 icon at LARGE size (48px in default theme, 64px in large icons    
 theme)

 Namespaced (Future)

 - "jpad.home" - Namespaced semantic name
 - Combined with size: ThemeManager.getIcon("jpad.home", IconSize.SMALL)

 ---
 Complete JSON Schema Examples

 Example 1: Default Theme (config/themes/default.json)

 Complete theme file showing all three color levels and icon metadata:

 {
   "name": "default",
   "displayName": "Default Theme",
   "description": "Default VirtualDesktop theme with standard sizes and light colors",
   "version": "1.0.0",

   "iconSizes": {
     "small": 16,
     "menu": 18,
     "toolbar": 24,
     "large": 48,
     "xlarge": 64
   },

   "palette": {
     "blue50": "#E3F2FD",
     "blue100": "#BBDEFB",
     "blue500": "#2196F3",
     "blue700": "#1976D2",
     "blue900": "#0D47A1",

     "gray50": "#FAFAFA",
     "gray100": "#F5F5F5",
     "gray300": "#E0E0E0",
     "gray500": "#9E9E9E",
     "gray700": "#616161",
     "gray900": "#212121",

     "red500": "#F44336",
     "orange500": "#FF9800",
     "green500": "#4CAF50",
     "yellow500": "#FFEB3B",

     "white": "#FFFFFF",
     "black": "#000000",
     "transparent": "rgba(0,0,0,0)"
   },

   "colors": {
     "background": {
       "desktop": "blue900",
       "window": "white",
       "panel": "gray100",
       "menubar": "gray100",
       "toolbar": "gray50"
     },

     "foreground": {
       "primary": "gray900",
       "secondary": "gray700",
       "disabled": "gray500",
       "inverse": "white"
     },

     "border": {
       "default": "gray300",
       "focus": "blue500",
       "disabled": "gray300"
     },

     "icon": {
       "default": "gray700",
       "menu": "gray700",
       "toolbar": "gray700",
       "desktop": "white",
       "disabled": "gray500"
     },

     "status": {
       "info": "blue500",
       "success": "green500",
       "warning": "orange500",
       "error": "red500"
     }
   },

   "components": {
     "menuIcon": {
       "foreground": "icon.menu",
       "background": "transparent"
     },

     "toolbarIcon": {
       "foreground": "icon.toolbar",
       "background": "transparent",
       "hoverBackground": "gray100"
     },

     "desktopIcon": {
       "foreground": "icon.desktop",
       "background": "transparent",
       "labelColor": "foreground.inverse",
       "labelBackground": "rgba(0,0,0,0.3)"
     },

     "listIcon": {
       "foreground": "icon.default",
       "background": "transparent"
     }
   },

   "fonts": {
     "base": {
       "family": "Dialog",
       "style": "PLAIN",
       "size": 12
     },
     "title": {
       "family": "Dialog",
       "style": "BOLD",
       "size": 14
     },
     "menu": {
       "family": "Dialog",
       "style": "PLAIN",
       "size": 12
     }
   },

   "iconMetadata": {
     "home156": {
       "colorable": false,
       "type": "raster"
     },
     "calendar168": {
       "colorable": false,
       "type": "raster"
     },
     "document176": {
       "colorable": false,
       "type": "raster"
     },
     "rubbish1": {
       "colorable": false,
       "type": "raster"
     },
     "winking18": {
       "colorable": false,
       "type": "raster"
     }
   }
 }

 Example 2: Large Icons Theme (config/themes/large-icons.json)

 Theme inheritance example - extends default, overrides only sizes:

 {
   "name": "large-icons",
   "displayName": "Large Icons",
   "description": "Larger icon sizes for better visibility",
   "version": "1.0.0",
   "extends": "default",

   "iconSizes": {
     "small": 24,
     "menu": 20,
     "toolbar": 32,
     "large": 64,
     "xlarge": 96
   }
 }

 Note: When a theme extends another, only the specified fields are overridden. All other       
 values (palette, colors, components, fonts, iconMetadata) are inherited from the base theme.  

 Example 3: Dark Theme (config/themes/dark.json)

 Shows how swapping palette creates dark theme while keeping semantic structure:

 {
   "name": "dark",
   "displayName": "Dark Theme",
   "description": "Dark color scheme for reduced eye strain",
   "version": "1.0.0",
   "extends": "default",

   "palette": {
     "blue50": "#0D47A1",
     "blue100": "#1976D2",
     "blue500": "#2196F3",
     "blue700": "#42A5F5",
     "blue900": "#64B5F6",

     "gray50": "#212121",
     "gray100": "#424242",
     "gray300": "#616161",
     "gray500": "#9E9E9E",
     "gray700": "#BDBDBD",
     "gray900": "#E0E0E0",

     "red500": "#EF5350",
     "orange500": "#FFA726",
     "green500": "#66BB6A",
     "yellow500": "#FFEE58",

     "white": "#FFFFFF",
     "black": "#000000",
     "transparent": "rgba(0,0,0,0)"
   },

   "colors": {
     "background": {
       "desktop": "blue900",
       "window": "gray50",
       "panel": "gray100",
       "menubar": "gray100",
       "toolbar": "gray50"
     }
   }
 }

 Note: By extending "default" and only replacing the palette, the dark theme automatically     
 gets inverted colors throughout while maintaining the same semantic structure.

 Example 4: User Preferences (config/preferences.json)

 {
   "theme": "default",
   "lastModified": "2026-01-11T10:30:00Z",
   "version": "1.0.0"
 }

 When user switches to "large-icons" theme, this file is updated:

 {
   "theme": "large-icons",
   "lastModified": "2026-01-11T14:45:22Z",
   "version": "1.0.0"
 }

 ---
 Benefits

 1. Theme Independence - Icons don't hard-code sizes
 2. User Customization - Users can select "Large Icons" theme
 3. Accessibility - High contrast themes for visually impaired
 4. Consistency - All icon sizes defined in one place
 5. Maintainability - Change sizes globally, not scattered in code
 6. Extensibility - Easy to add new themes
 7. Configuration - Themes can be loaded from files
 8. Backward Compatible - Works with existing DSP.Icons

 ---
 Critical Files

 New Files to Create

 - src/main/java/org/jwellman/virtualdesktop/theme/IconSize.java
 - src/main/java/org/jwellman/virtualdesktop/theme/Theme.java
 - src/main/java/org/jwellman/virtualdesktop/theme/DefaultTheme.java
 - src/main/java/org/jwellman/virtualdesktop/theme/ThemeManager.java
 - src/main/java/org/jwellman/virtualdesktop/theme/LargeIconsTheme.java (optional)
 - config/theme.properties (optional, for configuration)

 Files to Modify

 - src/main/java/org/jwellman/dsp/DirectoryIconProvider.java - Accept IconSize instead of int  
 - src/main/java/org/jwellman/virtualdesktop/App.java - Use ThemeManager
 - src/main/java/org/jwellman/virtualdesktop/vapps/ActionFactory.java - Use semantic icon keys 

 ---
 Future Enhancements

 1. Theme Loading - Load themes from properties/JSON files
 2. Theme Editor - GUI for creating custom themes
 3. Icon Mapping - Theme can remap icon names (e.g., "home" → "house" in different theme)      
 4. Dynamic Switching - Change theme at runtime without restart
 5. Per-Component Sizing - Override sizes for specific components
 6. Icon Variants - Light/dark variants of same icon
 7. Migration to Foundation - Move to Foundation project when ready

 ---
 Design Validation Checklist

 This section confirms that the design meets all user requirements:

 - ✅ JSON Configuration Format - All themes use human-readable JSON files
 - ✅ Theme Selection - Users can select from preset themes (Default, Dark, High Contrast,     
 Large Icons)
 - ✅ Custom Icon Sizes - Each theme defines its own icon size mappings
 - ✅ Theme Inheritance - Custom themes can extend base themes and override only specific      
 values
 - ✅ Hot-Swap Support - Themes switch at runtime without application restart
 - ✅ Three-Level Color System - Palette → Semantic Roles → Component Mappings
 - ✅ Icon Metadata in Theme - Theme JSON contains icon metadata (colorable, type)
 - ✅ User Preferences Persistence - Selected theme saved in config/preferences.json
 - ✅ System-Wide Theme - No per-app preferences, theme applies globally
 - ✅ LAF Separation - Look and Feel is separate from theming
 - ⏳ SVG Color Replacement - Future enhancement, lower priority

 Open Questions (If Any)

 1. Icon Size Values: Are the proposed default pixel sizes acceptable?
   - SMALL: 16px
   - MENU: 18px
   - TOOLBAR: 24px
   - LARGE: 48px
   - XLARGE: 64px
 2. Additional Semantic Sizes: Do we need TINY (12px) or HUGE (96px) sizes?
 3. Theme Selection UI: Where should the theme selector be placed?
   - Menu: "View → Themes → [theme list]"
   - Preferences dialog
   - Toolbar dropdown
   - Desktop right-click menu
 4. Font Theming Scope: Should themes control all fonts or just specific ones (menu, title,    
 etc.)?
 
 
Yes, but lets not implement hot swap right now.  I want to control its implementation
and start small due to the complexity and how much it will affect the application.
Answers to questions: 
1) proposed default pixel sizes are acceptable. 
2) do not need additional sizes. 
3) Theme selector should be its own vapp
4) Lets add proposed configuration for fonts but not integrate that yet.
auto-accept edits.  

Phase 1 Requirements (Current Implementation)

 - ✅ JSON Configuration Format - All themes use human-readable JSON files
 - ✅ Theme Selection - Users can select from preset themes via dedicated vapp
 - ✅ Custom Icon Sizes - Each theme defines its own icon size mappings
 - ✅ Confirmed Icon Sizes - SMALL=16px, MENU=18px, TOOLBAR=24px, LARGE=48px, XLARGE=64px (no  
 additional sizes)
 - ✅ Theme Inheritance - Custom themes can extend base themes and override only specific      
 values
 - ✅ Three-Level Color System - Palette → Semantic Roles → Component Mappings
 - ✅ Icon Metadata in Theme - Theme JSON contains icon metadata (colorable, type)
 - ✅ User Preferences Persistence - Selected theme saved in config/preferences.json
 - ✅ System-Wide Theme - No per-app preferences, theme applies globally
 - ✅ LAF Separation - Look and Feel is separate from theming
 - ✅ Font Configuration Defined - Font config in theme JSON but not integrated yet
 - ✅ Restart-Based Theme Switch - Theme changes require application restart (Phase 1)
 - ✅ Theme Selector Vapp - Dedicated vapp for theme selection

 Phase 2 Requirements (Future Implementation)

 - ⏳ Hot-Swap Support - User controls implementation timing due to complexity
 - ⏳ SVG Color Replacement - Future enhancement, lower priority
 
 

  