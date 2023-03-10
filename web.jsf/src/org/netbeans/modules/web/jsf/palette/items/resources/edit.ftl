<#if comment>

  TEMPLATE DESCRIPTION:

  This is XHTML template for 'JSF Editable Form From Entity' action. Templating
  is performed using FreeMaker (http://freemarker.org/) - see its documentation
  for full syntax. Variables available for templating are:

    prefixResolver - helps resolve prefix for given template (call prefixForNS(namespace, fallbackPrefix) method)
    entityName - name of entity being modified (type: String)
    managedBean - name of managed choosen in UI (type: String)
    managedBeanProperty - name of managed bean property choosen in UI (type: String)
    item - name of property used for dataTable iteration (type: String)
    comment - always set to "false" (type: Boolean)
    entityDescriptors - list of beans describing individual entities. Bean has following properties:
        label - field label (type: String)
        name - field property name (type: String)
        dateTimeFormat - date/time/datetime formatting (type: String)
        blob - does field represents a large block of text? (type: boolean)
        relationshipOne - does field represent one to one or many to one relationship (type: boolean)
        relationshipMany - does field represent one to many relationship (type: boolean)
        id - field id name (type: String)
        required - is field optional and nullable or it is not? (type: boolean)
        valuesGetter - if item is of type 1:1 or 1:many relationship then use this
            getter to populate <h:selectOneMenu> or <h:selectManyMenu>

  This template is accessible via top level menu Tools->Templates and can
  be found in category JavaServer Faces->JSF Data/Form from Entity.

</#if>

<#assign htmlTagPrefix=prefixResolver.getPrefixForNS("http://xmlns.jcp.org/jsf/html", "h")>
<#assign coreTagPrefix=prefixResolver.getPrefixForNS("http://xmlns.jcp.org/jsf/core", "f")>

<${prefixResolver.getPrefixForNS("http://xmlns.jcp.org/jsf/html", "h")}:form>
    <h1><${htmlTagPrefix}:outputText value="Create/Edit"/></h1>
    <${htmlTagPrefix}:panelGrid columns="2">
<#list entityDescriptors as entityDescriptor>
        <${htmlTagPrefix}:outputLabel value="${entityDescriptor.label}:" for="${entityDescriptor.id}" />
<#if entityDescriptor.dateTimeFormat?? && entityDescriptor.dateTimeFormat != "">
        <${htmlTagPrefix}:inputText id="${entityDescriptor.id}" value="${r"#{"}${entityDescriptor.name}${r"}"}" title="${entityDescriptor.label}" <#if entityDescriptor.required>required="true" requiredMessage="The ${entityDescriptor.label} field is required."</#if>>
            <${coreTagPrefix}:convertDateTime pattern="${entityDescriptor.dateTimeFormat}" />
        </${htmlTagPrefix}:inputText>
<#elseif entityDescriptor.blob>
        <${htmlTagPrefix}:inputTextarea rows="4" cols="30" id="${entityDescriptor.id}" value="${r"#{"}${entityDescriptor.name}${r"}"}" title="${entityDescriptor.label}" <#if entityDescriptor.required>required="true" requiredMessage="The ${entityDescriptor.label} field is required."</#if>/>
<#elseif entityDescriptor.relationshipOne>
        <${htmlTagPrefix}:selectOneMenu id="${entityDescriptor.id}" value="${r"#{"}${entityDescriptor.name}${r"}"}" title="${entityDescriptor.label}" <#if entityDescriptor.required>required="true" requiredMessage="The ${entityDescriptor.label} field is required."</#if>>
            <!-- TODO: update below reference to list of available items-->
            <${coreTagPrefix}:selectItems value="${r"#{"}fixme${r"}"}"/>
        </${htmlTagPrefix}:selectOneMenu>
<#elseif entityDescriptor.relationshipMany>
        <${htmlTagPrefix}:selectManyMenu id="${entityDescriptor.id}" value="${r"#{"}${entityDescriptor.name}${r"}"}" title="${entityDescriptor.label}" <#if entityDescriptor.required>required="true" requiredMessage="The ${entityDescriptor.label} field is required."</#if>>
            <!-- TODO: update below reference to list of available items-->
            <${coreTagPrefix}:selectItems value="${r"#{"}fixme${r"}"}"/>
        </${htmlTagPrefix}:selectManyMenu>
<#else>
        <${htmlTagPrefix}:inputText id="${entityDescriptor.id}" value="${r"#{"}${entityDescriptor.name}${r"}"}" title="${entityDescriptor.label}" <#if entityDescriptor.required>required="true" requiredMessage="The ${entityDescriptor.label} field is required."</#if>/>
</#if>
</#list>
    </${htmlTagPrefix}:panelGrid>
</${htmlTagPrefix}:form>
