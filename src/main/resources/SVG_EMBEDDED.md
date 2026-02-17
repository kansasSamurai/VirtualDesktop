Absolutely. Since SVG is just another XML vocabulary, XSLT treats it exactly like it treats XSL-FO. You don’t even need an "external" file; you can literally write the SVG code directly into your XSL template and use XSLT instructions (`xsl:value-of`, `xsl:if`, `xsl:attribute`) to "inject" data into the graphics.

The element you need for this is **`fo:instream-foreign-object`**.

### 1. The Technique: "Instream" Injection

Instead of using `fo:external-graphic` (which points to a file), you use `fo:instream-foreign-object`. Inside that, you place your SVG code.

**Example: A Dynamic "Status Indicator" Bar**

```xml
<fo:block>
  <fo:instream-foreign-object>
    <svg:svg width="100mm" height="10mm" xmlns:svg="http://www.w3.org/2000/svg">
      <svg:rect x="0" y="0" width="100" height="10" fill="#eeeeee" />
      
      <svg:rect x="0" y="0" fill="#4a90e2" height="10">
        <xsl:attribute name="width">
          <xsl:value-of select="current_score"/> </xsl:attribute>
      </svg:rect>
      
      <svg:text x="50" y="7" font-family="Arial" font-size="5" text-anchor="middle">
        Score: <xsl:value-of select="current_score"/>%
      </svg:text>
    </svg:svg>
  </fo:instream-foreign-object>
</fo:block>

```

---

### 2. Key Rules for the "SVG Template"

* **Namespaces:** You must define the SVG namespace (usually `xmlns:svg="http://www.w3.org/2000/svg"`) so the XSL processor knows these aren't FO commands.
* **No "File" Needed:** Because it's "instream," the SVG is generated on-the-fly as FOP processes the document. This is incredibly fast compared to loading 100 separate `.svg` files from a disk.
* **XSLT Power:** You can use `<xsl:choose>` to change colors based on values (e.g., make the bar red if the score is ).

---

### 3. The "Subversive" Advantage

This is a huge "win" for your report cleanup because:

1. **Resolution:** SVGs are vectors. Your icons/charts will look perfect at any zoom level.
2. **Accessibility:** Since the SVG text is technically part of the FO tree, many PDF readers can actually "see" that text (though you should still provide `fox:alt-text` on the container).
3. **The "Wow" Factor:** You can create dynamic charts, sparklines, or custom progress bars without needing a 3rd party charting library.

### One FOP Warning

Apache FOP uses **Batik** to render SVG. It is very capable, but it can be picky about CSS styles inside SVG. It's always safer to use **XML attributes** (like `fill="#ff0000"`) rather than the `style="fill:#ff0000"` attribute for maximum compatibility.

### Would you like to see...

How to use an **`xsl:template`** specifically for a reusable SVG "Component" (like a custom bullet or a status light) so you don't have to rewrite the SVG code every time you use it?

If you are handed an SVG where the coordinate system is "baked in" (e.g., the bar is exactly 452 pixels wide in the source file), you have two main ways to handle the math without having to manually redraw the whole thing.

### 1. The "Pure XSLT" Math Approach

Since XSLT is a functional programming language, you can perform the math directly inside the attribute. If your "Max Width" in the SVG geometry is `452` and your data is a percentage:

```xml
<svg:rect y="10" height="20" fill="blue">
  <xsl:attribute name="width">
    <xsl:value-of select="(data/score div 100) * 452"/>
  </xsl:attribute>
</svg:rect>

```

### 2. The `viewBox` Trick (The "Developer's Cheat Code")

This is the most powerful way to handle "given" geometry. You can redefine the coordinate system of the SVG to be whatever you want (like 0 to 100) regardless of the actual physical size it occupies in the PDF.

If you set `viewBox="0 0 100 20"`, then `1 unit` in your SVG math equals `1%` of the width.

```xml
<svg:svg width="100mm" height="20mm" viewBox="0 0 100 20">
   <svg:rect x="0" y="0" width="{data/score}" height="20" fill="green" />
</svg:svg>

```

*Note: Using the curly braces `{}` is the "Attribute Value Template" shorthand for `xsl:value-of`.*

---

### 3. Handling Complex "Given" SVGs (The `transform`)

If the SVG is a complex shape (like a map or a custom flourish) and you need to "fill" it partially, you can use a **Clip Path**.

