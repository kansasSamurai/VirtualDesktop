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

