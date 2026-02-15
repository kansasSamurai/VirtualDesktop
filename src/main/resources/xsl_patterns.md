Moving from legacy XSL-FO code to modern best practices usually involves shedding a few "defensive coding" habits. Here is the breakdown on `block-container` and the table-in-block mystery.

---

## 1. The "Proper" Use of `fo:block-container`

Think of a `fo:block` as a paragraph and a `fo:block-container` as a **layout box**. You don't need a container for standard flow text, but you *must* use it when you need to break out of the normal document flow.

### Required/Proper Use Cases:

* **Absolute Positioning:** If you need to place an element at specific coordinates (e.g., a "Draft" watermark or a fixed header at ), you need a `block-container` with `position="absolute"`.
* **Rotation:** If you want to rotate a specific section of content (like a wide table) 90 degrees without rotating the whole page, you wrap it in a container with `reference-orientation="90"`.
* **Fixed Dimensions:** When you need a box with a specific `height` and `width` that should not grow with its content (perhaps with `overflow="scroll"` or `"hidden"`).
* **Multi-column Layouts:** If you want a specific part of a page to have three columns while the rest has one, the `column-count` attribute is applied to the container.

**The Rule of Thumb:** If you just want to set margins, padding, or alignment, use a `fo:block`. If you want to manipulate the **geometry** or **coordinate system** of the area, use a `fo:block-container`.

---

## 2. Wrapping Tables in Blocks: Is it Necessary?

The short answer: **No, it is not required by the XSL-FO specification.** An `fo:table` is a formal FO object that can live happily as a direct child of `fo:flow`.

### Why the legacy code does it:

In the early days of FO processors (like older versions of FOP), tables sometimes struggled with "keep-with-next" logic or spacing attributes. Wrapping them in a block was a "hack" to force the engine to treat the table as a single block-level unit for page breaking.

### Pros and Cons of Wrapping `fo:table` in `fo:block`

| Feature | Wrapping in `fo:block` | Direct `fo:table` |
| --- | --- | --- |
| **Spacing** | Allows you to use `space-before` on the block, which is sometimes more reliably rendered than on the table itself. | Standard; `space-before` on the table is valid and cleaner. |
| **Logic** | Can help "glue" a heading to a table by putting both inside one block with `keep-together.within-page="always"`. | Requires `keep-with-after` on the heading or `keep-with-previous` on the table. |
| **Inheritance** | **Con:** Tables might unexpectedly inherit text-alignment or line-height from the wrapper block, causing weird cell formatting. | **Pro:** Cleaner inheritance; the table styles are isolated. |
| **Code Weight** | Increases nesting depth, making the XSLT harder to read and the FO file larger. | Minimally nested and follows the spec's intent. |

### The Verdict

Unless you are using an ancient formatter or trying to solve a very specific page-breaking bug where a table is splitting away from its title, **stop wrapping them.** It adds unnecessary "div soup" to your FO and can cause headaches with inherited properties.

---

Here is a clean, "modern" XSL-FO starter template for an A4 page. I’ve utilized `fo:static-content` for the headers and footers and used standard `space-before` attributes on the table rather than wrapping it in a block.

### Modern XSL-FO Starter Template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
  
  <fo:layout-master-set>
    <fo:simple-page-master master-name="A4-Standard" 
      page-height="297mm" page-width="210mm" 
      margin-top="10mm" margin-bottom="10mm" margin-left="20mm" margin-right="20mm">
      
      <fo:region-body margin-top="20mm" margin-bottom="15mm"/>
      
      <fo:region-before extent="15mm" display-align="before"/>
      
      <fo:region-after extent="10mm" display-align="after"/>
      
    </fo:simple-page-master>
  </fo:layout-master-set>

  <fo:page-sequence master-reference="A4-Standard">
    
    <fo:static-content flow-name="xsl-region-before">
      <fo:table table-layout="fixed" width="100%" border-bottom="0.5pt solid black">
        <fo:table-column column-width="50%"/>
        <fo:table-column column-width="50%"/>
        <fo:table-body>
          <fo:table-row font-size="9pt" color="#666666">
            <fo:table-cell>
              <fo:block>Company Name | Internal Document</fo:block>
            </fo:table-cell>
            <fo:table-cell text-align="right">
              <fo:block>Confidential</fo:block>
            </fo:table-cell>
          </fo:table-row>
        </fo:table-body>
      </fo:table>
    </fo:static-content>

    <fo:static-content flow-name="xsl-region-after">
      <fo:block text-align="center" font-size="9pt" border-top="0.5pt solid #CCCCCC" padding-top="2mm">
        Page <fo:page-number/> of <fo:page-number-citation ref-id="end-of-doc"/>
      </fo:block>
    </fo:static-content>

    <fo:flow flow-name="xsl-region-body" font-family="Helvetica, Arial, sans-serif">
      
      <fo:block font-size="18pt" font-weight="bold" space-after="5mm">
        Document Title
      </fo:block>

      <fo:block space-after="10mm">
        This is a clean starter template. Notice that the table below is not wrapped 
        in a block; we use <fo:inline font-family="monospace">space-before</fo:inline> 
        directly on the table element.
      </fo:block>

      <fo:table table-layout="fixed" width="100%" space-before="5mm" border="1pt solid black">
        <fo:table-column column-width="30%"/>
        <fo:table-column column-width="70%"/>
        
        <fo:table-header background-color="#EEEEEE" font-weight="bold">
          <fo:table-row>
            <fo:table-cell padding="2mm" border="0.5pt solid black">
              <fo:block>Key</fo:block>
            </fo:table-cell>
            <fo:table-cell padding="2mm" border="0.5pt solid black">
              <fo:block>Value Description</fo:block>
            </fo:table-cell>
          </fo:table-row>
        </fo:table-header>

        <fo:table-body>
          <fo:table-row>
            <fo:table-cell padding="2mm" border="0.5pt solid black">
              <fo:block>Status</fo:block>
            </fo:table-cell>
            <fo:table-cell padding="2mm" border="0.5pt solid black">
              <fo:block>Active - No block wrapper needed here.</fo:block>
            </fo:table-cell>
          </fo:table-row>
        </fo:table-body>
      </fo:table>

      <fo:block id="end-of-doc"/>
    </fo:flow>
  </fo:page-sequence>
</fo:root>

```

### Key "Modern" Patterns Included:

* **`table-layout="fixed"`:** Always use this. It’s significantly faster for processors than `auto` because the engine doesn't have to scan the entire table content to calculate column widths.
* **The Footer "Total Pages" Hack:** The `fo:page-number-citation` pointing to an ID at the very end of the document is the standard way to get a "Page X of Y" effect.
* **`display-align`:** Used in `fo:region-before/after`. This is much easier than using padding or empty blocks to vertically center your header/footer text.
* **Direct Table Spacing:** I used `space-before="5mm"` on the `fo:table`. Modern engines (FOP, AH, XEP) handle this perfectly.

Would you like me to add a section showing how to handle **conditional headers** (e.g., a different header for the first page versus subsequent pages)?

To handle a different header for the first page while keeping the body content flowing seamlessly, you need a **Page Sequence Master**.

In XSL-FO, you don't just point your content at a single page; you point it at a "Master" that contains a set of rules (alternatives).

### The "Conditional" Logic

The pattern below uses `fo:repeatable-page-master-alternatives`. The engine checks each condition in order:

1. **Is this the first page?** If yes, use the `first-page` master.
2. **Is it any other page?** Use the `rest-of-pages` master.

### Modern Template with Conditional Headers

```xml
<?xml version="1.0" encoding="UTF-8"?>
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
  
  <fo:layout-master-set>
    <fo:simple-page-master master-name="first-page" page-height="297mm" page-width="210mm" margin="15mm">
      <fo:region-body margin-top="40mm"/> <fo:region-before region-name="header-first" extent="35mm"/>
      <fo:region-after extent="10mm"/>
    </fo:simple-page-master>

    <fo:simple-page-master master-name="rest-of-pages" page-height="297mm" page-width="210mm" margin="15mm">
      <fo:region-body margin-top="20mm"/> <fo:region-before region-name="header-rest" extent="15mm"/>
      <fo:region-after extent="10mm"/>
    </fo:simple-page-master>

    <fo:page-sequence-master master-name="document-master">
      <fo:repeatable-page-master-alternatives>
        <fo:conditional-page-master-reference master-reference="first-page" page-position="first"/>
        <fo:conditional-page-master-reference master-reference="rest-of-pages" page-position="any"/>
      </fo:repeatable-page-master-alternatives>
    </fo:page-sequence-master>
  </fo:layout-master-set>

  <fo:page-sequence master-reference="document-master">
    
    <fo:static-content flow-name="header-first">
      <fo:block font-size="24pt" font-weight="bold" color="navy" border-bottom="2pt solid navy">
        Big First Page Header
      </fo:block>
      <fo:block font-size="10pt" space-before="2mm">Report Date: 2026-02-06</fo:block>
    </fo:static-content>

    <fo:static-content flow-name="header-rest">
      <fo:block font-size="9pt" text-align="right" color="grey" border-bottom="0.5pt solid grey">
        Document Title - Continued
      </fo:block>
    </fo:static-content>

    <fo:flow flow-name="xsl-region-body" font-family="Helvetica">
      <fo:block font-size="12pt">
        The content here will start on the first page (under the big header) 
        and naturally flow onto page 2 (under the slim header).
      </fo:block>

      <fo:table table-layout="fixed" width="100%" space-before="10mm">
        <fo:table-column column-width="50%"/>
        <fo:table-column column-width="50%"/>
        <fo:table-body>
          <fo:table-row>
            <fo:table-cell border="1pt solid black" padding="5mm">
              <fo:block>Notice no block-container is needed for basic layout.</fo:block>
            </fo:table-cell>
            <fo:table-cell border="1pt solid black" padding="5mm">
              <fo:block>Table content here.</fo:block>
            </fo:table-cell>
          </fo:table-row>
        </fo:table-body>
      </fo:table>
    </fo:flow>
  </fo:page-sequence>
</fo:root>

