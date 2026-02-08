<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format">

<!--
    XSL Viewer Feature Test File
    ============================
    This file exercises supported features for visual regression testing.
    Load into XSLT tab and click "Render Preview & Save" to verify rendering.

    Expected result: 3 pages displayed vertically with gap between them.
    - Page 1: Sections 1-9 (fonts, inheritance, tables, leaders, etc.)
    - Page 2: Sections 10-14 (inline legend, shims, inline-containers, nested templates, java maps)
    - Page 3: Summary

    REGRESSION TESTS:
    - Template in static-content with XPath expressions (fixes doc.evaluate error)
    - Nested templates (template calling template)
    - Whitespace between inline-containers
    - XML comments not creating empty divs
    - Java Map with multiple map entries (dot notation and map:get syntax)

    SAMPLE XML DATA for testing (paste into XML DATA tab):
    <report>
        <title>Test Report</title>
        <logoUrl>test-image.png</logoUrl>
    </report>

    SAMPLE JAVA MAP for testing (paste into JAVA MAP tab):
    {
      "firstMap": {"reportTitle": "QUARTERLY AUDIT REPORT"},
      "secondMap": {"auditorName": "John Doe"}
    }
-->

<xsl:template match="/">
    <fo:root>
        <fo:layout-master-set>
            <fo:simple-page-master master-name="test-page" page-width="8.5in" page-height="11in">
                <fo:region-body margin="1in"/>
            </fo:simple-page-master>
        </fo:layout-master-set>

        <fo:page-sequence master-reference="test-page"
                          font-family="Arial, sans-serif"
                          font-size="10pt"
                          color="#333333">
            <!-- Header (region-before) -->
            <!-- REGRESSION TEST: Template called from static-content with XPath expressions -->
            <fo:static-content flow-name="xsl-region-before">
                <xsl:call-template name="page.header"/>
            </fo:static-content>

            <!-- Footer (region-after) with Page X of N -->
            <fo:static-content flow-name="xsl-region-after">
                <fo:block text-align="center" font-size="9pt" color="#666">
                    Page <fo:page-number/> of <fo:page-number-citation-last ref-id="end-of-document"/>
                </fo:block>
            </fo:static-content>

            <fo:flow flow-name="xsl-region-body">

                <!-- ============================================ -->
                <!-- SECTION 1: Font Properties -->
                <!-- ============================================ -->
                <fo:block font-size="16pt" font-weight="bold" space-after="12pt">
                    1. Font Properties
                </fo:block>

                <fo:block space-after="6pt">
                    Default font - inherits Arial 10pt #333 from page-sequence
                </fo:block>

                <fo:block font-family="Georgia, serif" font-size="10pt" space-after="6pt">
                    Georgia serif font
                </fo:block>

                <fo:block font-family="Courier New, monospace" font-size="10pt" space-after="6pt">
                    Courier New monospace font
                </fo:block>

                <fo:block font-family="Arial, sans-serif" font-size="10pt" font-weight="bold" color="navy" space-after="12pt">
                    Arial bold navy text
                </fo:block>

                <!-- ============================================ -->
                <!-- SECTION 2: Font Inheritance -->
                <!-- ============================================ -->
                <fo:block font-size="16pt" font-weight="bold" space-after="12pt">
                    2. Font Inheritance Test
                </fo:block>

                <fo:block font-family="Georgia, serif" font-size="11pt" color="darkgreen" space-after="12pt">
                    Parent block with Georgia font and green color.
                    <fo:block space-before="6pt">
                        Nested child block - should inherit Georgia and green.
                        <fo:block space-before="6pt">
                            Deeply nested block - still Georgia and green.
                        </fo:block>
                    </fo:block>
                    <fo:inline font-weight="bold"> Inline child - bold Georgia green.</fo:inline>
                </fo:block>

                <!-- ============================================ -->
                <!-- SECTION 3: Tables -->
                <!-- ============================================ -->
                <fo:block font-size="16pt" font-weight="bold" space-after="12pt">
                    3. Tables with Spanning
                </fo:block>

                <fo:block space-after="8pt" font-size="9pt" color="gray">
                    XSL comments should not render as empty div elements. There is a comment
                    inside this table's first data row and it should not interfere with table rendering.
                </fo:block>

                <fo:table border="1pt solid black" width="100%" space-after="12pt">
                    <fo:table-column column-width="25%"/>
                    <fo:table-column column-width="25%"/>
                    <fo:table-column column-width="25%"/>
                    <fo:table-column column-width="25%"/>
                    <fo:table-body>
                        <fo:table-row>
                            <fo:table-cell border="1pt solid gray" padding="4pt" number-columns-spanned="4">
                                <fo:block font-weight="bold" text-align="center">Header spanning 4 columns</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <!-- This comment is inside a table-row to verify comments don't create empty divs -->
                        <fo:table-row>
                            <fo:table-cell border="1pt solid gray" padding="4pt" number-rows-spanned="2">
                                <fo:block>Row span 2</fo:block>
                            </fo:table-cell>
                            <fo:table-cell border="1pt solid gray" padding="4pt">
                                <fo:block>Cell A</fo:block>
                            </fo:table-cell>
                            <fo:table-cell border="1pt solid gray" padding="4pt">
                                <fo:block>Cell B</fo:block>
                            </fo:table-cell>
                            <fo:table-cell border="1pt solid gray" padding="4pt">
                                <fo:block>Cell C</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <fo:table-row>
                            <fo:table-cell border="1pt solid gray" padding="4pt" number-columns-spanned="2">
                                <fo:block text-align="center">Colspan 2</fo:block>
                            </fo:table-cell>
                            <fo:table-cell border="1pt solid gray" padding="4pt">
                                <fo:block>Cell D</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </fo:table-body>
                </fo:table>

                <!-- ============================================ -->
                <!-- SECTION 4: Leaders -->
                <!-- ============================================ -->
                <fo:block font-size="16pt" font-weight="bold" space-after="12pt">
                    4. Leaders
                </fo:block>

                <fo:block space-after="6pt">
                    Chapter 1<fo:leader leader-pattern="dots"/>Page 1
                </fo:block>
                <fo:block space-after="6pt">
                    Chapter 2<fo:leader leader-pattern="dots"/>Page 15
                </fo:block>
                <fo:block space-after="12pt">
                    Appendix A<fo:leader leader-pattern="dots"/>Page 42
                </fo:block>

                <!-- ============================================ -->
                <!-- SECTION 5: Page Number -->
                <!-- ============================================ -->
                <fo:block font-size="16pt" font-weight="bold" space-after="12pt">
                    5. Page Number
                </fo:block>

                <fo:block space-after="12pt">
                    Current page: <fo:page-number/> (displays as placeholder)
                </fo:block>

                <!-- ============================================ -->
                <!-- SECTION 6: External Graphic -->
                <!-- ============================================ -->
                <fo:block font-size="16pt" font-weight="bold" space-after="12pt">
                    6. External Graphic
                </fo:block>

                <fo:block space-after="6pt">
                    Image from virtual registry (requires IMAGES tab entry):
                </fo:block>
                <fo:block space-after="12pt">
                    <fo:external-graphic src="test-image.png" content-width="50pt" content-height="50pt"/>
                </fo:block>

                <!-- ============================================ -->
                <!-- SECTION 7: Spacing -->
                <!-- ============================================ -->
                <fo:block font-size="16pt" font-weight="bold" space-after="12pt">
                    7. Spacing (space-before / space-after)
                </fo:block>

                <fo:block background-color="#eeeeee" padding="4pt">
                    Block with no extra spacing
                </fo:block>
                <fo:block background-color="#dddddd" padding="4pt" space-before="20pt">
                    Block with 20pt space-before
                </fo:block>
                <fo:block background-color="#eeeeee" padding="4pt" space-after="20pt">
                    Block with 20pt space-after
                </fo:block>
                <fo:block background-color="#dddddd" padding="4pt">
                    Block with no extra spacing
                </fo:block>

                <!-- ============================================ -->
                <!-- SECTION 8: Text Alignment -->
                <!-- ============================================ -->
                <fo:block font-size="16pt" font-weight="bold" space-before="12pt" space-after="12pt">
                    8. Text Alignment
                </fo:block>

                <fo:block text-align="left" background-color="#f0f0f0" padding="4pt" space-after="4pt">
                    Left aligned text
                </fo:block>
                <fo:block text-align="center" background-color="#e0e0e0" padding="4pt" space-after="4pt">
                    Center aligned text
                </fo:block>
                <fo:block text-align="right" background-color="#f0f0f0" padding="4pt" space-after="4pt">
                    Right aligned text
                </fo:block>

                <!-- ============================================ -->
                <!-- SECTION 9: Page Breaks -->
                <!-- ============================================ -->
                <fo:block font-size="16pt" font-weight="bold" space-before="12pt" space-after="12pt">
                    9. Page Breaks (end of Page 1)
                </fo:block>

                <fo:block space-after="12pt">
                    This content is on the first page. The next block has break-before="page".
                </fo:block>

                <!-- PAGE BREAK HERE -->
                <fo:block break-before="page" font-size="16pt" font-weight="bold" color="darkblue" space-after="12pt">
                    PAGE 2: Inline Legend Pattern
                </fo:block>

                <fo:block space-after="12pt">
                    This demonstrates an inline legend using alignment-baseline for vertical centering.
                    Icons and labels flow like text and wrap naturally.
                </fo:block>

                <!-- ============================================ -->
                <!-- SECTION 10: Inline Legend with alignment-baseline -->
                <!-- ============================================ -->
                <fo:block font-size="14pt" font-weight="bold" space-after="8pt">
                    10. Dynamic Inline Legend
                </fo:block>

                <!-- Legend using the ui.legend.item template pattern -->
                <fo:block space-before="6pt" space-after="12pt" line-height="1.8" background-color="#f9f9f9" padding="8pt">
                    <fo:inline font-weight="bold" padding-right="5pt">Legend:</fo:inline>

                    <!-- Legend Item 1: Approved -->
                    <xsl:call-template name="ui.legend.item">
                        <xsl:with-param name="icon-url">icon-check.png</xsl:with-param>
                        <xsl:with-param name="label">Approved</xsl:with-param>
                    </xsl:call-template>

                    <!-- Legend Item 2: Pending -->
                    <xsl:call-template name="ui.legend.item">
                        <xsl:with-param name="icon-url">icon-clock.png</xsl:with-param>
                        <xsl:with-param name="label">Pending Review</xsl:with-param>
                    </xsl:call-template>

                    <!-- Legend Item 3: Rejected -->
                    <xsl:call-template name="ui.legend.item">
                        <xsl:with-param name="icon-url">icon-x.png</xsl:with-param>
                        <xsl:with-param name="label">Rejected</xsl:with-param>
                    </xsl:call-template>

                    <!-- Legend Item 4: Warning -->
                    <xsl:call-template name="ui.legend.item">
                        <xsl:with-param name="icon-url">icon-warning.png</xsl:with-param>
                        <xsl:with-param name="label">Needs Attention</xsl:with-param>
                    </xsl:call-template>
                </fo:block>

                <fo:block space-after="8pt" font-size="10pt" color="gray">
                    Note: Icons show as "MISSING" placeholders unless you add them to the IMAGES tab.
                    The key feature is that icon-text pairs stay together (keep-together.within-line)
                    while the overall legend wraps naturally like a paragraph.
                </fo:block>

                <!-- Show the alignment-baseline effect more clearly -->
                <fo:block font-size="14pt" font-weight="bold" space-before="16pt" space-after="8pt">
                    Alignment Baseline Comparison
                </fo:block>

                <fo:block space-after="6pt" background-color="#eef">
                    <fo:inline>Default baseline: </fo:inline>
                    <fo:external-graphic src="icon-check.png" width="16pt" content-width="scale-to-fit"/>
                    <fo:inline> text after icon</fo:inline>
                </fo:block>

                <fo:block space-after="6pt" background-color="#efe">
                    <fo:inline>Middle baseline: </fo:inline>
                    <fo:external-graphic src="icon-check.png" width="16pt" content-width="scale-to-fit" alignment-baseline="middle"/>
                    <fo:inline alignment-baseline="middle"> text after icon (both aligned middle)</fo:inline>
                </fo:block>

                <fo:block space-after="12pt" break-after="page">
                    End of Page 2 - Legend demonstration complete.
                </fo:block>

                <!-- PAGE 3 CONTENT -->
                <fo:block font-size="16pt" font-weight="bold" color="darkgreen" space-after="12pt">
                    PAGE 3: After break-after
                </fo:block>

                <fo:block space-after="12pt">
                    This is the final page content. Pagination test complete!
                </fo:block>

                <fo:block font-family="Courier New, monospace" background-color="#f5f5f5" padding="8pt">
                    Summary: You should see 3 separate pages stacked vertically.
                </fo:block>

                <!-- ============================================ -->
                <!-- SECTION 11: Shims Example -->
                <!-- ============================================ -->
                <fo:block font-size="16pt" font-weight="bold" space-before="20pt" space-after="12pt">
                    11. Shims Example
                </fo:block>

                <fo:block space-after="8pt">
                    The element below uses a proprietary tag "fo:custom-highlight" which is not
                    standard XSL-FO. Without a shim, it would not render correctly.
                </fo:block>

                <fo:block space-after="8pt">
                    To test shims, add this line to the SHIMS tab:
                </fo:block>

                <fo:block font-family="Courier New, monospace" background-color="#ffffcc" padding="8pt" space-after="8pt">
                    fo:custom-highlight|fo:inline background-color="#ffff00"
                </fo:block>

                <fo:block space-after="8pt">
                    Then the following proprietary element:
                </fo:block>

                <fo:block space-after="8pt">
                    This text has a <fo:custom-highlight>highlighted phrase</fo:custom-highlight> in the middle.
                </fo:block>

                <fo:block font-size="9pt" color="gray" space-after="12pt">
                    Note: The shim replaces "fo:custom-highlight" with "fo:inline background-color="#ffff00""
                    turning the unsupported element into a standard fo:inline with yellow background.
                </fo:block>

                <!-- ============================================ -->
                <!-- SECTION 12: Inline Containers Side-by-Side -->
                <!-- ============================================ -->
                <fo:block font-size="16pt" font-weight="bold" space-before="20pt" space-after="12pt">
                    12. Inline Containers (50% Width Test)
                </fo:block>

                <fo:block space-after="8pt" font-size="10pt" color="gray">
                    Testing two inline-containers at 50% width. If the second wraps to the next line,
                    possible causes: whitespace between elements, padding/border adding to width, or margins.
                </fo:block>

                <!-- Test A: Basic 50% + 50% -->
                <fo:block font-weight="bold" space-after="4pt">Test A: Basic 50% + 50%</fo:block>
                <fo:block space-after="8pt" background-color="#eee">
                    <fo:inline-container width="50%">
                        <fo:block background-color="#cfc" padding="4pt">Left 50%</fo:block>
                    </fo:inline-container>
                    <fo:inline-container width="50%">
                        <fo:block background-color="#ccf" padding="4pt">Right 50%</fo:block>
                    </fo:inline-container>
                </fo:block>

                <!-- Test B: No whitespace between containers -->
                <fo:block font-weight="bold" space-after="4pt">Test B: No whitespace between (on same line)</fo:block>
                <fo:block space-after="8pt" background-color="#eee"><fo:inline-container width="50%"><fo:block background-color="#cfc" padding="4pt">Left 50%</fo:block></fo:inline-container><fo:inline-container width="50%"><fo:block background-color="#ccf" padding="4pt">Right 50%</fo:block></fo:inline-container></fo:block>

                <!-- Test C: 49% + 49% to account for rounding/whitespace -->
                <fo:block font-weight="bold" space-after="4pt">Test C: 49% + 49% (leaving gap for whitespace)</fo:block>
                <fo:block space-after="8pt" background-color="#eee">
                    <fo:inline-container width="49%">
                        <fo:block background-color="#cfc" padding="4pt">Left 49%</fo:block>
                    </fo:inline-container>
                    <fo:inline-container width="49%">
                        <fo:block background-color="#ccf" padding="4pt">Right 49%</fo:block>
                    </fo:inline-container>
                </fo:block>

                <!-- Test D: fo:inline does NOT support width (expected to stack/overlap) -->
                <!-- NOTE: fo:inline is for inline text formatting only. Use fo:inline-container for
                     block content with width. This test intentionally shows incorrect behavior as a reminder. -->
                <fo:block font-weight="bold" space-after="4pt">Test D: fo:inline with width (EXPECTED TO FAIL)</fo:block>
                <fo:block font-size="9pt" color="gray" space-after="4pt">
                    fo:inline does not support width in XSL-FO spec. Use fo:inline-container instead.
                </fo:block>
                <fo:block space-after="8pt" background-color="#eee">
                    <fo:inline width="50%">
                        <fo:block background-color="#fcc" padding="4pt">Left inline 50%</fo:block>
                    </fo:inline>
                    <fo:inline width="50%">
                        <fo:block background-color="#ffc" padding="4pt">Right inline 50%</fo:block>
                    </fo:inline>
                </fo:block>

                <!-- Test E: Fixed widths (3in + 3in on 6.5in body) -->
                <fo:block font-weight="bold" space-after="4pt">Test E: Fixed widths (3in + 3in)</fo:block>
                <fo:block space-after="12pt" background-color="#eee">
                    <fo:inline-container width="3in">
                        <fo:block background-color="#cfc" padding="4pt">Left 3in</fo:block>
                    </fo:inline-container>
                    <fo:inline-container width="3in">
                        <fo:block background-color="#ccf" padding="4pt">Right 3in</fo:block>
                    </fo:inline-container>
                </fo:block>

                <!-- ============================================ -->
                <!-- SECTION 13: Nested Templates -->
                <!-- ============================================ -->
                <fo:block font-size="16pt" font-weight="bold" space-before="20pt" space-after="12pt">
                    13. Nested Templates (Template calling Template)
                </fo:block>

                <fo:block space-after="8pt" font-size="10pt" color="gray">
                    This tests a template that calls another template. The outer template wraps
                    content in a bordered box, the inner template renders an icon with text.
                </fo:block>

                <!-- Call the outer template, which internally calls the inner template -->
                <xsl:call-template name="test.outer-wrapper">
                    <xsl:with-param name="title">Nested Template Test</xsl:with-param>
                </xsl:call-template>

                <!-- ============================================ -->
                <!-- SECTION 14: Java Map (Multiple Maps) -->
                <!-- ============================================ -->
                <fo:block font-size="16pt" font-weight="bold" space-before="20pt" space-after="12pt">
                    14. Java Map (Multiple Map Entries)
                </fo:block>

                <fo:block space-after="8pt" font-size="10pt" color="gray">
                    Tests the JAVA MAP tab feature with multiple map entries.
                    Supports two syntaxes: $mapName.key (dot notation) and map:get($mapName, 'key').
                </fo:block>

                <fo:block space-after="6pt" font-size="9pt" color="#666" font-style="italic">
                    Required JSON in JAVA MAP tab:
                    {"firstMap": {"reportTitle": "QUARTERLY AUDIT REPORT"}, "secondMap": {"auditorName": "John Doe"}}
                </fo:block>

                <fo:block space-after="10pt">
                    <fo:block space-after="4pt">
                        <fo:inline font-weight="bold">Dot Notation: </fo:inline>
                        Title = <xsl:value-of select="$firstMap.reportTitle"/>,
                        Auditor = <xsl:value-of select="$secondMap.auditorName"/>
                    </fo:block>
                    <fo:block>
                        <fo:inline font-weight="bold">map:get() Syntax: </fo:inline>
                        Title = <xsl:value-of select="map:get($firstMap, 'reportTitle')"/>,
                        Auditor = <xsl:value-of select="map:get($secondMap, 'auditorName')"/>
                    </fo:block>
                </fo:block>

            </fo:flow>
        </fo:page-sequence>
    </fo:root>
