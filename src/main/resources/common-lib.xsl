<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">


  <xsl:template name="address-block">
    <fo:block font-size="10pt" color="gray" space-after="10mm">
      <fo:block><xsl:value-of select="/invoice/company"/></fo:block>
      <fo:block><xsl:value-of select="/invoice/street"/></fo:block>
      <fo:block><xsl:value-of select="/invoice/city"/>, <xsl:value-of select="/invoice/zip"/></fo:block>
    </fo:block>
  </xsl:template>

</xsl:stylesheet>