```

### Key Representative Patterns Used:

* **Custom `region-name`:** By default, `fo:region-before` is named `xsl-region-before`. However, if you have two different headers, you **must** give them unique names (like `header-first` and `header-rest`) so the `fo:static-content` knows which one to "plug into."
* **Variable `margin-top`:** On the first page, the `region-body` has a `margin-top="40mm"` to make room for the large header. On subsequent pages, it’s only `20mm`. This prevents your body text from overlapping your header.
* **Ordered Logic:** In the `page-sequence-master`, the `page-position="first"` rule comes before `page-position="any"`. XSL-FO processors take the first rule that matches.

Would you like me to show how to add a "Last Page Only" footer for something like a signature block?

To target the last page specifically, we expand our `page-sequence-master` logic. While there is a `page-position="last"` attribute, it’s worth noting that some older FO engines struggle to predict the last page accurately if the content is dynamic. However, in modern XSL-FO, this is the standard approach.

### The "Last Page" Pattern

We will add a third page master specifically for the end of the document. This allows you to have a unique footer (like a summary or "End of Report" notice) that only appears once.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
  
  <fo:layout-master-set>
    <fo:simple-page-master master-name="first" page-height="297mm" page-width="210mm" margin="15mm">
      <fo:region-body margin-top="20mm" margin-bottom="20mm"/>
      <fo:region-before region-name="header-first" extent="15mm"/>
      <fo:region-after region-name="footer-regular" extent="15mm"/>
    </fo:simple-page-master>

    <fo:simple-page-master master-name="rest" page-height="297mm" page-width="210mm" margin="15mm">
      <fo:region-body margin-top="20mm" margin-bottom="20mm"/>
      <fo:region-before region-name="header-rest" extent="15mm"/>
      <fo:region-after region-name="footer-regular" extent="15mm"/>
    </fo:simple-page-master>

    <fo:simple-page-master master-name="last" page-height="297mm" page-width="210mm" margin="15mm">
      <fo:region-body margin-top="20mm" margin-bottom="40mm"/> <fo:region-before region-name="header-rest" extent="15mm"/>
      <fo:region-after region-name="footer-final" extent="35mm"/>
    </fo:simple-page-master>

    <fo:page-sequence-master master-name="main-sequence">
      <fo:repeatable-page-master-alternatives>
        <fo:conditional-page-master-reference master-reference="first" page-position="first"/>
        <fo:conditional-page-master-reference master-reference="last" page-position="last"/>
        <fo:conditional-page-master-reference master-reference="rest" page-position="any"/>
      </fo:repeatable-page-master-alternatives>
    </fo:page-sequence-master>
  </fo:layout-master-set>

  <fo:page-sequence master-reference="main-sequence">
    
    <fo:static-content flow-name="header-first">
      <fo:block font-weight="bold">First Page Header</fo:block>
    </fo:static-content>

    <fo:static-content flow-name="header-rest">
      <fo:block font-size="9pt">Continued...</fo:block>
    </fo:static-content>

    <fo:static-content flow-name="footer-regular">
      <fo:block text-align="center" font-size="8pt" border-top="0.5pt solid black">
        Standard Footer - Page <fo:page-number/>
      </fo:block>
    </fo:static-content>

    <fo:static-content flow-name="footer-final">
      <fo:block padding="5mm" background-color="#F9F9F9" border="1pt dashed #666">
        <fo:block font-weight="bold" space-after="2mm">Final Summary / Conclusion:</fo:block>
        <fo:block font-size="9pt">This content only appears at the bottom of the very last page.</fo:block>
        <fo:block text-align="right" font-size="8pt" space-top="5mm">
            End of Document - Page <fo:page-number/>
        </fo:block>
      </fo:block>
    </fo:static-content>

    <fo:flow flow-name="xsl-region-body">
      <fo:block>
        Content goes here. If the content is long enough to push to a second page, 
        that page will be considered the "last" page and will show the final footer.
      </fo:block>
    </fo:flow>
  </fo:page-sequence>
</fo:root>

```

### Pro-Tips for "Last Page" Logic:

1. **The Priority Trap:** Always place `page-position="first"` and `page-position="last"` **above** `page-position="any"`. The engine reads top-down; if "any" is first, it will catch every page and never look at your special rules.
2. **What if the doc is only 1 page?** If your document is only one page long, the engine has a conflict: is it "first" or "last"? By default, "first" usually wins. If you want a 1-page document to show the "Last" footer, you can use `page-position="only"`.
3. **Forced Last Page:** If you have content that *must* be on its own page at the very end (like a terms and conditions page), don't use `static-content`. Instead, put it at the end of your `fo:flow` and use `break-before="page"`.

Would you like to see how to handle **landscape pages** mixed into this same portrait document (e.g., for a very wide table)?

Integrating a landscape page into a portrait document is a classic XSL-FO requirement. The "modern" way to do this is to define a landscape `simple-page-master` and then use a specific property to trigger it mid-flow.

There are two ways to do this, but the **"Master Reference Override"** is the most robust.

### 1. Define the Landscape Master

First, add a new master to your `fo:layout-master-set`. Notice that the `page-height` and `page-width` are swapped compared to A4 portrait.

```xml
<fo:simple-page-master master-name="A4-Landscape" 
  page-height="210mm" page-width="297mm" 
  margin="15mm">
  <fo:region-body margin-top="20mm" margin-bottom="15mm"/>
  <fo:region-before extent="15mm"/>
  <fo:region-after extent="10mm"/>
</fo:simple-page-master>

```

### 2. The Logic: How to Switch

You have two choices depending on your needs:

#### Option A: The "Force Break" (Most Common)

If you have a wide table in the middle of a report, you can force the document to switch masters specifically for that section using the `master-reference` attribute on an `fo:block` or `fo:table`.

> **Note:** This requires your XSL-FO processor to support the `axf:master-reference` (Antennahouse) or similar extensions, but the standard way is actually to end one `fo:page-sequence` and start another.

#### Option B: Multiple Page Sequences (The Standard Way)

This is the most "portable" method. You end your portrait sequence and start a new landscape one.

```xml
<fo:page-sequence master-reference="main-sequence">
  <fo:flow flow-name="xsl-region-body">
    <fo:block>This content is in portrait mode.</fo:block>
  </fo:flow>
</fo:page-sequence>

<fo:page-sequence master-reference="A4-Landscape">
  <fo:static-content flow-name="xsl-region-before">
    <fo:block text-align="center">Wide Table View (Landscape)</fo:block>
  </fo:static-content>

  <fo:flow flow-name="xsl-region-body">
    <fo:table table-layout="fixed" width="100%">
      <fo:table-column column-width="20%"/>
      <fo:table-column column-width="20%"/>
      <fo:table-column column-width="20%"/>
      <fo:table-column column-width="20%"/>
      <fo:table-column column-width="20%"/>
      <fo:table-body>
        <fo:table-row>
          <fo:table-cell border="1pt solid black"><fo:block>Wide Data</fo:block></fo:table-cell>
          <fo:table-cell border="1pt solid black"><fo:block>Wide Data</fo:block></fo:table-cell>
          <fo:table-cell border="1pt solid black"><fo:block>Wide Data</fo:block></fo:table-cell>
          <fo:table-cell border="1pt solid black"><fo:block>Wide Data</fo:block></fo:table-cell>
          <fo:table-cell border="1pt solid black"><fo:block>Wide Data</fo:block></fo:table-cell>
        </fo:table-row>
      </fo:table-body>
    </fo:table>
  </fo:flow>
</fo:page-sequence>

<fo:page-sequence master-reference="main-sequence">
  <fo:flow flow-name="xsl-region-body">
    <fo:block>Back to normal portrait mode.</fo:block>
  </fo:flow>
</fo:page-sequence>

```

### Key Considerations for Landscape

* **Header/Footer Replication:** When you switch sequences, the engine "forgets" the `fo:static-content` from the previous sequence. You must copy/paste (or use XSLT templates to reuse) your headers and footers into every `fo:page-sequence`.
* **Page Numbering:** By default, a new sequence resets the page count to 1. To keep it continuous, use `initial-page-number="auto"` on your subsequent sequences.
* **The "Block-Container" Cheat:** If you only have one small table that needs to be landscape and you don't want to mess with page masters, you can stay in Portrait and use:
```xml
<fo:block-container reference-orientation="90">
   </fo:block-container>

```


*This is where `block-container` actually shines!* It rotates the content *inside* the portrait page.

When generating XSL-FO with XSLT, the biggest "anti-pattern" is hard-coding styles into every table cell. This makes maintenance a nightmare.

The modern best practice is to treat **XSLT Named Templates** like "UI Components" or "CSS Classes." This keeps your logic clean and ensures all table headers across your document look identical.

---

## The "Component" Pattern for Table Headers

Instead of writing `fo:table-cell` properties over and over, you create a reusable template for the "Header Cell."

### 1. The Named Template (The "Component")

Place this at the bottom of your XSLT file. It accepts a parameter for the text but handles all the "styling" and "structural" FO code itself.

```xml
<xsl:template name="tpl.table.header.cell">
  <xsl:param name="content" />
  <xsl:param name="alignment" select="'left'" /> <fo:table-cell padding="2mm" 
                 background-color="#2D5A27" 
                 color="white" 
                 display-align="center"
                 border="0.5pt solid black">
    <fo:block font-weight="bold" 
              font-size="10pt" 
              text-align="{$alignment}">
      <xsl:value-of select="$content" />
    </fo:block>
  </fo:table-cell>
</xsl:template>

```

### 2. Implementation in the Table

Now, your main table logic becomes much more readable. You aren't bogged down by styling attributes; you are just passing data.

```xml
<fo:table table-layout="fixed" width="100%" space-before="5mm">
  <fo:table-column column-width="40%"/>
  <fo:table-column column-width="60%"/>
  
  <fo:table-header>
    <fo:table-row>
      <xsl:call-template name="tpl.table.header.cell">
        <xsl:with-param name="content" select="'Product Description'"/>
      </xsl:call-template>
      
      <xsl:call-template name="tpl.table.header.cell">
        <xsl:with-param name="content" select="'Unit Price'"/>
        <xsl:with-param name="alignment" select="'right'"/>
      </xsl:call-template>
    </fo:table-row>
  </fo:table-header>

  <fo:table-body>
    </fo:table-body>
</fo:table>

```

---

## Why this is a "Best Practice"

