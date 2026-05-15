# Global Validation Framework

## Introduction

This design achieves a "Single Source of Truth" for application constraints by anchoring them in Java and projecting them into the Database, Backend, and Frontend layers. It eliminates manual duplication and ensures that a change in one Java file updates the entire ecosystem.

---

## **1. Architectural Overview**

The architecture centers on the **ValidationRegistry**, which serves as both the definition of constants and the mechanism for exporting them.

* **The Definition:** Constants are organized into nested static classes to create a logical namespace.
* **The Enforcement:** Java Backend uses these constants via `@Size` or `@Column` annotations.
* **The Projection:** A reflection-based scanner converts the nested classes into a JSON-ready Map.
* **The Delivery:** A JSP acts as a "Dynamic JS Asset," serving the JSON to the browser with aggressive caching headers.

---

## **2. Core Components**

### **A. The Registry (Source of Truth)**

This class holds the values and handles the translation to other formats.

```java
public class ValidationRegistry {
    // Nested classes for namespacing (Java-side access)
    public static class User {
        public static class Name {
            public static final int MAX = 20;
            public static final int MIN = 2;
        }
    }

    private static String cachedJson;
    private static final Map<String, Object> root = new HashMap<>();

    static {
        // 1. Reflection walks the classes to build a nested Map
        buildMapFromClass(ValidationRegistry.class, root);
        // 2. ObjectMapper serializes to JSON string once at startup
        cachedJson = new ObjectMapper().writeValueAsString(root);
    }
    
    public static String getJson() { return cachedJson; }
}

```

### **B. The Bridge (validation-init.jsp)**

This file delivers the metadata to the browser. By setting a `max-age`, we offload the "request cost" to the browser's local cache.

```jsp
<%@ page contentType="application/javascript" %>
<%
    response.setHeader("Cache-Control", "public, max-age=86400"); // 24hr cache
%>
(function(window) {
    window.Validation = Object.freeze(<%= ValidationRegistry.getJson() %>);
})(window);

```

### **C. Global UI Exposure (@ControllerAdvice)**

To make the values available in JSP Expression Language (`${...}`) without manual model-adding in every controller.

```java
@ControllerAdvice
public class GlobalValidationAdvice {
    @ModelAttribute("val")
    public Map<String, Object> exposeValidation() {
        return ValidationRegistry.getRootMap();
    }
}

```

---

## **3. Layered Enforcement Summary**

| Layer | Implementation | Access Syntax |
| --- | --- | --- |
| **Java Domain** | `@Size(max = ValidationRegistry.User.Name.MAX)` | `ValidationRegistry.User.Name.MAX` |
| **Database** | `@Column(length = ValidationRegistry.User.Name.MAX)` | (Synced via Hibernate/JPA) |
| **JSP HTML** | `<input maxlength="${val.user.name.max}">` | `${val.path.to.constant}` |
| **JavaScript** | `if (val.length > Validation.user.name.max)` | `Validation.path.to.constant` |

---

## **4. Maintenance Workflow**

1. **Modify:** Change a value in the `ValidationRegistry` inner classes (e.g., change `20` to `30`).
2. **Deploy:** The application is recompiled and redeployed.
3. **Automatic Sync:**

* The **Backend** immediately enforces `30`.
* The **Database** schema is updated (if using migration tools).
* The **JSP** renders `maxlength="30"`.
* The **JavaScript** pulls the new value when the cache expires or when the version query-string (e.g., `?v=2`) is updated.

---

## **5. Key Advantages**

* **Compile-time Safety:** Java access is verified by the compiler.
* **Performance:** JSON is generated once at startup; JS files are cached by the browser; Map lookups in JSPs are O(1).
* **DRY Compliance:** The literal number exists in exactly one place in the codebase.
