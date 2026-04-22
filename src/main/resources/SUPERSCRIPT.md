Here's a clean XSLT solution. The simplest approach is to use a delimiter like `^` before the superscript character(s), similar to how many markup systems work (e.g., `Category^1`).

```xml
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <!-- Template to render a string with ^X as superscript -->
  <xsl:template name="render-with-superscript">
    <xsl:param name="text"/>

    <xsl:choose>
      <xsl:when test="contains($text, '^')">
        <!-- Output everything before the ^ -->
        <xsl:value-of select="substring-before($text, '^')"/>
        <!-- Output the single character after ^ as superscript -->
        <sup>
          <xsl:value-of select="substring(substring-after($text, '^'), 1, 1)"/>
        </sup>
        <!-- Recurse on the remainder after the superscript character -->
        <xsl:call-template name="render-with-superscript">
          <xsl:with-param name="text" select="substring(substring-after($text, '^'), 2)"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$text"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
```

**How it works:**

The template splits on `^`, outputs the text before it as normal, wraps the single character immediately after `^` in `<sup>`, then recurses on the remainder. This handles multiple superscripts in one string naturally.

**Usage examples:**

| Input string | Output |
|---|---|
| `Category^1` | Category¹ |
| `H^2O` | H²O |
| `x^2 + y^2` | x² + y² |
| `E=mc^2` | E=mc² |

**Calling the template:**

```xml
<xsl:call-template name="render-with-superscript">
  <xsl:with-param name="text" select="'Category^1'"/>
</xsl:call-template>

<!-- Or from an XML node value: -->
<xsl:call-template name="render-with-superscript">
  <xsl:with-param name="text" select="@label"/>
</xsl:call-template>
```

**Why `^` over other options:**

- `%` is common in URLs and can cause confusion
- `^` is a widely recognized "superscript" symbol (used in math, Markdown, etc.)
- It keeps the source strings readable at a glance

If you need to superscript more than one character (e.g., `^12` → ¹²), just change the `substring(..., 1, 1)` to grab more characters, or use closing delimiter like `^12^` and adjust accordingly — but single-character is almost always sufficient for category labels.