1. **Centralized Styling:** If your branding changes from Green (`#2D5A27`) to Blue, you change **one** line of code in the template, and every table in your 100-page document updates.
2. **Cleaner Logic:** Your main XSLT flow focuses on the *structure* of the data rather than the *visuals* of the PDF.
3. **Encapsulation:** You can bake logic into the header. For example, you could add an `xsl:if` inside the template to automatically add a "Required" asterisk if a certain condition is met.
4. **Consistency:** It prevents "Style Drift" where one developer uses `padding="2mm"` and another uses `padding="5px"`.

---

## Going Further: Attribute Sets

If you find named templates too "wordy," XSLT also offers `attribute-sets`, which act exactly like CSS classes:

```xml
<xsl:attribute-set name="header-style">
  <xsl:attribute name="font-weight">bold</xsl:attribute>
  <xsl:attribute name="background-color">#EEEEEE</xsl:attribute>
  <xsl:attribute name="padding">2mm</xsl:attribute>
</xsl:attribute-set>

<fo:table-cell xsl:use-attribute-sets="header-style">
  <fo:block>Header Text</fo:block>
</fo:table-cell>

```

The tension in XSL-FO development: **Attributes vs. Structure.**

The general consensus among XSL-FO pros is:

* Use **Attribute Sets** for "CSS-like" properties (colors, fonts, margins).
* Use **Named Templates** for "Component-like" structures (adding the `fo:block` inside the cell, handling empty values, or complex borders).

Here is a "Style Sheet" pattern that combines both for a robust, maintainable project.

---

## 1. The Global "Theme" (Attribute Sets)

Create a separate XSL file (e.g., `theme.xsl`) and include it. This is your single source of truth for visuals.

```xml
<xsl:attribute-set name="attr.type.body">
  <xsl:attribute name="font-family">Helvetica, sans-serif</xsl:attribute>
  <xsl:attribute name="font-size">10pt</xsl:attribute>
  <xsl:attribute name="line-height">1.4</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="attr.type.h1">
  <xsl:attribute name="font-size">18pt</xsl:attribute>
  <xsl:attribute name="font-weight">bold</xsl:attribute>
  <xsl:attribute name="color">#1A3A5E</xsl:attribute>
  <xsl:attribute name="space-after">5mm</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="attr.table.cell.base">
  <xsl:attribute name="padding">2mm</xsl:attribute>
  <xsl:attribute name="border">0.5pt solid #CCCCCC</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="attr.table.header" use-attribute-sets="attr.table.cell.base">
  <xsl:attribute name="background-color">#1A3A5E</xsl:attribute>
  <xsl:attribute name="color">white</xsl:attribute>
  <xsl:attribute name="font-weight">bold</xsl:attribute>
</xsl:attribute-set>

```

---

## 2. The "Logic" Layer (Templates)

This handles the "complicated" FO bits so you don't have to remember them. For example, a "Smart Cell" that handles null values so your table borders don't collapse.

```xml
<xsl:template name="ui.cell">
  <xsl:param name="content" />
  <xsl:param name="style" select="'attr.table.cell.base'" />
  
  <fo:table-cell xsl:use-attribute-sets="{$style}">
    <fo:block>
      <xsl:choose>
        <xsl:when test="not($content) or $content = ''">
          <xsl:text>&#160;</xsl:text> </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$content" />
        </xsl:otherwise>
      </xsl:choose>
    </fo:block>
  </fo:table-cell>
</xsl:template>

```

---

## 3. The Implementation (Clean & Modern)

When you build your table, you get the best of both worlds. The code is readable, and the complexity is hidden.

```xml
<fo:table table-layout="fixed" width="100%">
  <fo:table-column column-width="50%"/>
  <fo:table-column column-width="50%"/>

  <fo:table-header>
    <fo:table-row>
      <xsl:call-template name="ui.cell">
        <xsl:with-param name="content" select="'Description'"/>
        <xsl:with-param name="style" select="'attr.table.header'"/>
      </xsl:call-template>
      <xsl:call-template name="ui.cell">
        <xsl:with-param name="content" select="'Amount'"/>
        <xsl:with-param name="style" select="'attr.table.header'"/>
      </xsl:call-template>
    </fo:table-row>
  </fo:table-header>

  <fo:table-body xsl:use-attribute-sets="attr.type.body">
    <xsl:for-each select="items/item">
      <fo:table-row>
        <xsl:call-template name="ui.cell">
          <xsl:with-param name="content" select="desc"/>
        </xsl:call-template>
        <xsl:call-template name="ui.cell">
          <xsl:with-param name="content" select="price"/>
        </xsl:call-template>
      </fo:table-row>
    </xsl:for-each>
  </fo:table-body>
</fo:table>

```

---

## Why this wins:

* **The "Shadow" Attribute Set:** Notice `attr.table.header` uses `use-attribute-sets="attr.table.cell.base"`. This is **attribute inheritance**. It means headers automatically get the same padding and borders as body cells without you retyping them.
* **Dynamic Styles:** In the template, the `style` parameter uses a "variable attribute set" name. While standard XSLT 1.0 is picky about dynamic names in `xsl:use-attribute-sets`, most modern processors handle this or allow a simple `xsl:choose` inside the template to switch between sets.
* **Maintenance:** If you want to change the font globally, you touch one line in `theme.xsl`. If you want to change how *all* cells handle empty data, you touch one line in the `ui.cell` template.

**One quick tip:** If you use this approach, keep your attribute sets in a dedicated file and use `<xsl:import href="theme.xsl"/>`. This allows you to have different themes (e.g., `print-theme.xsl` and `web-theme.xsl`) for the same logic.

To implement zebra striping effectively, we combine the **XSLT position()** function with our existing template logic.

The "modern" way to do this is to pass a boolean "even or odd" flag to your row or cell template. This avoids cluttering your main data loop with color logic.

---

## 1. Update the Theme

First, add a new attribute set to your `theme.xsl` for the striped row background.

```xml
<xsl:attribute-set name="attr.table.cell.base">
  <xsl:attribute name="padding">2mm</xsl:attribute>
  <xsl:attribute name="border">0.5pt solid #CCCCCC</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="attr.table.cell.stripe">
  <xsl:attribute name="background-color">#F2F5F8</xsl:attribute>
</xsl:attribute-set>

```

---

## 2. Update the "Smart Cell" Template

We modify the template to accept an `is-striped` parameter. If true, it merges the base style with the stripe style.

```xml
<xsl:template name="ui.cell">
  <xsl:param name="content" />
  <xsl:param name="style" select="'attr.table.cell.base'" />
  <xsl:param name="is-striped" select="false()" />
  
  <fo:table-cell xsl:use-attribute-sets="attr.table.cell.base">
    <xsl:if test="$is-striped">
      <xsl:attribute name="background-color">#F2F5F8</xsl:attribute>
    </xsl:if>
    
    <xsl:if test="$style != 'attr.table.cell.base'">
       </xsl:if>

    <fo:block>
      <xsl:value-of select="normalize-space($content)" />
      <xsl:if test="normalize-space($content) = ''">&#160;</xsl:if>
    </fo:block>
  </fo:table-cell>
</xsl:template>

```

---

## 3. The Implementation (The "Zebra" Loop)

In your main loop, use the modulo operator `$pos mod 2` to determine if the current row is even or odd.

```xml
<fo:table-body xsl:use-attribute-sets="attr.type.body">
  <xsl:for-each select="items/item">
    <xsl:variable name="is-even" select="position() mod 2 = 0"/>
    
    <fo:table-row>
      <xsl:call-template name="ui.cell">
        <xsl:with-param name="content" select="product_name"/>
        <xsl:with-param name="is-striped" select="$is-even"/>
      </xsl:call-template>
      
      <xsl:call-template name="ui.cell">
        <xsl:with-param name="content" select="total_amount"/>
        <xsl:with-param name="is-striped" select="$is-even"/>
      </xsl:call-template>
    </fo:table-row>
  </xsl:for-each>
</fo:table-body>

```

---

## Why This Pattern is Robust

* **Encapsulation:** The logic for *how* an even row looks (is it grey? light blue? a different border?) stays in the template or theme, not in the data loop.
* **Processor Friendly:** Using `xsl:attribute` inside the cell is standard XSLT and works across all FO engines (FOP, Antenna House, XEP).
* **Flexibility:** If you later decide that only the *first* column should be striped, or only rows with a "Pending" status, you just change the logic of the `$is-even` variable.

### A Quick "Gotcha" Note

If you are using `fo:table-row` with a background color instead of the cell, be aware that some older PDF viewers (and very rarely, older FO engines) have "bleeding" issues where the row color doesn't perfectly fill the cell borders. Applying the background to the **cell** (as shown above) is generally the "safest" modern practice for pixel-perfect PDF rendering.

Generating a **Table of Contents (TOC)** and handling **Images** are the two areas where XSL-FO can feel the most "finicky." Let’s tackle the TOC first, then dive into the nuances of images.

---

## 1. Table of Contents (The "Leader" Pattern)

In XSL-FO, a TOC relies on `fo:page-number-citation`. This dynamically looks up the page number of a specific `id` in your document.

The "modern" look uses a "leader"—those dots that connect the title to the page number.

### The TOC Template

```xml
<xsl:template name="ui.toc.line">
  <xsl:param name="label" />
  <xsl:param name="target-id" />
  
  <fo:block text-align-last="justify" font-size="10pt" space-after="2mm">
    <fo:basic-link internal-destination="{$target-id}">
      <xsl:value-of select="$label"/>
      <fo:leader leader-pattern="dots" 
                 leader-pattern-width="3pt" 
                 leader-alignment="reference-area"/>
      <fo:page-number-citation ref-id="{$target-id}"/>
    </fo:basic-link>
  </fo:block>
</xsl:template>

```

### Key Representative Pattern:

* **`text-align-last="justify"`:** This is the secret sauce. It forces the text to the left and the page number to the far right, stretching the dots in between.
* **`fo:basic-link`:** Always wrap your TOC lines in this. It makes the PDF interactive (clickable).
* **`leader-alignment`:** Using `reference-area` ensures the dots align vertically across different lines, so they don't look "staggered."

---

## 2. Images: Mastering `fo:external-graphic`