</xsl:template>

<!-- ============================================ -->
<!-- NAMED TEMPLATE: Legend Item -->
<!-- ============================================ -->
<!--
    Reusable template for icon + label pairs in a legend.
    Uses alignment-baseline="middle" for vertical centering.
    Uses keep-together.within-line="always" to prevent awkward breaks.
-->
<xsl:template name="ui.legend.item">
    <xsl:param name="icon-url"/>
    <xsl:param name="label"/>
    <xsl:param name="icon-width" select="'12pt'"/>

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

<!-- ============================================ -->
<!-- NESTED TEMPLATE TEST: Outer Wrapper -->
<!-- ============================================ -->
<!--
    Outer template that creates a bordered container and calls
    the inner template to render content inside it.
-->
<xsl:template name="test.outer-wrapper">
    <xsl:param name="title"/>

    <fo:block border="2pt solid #336699" padding="10pt" background-color="#f0f8ff" space-after="12pt">
        <fo:block font-weight="bold" font-size="12pt" color="#336699" space-after="8pt">
            <xsl:value-of select="$title"/>
        </fo:block>

        <!-- Call the inner template -->
        <xsl:call-template name="test.inner-content"/>
    </fo:block>
</xsl:template>

<!-- ============================================ -->
<!-- NESTED TEMPLATE TEST: Inner Content -->
<!-- ============================================ -->
<!--
    Inner template that renders a block with text and an external graphic.
    Called from within test.outer-wrapper.
