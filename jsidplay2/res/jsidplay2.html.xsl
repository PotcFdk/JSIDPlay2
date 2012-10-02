<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output indent="yes" encoding="UTF-8" method="html"
		doctype-public="-//W3C//DTD HTML 4.0 //EN" />
	<xsl:param name="name" />
	<xsl:param name="width" />
	<xsl:param name="height" />
	<xsl:param name="main" />
	<xsl:param name="icon" />

	<xsl:template match="*|@*|comment()">
		<xsl:copy>
			<xsl:copy-of select="@*" />
			<xsl:apply-templates />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="LINK">
		<xsl:copy>
			<xsl:copy-of select="@*" />
			<xsl:attribute name="HREF">
				<xsl:value-of select="$icon" />
			</xsl:attribute>
			<xsl:apply-templates />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="TITLE">
		<xsl:copy>
			<xsl:value-of select="$name" />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="APPLET">
		<xsl:copy>
			<xsl:attribute name="width">
				<xsl:value-of select="$width"></xsl:value-of>
			</xsl:attribute>
			<xsl:attribute name="height">
				<xsl:value-of select="$height"></xsl:value-of>
			</xsl:attribute>
			<xsl:attribute name="code">
				<xsl:value-of select="$main"></xsl:value-of>
			</xsl:attribute>
			<xsl:apply-templates />
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>