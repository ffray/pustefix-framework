<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

  <xsl:include href="create_lib.xsl"/>
  <xsl:output method="text" encoding="ISO-8859-1" indent="no"/>

  <xsl:param name="java_home"/>
  <xsl:template match="/">
#
# NOTE: this is only a sample file, suited for a single machine without any clustering.
#       You may need to tweak this file and copy it somewhere else so your changes will not
#       get lost.
#

workers.tomcat_home=<xsl:value-of select="$docroot"/>/servletconf/tomcat
workers.java_home=<xsl:value-of select="$java_home"/>
worker.list=<xsl:apply-templates select="/projects/common/tomcat/jkmount/node()"/>

ps=/
worker.router.host=<xsl:apply-templates select="/projects/common/tomcat/jkhost/node()"/>
worker.router.port=8009
worker.router.type=ajp13
 </xsl:template>
</xsl:stylesheet>
