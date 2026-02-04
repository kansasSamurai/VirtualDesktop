<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
  <xsl:include href="common-lib.xsl"/>

  <xsl:template match="/">
    <fo:root>
      <fo:layout-master-set>
        <fo:simple-page-master master-name="A4" page-height="297mm" page-width="210mm">
          <fo:region-body margin="20mm"/>
        </fo:simple-page-master>
      </fo:layout-master-set>
      
      <fo:page-sequence master-reference="A4">
        <fo:flow flow-name="xsl-region-body">
          
          <xsl:call-template name="address-block"/>

          <fo:block font-size="18pt" font-weight="bold" space-after="5mm">Invoice Details</fo:block>

          <fo:table width="100%" border="1pt solid black">
            <fo:table-body>
              <xsl:for-each select="invoice/items/item">
                <fo:table-row border-bottom="0.5pt solid #eee">
                  <fo:table-cell padding="5pt">
                    <fo:block><xsl:value-of select="description"/></fo:block>
                  </fo:table-cell>
                  <fo:table-cell padding="5pt" text-align="right">
                    <fo:block><xsl:value-of select="price"/></fo:block>
                  </fo:table-cell>
                </fo:table-row>
              </xsl:for-each>
            </fo:table-body>
          </fo:table>

        </fo:flow>
      </fo:page-sequence>
    </fo:root>
  </xsl:template>
</xsl:stylesheet>