1. Keep the "Given" SVG geometry exactly as it is.
2. Define a `clipPath` that contains a simple rectangle.
3. Use XSLT to change the width of that **rectangle**.
4. Apply that `clipPath` to the complex "Given" object.

```xml
<svg:defs>
  <svg:clipPath id="myClip">
    <svg:rect x="0" y="0" height="1000" width="{$calculatedWidth}" />
  </svg:clipPath>
</svg:defs>

<svg:path d="M12.3..." clip-path="url(#myClip)" fill="gold" />

```

### A Quick Word on XSLT Math Units

XSLT 1.0 (which most FOP environments use) doesn't have a built-in "round" function that is very sophisticated, but it handles basic floating point math () perfectly. Just remember:

* Use `div` for division (the `/` character is reserved for XML paths).
* If you need to round to a whole number for SVG coordinates, use `round(your_math_here)`.

### The "Subversive" Takeaway

By using the **`viewBox`** method, you can take a complex SVG created by a designer in Illustrator, wrap it in a `viewBox="0 0 100 100"`, and then talk to it using simple percentages. You don't have to care if the original designer used "pixels," "points," or "picas."

Does that cover the "Given Geometry" headache, or are you looking at a specific piece of math (like calculating coordinates for a Pie Chart)?

You are wise to be skeptical. Generating a pie chart in pure XSLT 1.0 is a "flex" that often results in a maintenance nightmare. A pie chart requires calculating coordinates on a circle using sine and cosine, and since XSLT 1.0 lacks built-in trigonometric functions, you usually end up having to import a massive "math library" stylesheet just to find a coordinate.

However, for your "inventory cleanup," there is a **modern SVG trick** that handles 0% sections, doesn't require trigonometry, and works beautifully for "basic to medium" cases.

### The "Stroke-Dasharray" Hack

Instead of drawing complex "pie slices" (paths), you draw a **circle** with a very thick border. You then use the `stroke-dasharray` property to "reveal" sections of that border.

**How it works:**

1. Draw a circle with a circumference of exactly **100 units**.
2. Set the `stroke-width` to be the same as the radius (this fills the circle).
3. Use `stroke-dasharray` to set the "dash" (the colored slice) and the "gap" (the empty space).

#### The Template

```xml
<svg:svg width="50mm" height="50mm" viewBox="0 0 32 32">
  <svg:circle r="16" cx="16" cy="16" fill="white" stroke="#eeeeee" stroke-width="32" />
  
  <svg:circle r="16" cx="16" cy="16" fill="none" stroke="#4a90e2" stroke-width="32"
              stroke-dasharray="{$percentage} 100"
              transform="rotate(-90 16 16)" />
</svg:svg>

```

---

### Handling the "Hidden Expectations"

| Expectation | The "Instream" SVG Solution |
| --- | --- |
| **0% Sections** | If `{$percentage}` is 0, the dash is 0 length. The slice simply vanishes perfectly. |
| **100% Sections** | The dash fills the entire circumference. |
| **Labels** | You can place an `svg:text` element in the center of the `viewBox="0 0 32 32"` to show the percentage. |
| **Multi-slice** | This is the limit: To do multiple colors, you have to stack circles and calculate the `stroke-dashoffset` (rotation) for each. |

### The Verdict: When to keep the 3rd Party API

You should **stick with your API** if:

