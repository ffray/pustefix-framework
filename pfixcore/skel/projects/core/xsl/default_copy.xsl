<?xml version="1.0" encoding="ISO-8859-1"?><xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:pfx="http://www.schlund.de/pustefix/core" xmlns:cus="http://www.schlund.de/pustefix/customize" version="1.0">
  
  <xsl:template match="*">
    <xsl:param name="__env"/>
    <xsl:copy>
      <xsl:copy-of select="./@*"/>
      <xsl:apply-templates>
        <xsl:with-param name="__env" select="$__env"/>
      </xsl:apply-templates>
    </xsl:copy>
  </xsl:template>
  
</xsl:stylesheet>