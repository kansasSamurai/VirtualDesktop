<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format"
    xmlns:fox="http://xmlgraphics.apache.org/fop/extensions">

    <xsl:template match="/">
        <fo:root>

            <fo:layout-master-set>

                <fo:simple-page-master master-name="PortraitMain" page-width="216mm" page-height="279mm" margin="10mm">
                    <fo:region-body margin-top="20mm" margin-bottom="20mm"/>
                    <fo:region-after extent="10mm"/>
                </fo:simple-page-master>

                <fo:simple-page-master master-name="LandscapeExtra" page-width="279mm" page-height="216mm" margin="10mm">
                    <fo:region-body margin-top="20mm" margin-bottom="30mm"/>
                    <fo:region-after extent="25mm"/>
                </fo:simple-page-master>

                <fo:simple-page-master master-name="LandscapeStandard" page-width="279mm" page-height="216mm" margin="10mm">
                    <fo:region-body margin-top="20mm" margin-bottom="20mm"/>
                    <fo:region-after extent="10mm"/>
                </fo:simple-page-master>

                <fo:page-sequence-master master-name="LandscapeSectionController">
                    <fo:repeatable-page-master-alternatives>
                        <fo:conditional-page-master-reference master-reference="LandscapeExtra" page-position="first"/>
                        <fo:conditional-page-master-reference master-reference="LandscapeStandard" page-position="rest"/>
                    </fo:repeatable-page-master-alternatives>
                </fo:page-sequence-master>

            </fo:layout-master-set>

            <!-- ========== PORTRAIT SECTION ========== -->
            <fo:page-sequence master-reference="PortraitMain">
                <fo:static-content flow-name="xsl-region-after">
                    <fo:block text-align="center" font-size="8pt" color="#666">
                        Portrait Footer — Page <fo:page-number/>
                    </fo:block>
                </fo:static-content>

                <fo:flow flow-name="xsl-region-body">
                    <fo:block font-size="20pt" font-weight="bold" space-after="8mm"
                        color="#1565c0" border-bottom="2pt solid #1565c0" padding-bottom="3mm">
                        Portrait Section — Project Overview
                    </fo:block>

                    <fo:block space-after="5mm">
                        This document demonstrates conditional page masters using
                        page-sequence-master with repeatable-page-master-alternatives.
                        The portrait section uses a single page master for all pages,
                        while the landscape section uses a different master for its
                        first page (with an expanded footer) versus subsequent pages.
                    </fo:block>

                    <fo:block font-style="italic" color="#666">
                        Detailed data follows in the landscape section.
                    </fo:block>
                </fo:flow>
            </fo:page-sequence>

            <!-- ========== LANDSCAPE SECTION ========== -->
            <!-- Uses LandscapeSectionController: 
                 first page gets LandscapeExtra (special footer),
                 subsequent pages get LandscapeStandard (normal footer) -->
            <fo:page-sequence master-reference="LandscapeSectionController">

                <!-- NOTE: In full XSL-FO, page-sequence-master would route different
                     footers to first vs rest pages. The viewer does not yet support
                     conditional-page-master-reference, so we use a single footer here.
                     The "special" first-page footer is shown on all landscape pages. -->
                <fo:static-content flow-name="xsl-region-after">
                    <fo:block border="1pt solid #dc3545" padding="2mm" background-color="#fff5f5"
                        font-size="9pt" color="#dc3545" space-after="2mm">
                        NOTICE: This data extract is classified as internal-only. Distribution
                        outside the project team requires written authorization from the program
                        manager. Data refresh date: February 2026.
                    </fo:block>
                    <fo:block text-align="center" font-size="8pt" color="#666">
                        Landscape Section — Page <fo:page-number/>
                    </fo:block>
                </fo:static-content>

                <fo:flow flow-name="xsl-region-body">

                    <fo:block font-size="18pt" font-weight="bold" space-after="6mm"
                        color="#e65100" border-bottom="2pt solid #e65100" padding-bottom="3mm">
                        Landscape Section - Project Details
                    </fo:block>


                    <!-- Second page of landscape content (should get normal footer) -->
                    <fo:block break-before="page"/>

                    <fo:block font-size="14pt" font-weight="bold" space-after="6mm" color="#e65100">
                        Landscape Section - Page 2 - Custom Footer
                    </fo:block>

                    <fo:block space-before="6mm" font-style="italic" color="#666" text-align="center"
                        border-top="1pt solid #ccc" padding-top="4mm">
                        End of landscape data section. Note: this page should have the standard
                        footer (page number only), while the first landscape page should have
                        the special classification notice footer.
                    </fo:block>
                </fo:flow>
            </fo:page-sequence>

        </fo:root>
    </xsl:template>

</xsl:stylesheet>