* You need **Auto-labeling:** (e.g., smart lines that point to tiny slices so text doesn't overlap).
* You have **Dynamic Legend placement:** (e.g., moving the legend if the chart is too wide).
* You have **3D/Gradient effects:** (Batik/FOP can do these, but they are verbose in SVG).

### Why "Instream" is still worth it for Bars

While Pie charts are tricky, **Bar charts** are where you can fire your 3rd party API today.

* **Accessibility:** You can add `fox:alt-text` to the bar chart container that summarizes the data (e.g., "Sales Bar Chart: North 20, South 45").
* **Branding:** You can exactly match your report's font and line weights, which APIs often struggle with.

**Final Tip for your Cleanup:**
If you find a report using a 3rd party API just to generate a **Single Progress Bar** or a **Small Sparkline**, replace it with an "Instream SVG." It’s one less dependency, it’s faster, and it won't break if the API endpoint changes its security headers.

Since you're wrapping up, would you like me to leave you with the **Attribute Value Template** syntax `{...}` for your notes? It's the cleanest way to "inject" XSL data into SVG attributes.

The **Attribute Value Template (AVT)** is the "secret sauce" for keeping your SVG code readable. Without it, your XSL becomes a mess of `<xsl:attribute>` tags that obscure the actual drawing code.

### The Syntax: `{ }`

Anything inside curly braces `{ }` within an attribute is treated as an **Xpath expression**. The XSL processor evaluates the math or data inside and "bakes" the result into the attribute before handing it to the SVG renderer.

#### Comparison: The Old Way vs. The AVT Way

**The "Clunky" Way (Hard to read):**

```xml
<svg:rect x="0" y="0" height="10">
  <xsl:attribute name="width">
    <xsl:value-of select="current_score * 2"/>
  </xsl:attribute>
  <xsl:attribute name="fill">
    <xsl:choose>
      <xsl:when test="current_score &gt; 80">green</xsl:when>
      <xsl:otherwise>red</xsl:otherwise>
    </xsl:choose>
  </xsl:attribute>
</svg:rect>

```

**The AVT Way (Looks like HTML/SVG):**

```xml
<svg:rect x="0" y="0" width="{current_score * 2}" height="10" fill="{$barColor}" />

```

---

### 3 Rules for AVTs:

1. **Placement:** They only work inside **attribute values**. You cannot use them to generate node names or plain text between tags (use `<xsl:value-of/>` for that).
2. **Math is Legal:** You can do math directly inside the braces: `{(@score div @total) * 100}`.
3. **Escaping Braces:** If you ever need to use literal curly braces inside an attribute (rare in SVG, but happens in some CSS), you double them: `{{ }}`.

### Why this matters for your SVG Bar Chart:

When you are looking at a "Given" SVG with complex geometry, you can just find the `width` or `x` attribute and swap the hardcoded number for a `{math}` expression. It makes your "SVG Template" look like actual SVG code, which is much easier for your team to maintain.

---

**You are now fully armed for the Great 2026 Cleanup!** You have:

* **The Page Layouts** (Metric US Letter)
* **The Table Hacks** (3-row spanned headers & "continued" footers)
* **The Flow Logic** (Mixed orientation & first-page-only footers)
* **The Dynamic Graphics** (Instream SVG with AVT injection)

**Would you like me to provide a final "Cheat Sheet" of these SVG Attribute Value Templates for your reference library?**

This cheat sheet focuses on the **Attribute Value Template (AVT)**—the bridge that lets you turn static SVG "given geometry" into a dynamic, data-driven template.

---

### 📝 SVG Attribute Value Template (AVT) Cheat Sheet

| Feature | The AVT Syntax `{ }` | Use Case |
| --- | --- | --- |
| **Simple Mapping** | `width="{score}"` | Direct injection of a value into a coordinate. |
| **Basic Math** | `width="{score * 5}"` | Scaling a value to fit a larger `viewBox`. |
| **Percentage** | `width="{(current div total) * 100}"` | Calculating bar width on the fly. |
| **Dynamic Color** | `fill="{$statusColor}"` | Using a pre-defined XSL variable for logic-based colors. |
| **Coordinates** | `cx="{x_pos}" cy="{y_pos}"` | Placing data points in a scatter or line chart. |

---

### 🛠️ Example: The "Progress Bar" Snippet

This snippet is perfect for your "Subversive Deployment" strategy. It looks like a complex graphic but is only a few lines of code.

```xml
<fo:instream-foreign-object>
  <svg:svg width="50mm" height="10mm" viewBox="0 0 100 10" xmlns:svg="http://www.w3.org/2000/svg">
    
    <svg:rect x="0" y="0" width="100" height="10" fill="#EEEEEE" rx="2" />
    
    <svg:rect x="0" y="0" height="10" rx="2"
              width="{(score_data div total_data) * 100}" 
              fill="{if (score_data > 80) then '#228B22' else '#4A90E2'}" />
              
  </svg:svg>
</fo:instream-foreign-object>

```

*Note: The `if/then/else` syntax inside `{}` requires XSLT 2.0. If you are strictly on XSLT 1.0, use an `<xsl:variable>` to calculate the color first, then inject it with `fill="{$yourVariable}"`.*

---

### ⚠️ The "FOP/Batik" SVG Gotchas

* **Case Sensitivity:** SVG is case-sensitive. `viewBox` must have a capital **B**.
* **Namespace:** Always include `xmlns:svg="http://www.w3.org/2000/svg"` inside the `svg:svg` tag or at the root of your XSL.
* **Units:** Inside a `viewBox`, **do not** use units like `px` or `mm`. Just use the raw numbers. The `svg:svg` tag's `width` and `height` handle the physical size in the PDF.
* **Text:** If your SVG contains text, use `font-family="sans-serif"`. FOP is much better at mapping "sans-serif" to a standard PDF font than trying to find a specific system font like "Arial."

---

That mockup is a perfect candidate for an **instream SVG**. It uses a "dual-bar" design: a static vertical anchor on the left and a dynamic horizontal progress bar.

By using `viewBox`, we can set the "math" to be out of 100, which makes your XSLT logic much simpler.

### SVG Implementation for Mockup

```xml
<fo:block space-after="5mm">
  <fo:instream-foreign-object>
    <svg:svg width="80mm" height="24mm" viewBox="0 0 200 60" xmlns:svg="http://www.w3.org/2000/svg">
      
      <svg:rect x="0" y="5" width="8" height="50" fill="none" stroke="black" stroke-width="1.5" />
      
      <svg:rect x="8" y="15" height="30" fill="#4A90E2">
        <xsl:attribute name="width">
          <xsl:value-of select="(count(found_items) div count(total_items)) * 120"/>
        </xsl:attribute>
      </svg:rect>
      
      <svg:text x="135" y="38" font-family="sans-serif" font-size="14" font-weight="bold" fill="black">
        <xsl:value-of select="round((count(found_items) div count(total_items)) * 100)"/>%
      </svg:text>
      
      <svg:text x="50" y="58" font-family="sans-serif" font-size="10" fill="#333333">
        Found <xsl:value-of select="count(found_items)"/> of <xsl:value-of select="count(total_items)"/> items.
      </svg:text>
      
    </svg:svg>
  </fo:instream-foreign-object>
</fo:block>

```

---

### Why this works for your Cleanup:

* **Vertical Alignment:** Using `svg:text` nodes inside the same `viewBox` as the rectangles ensures that the text and the bar never "drift" apart, even if the PDF zooms or the page flows.
* **The "Anchor" Look:** By setting the vertical bar to `fill="none"` and `stroke="black"`, it perfectly emulates that "base" look from your screenshot.
* **Font Handling:** I used `font-family="sans-serif"`. When FOP processes this via Batik, it will map this to the standard PDF Helvetica or Arial, keeping the text crisp and searchable.
* **Easy Templating:** Since the "Found X of Y" is just a standard `<xsl:value-of>`, you can put whatever complex Xpath logic you need in there (counting nodes, summing values, etc.) without touching the SVG geometry.

### One Small Tweak:

In the mockup, the subtext "Found 5 of 25 items" is slightly indented. I set the `x="50"` in that text node to shift it right. You can adjust that `x` value to get the exact alignment your "particular" stakeholders prefer.

**Since you're using this for progress tracking, would you like to see how to add an `<xsl:choose>` to change the bar color to red if the progress is below a certain threshold?**

To handle that logic-based color shift in **XSLT 1.0** (the standard for most Apache FOP environments), the cleanest approach is to calculate the color in a variable first and then inject it into the SVG `fill` attribute using an **Attribute Value Template**.

### Dynamic Color Logic for the Progress Bar

Here is how you can wrap the progress bar in a conditional color check:

```xml
<fo:block space-after="5mm">
  <xsl:variable name="barColor">
    <xsl:choose>
      <xsl:when test="(count(found_items) div count(total_items)) &lt; 0.50">
        <xsl:text>#D0021B</xsl:text> </xsl:when>
      <xsl:otherwise>
        <xsl:text>#4A90E2</xsl:text> </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <fo:instream-foreign-object>
    <svg:svg width="80mm" height="24mm" viewBox="0 0 200 60" xmlns:svg="http://www.w3.org/2000/svg">
      
      <svg:rect x="0" y="5" width="8" height="50" fill="none" stroke="black" stroke-width="1.5" />
      
      <svg:rect x="8" y="15" height="30" fill="{$barColor}">
        <xsl:attribute name="width">
          <xsl:value-of select="(count(found_items) div count(total_items)) * 120"/>
        </xsl:attribute>
      </svg:rect>
      
      <svg:text x="135" y="38" font-family="sans-serif" font-size="14" font-weight="bold" fill="black">
        <xsl:value-of select="round((count(found_items) div count(total_items)) * 100)"/>%
      </svg:text>
      
      <svg:text x="50" y="58" font-family="sans-serif" font-size="10" fill="#333333">
        Found <xsl:value-of select="count(found_items)"/> of <xsl:value-of select="count(total_items)"/> items.
      </svg:text>
      
    </svg:svg>
  </fo:instream-foreign-object>
</fo:block>

```

### Why this works for your "Subversive" Cleanup:

* **Visual Urgency:** If a user sees a red bar, they instantly know something needs attention without reading the numbers. It’s the kind of "accidental" feature that stakeholders end up loving.
* **Centralized Logic:** By using the `<xsl:variable>`, you don't have to repeat the `count(...)` logic inside the SVG attributes. It keeps the actual drawing code clean.
* **Compatibility:** SVG `fill` attributes accept standard Hex codes, which are very safe for PDF rendering in Apache FOP.

### Implementation Tip:

If you find yourself using this specific "Bar with Base" graphic in multiple reports, you can move this entire block into its own named `<xsl:template name="ProgressBar">`. Then, you can simply call it with parameters:
`<xsl:call-template name="ProgressBar"><xsl:with-param name="found" select="..."/></xsl:call-template>`

This would give you that **Global Policy Change** ability you were worried about earlier—change the bar height or font in one template, and every report updates automatically.

**Would you like me to mock up what that reusable named template would look like for your core library?**

Moving this into a named template is the ultimate way to protect your "Subversive Deployment" strategy. It allows you to fix a bug or change a color for every report in your system simultaneously, without your "particular" stakeholders ever needing to touch the underlying math.

### The Reusable "Progress Bar" Component

You can place this in your core library. It accepts the `found` and `total` counts as parameters, handles the math, and even includes a default color logic.

```xml
<xsl:template name="draw-progress-bar">
  <xsl:param name="found" select="0"/>
  <xsl:param name="total" select="1"/>
  
  <xsl:variable name="percent" select="($found div $total) * 100"/>
  
  <xsl:variable name="barColor">
    <xsl:choose>
      <xsl:when test="$percent &lt; 50">#D0021B</xsl:when> <xsl:when test="$percent &lt; 80">#F5A623</xsl:when> <xsl:otherwise>#4A90E2</xsl:otherwise>             </xsl:choose>
  </xsl:variable>

  <fo:block space-after="5mm">
    <fo:instream-foreign-object>
      <svg:svg width="80mm" height="24mm" viewBox="0 0 200 60" xmlns:svg="http://www.w3.org/2000/svg">
        
        <svg:rect x="0" y="5" width="8" height="50" fill="none" stroke="black" stroke-width="1.5" />
        
        <svg:rect x="8" y="15" height="30" fill="{$barColor}" width="{($found div $total) * 120}" />
        
        <svg:text x="135" y="38" font-family="sans-serif" font-size="14" font-weight="bold">
          <xsl:value-of select="round($percent)"/>%
        </svg:text>
        
        <svg:text x="50" y="58" font-family="sans-serif" font-size="10" fill="#333333">
          Found <xsl:value-of select="$found"/> of <xsl:value-of select="$total"/> items.
        </svg:text>
      </svg:svg>
    </fo:instream-foreign-object>
  </fo:block>
</xsl:template>

```

---

### How to call it from your reports

Now, in any of your 30 reports, you don't have to look at SVG code ever again. You just call the template:

```xml
<xsl:call-template name="draw-progress-bar">
  <xsl:with-param name="found" select="count(item[@status='complete'])"/>
  <xsl:with-param name="total" select="count(item)"/>
</xsl:call-template>

```

### Why this is the "Pro" Move

* **Encapsulation:** If you want to change the "Found X of Y" font size, you change it **once** in the named template, and it propagates everywhere.
* **Cleanliness:** Your main report flow stays focused on business logic, not "geometry math."
* **Error Handling:** You can add logic to the template to handle `total = 0` (to avoid division-by-zero errors) in one central place.

That should give you a very clean, maintainable way to roll out these graphics. You’ve gone from "standardizing margins" to "building a component-based reporting system" in a single day!
