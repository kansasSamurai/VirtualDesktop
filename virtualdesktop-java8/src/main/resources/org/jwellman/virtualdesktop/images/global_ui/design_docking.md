
I've created a comprehensive design plan for abstracting the docking framework. The plan includes:

#  Summary

  Goal: Create a clean abstraction layer to decouple VirtualDesktop from the Docking Frames (bibliothek) library.

  Approach:
  - Use Facade, Adapter, and Service Provider patterns
  - Create 8 core interfaces (IDockingService, IDockingWorkspace, IDockable, etc.)
  - Wrap bibliothek framework in adapter classes
  - Minimal changes to existing code (3 files to modify, 20-25 new files to create)

  Key Features:
  - Framework-agnostic API
  - Maintains cross-frame drag-and-drop
  - Builder pattern for creating dockables
  - Static factory methods for positioning
  - Future-proof for framework replacement
  - Java 8 compatible

  Critical Changes:
  1. App.java - Replace SpecDocking.setJFrame(this) with service initialization
  2. VirtualAppSpec.java - Replace CControl/CContentArea with IDockingService/IDockingWorkspace
  3. SpecDocking.java - Refactor demo to use new API

  No Changes Needed:
  - DesktopManager.java already uses abstracted methods
  - Other vapps continue to work without modification

  The plan maintains all current functionality while providing clean separation and enabling future framework swapping.

───────────────────────────────────────────────────────────────────────────────────────────────
 Ready to code?

 Here is Claude's plan:
╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌
 Docking Framework Abstraction Layer - Design Plan

 Overview

 Create a clean abstraction layer to decouple VirtualDesktop from the Docking Frames
 (bibliothek) library, enabling future framework replacement and better encapsulation.

 Current State Analysis

 Docking Framework Integration

 Library: Docking Frames 1.1.3 (bibliothek package)
 - docking-frames-core (version 1.1.3)
 - docking-frames-common (version 1.1.3)

 Key Integration Points:

 1. VirtualAppSpec.java (lines 41-99)
   - static CControl control - Shared across all vapps for drag-drop between frames
   - protected CContentArea dockingcontent - Per-instance workspace
   - setJFrame(JFrame) - Initializes static CControl with Eclipse theme
   - addDockable(JComponent) - Adds components to docking framework
   - setContent(JPanel) - Also creates CContentArea (lines 186-202)
 2. App.java (line 216)
   - SpecDocking.setJFrame(this) - One-line initialization call
 3. DesktopManager.java (lines 183-185)
   - Checks spec.isDockable()
   - Sets frame.setContentPane(spec.getDockableContent())
   - Calls spec.addDockable(spec.getContent())
 4. SpecDocking.java
   - Demonstration vapp with 5 version implementations
   - Shows evolution of docking integration approaches

 Key Design Constraints

 - Static CControl Required: Enables cross-frame drag-and-drop (critical feature)
 - Java 8 Compatibility: No Java 9+ features allowed
 - Current Architecture: All vapps are dockable (isDockable() returns true)

 Proposed Abstraction Design

 Design Patterns

 - Facade Pattern: Single entry point via IDockingService
 - Adapter Pattern: Wrap bibliothek framework behind interfaces
 - Service Provider Interface (SPI): Enable future framework swapping
 - Builder Pattern: Fluent API for creating dockables

 Core Abstraction Interfaces

 1. IDockingService (Singleton Facade)

 Main entry point for all docking operations.

 public interface IDockingService {
     void initialize(JFrame mainFrame);
     boolean isInitialized();
     IDockingWorkspace createWorkspace(String workspaceId) throws DockingException;
     IDockingWorkspace getDefaultWorkspace();
     void setTheme(DockingTheme theme);
     void shutdown();
 }

 Accessed via: DockingServiceFactory.getInstance()

 2. IDockingWorkspace

 Represents a docking workspace (abstracts CContentArea). Each VirtualAppFrame has one.        

 public interface IDockingWorkspace {
     String getWorkspaceId();
     Container getContainer();
     void addDockable(IDockable dockable);
     void removeDockable(String dockableId);
     IDockable getDockable(String dockableId);
     boolean hasDockable(String dockableId);
 }

 3. IDockable

 Represents a single dockable component (abstracts SingleCDockable).

 public interface IDockable {
     String getId();
     String getTitle();
     JComponent getComponent();
     void setLocation(DockableLocation location);
     void setVisible(boolean visible);
     boolean isVisible();
     void close();
 }

 4. DockableLocation (Abstract Class)

 Encapsulates positioning (abstracts CLocation).

 public abstract class DockableLocation {
     static DockableLocation normalIn(IDockingWorkspace workspace);
     static DockableLocation minimalNorthIn(IDockingWorkspace workspace, int index);
     static DockableLocation minimalSouthIn(IDockingWorkspace workspace, int index);
     static DockableLocation minimalWestIn(IDockingWorkspace workspace, int index);
     static DockableLocation minimalEastIn(IDockingWorkspace workspace, int index);
     static DockableLocation external(int x, int y, int width, int height);

     abstract Object toNativeLocation(); // package-private
 }

 5. DockableBuilder (Fluent API)

 Builder for creating dockables.

 public interface DockableBuilder {
     DockableBuilder withId(String id);
     DockableBuilder withTitle(String title);
     DockableBuilder withComponent(JComponent component);
     DockableBuilder withIcon(Icon icon);
     DockableBuilder withLocation(DockableLocation location);
     DockableBuilder withVisible(boolean visible);
     IDockable build() throws DockingException;
 }

 6. DockingTheme (Enum)

 Available themes.

 public enum DockingTheme {
     FLAT, ECLIPSE, SMOOTH, BASIC, BUBBLE
 }

 7. IDockingProvider (SPI)

 Service Provider Interface for pluggable implementations.

 public interface IDockingProvider {
     String getProviderName();
     String getProviderVersion();
     void initialize(JFrame mainFrame) throws DockingException;
     IDockingWorkspace createWorkspace(String workspaceId) throws DockingException;
     IDockingWorkspace getDefaultWorkspace();
     DockableBuilder createDockableBuilder();
     void setTheme(DockingTheme theme);
     boolean isInitialized();
     void shutdown();
 }

 Package Structure

 org.jwellman.virtualdesktop.docking/
 ├── IDockingService.java
 ├── IDockingWorkspace.java
 ├── IDockable.java
 ├── DockableLocation.java
 ├── DockingTheme.java
 ├── DockingException.java
 ├── DockableBuilder.java
 ├── DockingServiceFactory.java
 │
 ├── spi/
 │   └── IDockingProvider.java
 │
 └── impl/
     ├── DockingServiceImpl.java
     ├── bibliothek/
     │   ├── BibliothekDockingProvider.java    (wraps CControl)
     │   ├── BibliothekWorkspace.java          (wraps CContentArea)
     │   ├── BibliothekDockable.java           (wraps SingleCDockable)
     │   ├── BibliothekDockableBuilder.java
     │   └── [Location implementations]
     └── noop/
         └── NoOpDockingProvider.java           (fallback)

 Implementation Steps

 Phase 1: Create Abstraction Layer (No Behavior Change)

 Goal: Install abstraction without breaking existing code.

 Step 1.1: Create Interface Package

 - Create org.jwellman.virtualdesktop.docking package
 - Implement all interface files:
   - IDockingService.java
   - IDockingWorkspace.java
   - IDockable.java
   - DockableLocation.java (abstract class with static factories)
   - DockingTheme.java (enum)
   - DockingException.java
   - DockableBuilder.java
   - DockingServiceFactory.java

 Step 1.2: Create SPI Package

 - Create org.jwellman.virtualdesktop.docking.spi package
 - Implement IDockingProvider.java

 Step 1.3: Create Implementation Package

 - Create org.jwellman.virtualdesktop.docking.impl package
 - Implement DockingServiceImpl.java (delegates to provider)

 Step 1.4: Create Bibliothek Adapter

 - Create org.jwellman.virtualdesktop.docking.impl.bibliothek package
 - Implement adapter classes:
   - BibliothekDockingProvider.java - Wraps CControl, manages initialization
   - BibliothekWorkspace.java - Wraps CContentArea
   - BibliothekDockable.java - Wraps SingleCDockable
   - BibliothekDockableBuilder.java - Builder implementation
   - Location classes: NormalLocation, MinimalNorthLocation, MinimalSouthLocation,
 MinimalWestLocation, MinimalEastLocation, ExternalLocation

 Step 1.5: Update App.java Initialization

 Replace initialization in App.java (line 216):

 // OLD:
 SpecDocking.setJFrame(this);

 // NEW:
 IDockingService dockingService = DockingServiceFactory.getInstance();
 dockingService.initialize(this);
 dockingService.setTheme(DockingTheme.ECLIPSE);

 Step 1.6: Update VirtualAppSpec.java

 Major refactoring of VirtualAppSpec.java:

 Replace static/instance variables (lines 47-51):
 // OLD:
 protected CContentArea dockingcontent = null;
 protected static CControl control;

 // NEW:
 protected IDockingWorkspace workspace = null;
 protected static IDockingService dockingService = DockingServiceFactory.getInstance();        

 Update setJFrame() (lines 53-70):
 // OLD:
 public static void setJFrame(JFrame frame) {
     if (control == null) {
         control = new CControl(frame);
         final ThemeMap themes = control.getThemes();
         themes.select(ThemeMap.KEY_ECLIPSE_THEME);
     } else {
         System.out.println("Warning: Tried to reinitialize Docking");
     }
 }

 // NEW:
 public static void setJFrame(JFrame frame) {
     if (!dockingService.isInitialized()) {
         dockingService.initialize(frame);
         dockingService.setTheme(DockingTheme.ECLIPSE);
     } else {
         System.out.println("Warning: Tried to reinitialize Docking");
     }
 }

 Update addDockable() (lines 76-95):
 // OLD:
 public void addDockable(JComponent c) {
     String dockid = this.getTitle();
     SingleCDockable check = control.getSingleDockable(this.getTitle());
     if (check != null) {
         dockid = this.getTitle() + duplicateCounter++;
     }
     SingleCDockable dockable = new DefaultSingleCDockable(dockid, this.getTitle(), c);        
     control.addDockable(dockable);
     dockable.setLocation(CLocation.base(dockingcontent).normal());
     dockable.setVisible(true);
 }

 // NEW:
 public void addDockable(JComponent c) {
     try {
         String dockid = this.getTitle();

         // Check for duplicates
         if (workspace != null && workspace.hasDockable(dockid)) {
             dockid = this.getTitle() + duplicateCounter++;
         }

         // Get provider for builder
         IDockingProvider provider = ((DockingServiceImpl) dockingService).getProvider();      

         // Build and add dockable
         IDockable dockable = provider.createDockableBuilder()
             .withId(dockid)
             .withTitle(this.getTitle())
             .withComponent(c)
             .withLocation(DockableLocation.normalIn(workspace))
             .withVisible(true)
             .build();

         workspace.addDockable(dockable);

     } catch (DockingException e) {
         throw new RuntimeException("Failed to add dockable", e);
     }
 }

 Update setContent() (lines 184-203):
 // OLD:
 public void setContent(JPanel content) {
     this.content = content;
     if (this.dockingcontent == null) {
         String dockid = this.getTitle() == null ? "FIXME" : this.getTitle();
         boolean added = false; int counter = 1;
         do {
             try {
                 this.dockingcontent = control.createContentArea(dockid);
                 added = true;
             } catch (Exception e) {
                 dockid += dockid + "-" + counter++;
             }
         } while (!added && counter < 100);
     }
 }

 // NEW:
 public void setContent(JPanel content) {
     this.content = content;
     if (this.workspace == null) {
         String workspaceId = this.getTitle() == null ? "FIXME" : this.getTitle();
         boolean created = false; int counter = 1;
         do {
             try {
                 this.workspace = dockingService.createWorkspace(workspaceId);
                 created = true;
             } catch (DockingException e) {
                 workspaceId = workspaceId + "-" + counter++;
             }
         } while (!created && counter < 100);
     }
 }

 Update getDockableContent() (lines 238-240):
 // OLD:
 public Container getDockableContent() {
     return this.dockingcontent;
 }

 // NEW:
 public Container getDockableContent() {
     return this.workspace != null ? this.workspace.getContainer() : null;
 }

 Remove bibliothek imports (lines 15-20):
 Remove all imports from bibliothek.gui.dock.common.*

 Step 1.7: Update DesktopManager.java

 No changes needed! The code at lines 183-185 already uses the abstracted methods:
 if (spec.isDockable()) {
     frame.setContentPane(spec.getDockableContent());  // Returns Container
     spec.addDockable(spec.getContent());               // Uses new implementation
 }

 Step 1.8: Update SpecDocking.java

 Refactor the demonstration vapp to use the new API. This serves as:
 - Migration example for other vapps
 - Test case for the abstraction layer

 Phase 2: Testing and Validation

 Step 2.1: Manual Testing

 - Launch application
 - Create multiple dockable vapps
 - Test drag-and-drop between internal frames
 - Test all location types (normal, minimal north/south/east/west)
 - Verify theme application
 - Check for visual regressions

 Step 2.2: Verify All Vapps

 Check that existing vapps still work:
 - SpecBeanShell
 - SpecJCXConsole
 - SpecHyperSQL
 - SpecJFreeChart
 - SpecXChartDemo
 - SpecXionFM

 Phase 3: Documentation

 Step 3.1: JavaDoc

 - Add comprehensive JavaDoc to all public interfaces
 - Include usage examples in key classes

 Step 3.2: Migration Guide

 - Document how to update custom vapps
 - Provide before/after code examples

 Step 3.3: Architecture Documentation

 - Update CLAUDE.md with abstraction layer details
 - Document provider swapping mechanism (for future)

 Key Design Decisions

 1. Static Service Pattern

 Decision: Keep singleton pattern but hide behind IDockingService.

 Rationale:
 - Static CControl is required for cross-frame drag-drop
 - Abstraction makes static state controllable and testable
 - Future implementations may not need static state

 2. Workspace = Content Area

 Decision: Map CContentArea to IDockingWorkspace.

 Rationale:
 - "Workspace" is framework-agnostic terminology
 - One workspace per VirtualAppFrame maintains current architecture
 - Clear separation of concerns

 3. Location Abstraction

 Decision: Static factory methods on DockableLocation.

 Rationale:
 - Hides complex CLocation API
 - Intuitive: DockableLocation.normalIn(workspace)
 - Covers 95% of use cases
 - Easy to map to other frameworks

 4. Builder Pattern

 Decision: Fluent builder for creating dockables.

 Rationale:
 - Many optional properties (icon, location, visibility)
 - Clear, readable code
 - Easy to extend without breaking API

 5. SPI Layer

 Decision: Include Service Provider Interface despite single implementation.

 Rationale:
 - Future-proof for framework replacement
 - Minimal overhead
 - Improves testability

 6. No ServiceLoader Initially

 Decision: Hardcode BibliothekDockingProvider in DockingServiceImpl.

 Rationale:
 - YAGNI principle - only one provider exists
 - Simpler implementation
 - Can add Java ServiceLoader later when needed

 Critical Files

 Files to Create (25 new files)

 1. org/jwellman/virtualdesktop/docking/IDockingService.java
 2. org/jwellman/virtualdesktop/docking/IDockingWorkspace.java
 3. org/jwellman/virtualdesktop/docking/IDockable.java
 4. org/jwellman/virtualdesktop/docking/DockableLocation.java
 5. org/jwellman/virtualdesktop/docking/DockingTheme.java
 6. org/jwellman/virtualdesktop/docking/DockingException.java
 7. org/jwellman/virtualdesktop/docking/DockableBuilder.java
 8. org/jwellman/virtualdesktop/docking/DockingServiceFactory.java
 9. org/jwellman/virtualdesktop/docking/spi/IDockingProvider.java
 10. org/jwellman/virtualdesktop/docking/impl/DockingServiceImpl.java
 11. org/jwellman/virtualdesktop/docking/impl/bibliothek/BibliothekDockingProvider.java        
 12. org/jwellman/virtualdesktop/docking/impl/bibliothek/BibliothekWorkspace.java
 13. org/jwellman/virtualdesktop/docking/impl/bibliothek/BibliothekDockable.java
 14. org/jwellman/virtualdesktop/docking/impl/bibliothek/BibliothekDockableBuilder.java        
 15. org/jwellman/virtualdesktop/docking/impl/bibliothek/NormalLocation.java
 16. org/jwellman/virtualdesktop/docking/impl/bibliothek/MinimalNorthLocation.java
 17. org/jwellman/virtualdesktop/docking/impl/bibliothek/MinimalSouthLocation.java
 18. org/jwellman/virtualdesktop/docking/impl/bibliothek/MinimalWestLocation.java
 19. org/jwellman/virtualdesktop/docking/impl/bibliothek/MinimalEastLocation.java
 20. org/jwellman/virtualdesktop/docking/impl/bibliothek/ExternalLocation.java
 21-25. (Optional NoOp provider for future use)

 Files to Modify (3 files)

 1. src/main/java/org/jwellman/virtualdesktop/App.java (line 216)
 2. src/main/java/org/jwellman/virtualdesktop/vapps/VirtualAppSpec.java (lines 15-20, 47-51,   
 53-70, 76-95, 184-203, 238-240)
 3. src/main/java/org/jwellman/virtualdesktop/vapps/SpecDocking.java (demonstration refactor)  

 Verification Plan

 Build Verification

 mvn clean compile
 mvn package

 Runtime Verification

 1. Launch VirtualDesktop application
 2. Open multiple dockable vapps (BeanShell, JCXConsole, etc.)
 3. Verify drag-and-drop between internal frames works
 4. Test docking/undocking
 5. Test minimize to north/south/east/west
 6. Verify no visual regressions
 7. Check console for warnings/errors

 Code Quality Checks

 - No bibliothek imports in VirtualAppSpec.java
 - No bibliothek types in public APIs outside impl.bibliothek package
 - All interfaces have JavaDoc
 - No compiler warnings

 Benefits

 1. Clean Separation: No framework-specific types leak into app code
 2. Future-Proof: Can swap docking frameworks via provider implementation
 3. Testable: Can mock IDockingService for testing
 4. Maintainable: Clear boundaries and responsibilities
 5. Java 8 Compatible: No modern Java features used
 6. Minimal Disruption: Existing vapps require minimal changes
 7. Feature Preservation: All current functionality maintained

 Risks and Mitigations

 Risk 1: Breaking Drag-and-Drop

 Mitigation: Keep static controller pattern; test thoroughly

 Risk 2: Performance Regression

 Mitigation: Abstraction adds minimal overhead; profile if concerned

 Risk 3: Missing Edge Cases

 Mitigation: Keep SpecDocking as comprehensive test; verify all 5 versions work

 Risk 4: Complex Builder Implementation

 Mitigation: Start with simple builder; add features incrementally

 Future Enhancements

 1. ServiceLoader Integration: Use Java ServiceLoader for dynamic provider selection
 2. Configuration Properties: virtualdesktop.docking.provider=bibliothek
 3. Alternative Providers: Implement providers for other docking frameworks
 4. Layout Persistence: Add save/load workspace layout to abstraction
 5. Event System: Add docking event listeners to abstraction