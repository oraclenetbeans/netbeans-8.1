<#assign licenseFirst = "/*">
<#assign licensePrefix = " * ">
<#assign licenseLast = " */">
<#include "${project.licensePath}">

<#if package?? && package != "">
package ${package}

</#if>
/**
 *
 * @author ${user}
 */
class ${name} {
	
}