Images are tricky because XSL-FO has to balance the **Intrinsic Size** (the image's actual pixels/dpi) with the **Content Area** (the space you’ve given it).

### Recommended Parameters

| Parameter | Best Practice / Use Case |
| --- | --- |
| **`content-width="scale-to-fit"`** | **The Gold Standard.** Use this with a fixed `width`. It ensures the image shrinks if it's too big but maintains its aspect ratio. |
| **`width="100%"`** | Tells the image to fill the available horizontal space (like the column or page). |
| **`scaling="uniform"`** | Always use this. It prevents the "funhouse mirror" effect where images get stretched vertically or horizontally. |
| **`content-height`** | Usually leave this alone if you’ve set `content-width`. Setting both risks breaking the aspect ratio unless you use `scaling="uniform"`. |

### The "Safe" Modern Pattern

```xml
<fo:block text-align="center" space-after="5mm">
  <fo:external-graphic src="url('logo.png')" 
                       width="50mm" 
                       content-width="scale-to-fit" 
                       scaling="uniform" />
</fo:block>

```

### Tricky Image Tips & Nuances:

1. **The "Resolution" Headache:** If an image looks huge or tiny, it's likely a DPI issue. XSL-FO engines usually assume 72dpi or 96dpi. If you want precise control, explicitly set the `width` in `mm` or `in` rather than relying on pixels.
2. **Avoid `height` when possible:** If you define both `width` and `height`, and the image aspect ratio doesn't match, the engine will either leave white space (default) or stretch it. It’s almost always better to define the **maximum width** and let the height be "auto."
3. **Vertical Alignment:** If you put an image inside a table cell and it's not centering, remember that `display-align="center"` on the `fo:table-cell` handles the vertical, but the `fo:block` inside the cell needs `text-align="center"` for the horizontal.
4. **The "Ghost" Space:** If you have a mysterious 3pt gap below your image, it’s usually because the image is treated like a character in a line of text. Setting `line-height="0"` on the parent `fo:block` often fixes this.
5. **SVGs are King:** For logos and icons, use SVGs. They are XML-based, infinitely scalable, and usually result in much smaller PDF file sizes.

### Advanced Trick: The "Dynamic Image Box"

If you need to fit an image into a strict 50x50mm square without knowing its orientation (Portrait vs Landscape):

* Set `width="50mm"` and `height="50mm"`.
* Set `content-width="scale-to-fit"` and `content-height="scale-to-fit"`.
* Set `scaling="uniform"`.
* The engine will make the image as large as possible within that box without cropping or stretching it.

Performance and bitmap handling are where the "theory" of XSL-FO meets the "reality" of the rendering engine. Here are the practical takeaways for your toolkit.

### 1. The Bitmap "Format War": PNG vs. JPEG vs. BMP

While most modern engines (FOP, Antenna House) support multiple formats, they are not treated equally under the hood:

* **Avoid BMP:** Honestly, just don't use it. BMPs are uncompressed. The FO engine has to load the entire uncompressed file into memory, and then, often, it embeds that raw data into the PDF, leading to massive file sizes.
* **PNG for Graphics:** Use PNG for logos, screenshots, or anything with text/flat colors. PNG supports **alpha transparency**, which is vital if you have a background color on your page or table cell.
* **JPEG for Photos:** Use JPEG for actual photographs. JEPGs are significantly smaller for complex imagery.
* **The "Pass-Through" Factor:** High-end engines (like Antenna House) can often "pass through" a JPEG directly into the PDF wrapper without re-encoding it. This is lightning-fast and keeps the quality identical to the source.

**The "DPI" Trap:**
Bitmaps don't have an inherent size in millimeters; they have pixels. If your PNG is  pixels:

* At **72 DPI**, it will render as ~**105mm**.
* At **300 DPI**, it will render as ~**25mm**.
If you don't specify a `width` in your XSL-FO, the engine makes a guess based on the "Source Resolution" setting (usually 72 or 96). **Best Practice:** Always define the `width` or `content-width` in `mm` or `in` to stay in control.

---

### 2. Performance: Keeping the Engine Fast

If your documents stay under 50 pages, you won't notice much. If you hit 500+ pages, these three things will save you:

* **`table-layout="fixed"`:** I mentioned this earlier, but it is the #1 performance booster. "Auto" layout requires the engine to "buffer" the entire table in memory to find the widest content before it can draw a single line. "Fixed" allows it to draw as it goes.
* **Avoid Deep Nesting:** Legacy code loves `block` inside `block` inside `block-container` inside `list-item`. Each level of nesting creates a new "area" in the engine's memory. Keep your FO as "flat" as possible.
* **Break Up Page Sequences:** This is the big one. An XSL-FO engine usually holds an entire `fo:page-sequence` in memory to calculate total pages and cross-references. If you have a 1,000-page report, don't put it in one `page-sequence`. If you can break it into chapters (new `page-sequence` for each), the engine can "flush" the memory after each chapter is rendered.

---

### 3. A Quick "Tricky Requirement" Tip: Watermarks

Since we talked about images and `block-container`, here is a representative pattern for a "Confidential" or "Draft" watermark using a bitmap or SVG:

```xml
<fo:static-content flow-name="xsl-region-before">
  <fo:block-container absolute-position="absolute" 
                       top="100mm" left="20mm" 
                       width="150mm" height="100mm" 
                       reference-orientation="45" 
                       opacity="0.2">
    <fo:block font-size="80pt" color="red" text-align="center">
        DRAFT
    </fo:block>
  </fo:block-container>
</fo:static-content>

```

*Note: `opacity` is an XSL-FO 2.0 / Extension property. If your engine doesn't support it, you'll need to use a very light grey color or a transparent PNG.*

---

### Final "General" Best Practice

**Use a Formatter-Specific Extension:** Most engines (FOP, AH, XEP) have their own namespace for specialized things (like PDF bookmarks, metadata, or encryption). Don't be afraid to use them, but keep them organized so you can swap them if you ever change engines.

In XSL-FO, when you move to an inline model (like a legend), you shift from **Cell Alignment** to **Baseline Alignment**.

### The Solution: `alignment-baseline` and `dominant-baseline`

When you place an image and text side-by-side in a `fo:block`, the engine tries to align the bottom of the image with the "alphabetic baseline" of the text (the invisible line letters sit on). To fix this, you want to align the **middle** of the objects to each other.

#### The Modern "Legend" Pattern

```xml
<fo:block space-before="5mm" line-height="20pt">
  <fo:external-graphic src="url('icon1.svg')" 
                       width="12pt" 
                       content-width="scale-to-fit" 
                       alignment-baseline="middle"/>
  <fo:inline padding-left="2pt" padding-right="10pt" alignment-baseline="middle">
      Approved
  </fo:inline>

  <fo:external-graphic src="url('icon2.svg')" 
                       width="12pt" 
                       content-width="scale-to-fit" 
                       alignment-baseline="middle"/>
  <fo:inline padding-left="2pt" padding-right="10pt" alignment-baseline="middle">
      Pending
  </fo:inline>
  
  </fo:block>

```

### Why this works:

1. **`alignment-baseline="middle"`:** This is the magic property. When applied to both the `external-graphic` and the `inline` text wrapper, the engine calculates the vertical midpoint of both and snaps them together.
2. **`line-height`:** Setting a slightly generous `line-height` on the parent `fo:block` ensures that if your icons are taller than your text, the lines of the legend don't "crash" into each other.
3. **`padding` vs Space:** Using `padding-right` on the `fo:inline` creates the "gutter" between your legend items without using a table's column spacing.

---

### A "Pro" Tip for Accessibility: The `alt` text equivalent

Since you are doing this for accessibility, remember that `fo:external-graphic` itself doesn't always provide a "description" field that translates well to PDF tags in every engine.

To be truly accessible:

* **For decorative icons:** If the text "Approved" is next to the icon, the icon is redundant. Some high-end engines allow you to "artifact" the image (hide it from screen readers).
* **For meaningful icons:** Ensure your SVG has a `<title>` and `<desc>` tag inside the XML, as some modern PDF taggers can pull from there.

---

### The "Inline-Container" Alternative

If your icons and text vary wildly in size and `alignment-baseline` isn't giving you the "pixel-perfect" look you want, you can use `fo:inline-container`.

**Warning:** Use this sparingly, as it’s essentially a "mini block" inside a line of text, and some older engines (like FOP) have historically struggled with them.

```xml
<fo:inline-container vertical-align="middle" width="15mm">
  <fo:block>
    <fo:external-graphic src="url('icon.svg')" width="10mm"/>
  </fo:block>
</fo:inline-container>
<fo:inline vertical-align="middle">Approved</fo:inline>

```

To make the accessible "Legend" both dynamic and maintainable, build a template.

The goal is to pass a "list" of items (icons and labels) and let XSLT handle the spacing.

### 1. The "Legend Item" Template

This template handles a single icon-text pair. Notice the use of `alignment-baseline` to keep things vertically centered without a table.

```xml
<xsl:template name="ui.legend.item">
  <xsl:param name="icon-url" />
  <xsl:param name="label" />
  <xsl:param name="icon-width" select="'12pt'" />
  
  <fo:inline keep-together.within-line="always">
    
    <fo:external-graphic src="url('{$icon-url}')" 
                         width="{$icon-width}" 
                         content-width="scale-to-fit" 
                         scaling="uniform"
                         alignment-baseline="middle"/>
    
    <fo:inline alignment-baseline="middle" 
               padding-left="3pt" 
               padding-right="12pt" 
               font-size="9pt">
      <xsl:value-of select="$label"/>
    </fo:inline>
    
  </fo:inline>
</xsl:template>

```

### 2. The Implementation (Dynamic Loop)

In your main flow, you simply loop through your XML data. Because these are all `fo:inline` elements, they will "flow" like words in a paragraph. If the legend is too long for one line, it will naturally wrap to the next line—something a table cannot do easily!

```xml
<fo:block space-before="10mm" line-height="1.5">
  <fo:inline font-weight="bold" padding-right="5pt">Legend:</fo:inline>
  
  <xsl:for-each select="report/metadata/legend-items">
    <xsl:call-template name="ui.legend.item">
      <xsl:with-param name="icon-url" select="icon_path" />
      <xsl:with-param name="label" select="display_name" />
    </xsl:call-template>
  </xsl:for-each>
</fo:block>

```

---

### Why this is a "Modern Best Practice":

1. **Accessibility (WCAG):** Since this is a standard `fo:block` containing text and images, a screen reader reads it in a logical linear flow: "Icon [or Alt text], Approved, Icon, Pending..." instead of getting lost in table coordinates.
2. **`keep-together.within-line`:** This is the most important "nuance." By wrapping the icon and its label in an `fo:inline` with this property, you ensure that an icon will never appear at the end of a line with its label stranded at the start of the next line. They move together as a single unit.
3. **Reflowable Layout:** If you change your page margins or switch to a two-column layout, this legend will automatically re-wrap itself. A table would simply overflow the margin or require manual recalculation of column widths.
4. **Baseline Harmony:** By applying `alignment-baseline="middle"` to both the graphic and the text, you avoid the common "floating icon" look where the image sits too high relative to the lowercase letters.

### One final "Trick" for Icons:

If your icons are SVGs and you want them to match the text color perfectly, some engines allow you to omit the color in the SVG code and set `color` on the `fo:external-graphic`. However, most people find it easier to just ensure the SVGs are exported with the correct "theme" colors from the start.

When `alignment-baseline="middle"` gets you 95% of the way there but still looks "off" to the eye (often because of the way a specific font's x-height interacts with an icon's geometry), you have a few ways to perform that final "nudge."

Since your icons are a consistent size, you can move away from the automatic "middle" calculation and use more surgical tools.

### 1. The `baseline-shift` (The Micro-Adjuster)

This is the most direct way to nudge an element up or down without affecting the flow of the rest of the line. You can use it on either the `fo:external-graphic` or the `fo:inline` wrapper.

```xml
<fo:external-graphic src="url('icon.svg')" 
                     width="12pt" 
                     baseline-shift="-1.5pt" /> 

```

* **Why use this:** It is specifically designed for "fine-tuning." It doesn't change the height of the line; it just shifts the element's "sit" relative to the baseline.

### 2. Manual Vertical Alignment (No `alignment-baseline`)

If you find the automatic "middle" is unpredictable, you can align the **bottoms** and use padding or shift.

* **The Bottom-Align + Nudge:**
If you set the icon to sit on the baseline (the default), it often looks too high because text has "descenders" (like the tails on 'g' or 'y') that the icon doesn't account for.
```xml
<fo:external-graphic src="url('icon.svg')" 
                     width="12pt" 
                     baseline-shift="-2pt" /> 
<fo:inline>Text sit here</fo:inline>

```



### 3. The "Transparent Border" Trick

If you don't want to use `baseline-shift` (which can occasionally be finicky in very old versions of FOP), you can use a transparent border or padding on the icon to "push" it into place.

```xml
<fo:external-graphic src="url('icon.svg')" 
                     width="12pt"
                     padding-top="2pt" 
                     border-top="1pt solid transparent" />

```

### 4. Which one is "Best Practice"?

For a modern, professional look, I recommend **`baseline-shift`**. It’s the XSL-FO equivalent of a "relative top" in CSS.

**Revised Legend Template with Micro-Adjustment:**
Adding a `shift` parameter to your template gives you total control:

```xml
<xsl:template name="ui.legend.item">
  <xsl:param name="icon-url" />
  <xsl:param name="label" />
  <xsl:param name="shift" select="'-1pt'" /> <fo:inline keep-together.within-line="always">
    <fo:external-graphic src="url('{$icon-url}')" 
                         width="12pt" 
                         content-width="scale-to-fit" 
                         baseline-shift="{$shift}"/>
    
    <fo:inline padding-left="3pt" padding-right="10pt">
      <xsl:value-of select="$label"/>
    </fo:inline>
  </fo:inline>
</xsl:template>

```

### A Note on the "Optical Center"

Icons often look "centered" to a computer but "uncentered" to a human because:

1. **Caps vs. Lowercase:** Text with lots of capital letters has a different visual center than text with all lowercase letters.
2. **The X-Height:** Humans usually perceive the vertical center of text to be the middle of the lowercase letters (the x-height), not the full height of the capital letters.

If your legend labels are all Caps, `baseline-shift="-1pt"` is usually enough. If they are mixed case, you might need `-2pt` to make the icon feel "weighted" correctly against the lowercase letters.


In an XSL-FO document, the "proper" place to define global font settings is on the **`fo:page-sequence`** element.

While you might be tempted to put them on the `fo:root`, the specification dictates that many inherited properties (like font settings) are actually passed from the `fo:page-sequence` down to the `fo:flow` and its children.

### 1. The Standard Approach (Inheritance)

By defining the font on the `fo:page-sequence`, every `fo:block`, `fo:table`, and `fo:inline` within that sequence will inherit those settings unless you explicitly override them.

```xml
<fo:page-sequence master-reference="A4-Standard" 
                  font-family="Helvetica, Arial, sans-serif" 
                  font-size="10pt" 
                  color="#333333">
    
    <fo:static-content flow-name="xsl-region-before">
        <fo:block>Header Text</fo:block>
    </fo:static-content>

    <fo:flow flow-name="xsl-region-body">
        <fo:block>Main Body Text</fo:block>
        
        <fo:block font-weight="bold">Bold Title</fo:block>
    </fo:flow>
</fo:page-sequence>

```

### 2. The "Modern" XSLT Approach (Attribute Sets)

As we discussed earlier, if you want your global font settings to be truly "global" across multiple page sequences or even multiple documents, you should define them in an **Attribute Set** and apply that set to your page sequence.

**In your `theme.xsl`:**

```xml
<xsl:attribute-set name="attr.global.font">
  <xsl:attribute name="font-family">Helvetica, sans-serif</xsl:attribute>
  <xsl:attribute name="font-size">10pt</xsl:attribute>
  <xsl:attribute name="line-height">1.4</xsl:attribute>
  <xsl:attribute name="color">#000000</xsl:attribute>
</xsl:attribute-set>

```

**In your main template:**

```xml
<fo:page-sequence master-reference="main" xsl:use-attribute-sets="attr.global.font">
   <fo:flow flow-name="xsl-region-body">
      </fo:flow>
</fo:page-sequence>

```

### Why not the `fo:root`?

Technically, some processors allow you to put font attributes on the `fo:root`. However, the XSL-FO recommendation (the spec) focuses on the `fo:page-sequence` as the primary container for a "stream" of content. Placing it there ensures the most consistent behavior across different rendering engines (FOP, Antenna House, etc.).

You are spot-on. In XSL-FO, the "Inheritance Rule" is your best friend for tables. Setting styles at the `fo:table` level is the standard best practice because it keeps your code DRY (Don't Repeat Yourself) and prevents "attribute bloat" in your cells.

However, there is a **technical nuance** regarding what actually inherits and what doesn't.

### 1. What Inherits (Set these on `fo:table`)

Most "textual" properties flow down perfectly from the table to the rows and cells. Define these once on the `fo:table`:

* **`font-family`**, **`font-size`**, and **`color`**.
* **`line-height`**.
* **`text-align`** (Note: Setting this on the table will align text in every cell unless overridden).

### 2. What Does NOT Inherit (The "Cell" Trap)

This is where developers often get tripped up. **Layout** and **Border** properties generally do **not** inherit from the table to the cells.

* **`padding`**: Setting padding on `fo:table` puts space *around the whole table*, not inside the cells. You must set padding on the `fo:table-cell` (or via an attribute set).
* **`border`**: Setting a border on `fo:table` puts a box around the outside. To get a grid, you must define borders on the cells.
* **`display-align`**: (Vertical alignment) This must be set on the `fo:table-cell` or `fo:table-row`.

---

### 3. The Modern Styling Pattern

The most robust way to style a table is a "Hybrid" approach using the **Attribute Sets** we discussed earlier.

```xml
<fo:table xsl:use-attribute-sets="attr.type.body" 
          table-layout="fixed" 
          width="100%" 
          space-before="5mm">
  
  <fo:table-column column-width="50%"/>
  <fo:table-column column-width="50%"/>

  <fo:table-body>
    <fo:table-row>
      <fo:table-cell xsl:use-attribute-sets="attr.table.cell.base">
        <fo:block>This inherits the font from the table.</fo:block>
      </fo:table-cell>
      
      <fo:table-cell xsl:use-attribute-sets="attr.table.cell.base">
        <fo:block>But gets its padding/border from the cell set.</fo:block>
      </fo:table-cell>
    </fo:table-row>
  </fo:table-body>
</fo:table>

```

### 4. Pro-Tip: The "Row" Override

If you have a table where 90% of the text is left-aligned, but one specific row (like a "Total" row) should be bold and right-aligned, you can put those properties on that specific `fo:table-row`. The cells in that row will prioritize the row's settings over the table's settings.

## Border Collapse

Border collapse as follows **`border-collapse="collapse"`** is the other "must-have" property for tables to look right!

In the world of XSL-FO tables, `border-collapse` is the difference between a professional, crisp grid and a messy, "doubled-up" look. It determines how the borders of adjacent cells (and the table itself) interact.

There are two primary models: **separate** and **collapse**.

---

### 1. `border-collapse="separate"` (The Default)

In this model, every cell has its own distinct borders. If Cell A has a 1pt border and Cell B has a 1pt border, you will see a **2pt gap** (or a 2pt thick line) between them.

* **The Look:** It often looks like a "nested box" effect.
* **The Issue:** It makes it very difficult to get clean, thin lines. If you try to give every cell a `border="1pt solid black"`, the interior lines will look twice as thick as the outer edges.
* **Use Case:** Almost never used in modern documents, except for specific "button" or "raised" cell effects.

### 2. `border-collapse="collapse"` (The Best Practice)

This model tells the engine: "If two cells share a boundary, merge their borders into a single line."

* **The Look:** A clean, single-line grid (like a standard Excel sheet).
* **The Advantage:** If Cell A and Cell B both define a 1pt border, the engine only draws **one** 1pt line between them.
* **Precision:** It simplifies your "Shim" logic significantly because you don't have to worry about "double borders" bloating the width of your table.

---

### 3. How to implement it

You apply this attribute to the `fo:table` element.

```xml
<fo:table table-layout="fixed" 
          width="100%" 
          border-collapse="collapse" 
          border="1pt solid black">
  </fo:table>

```

### 4. The "Border Conflict" Rules

When you use `collapse`, the engine has to decide which border "wins" if they are different. The spec follows these general rules:

1. **Thickness wins:** A `2pt` border will override a `1pt` border.
2. **Style wins:** A `solid` line usually beats a `dotted` line.
3. **Hidden wins:** If any element says `border-style="hidden"`, the border disappears entirely, even if another element wants a border there.

### 5. Interaction with `id` and Shims

Since you are building a tool to identify and fix layout issues, `border-collapse` is a common culprit for "overflow" errors.

* **In "Separate" mode**, the table's total width is: `sum(columns) + all border widths + border-separation`.
* **In "Collapse" mode**, the table's total width is simply: `sum(columns) + (outer left border / 2) + (outer right border / 2)`.

**Shim Tip:** If you see a table that is slightly too wide for the page (causing a "pushed to next page" error), check if it's using `border-collapse="separate"`. Changing it to `collapse` often gains you just enough millimeters to fix the layout without changing the font size.

---

### Summary Table for your Tool logic:

| Property | `separate` (Default) | `collapse` (Modern) |
| --- | --- | --- |
| **Grid Look** | Double lines / Gaps | Single, clean lines |
| **Precision** | Hard to predict total width | Very predictable |
| **Complexity** | High (must manage gaps) | Low (merge logic is automatic) |
| **Recommended?** | No | **Yes** |

That 30-50% "blank/default" gap is a classic XSL-FO technical debt trap. If you don't specify it, the engine defaults to `separate`, and suddenly your developers are fighting "thick lines" by setting `border-top` on some cells and `border-bottom` on others to avoid doubling up—a recipe for a maintenance nightmare.

Since you are building a tool to "shim" and preview these files, here is how you can handle that specific discovery.

### 1. The Preprocessor "Auto-Fix"

If your goal is to modernize the look without touching every file, you can have your preprocessor check for the absence of that attribute and inject it.

```xml
<xsl:template match="fo:table[not(@border-collapse)]">
  <xsl:copy>
    <xsl:attribute name="border-collapse">collapse</xsl:attribute>
    <xsl:apply-templates select="@*|node()"/>
  </xsl:copy>
</xsl:template>

```

### 2. What happens when you switch to `collapse`?

When you force a table from the default (`separate`) to `collapse`, you might see two immediate visual changes in your previewer:

* **The "Shifting" Table:** In `separate` mode, there is often a default `border-separation` (usually about `0.5pt` to `2pt`). When you collapse the borders, that gap vanishes. The table might look slightly narrower, which is usually a win.
* **Border Precision:** If a developer tried to "fix" double borders by only putting a border on the left and top of every cell, the table might suddenly look like it's missing its right and bottom outer edges in `collapse` mode.

### 3. Identifying "Problem" Tables via IDs

Since your tool identifies elements by `id`, you can create a "Table Audit" log. Your tool could flag any `fo:table` that lacks a `border-collapse` definition.

> **Audit Entry:** `Table [ID: shim_d1e99] is using default (separate) borders. This may cause alignment discrepancies in high-precision layouts.`

### 4. A Note on "Border-Separation"

If you find a table that *intended* to have gaps between cells (like a grid of polaroid photos or a fancy dashboard), you cannot use `collapse`. In that rare case, you must use `separate` and then use the `border-separation` attribute:

```xml
<fo:table border-collapse="separate" border-separation="2mm">
   </fo:table>

```

To ensure table headers repeat on every new page, the secret isn't just a property—it’s the **XML structure**. XSL-FO engines are hard-coded to look for a specific "container" to decide what gets repeated.

### 1. The Proper Structure

You must use the `fo:table-header` element. If you simply put your header text in the first `fo:table-row` of the `fo:table-body`, the engine treats it as regular data and it will disappear after the first page.

```xml
<fo:table table-layout="fixed" width="100%" border-collapse="collapse">
  
  <fo:table-column column-width="50%"/>
  <fo:table-column column-width="50%"/>

  <fo:table-header>
    <fo:table-row font-weight="bold" background-color="#EEEEEE">
      <fo:table-cell border="1pt solid black" padding="2mm">
        <fo:block>Product</fo:block>
      </fo:table-cell>
      <fo:table-cell border="1pt solid black" padding="2mm">
        <fo:block>Status</fo:block>
      </fo:table-cell>
    </fo:table-row>
  </fo:table-header>

  <fo:table-body>
    </fo:table-body>
</fo:table>

```

---

### 2. The "Must-Have" Attributes

While the structure above works 90% of the time, there are two attributes that control the "nuances" of repeating headers:

| Attribute | Where it goes | What it does |
| --- | --- | --- |
| **`table-omit-header-at-break`** | `fo:table` | **Default is "false".** This means "Repeat the header." If you set this to "true," the header will *only* appear on the first page. |
| **`table-omit-footer-at-break`** | `fo:table` | **Default is "false".** Same as above, but for `fo:table-footer`. Great for "Continued on next page" notes. |

---

### 3. Common Troubleshooting (For your Tool)

If you have a `fo:table-header` but it is **not** repeating, your tool should check for these three "Gotchas":

1. **The "Omit" Attribute:** Check if someone explicitly set `table-omit-header-at-break="true"`.
2. **The "Body" Overflow:** If a single table row is so tall that it takes up the whole page, the engine might struggle to find room for the header. (Use `keep-together.within-page="always"` on rows to prevent awkward splits).
3. **Nested Tables:** If a table is inside another table, the "inner" table header will only repeat if the "outer" cell allows it to break. This is a common source of layout "shims" in legacy documents.

### 4. Documentation "Pro-Tip": The Footer Strategy

A very professional pattern for long tables is to use the `fo:table-footer` to show a "Continued..." message.

> **Crucial Rule:** In XSL-FO 1.0, the `fo:table-footer` **must** appear *before* the `fo:table-body` in your code, even though it renders at the bottom.

```xml
<fo:table>
  <fo:table-column .../>
  <fo:table-header> ... </fo:table-header>
  
  <fo:table-footer>
    <fo:table-row>
      <fo:table-cell number-columns-spanned="2">
        <fo:block font-style="italic" font-size="8pt">Continued on next page...</fo:block>
      </fo:table-cell>
    </fo:table-row>
  </fo:table-footer>

  <fo:table-body> ... </fo:table-body>
</fo:table>

```

This is a great way to codify these "rules of the road." Here is a consolidated checklist based on our discussion, organized by functional area.

### 1. General & Global Styling

* [ ] **Define Fonts on the Sequence:** Set global `font-family` and `font-size` on the `fo:page-sequence` to ensure a consistent cascade.
* [ ] **Use Attribute Sets for Styles:** Treat `xsl:attribute-set` like CSS classes for reusable properties (colors, borders, padding).
* [ ] **Use Named Templates for Components:** Treat templates like UI components for complex FO structures (like table cells with logic).
* [ ] **Prioritize Absolute Units:** Use `pt` (typography/nudge) or `mm` (layout) for predictable PDF output.
* [ ] **Avoid `px`:** Pixels are DPI-dependent and unpredictable in print-first FO engines.
* [ ] **Use `id` for Targeting:** Ensure every structural element has a unique `id` for internal links, TOCs, and preprocessor "shim" targeting.

### 2. Table Best Practices

* [ ] **Always Set `table-layout="fixed"`:** For performance and layout predictability, always define your column widths.
* [ ] **Set `border-collapse="collapse"`:** Avoid the "double border" look and layout math headaches of the default `separate` model.
* [ ] **Inherit Text Styles:** Set font, color, and alignment at the `fo:table` level to keep child code clean.
* [ ] **Repeat Headers Properly:** Use the `fo:table-header` element for any row that must reappear on new pages.
* [ ] **Define Borders on the Cell:** Layout and border properties do **not** inherit; apply them directly to the `fo:table-cell` via attribute sets.
* [ ] **Use `display-align` for Vertical Centering:** Set this on the cell, not the block inside it.

### 3. Images & Icons

* [ ] **Prefer SVG:** For logos and icons, use SVGs to ensure infinite scalability and smaller file sizes.
* [ ] **Control Aspect Ratio:** Use `content-width="scale-to-fit"` and `scaling="uniform"` to prevent "funhouse mirror" stretching.
* [ ] **Define Width, not Height:** Set a fixed `width` and let the height be `auto` to maintain proportions.
* [ ] **Nudge with `baseline-shift`:** Use small `pt` values (e.g., `-1.5pt`) to optically align icons with text baselines.

### 4. Accessibility & Layout

* [ ] **Avoid Layout Tables:** Use `fo:block` and `fo:inline` for legends and side-by-side content to stay screen-reader friendly.
* [ ] **Use `alignment-baseline="middle"`:** Use this on inline elements to get them 95% of the way to vertical center.
* [ ] **Protect Wraps:** Use `keep-together.within-line="always"` on inline icon/label pairs so they don't break across lines.
* [ ] **Make Links Interactive:** Wrap TOC entries and cross-references in `fo:basic-link` with `internal-destination`.

#### Keeps and Breaks 

* [ ] **Headings:** Apply `keep-with-next.within-page="always"`.
* [ ] **Table Rows:** Apply `keep-together.within-page="always"` to prevent split text.
* [ ] **Global Flow:** Set `widows="2"` and `orphans="2"` on the page sequence.
* [ ] **Images:** Wrap `fo:external-graphic` in a `fo:block` with `keep-together.within-page="always"` so the image and its caption don't separate.

### 5. Performance & Maintenance

* [ ] **Break Up Page Sequences:** Use separate `fo:page-sequence` elements for different chapters/sections to manage memory.
* [ ] **Avoid Deep Nesting:** Keep the FO hierarchy as flat as possible (avoid unnecessary `fo:block` wrappers).
* [ ] **Identity Transform for Pre-processing:** Use an identity transform to inject synthetic IDs into "anonymous" blocks for easier debugging.

### Keeps and Breaks 
In the XSL-FO world, **"Keeps and Breaks"** are the tools that separate amateur PDFs from professional ones. Without them, you end up with "widows and orphans"—headings at the very bottom of a page with no content below them, or tables that split right after the header.

Here is the breakdown of the "Keep" logic you’ll want for your documentation and your tool’s shim-logic.

---

### 1. The Three "Keep" Attributes

There are three main properties, and they each serve a very specific structural purpose:

| Property | Typical Value | Purpose |
| --- | --- | --- |
| **`keep-with-next`** | `always` | Used on **Headings**. Ensures the heading never sits alone at the bottom of a page. |
| **`keep-with-previous`** | `always` | Used on **Table Footers** or "Total" rows. Ensures the conclusion stays with the data. |
| **`keep-together`** | `within-page="always"` | Used on **Table Rows** or small **Images**. Ensures the entire element stays on one page. |

---

### 2. The "Sticky Heading" Pattern (Best Practice)

A common mistake is trying to "force" a page break. Instead, you should make your headings "sticky" so they follow the content below them.

```xml
<fo:block font-weight="bold" 
          font-size="14pt" 
          keep-with-next.within-page="always" 
          space-after="2mm">
  Chapter 1: The Introduction
</fo:block>
<fo:block>
  This paragraph will now always stay with the heading above. 
  If the paragraph has to move to page 2, the heading will jump with it.
</fo:block>

```

---

### 3. Keeping Table Rows Together

By default, an FO engine will try to split a table row if it contains a lot of text. This often looks broken. To prevent a row from splitting across two pages:

```xml
<fo:table-row keep-together.within-page="always">
  <fo:table-cell>
    <fo:block>This row will never be sliced in half.</fo:block>
  </fo:table-cell>
</fo:table-row>

```

**Warning for your Tool:** If a single row is taller than an *entire page*, and you have `keep-together="always"`, the engine will often crash or "overflow" the page because it has been given an impossible instruction.

---

### 4. The "Orphan" and "Widow" Control

If you are dealing with long paragraphs, you don't want a single line of a paragraph left alone on a page.

* **`orphans="2"`**: The minimum number of lines left at the **bottom** of a page.
* **`widows="2"`**: The minimum number of lines left at the **top** of the next page.

*Set these globally on your `fo:page-sequence` or `fo:root` so they apply to all blocks.*

---

### 5. Troubleshooting / Shim Strategy

When you're building your visual previewer, "Keep" conflicts are a major source of "White Space Gaps."

* **The "Keep Chain" Conflict:** If you have ten blocks in a row all set to `keep-with-next="always"`, and they collectively exceed one page, the engine gets "stuck." It wants to keep them together, but it can't.
* **The "Broken Table" Shim:** If your tool detects a table header at the bottom of a page with no data, your shim should ensure the `fo:table-header` (or the first row of the body) hasn't lost its `keep-with-next` logic.

Cell spanning is the most common cause of table "blow-ups" in XSL-FO. If the math doesn't perfectly match the defined columns, the engine will either throw an error or—worse—render a garbled mess that pushes content off the edge of the page.

Here are the best practices for handling horizontal (column) and vertical (row) spanning.

---

### 1. Horizontal Spanning (`number-columns-spanned`)

This is the "merging" of columns. The most critical rule: **You must still account for the total column count.**

* **The Rule:** If you have 4 columns defined, and a cell spans 3, the next cell in that row must be the 4th column.
* **The Attribute:** `number-columns-spanned="[integer]"`

```xml
<fo:table table-layout="fixed" width="100%">
  <fo:table-column column-width="25%"/> <fo:table-column column-width="25%"/> <fo:table-column column-width="25%"/> <fo:table-column column-width="25%"/> <fo:table-body>
    <fo:table-row>
      <fo:table-cell number-columns-spanned="3" border="1pt solid black">
        <fo:block>I am taking up 75% of the table width</fo:block>
      </fo:table-cell>
      <fo:table-cell border="1pt solid black">
        <fo:block>Col 4</fo:block>
      </fo:table-cell>
    </fo:table-row>
  </fo:table-body>
</fo:table>

```

### 2. Vertical Spanning (`number-rows-spanned`)

This is significantly trickier because it affects the rows *below* the current one.

* **The Rule:** In the subsequent rows, you must **omit** the cell that the span is "dropping into."
* **The Attribute:** `number-rows-spanned="[integer]"`

```xml
<fo:table-body>
  <fo:table-row>
    <fo:table-cell number-rows-spanned="2" border="1pt solid black">
      <fo:block>I span this row and the next.</fo:block>
    </fo:table-cell>
    <fo:table-cell border="1pt solid black">
      <fo:block>Row 1, Col 2</fo:block>
    </fo:table-cell>
  </fo:table-row>
  
  <fo:table-row>
    <fo:table-cell border="1pt solid black">
      <fo:block>Row 2, Col 2</fo:block>
    </fo:table-cell>
  </fo:table-row>
</fo:table-body>

```

---

### 3. Common Troubleshooting (For your Tool/Shims)

* **The "Phantom Column" Bug:** If your code defines a span of 3 but still provides 4 cells in that row, the engine will create a "phantom" 5th column that overflows the page margin.
* **The "Border Gap" Bug:** In `border-collapse="separate"` mode, spanned cells can create weirdly thick borders or gaps. **Always use `border-collapse="collapse"` when spanning** to ensure the merged area looks like a single unit.
* **The Page Break Disaster:** If a cell spans 5 rows, but the page breaks after row 2, some older FO engines will duplicate the cell content or simply "clip" it.
* **Shim Fix:** For high-reliability layouts, use `keep-together.within-page="always"` on any row containing a `number-rows-spanned` attribute to ensure the entire spanned group stays on one page.

---

### 4. Cell Spanning Checklist

* [ ] **Math Check:** Ensure `number-columns-spanned` + remaining cells = total `fo:table-column` count.
* [ ] **Structural Omission:** In vertical spans, ensure the following rows "skip" the occupied column.
* [ ] **Collapse Borders:** Always use `border-collapse="collapse"` to keep spanned boundaries clean.
* [ ] **Alignment:** Use `display-align="center"` on spanned cells; large empty spaces in merged cells look better when centered vertically.
* [ ] **Page Breaks:** Use `keep-together` on groups of rows that share a vertical span to prevent "fragmented" merges across pages.

---

### Closing the Loop

You’ve now got a robust set of "Modern XSL-FO" principles—from accessible legends and micro-adjusted icons to bulletproof table logic.

In the world of XSL-FO, `inline-progression-dimension` (IPD) is essentially a fancy, internationalized way of saying **"Width"**—but with a catch.

Because XSL-FO was designed to handle languages that read horizontally (English) and vertically (traditional Chinese or Japanese), it doesn't use fixed terms like "width" or "height." Instead, it uses **"inline-progression"** (the direction text flows in a line) and **"block-progression"** (the direction blocks stack).

### 1. What it actually does

For standard English documents (left-to-right), `inline-progression-dimension` controls the **horizontal width** of an area.

Its intended usage is to define the boundaries of an object within a line. You will most commonly (and legitimately) see it used on:

* **`fo:external-graphic`**: To set the width of an image.
* **`fo:instream-foreign-object`**: To set the width of an SVG.
* **`fo:table`**: To define the total width of a table.
* **`fo:table-column`**: To set specific column widths.
* **`fo:block-container`**: To create a "box" with a specific width that is different from the parent.

---

### 2. Is it necessary? (The "Potpourri" Check)

In many legacy templates, `inline-progression-dimension` is used where simpler attributes or automatic behavior would suffice. Here is how to spot the "junk" usage:

#### Use `width` instead?

In almost all modern XSL-FO processors (like Apache FOP or Antenna House), `width` is a shorthand for `inline-progression-dimension`. If your templates are strictly English/horizontal, using `width="100px"` is cleaner and easier to read than the long-form IPD version.

#### The "Over-Specified" Block

If you see this on a standard `fo:block`:

```xml
<fo:block inline-progression-dimension="100%"> ... </fo:block>

```

**It is likely unnecessary.** Blocks naturally expand to fill the available width of their parent container. Explicitly setting it to 100% is redundant.

#### The "Misplaced" Property

If you see it on an `fo:inline` element, it's often ignored. Inline elements are supposed to shrink-wrap their content. Setting a width on them usually requires the element to be "stretched," which many processors don't support for simple inline text.

---

### 3. The "Correct" Syntax

If you do use it, it’s actually a "compound" property. While most people just use the shortcut value, the full specification allows for:

* **`.minimum`**
* **`.optimum`** (The one usually set when you provide a single value)
* **`.maximum`**

**Example of "Professional" Usage:**
To make a table flexible but capped:

```xml
<fo:table inline-progression-dimension.optimum="100%" 
          inline-progression-dimension.maximum="500pt">

```

---

### Red Flag Summary

If you are cleaning up legacy code, look for these signs of misuse:

| Scenario | Verdict | Fix |
| --- | --- | --- |
| On a standard `fo:block` | **Likely Redundant** | Remove it; let the block auto-fill. |
| On `fo:inline` | **Often Ignored** | Use a `fo:block-container` if you need a fixed-width box. |
| Set to `auto` | **Useless** | `auto` is the default value; delete it. |
| Hardcoded `px` or `pt` | **Rigid** | Consider if `100%` or `proportional-column-width()` is better for responsive layouts. |

> [TIP]
> If your templates are "English-only," you can safely replace `inline-progression-dimension` with `width` to make your code 50% more readable for the next person.

The short answer is: **Yes, but with a technical caveat.**

In the XSL-FO specification, `width` is defined as a **shorthand** for the `inline-progression-dimension` property. Because of this relationship, they share the same underlying structure, including the "compound" components.

### 1. The Syntax

When you use the shorthand `width`, you are typically setting the `.optimum` value by default. However, you can access the sub-components using the dot notation just like you would with the long-form version:

* **`width.minimum`**
* **`width.optimum`**
* **`width.maximum`**

**Example of valid usage:**

```xml
<fo:table width.minimum="50%" width.optimum="100%" width.maximum="10in">

```

---

### 2. How Processors Treat Them

While the spec says they are interchangeable, here is how it works "under the hood" in most engines (like Apache FOP, Antenna House, or RenderX):

| Property Used | What Happens |
| --- | --- |
| `width="100pt"` | The processor sets `inline-progression-dimension.optimum` to `100pt`. |
| `width.maximum="500pt"` | The processor sets `inline-progression-dimension.maximum` to `500pt`. |
| **Mixing both** | If you set `width="200pt"` and `inline-progression-dimension.maximum="500pt"`, the last one parsed usually wins or they merge, depending on the processor's strictness. |

---

### 3. The "Gotcha": Why stick to one?

The biggest risk with "Potpourri" legacy code is **Property Overlap**.

If a legacy template has:

```xml
<fo:table width="100%" inline-progression-dimension="auto">

```

The processor might get confused. In XSL-FO, **shorthands (like `width`) generally have lower priority than the specific property (`inline-progression-dimension`)**. If both are present, the long-form version will likely override the shorthand.

---

### Summary Recommendation

If you are cleaning up your templates for readability:

1. **Standardize on `width**`: It is much easier for humans to read.
2. **Use Dot Notation**: If you need range control, use `width.minimum` and `width.maximum`.
3. **Delete the Duplicates**: If a tag has both `width` and `inline-progression-dimension`, delete the latter (unless you are doing complex right-to-left/vertical writing layout).

> [!NOTE]
> **Check your Engine:** While `width.maximum` is valid XSL-FO, some very old or basic processors only recognize the simple `width="val"` string. If you use dot notation and it fails to render, your processor might only support the shorthand as a single literal value.

Would you like me to show you how to use these "min/max" values to create a table that shrinks or grows based on its content?

To create a table that dynamically shrinks or grows, we leverage the `.minimum`, `.optimum`, and `.maximum` components of the `width` (or `inline-progression-dimension`) property.

In XSL-FO, this is often referred to as **"Elastic Layout."** ### The Dynamic Table Strategy
To make a table respond to its content, you define a range rather than a fixed number. The processor will try to hit the `optimum`, but will "squeeze" down to the `minimum` if space is tight, or "stretch" up to the `maximum` if the content demands it.

```xml
<fo:table width.minimum="50%" 
          width.optimum="100%" 
          width.maximum="100%" 
          table-layout="auto"
          border="1pt solid black">

    <fo:table-column column-width="1in"/>
    <fo:table-column column-width="proportional-column-width(1)"/>

    <fo:table-body>
        <fo:table-row>
            <fo:table-cell>
                <fo:block>ID: 001</fo:block>
            </fo:table-cell>
            <fo:table-cell>
                <fo:block> This cell will expand the table width up to the maximum. </fo:block>
            </fo:table-cell>
        </fo:table-row>
    </fo:table-body>
</fo:table>

```

---

### Key Attributes for "Elasticity"

#### 1. `table-layout="auto"` vs `"fixed"`

This is the most critical setting.

* **`fixed` (Default in many FO engines):** The processor ignores the content and only looks at the defined column widths.
* **`auto`:** The processor looks at the actual text inside the cells before deciding how wide the columns (and the table) should be.

#### 2. The Width Range

* **`width.minimum="50%"`**: Tells the engine, "No matter how little text is inside, don't make the table skinnier than half the page."
* **`width.optimum="100%"`**: Tells the engine, "Ideally, I'd like this table to fill the whole page width."
* **`width.maximum="100%"`**: This is your "safety rail." It prevents the table from bleeding off the right side of the page if the content is extremely long.

#### 3. `proportional-column-width(1)`

When used in conjunction with a dynamic table width, this function tells the column to "grab all the leftover space." If you have one column at `1in` and another at `proportional-column-width(1)`, the second column will grow and shrink as the table width fluctuates between your minimum and maximum.

---

### A Common "Legacy" Trap

In your "potpourri" of templates, you might see this:
`width="100%"` combined with `inline-progression-dimension.maximum="100%"`.

**Why this is redundant:** Setting `width="100%"` is shorthand for setting the **optimum** to 100%. If the maximum is also 100%, the table has no "room" to be anything other than exactly 100%.

> [!TIP]
> **The Modern Cleanup:** If you want a table that always fills the page, just use `width="100%"`. If you want it to be "as small as possible but no bigger than the page," use `width.optimum="0pt" width.maximum="100%" table-layout="auto"`.

Would you like me to explain how `proportional-column-width` calculates those shares if you have multiple flexible columns?

Think of `proportional-column-width()` as a **pie chart** for your table. It’s the XSL-FO equivalent of the `fr` unit in CSS Grid or "weighted" constraints in other layout engines.

It doesn’t set a fixed size (like inches or points). Instead, it says, "After we subtract the fixed-width columns, divide the leftover space into shares."

### 1. How the "Shares" Work

The number inside the parentheses is the number of **shares** that column gets.

| Column | Code | Result |
| --- | --- | --- |
| **Column A** | `column-width="1in"` | Always **1 inch** (Fixed). |
| **Column B** | `column-width="proportional-column-width(1)"` | Gets **1 share** of what's left. |
| **Column C** | `column-width="proportional-column-width(2)"` | Gets **2 shares** of what's left (Twice as much as B). |

---

### 2. The Step-by-Step Calculation

If you have a **6-inch wide table** with the columns defined above:

1. **Subtract Fixed Widths:** The engine takes the total table width () and subtracts the fixed columns ().

```plain
Leftover space = 5 inches.
```

2. **Add Up the Shares:** It looks at the proportional columns. .

3. **Divide the Space:** It divides the  of leftover space by the  total shares.

```
Value of 1 share = 1.66 inches.
```

4. **Assign the Widths:**

```plain
Column B** () = **1.66"
Column C** () = **3.33"
```

---

### 3. The "Legacy" Red Flag: Using it with `width="auto"`

This is where many legacy templates break.

> [!CAUTION]
> **The Golden Rule:** You **cannot** use `proportional-column-width()` if the table doesn't have a defined total width (like `100%` or `7in`).

If your table width is set to `auto` (meaning "shrink-wrap the content"), the engine doesn't know how much "leftover space" there is to divide. In this scenario, most processors will either:

* Default the proportional columns to **0px** (hiding your text).
* Throw a **Validation Error**.
* Treat the share as a tiny, arbitrary value.

### 4. Best Practice Example

If you want a three-column table where the middle column is twice as wide as the outer two, and the whole thing fills the page:

```xml
<fo:table width="100%" table-layout="fixed">
  <fo:table-column column-width="proportional-column-width(1)"/>
  <fo:table-column column-width="proportional-column-width(2)"/>
  <fo:table-column column-width="proportional-column-width(1)"/>
  
  <fo:table-body>
    <fo:table-row>
      <fo:table-cell border="solid 1pt black"><fo:block>25%</fo:block></fo:table-cell>
      <fo:table-cell border="solid 1pt black"><fo:block>50%</fo:block></fo:table-cell>
      <fo:table-cell border="solid 1pt black"><fo:block>25%</fo:block></fo:table-cell>
    </fo:table-row>
  </fo:table-body>
</fo:table>

```

---

### Summary for your Cleanup

* **Check for `table-layout="fixed"`:** Proportional widths work best (and are most predictable) when the layout is fixed.
* **Check for Table Width:** Ensure the `fo:table` has an explicit `width` or `inline-progression-dimension`.
* **Simplify the Math:** If you see `proportional-column-width(50)` and `proportional-column-width(50)`, you can simplify that to `(1)` and `(1)`. It’s just a ratio!

Would you like to know how this interacts with `number-columns-spanned` if you have cells crossing over these proportional columns?

You are right to be suspicious—column spanning in legacy XSL-FO templates is often where the "math" of the layout finally breaks. When you combine `number-columns-spanned` with `proportional-column-width()`, things get interesting (and potentially messy).

Here is the rule of thumb: **Spanning doesn't change the column width; it just changes how many "buckets" a cell can sit in.**

---

### 1. How the Spanning Math Works

If you have three columns defined with proportional shares, the engine calculates their widths **first**, before it ever looks at the cells.

* **Column 1:** `proportional-column-width(1)` (e.g., 100pt)
* **Column 2:** `proportional-column-width(2)` (e.g., 200pt)
* **Column 3:** `proportional-column-width(1)` (e.g., 100pt)

If a cell has `number-columns-spanned="2"`, it simply takes the sum of the columns it covers. If it starts in Column 1, it will be **300pt wide** ().

### 2. The "Ghost Column" Trap

In legacy templates, you might find a row that looks like this:

```xml
<fo:table-row>
  <fo:table-cell number-columns-spanned="3">
    <fo:block>Header Text</fo:block>
  </fo:table-cell>
</fo:table-row>

```

**The Risk:** If someone previously deleted the `<fo:table-column>` definitions but left the `number-columns-spanned="3"`, the XSL-FO processor may fail or default to a single-column layout. **A spanned cell can only span as many columns as are explicitly defined at the top of the table.**

### 3. The "Missing Cell" Logic

In XSL-FO, you don't "skip" cells that are covered by a span. You simply **omit** the `<fo:table-cell>` elements that would have started in those slots.

**Example of a 3-Column Table with a Span:**

```xml
<fo:table-row>
  <fo:table-cell number-columns-spanned="2" border="1pt solid red">
    <fo:block>I take up 3 shares (1+2)</fo:block>
  </fo:table-cell>
  
  <fo:table-cell border="1pt solid blue">
    <fo:block>I take up 1 share</fo:block>
  </fo:table-cell>
</fo:table-row>

```

---

### 4. Legacy "Red Flags" to Look For

When auditing your library, keep an eye out for these two specific headaches:

#### A. Spanning with `table-layout="auto"`

If the table is `auto` (shrink-to-fit) and you have a spanned cell with a huge amount of text, the processor has to decide: *"Do I make Column 1 wider, or Column 2 wider?"*

* **The Result:** Different processors (FOP vs. Antenna House) handle this differently. It often leads to unpredictable column "jittering."
* **The Fix:** If you must span, try to keep `table-layout="fixed"` and use `proportional-column-width`.

#### B. The "Over-Span"

If a cell has `number-columns-spanned="5"` but there are only 4 `<fo:table-column>` elements:

* **The Result:** Most modern processors will truncate the span to 4 and move on, but some will throw a fatal error.
* **The Fix:** Always ensure your `table-column` count matches or exceeds your highest span.

---

### Summary Checklist for your Audit

1. **Count your `<fo:table-column>` tags.** Do they match the logic of the rows?
2. **Check for "Lone Spans."** Is there a `number-columns-spanned` on a cell where there is only one column defined? (Delete it; it’s doing nothing).
3. **Check for `from-table-column()` functions.** Sometimes legacy code uses this to pull properties (like background color) from the column definition into the spanned cell.

Do you have any tables that use `number-rows-spanned` (vertical spans)? Those are a whole different beast because they can break page-splitting logic.
