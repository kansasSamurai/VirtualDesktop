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

Good catch! In XSL-FO, superscript is handled via `baseline-shift` and typically a smaller font size on an `fo:inline` element rather than an HTML `<sup>` tag. Here's the corrected version:

```xml
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format">

  <!-- Template to render a string with ^X as superscript -->
  <xsl:template name="render-with-superscript">
    <xsl:param name="text"/>

    <xsl:choose>
      <xsl:when test="contains($text, '^')">
        <!-- Output everything before the ^ -->
        <fo:inline>
          <xsl:value-of select="substring-before($text, '^')"/>
        </fo:inline>
        <!-- Output the single character after ^ as superscript -->
        <fo:inline baseline-shift="super" font-size="75%">
          <xsl:value-of select="substring(substring-after($text, '^'), 1, 1)"/>
        </fo:inline>
        <!-- Recurse on the remainder after the superscript character -->
        <xsl:call-template name="render-with-superscript">
          <xsl:with-param name="text" select="substring(substring-after($text, '^'), 2)"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <fo:inline>
          <xsl:value-of select="$text"/>
        </fo:inline>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
```

The key differences from the HTML version:

- **`baseline-shift="super"`** raises the character above the baseline. You can also use an explicit value like `"0.4em"` if you need finer control over the exact position.
- **`font-size="75%"`** scales it down relative to the surrounding text, which is standard for superscripts. Adjust to taste — `65%` to `80%` are all common.
- **`fo:inline`** is the XSL-FO equivalent of an HTML `<span>`, used here to wrap both normal and superscript runs.

Here's the SQL update statement:

```sql
UPDATE your_table
SET your_column = CONCAT(LEFT(your_column, LEN(your_column) - 1), '^', RIGHT(your_column, 1))
WHERE your_column LIKE '%1';
```

Simple breakdown:
- `LEFT(..., LEN(...) - 1)` grabs everything except the last character
- `'^'` inserts the delimiter
- `RIGHT(..., 1)` re-appends the trailing `1`
- The `WHERE` clause limits updates to only rows ending in `1`

**Note on dialect differences** — the functions used above are T-SQL (SQL Server). Minor adjustments for other databases:

| Database | Length function | Notes |
|---|---|---|
| SQL Server | `LEN()` | As written above |
| MySQL / MariaDB | `LENGTH()` | Also has `CONCAT`, works as-is otherwise |
| PostgreSQL | `LENGTH()` | Use `\|\|` instead of `CONCAT`: `LEFT(...) \|\| '^' \|\| RIGHT(...)` |
| Oracle | `LENGTH()` | Use `\|\|` and `SUBSTR` instead of `LEFT`/`RIGHT` |

Let me know which database you're on if you need a dialect-specific version.

Here's the PostgreSQL version:

```sql
UPDATE your_table
SET your_column = LEFT(your_column, LENGTH(your_column) - 1) || '^' || RIGHT(your_column, 1)
WHERE your_column LIKE '%1';
```

PostgreSQL has `LEFT`, `RIGHT`, and `LENGTH` just like SQL Server, so the only change is swapping `CONCAT()` for the `||` concatenation operator.

The calling convention remains exactly the same as before.
