<#if comment>

  TEMPLATE DESCRIPTION:

  This is XHTML main application template for 'PrimeFaces Pages from Entity Classes'.
  Templating is performed using FreeMaker (http://freemarker.org/) - see its documentation
  for full syntax. Variables available for templating are:

    bundle - name of the bundle variable set in faces-config.xml (type: String)
    comment - always set to "false" (type: Boolean)
    jsfFolder - URL portion that holds the jsf pages, if any (type: String)
    entities - list of beans with following properites:
        entityClassName - controller class name (type: String)
        entityDescriptors - list of beans describing individual entities. Bean has following properties:
            label - part of bundle key name for label (type: String)
            title - part of bundle key name for title (type: String)
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
  be found in category PrimeFaces CRUD Generator->PrimeFaces Pages from Entity Classes.

</#if>
<#setting number_format="0">
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:ui="${nsLocation}/jsf/facelets"
      xmlns:h="${nsLocation}/jsf/html"
      xmlns:p="http://primefaces.org/ui">

    <h:head>
        <title><ui:insert name="title">Default Title</ui:insert></title>
        <h:outputStylesheet library="css" name="jsfcrud.css"/>
        <h:outputScript library="js" name="jsfcrud.js"/>
    </h:head>

    <h:body>

        <p:growl id="growl" life="3000" />

        <p:layout fullPage="true">
            <p:layoutUnit position="north" size="65" header="${r"#{"}${bundle}.AppName${r"}"}">
                <h:form id="menuForm">
                    <p:menubar>
                        <p:menuitem value="${r"#{"}${bundle}.Home${r"}"}" outcome="/index" icon="ui-icon-home"/>
                        <p:submenu label="${r"#{"}${bundle}.Maintenance${r"}"}">
<#list entities as entity>
                            <p:menuitem value="${entity.entityClassName}" outcome="${jsfFolder}/${entity.entityClassName?uncap_first}/List.xhtml" />
</#list>
                        </p:submenu>
                    </p:menubar>
                </h:form>
            </p:layoutUnit>

            <p:layoutUnit position="south" size="60">
                <ui:insert name="footer"/>
            </p:layoutUnit>

            <p:layoutUnit position="center">
                <ui:insert name="body"/>
            </p:layoutUnit>

        </p:layout>

    </h:body>

</html>
