var _____WB$wombat$assign$function_____ = function(name) {return (self._wb_wombat && self._wb_wombat.local_init && self._wb_wombat.local_init(name)) || self[name]; };
if (!self.__WB_pmw) { self.__WB_pmw = function(obj) { this.__WB_source = obj; return this; } }
{
  let window = _____WB$wombat$assign$function_____("window");
  let self = _____WB$wombat$assign$function_____("self");
  let document = _____WB$wombat$assign$function_____("document");
  let location = _____WB$wombat$assign$function_____("location");
  let top = _____WB$wombat$assign$function_____("top");
  let parent = _____WB$wombat$assign$function_____("parent");
  let frames = _____WB$wombat$assign$function_____("frames");
  let opener = _____WB$wombat$assign$function_____("opener");

// JavaScript Document

function startList() {
if (document.all&&document.getElementById) {
	navRoot = document.getElementById("nav");
	if (navRoot!=null) { //if the language panel is active
		for (i=0; i<navRoot.childNodes.length; i++) {
			node = navRoot.childNodes[i];
			if (node.nodeName=="LI") {
				node.onmouseover=function() {
					this.className+=" over";
				}
				node.onmouseout=function() {
					this.className=this.className.replace(" over", "");
				}
			}	
		}
	}
}
}
window.onload=startList;




}
/*
     FILE ARCHIVED ON 05:29:40 Jan 22, 2014 AND RETRIEVED FROM THE
     INTERNET ARCHIVE ON 20:50:28 Jan 18, 2023.
     JAVASCRIPT APPENDED BY WAYBACK MACHINE, COPYRIGHT INTERNET ARCHIVE.

     ALL OTHER CONTENT MAY ALSO BE PROTECTED BY COPYRIGHT (17 U.S.C.
     SECTION 108(a)(3)).
*/
/*
playback timings (ms):
  captures_list: 1133.924
  exclusion.robots: 0.103
  exclusion.robots.policy: 0.095
  cdx.remote: 0.065
  esindex: 0.009
  LoadShardBlock: 1063.752 (3)
  PetaboxLoader3.datanode: 791.093 (5)
  CDXLines.iter: 23.414 (3)
  load_resource: 243.611 (2)
  PetaboxLoader3.resolve: 94.587 (2)
*/