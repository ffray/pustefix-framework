<?xml version="1.0" encoding="ISO-8859-1"?><xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:template match="/">
    <html>
      <head>
        <title>Last DOM</title>
        <style type="text/css">
          body {font-family: monospace; }
          .bracket            {color: #0000cc;}
          .attribute          {color: #0000cc;}
          .tag                {color: #dd5522;}
          .value              {color: #22aa00;}
          .comment            {color: #666666;}
          .dimmed             {color: #aaaaaa;}
          .error              {background-color: #ffff00;}
        </style>
      </head>
      <body>
        <xsl:apply-templates mode="static_disp" select="/"/>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="*" mode="static_disp">
    <xsl:param name="ind">��</xsl:param>
    <xsl:param name="break">true</xsl:param>
    <xsl:param name="bold">true</xsl:param>
    <xsl:variable name="dim">
      <xsl:choose>
        <xsl:when test="ancestor-or-self::navigation[1] and generate-id(ancestor-or-self::navigation[1]) = generate-id(/formresult/navigation)">true</xsl:when>
        <xsl:when test="ancestor-or-self::pageflow[1] and generate-id(ancestor-or-self::pageflow[1]) = generate-id(/formresult/pageflow)">true</xsl:when>
        <xsl:when test="ancestor-or-self::formhiddenvals[1] and generate-id(ancestor-or-self::formhiddenvals[1]) = generate-id(/formresult/formhiddenvals)">true</xsl:when>
        <xsl:when test="generate-id(current()) = generate-id(/formresult/formerrors)">true</xsl:when>
        <xsl:when test="ancestor-or-self::formvalues[1] and generate-id(ancestor-or-self::formvalues[1]) = generate-id(/formresult/formvalues)">true</xsl:when>
        <xsl:when test="ancestor-or-self::iwrappergroups[1] and generate-id(ancestor-or-self::iwrappergroups[1]) = generate-id(/formresult/iwrappergroups)">true</xsl:when>
        <xsl:when test="ancestor-or-self::iwrapperinfo[1] and generate-id(ancestor-or-self::iwrapperinfo[1]) = generate-id(/formresult/iwrapperinfo)">true</xsl:when>
        <xsl:otherwise>false</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="error">
      <xsl:choose>
        <xsl:when test="name() = 'formerrors' and count(./node()) &gt; 0">true</xsl:when>
        <xsl:when test="ancestor::formerrors">true</xsl:when>
        <xsl:otherwise>false</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="tagclass"> 
      <xsl:choose> 
        <xsl:when test="$dim = 'false'">
          <xsl:choose>
            <xsl:when test="$error = 'false'">tag</xsl:when>
            <xsl:otherwise>tag error</xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>dimmed</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="attrclass">
      <xsl:choose> 
        <xsl:when test="$dim = 'false'">attribute</xsl:when>
        <xsl:otherwise>dimmed</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="valueclass">
      <xsl:choose>
        <xsl:when test="$dim = 'false'">value</xsl:when>
        <xsl:otherwise>dimmed</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:if test="$break='false'">
      <br/>
    </xsl:if>
    <xsl:value-of select="$ind"/>
    <span class="bracket">
      <xsl:text>&lt;</xsl:text>
    </span>
    <span class="{$tagclass}">
      <xsl:if test="$bold = 'true' and $dim = 'false'">
        <xsl:attribute name="style">font-weight: bold;</xsl:attribute>
      </xsl:if>
    <xsl:value-of select="name()"/></span>
    <xsl:for-each select="@*">
      <xsl:text>�</xsl:text>
      <span class="{$attrclass}"><xsl:value-of select="name()"/></span>
      <xsl:text>="</xsl:text><span class="{$valueclass}"><xsl:value-of select="."/></span>
      <xsl:text>"</xsl:text>
    </xsl:for-each>
    <span class="bracket"><xsl:if test="count(./node()) = 0">/</xsl:if>&gt;</span>
    <xsl:apply-templates mode="static_disp">
      <xsl:with-param name="bold">
        <xsl:choose>
          <xsl:when test="count(ancestor::node()) &gt; 1">false</xsl:when>
          <xsl:otherwise>true</xsl:otherwise>
        </xsl:choose>
        </xsl:with-param>
        <xsl:with-param name="ind">
        <xsl:value-of select="$ind"/>����</xsl:with-param>
      <xsl:with-param name="break">false</xsl:with-param>
    </xsl:apply-templates>
    <xsl:if test="not(count(./node()) = 0)">
      <xsl:if test="count(./*) &gt; 0">
        <br/>
        <xsl:value-of select="$ind"/>
      </xsl:if>
      <span class="bracket">&lt;/</span>
      <span class="{$tagclass}">
      <xsl:if test="$bold = 'true' and $dim = 'false'">
        <xsl:attribute name="style">font-weight: bold;</xsl:attribute>
      </xsl:if>
      <xsl:value-of select="name()"/></span>
      <span class="bracket">&gt;</span>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="text()" mode="static_disp">
    <xsl:value-of select="normalize-space(current())"/>
  </xsl:template>

  <xsl:template match="comment()" mode="static_disp">
    <br/> <span class="comment">&lt;!--<xsl:value-of select="."/>--&gt;</span>
  </xsl:template>

  

</xsl:stylesheet>