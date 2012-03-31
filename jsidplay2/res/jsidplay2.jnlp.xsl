<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output indent="yes" encoding="UTF-8" />
	<xsl:param name="filename" />
	<xsl:param name="applet" />
	<xsl:param name="name" />
	<xsl:param name="width" />
	<xsl:param name="height" />
	<xsl:param name="classpath" />
	<xsl:param name="jars_sizes" />
	<xsl:param name="jar" />
	<xsl:param name="main" />
	<xsl:param name="size" />
	<xsl:param name="sizeLinux" />
	<xsl:param name="sizeOsx" />
	<xsl:param name="sizeWindows" />
	<xsl:param name="sizeIcon" />

	<xsl:template match="*|@*|comment()">
		<xsl:copy>
			<xsl:copy-of select="@*" />
			<xsl:apply-templates />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="jnlp">
		<xsl:copy>
			<xsl:copy-of select="@*" />
			<xsl:attribute name="href">
				<xsl:value-of select="$filename" />
			</xsl:attribute>
			<xsl:apply-templates />
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="resources[not(@os) and not(@arch)]">
		<xsl:copy>
			<xsl:apply-templates />
			<xsl:call-template name="createResource">
				<xsl:with-param name="classpathList" select="$classpath" />
				<xsl:with-param name="classpathSizeList" select="$jars_sizes" />
			</xsl:call-template>
		</xsl:copy>
	</xsl:template>
	<xsl:template name="createResource">
		<xsl:param name="classpathList" select="''" />
		<xsl:param name="classpathSizeList" select="''" />
		<xsl:variable name="resource" select="substring-before($classpathList,' ')" />
		<xsl:variable name="jarSize" select="substring-before($classpathSizeList,' ')" />
		<xsl:choose>
			<xsl:when test="string-length($resource) &gt; 0">
				<jar>
					<xsl:attribute name="href">
						<xsl:value-of select="$resource" />
					</xsl:attribute>
					<xsl:attribute name="size">
						<xsl:value-of select="$jarSize" />
					</xsl:attribute>
					<!-- other jar -->
					<xsl:attribute name="download">
						<xsl:value-of select="'eager'" />
					</xsl:attribute>
					<xsl:attribute name="part">
						<xsl:value-of select="'externals'" />
					</xsl:attribute>
				</jar>
				<xsl:call-template name="createResource">
					<xsl:with-param name="classpathList">
						<xsl:value-of select="substring-after($classpathList,' ')" />
					</xsl:with-param>
					<xsl:with-param name="classpathSizeList">
						<xsl:value-of select="substring-after($classpathSizeList,' ')" />
					</xsl:with-param>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<jar>
					<!-- other jar -->
					<xsl:attribute name="href">
						<xsl:value-of select="$classpathList" />
					</xsl:attribute>
					<xsl:attribute name="size">
						<xsl:value-of select="$classpathSizeList" />
					</xsl:attribute>
					<xsl:attribute name="download">
						<xsl:value-of select="'eager'" />
					</xsl:attribute>
					<xsl:attribute name="part">
						<xsl:value-of select="'externals'" />
					</xsl:attribute>
				</jar>
				<jar>
					<xsl:attribute name="href">
						<xsl:value-of select="$jar" />
					</xsl:attribute>
					<!-- main jar -->
					<xsl:attribute name="size">
						<xsl:value-of select="$size" />
					</xsl:attribute>
					<xsl:attribute name="main">
						<xsl:value-of select="'true'" />
					</xsl:attribute>
					<xsl:attribute name="part">
						<xsl:value-of select="'main'" />
					</xsl:attribute>
				</jar>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- Create application descriptor -->
	<xsl:template match="x-desc[$applet='false']">
		<xsl:element name="application-desc">
			<xsl:attribute name="main-class">
				<xsl:value-of select="$main" />
			</xsl:attribute>
		</xsl:element>
	</xsl:template>
	
	<!-- Create applet descriptor -->
	<xsl:template match="x-desc[$applet='true']">
		<xsl:element name="applet-desc">
			<xsl:attribute name="name">
				<xsl:value-of select="$name" />
			</xsl:attribute>
			<xsl:attribute name="main-class">
				<xsl:value-of select="$main" />
			</xsl:attribute>
			<xsl:attribute name="width">
				<xsl:value-of select="$width" />
			</xsl:attribute>
			<xsl:attribute name="height">
				<xsl:value-of select="$height" />
			</xsl:attribute>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="resources[@os='Linux']">
		<xsl:copy>
			<xsl:copy-of select="@*" />
			<nativelib href="jinput-linux-native.jar" size="{$sizeLinux}" download="eager" part="externals"/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="resources[@os='Mac OS']">
		<xsl:copy>
			<xsl:copy-of select="@*" />
			<nativelib href="jinput-osx-native.jar" size="{$sizeOsx}" download="eager" part="externals"/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="resources[@os='Windows']">
		<xsl:copy>
			<xsl:copy-of select="@*" />
			<nativelib href="jinput-windows-native.jar" size="{$sizeWindows}" download="eager" part="externals"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="icon">
		<xsl:copy>
			<xsl:copy-of select="@*" />
			<xsl:attribute name="size">
				<xsl:value-of select="$sizeIcon" />
			</xsl:attribute>
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>