-->
<xsl:template name="test.inner-content">
    <fo:block space-after="6pt">
        This content is rendered by the inner template (test.inner-content).
    </fo:block>
    <fo:block>
        <fo:external-graphic src="test-image.png" content-width="30pt" content-height="30pt"/>
        <fo:inline padding-left="8pt">Icon rendered from nested template call.</fo:inline>
    </fo:block>
</xsl:template>

<!-- ============================================ -->
<!-- REGRESSION TEST: Page Header Template -->
<!-- ============================================ -->
<!--
    Tests template called from static-content (region-before) that uses
    XPath expressions for dynamic attributes. This previously caused:
    "TypeError: doc.evaluate is not a function"

    The fix ensures dataContext is properly unwrapped when evaluating
    XPath inside templates called from static-content.
-->
<xsl:template name="page.header">
    <fo:block>
        <!-- Header with logo from XML data and static title -->
        <fo:inline-container width="20%">
            <fo:block>
                <!-- XPath-based src attribute - the key regression test -->
                <fo:external-graphic content-width="20pt" content-height="20pt">
                    <xsl:attribute name="src">
                        <xsl:value-of select="/report/logoUrl"/>
                    </xsl:attribute>
                </fo:external-graphic>
            </fo:block>
        </fo:inline-container>
        <fo:inline-container width="60%">
            <fo:block text-align="center" font-weight="bold" font-size="10pt" color="#333">
                XSL Viewer Feature Test Report
            </fo:block>
        </fo:inline-container>
        <fo:inline-container width="20%">
            <fo:block text-align="right" font-size="8pt" color="#666">
                <xsl:value-of select="/report/title"/>
            </fo:block>
        </fo:inline-container>
    </fo:block>
    <!-- Watermark: absolutely positioned, should overlay the page -->
    <fo:block position="absolute" top="40%" left="10%" font-size="72pt" color="rgba(200,200,200,0.3)"
              font-weight="bold" transform="rotate(-30deg)">
        SAMPLE
    </fo:block>
</xsl:template>

</xsl:stylesheet>
