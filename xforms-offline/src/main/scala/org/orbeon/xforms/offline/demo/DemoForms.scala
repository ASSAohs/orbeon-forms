package org.orbeon.xforms.offline.demo


object DemoForms {

  val HelloForm: String =
    """{"nonDefaultProperties":{"format.output.date":["if (. castable as xs:date) then format-date(xs:date(.), '[M]/[D]/[Y]', xxf:lang(), (), ()) else .",false],"format.input.time":["[h]:[m]:[s] [P,*-2]",false],"show-recoverable-errors":["0",false],"format.output.time":["if (. castable as xs:time) then format-time(xs:time(.), '[h]:[m]:[s] [P,*-2]', xxf:lang(), (), ()) else .",false],"format.output.dateTime":["if (. castable as xs:dateTime) then format-dateTime(xs:dateTime(.), '[M]/[D]/[Y] [h]:[m]:[s] [P,*-2]', xxf:lang(), (), ()) else .",false],"submission-poll-delay":["2000",false],"format.output.decimal":["if (. castable as xs:decimal) then format-number(xs:decimal(.),'#,##0.00') else .",false],"delay-before-display-loading":["0",false],"format.output.integer":["if (. castable as xs:integer) then format-number(xs:integer(.),'#,##0') else .",false]},"topLevelPart":{"startScope":{"parent":null,"scopeId":"","idMap":{"xf-2":"xf-2","xf-5":"xf-5","xf-1":"xf-1","xf-4":"xf-4","xf-3":"xf-3","xf-6":"xf-6","#document":"#document"}},"topLevelControls":[{"index":0,"name":"root","element":{"name":{"localName":"root","prefix":"","uri":""},"atts":[[{"localName":"id","prefix":"","uri":""},"#document"]]},"staticId":"#document","prefixedId":"#document","namespaceMapping":{"hash":"-1609326920","mapping":{}},"scopeRef":"","containerScopeRef":"","modelRef":"xf-2","langRef":{"None":{}},"children":[{"index":2,"name":"model","element":{"name":{"localName":"model","prefix":"xf","uri":"http://www.w3.org/2002/xforms"},"atts":[[{"localName":"id","prefix":"","uri":""},"xf-2"],[{"localName":"base","prefix":"xml","uri":"http://www.w3.org/XML/1998/namespace"},"."]]},"staticId":"xf-2","prefixedId":"xf-2","namespaceMapping":{"hash":"730181327","mapping":{"xml":"http://www.w3.org/XML/1998/namespace","xh":"http://www.w3.org/1999/xhtml","xf":"http://www.w3.org/2002/xforms"}},"scopeRef":"","containerScopeRef":"","modelRef":"xf-2","langRef":{"Undefined":{}},"children":[{"index":3,"name":"instance","element":{"name":{"localName":"instance","prefix":"xf","uri":"http://www.w3.org/2002/xforms"},"atts":[[{"localName":"id","prefix":"","uri":""},"xf-3"]]},"staticId":"xf-3","prefixedId":"xf-3","namespaceMapping":{"hash":"730181327","mapping":{"xml":"http://www.w3.org/XML/1998/namespace","xh":"http://www.w3.org/1999/xhtml","xf":"http://www.w3.org/2002/xforms"}},"scopeRef":"","containerScopeRef":"","modelRef":"xf-2","langRef":{"Undefined":{}},"readonly":false,"cache":false,"timeToLive":-1,"exposeXPathTypes":false,"indexIds":false,"indexClasses":false,"isLaxValidation":true,"isStrictValidation":false,"isSchemaValidation":true,"credentials":null,"excludeResultPrefixes":[],"useInlineContent":true,"useExternalContent":false,"instanceSource":null,"inlineRootElem":{"name":{"localName":"first-name","prefix":"","uri":""},"atts":[]}}]},{"index":4,"name":"input","element":{"name":{"localName":"input","prefix":"xf","uri":"http://www.w3.org/2002/xforms"},"atts":[[{"localName":"ref","prefix":"","uri":""},"/first-name"],[{"localName":"incremental","prefix":"","uri":""},"true"],[{"localName":"id","prefix":"","uri":""},"xf-4"],[{"localName":"base","prefix":"xml","uri":"http://www.w3.org/XML/1998/namespace"},"."]]},"staticId":"xf-4","prefixedId":"xf-4","namespaceMapping":{"hash":"730181327","mapping":{"xml":"http://www.w3.org/XML/1998/namespace","xh":"http://www.w3.org/1999/xhtml","xf":"http://www.w3.org/2002/xforms"}},"scopeRef":"","containerScopeRef":"","modelRef":"xf-2","langRef":{"Undefined":{}},"children":[{"index":5,"name":"label","element":{"name":{"localName":"label","prefix":"xf","uri":"http://www.w3.org/2002/xforms"},"atts":[[{"localName":"id","prefix":"","uri":""},"xf-5"]]},"staticId":"xf-5","prefixedId":"xf-5","namespaceMapping":{"hash":"730181327","mapping":{"xml":"http://www.w3.org/XML/1998/namespace","xh":"http://www.w3.org/1999/xhtml","xf":"http://www.w3.org/2002/xforms"}},"scopeRef":"","containerScopeRef":"","modelRef":"xf-2","langRef":{"Undefined":{}}}]},{"index":6,"name":"output","element":{"name":{"localName":"output","prefix":"xf","uri":"http://www.w3.org/2002/xforms"},"atts":[[{"localName":"value","prefix":"","uri":""},"if (normalize-space(/first-name) = '') then '' else concat('Hello, ', /first-name, '!')"],[{"localName":"id","prefix":"","uri":""},"xf-6"],[{"localName":"base","prefix":"xml","uri":"http://www.w3.org/XML/1998/namespace"},"."]]},"staticId":"xf-6","prefixedId":"xf-6","namespaceMapping":{"hash":"730181327","mapping":{"xml":"http://www.w3.org/XML/1998/namespace","xh":"http://www.w3.org/1999/xhtml","xf":"http://www.w3.org/2002/xforms"}},"scopeRef":"","containerScopeRef":"","modelRef":"xf-2","langRef":{"Undefined":{}}}]}]},"template":{"saxStore":{"eventBufferPosition":240,"eventBuffer":[0,10,10,2,2,2,4,3,2,2,2,3,3,3,3,2,10,2,4,2,4,2,4,3,4,2,4,2,4,3,4,2,4,3,4,2,4,3,4,3,4,3,4,3,5,10,2,4,2,4,2,4,3,4,2,4,2,4,3,4,2,4,2,2,4,3,4,3,4,2,2,4,3,4,3,4,2,4,2,4,3,4,2,4,2,4,2,4,3,4,3,4,2,4,3,4,2,4,2,4,3,4,3,4,3,4,3,4,2,4,2,4,3,4,3,4,3,4,2,4,2,4,2,4,2,3,4,2,4,3,4,3,4,3,4,3,4,2,4,2,4,2,4,2,3,4,2,4,3,4,3,4,3,4,2,3,4,3,4,3,4,3,4,2,4,2,4,2,4,3,4,3,4,2,4,2,4,3,4,3,4,2,4,2,4,3,4,3,4,3,4,3,5,10,2,4,2,4,3,4,2,4,2,3,4,2,4,2,4,3,4,3,4,3,4,3,5,4,2,4,2,4,2,4,3,4,3,4,3,4,2,4,2,3,4,3,4,3,3,1],"charBufferPosition":2335,"charBuffer":["X","F","o","r","m","s"," ","H","e","l","l","o","\n"," "," "," "," ","\n"," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," ","Y","o","u","r"," ","b","r","o","w","s","e","r"," ","d","o","e","s"," ","n","o","t"," ","a","p","p","e","a","r"," ","t","o"," ","s","u","p","p","o","r","t"," ","J","a","v","a","S","c","r","i","p","t","."," ","Y","o","u"," ","m","a","y"," ","w","a","n","t"," ","t","o"," ","t","r","y"," ","o","n","e"," ","o","f"," ","t","h","e"," ","f","o","l","l","o","w","i","n","g",":","\n"," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," ","T","u","r","n"," ","o","n"," ","J","a","v","a","S","c","r","i","p","t"," ","i","n"," ","y","o","u","r"," ","b","r","o","w","s","e","r"," ","i","f"," ","i","t"," ","s","u","p","p","o","r","t","s"," ","i","t",".","\n"," "," "," "," "," "," "," "," "," "," "," "," ","U","s","e"," ","a"," ","b","r","o","w","s","e","r"," ","t","h","a","t"," ","s","u","p","p","o","r","t","s"," ","J","a","v","a","S","c","r","i","p","t",".","\n"," "," "," "," "," "," "," "," "," "," "," "," ","T","r","y"," ","a"," ","J","a","v","a","S","c","r","i","p","t","-","f","r","e","e"," ","v","e","r","s","i","o","n"," ","o","f"," ","t","h","i","s"," ","p","a","g","e",","," ","i","f"," ","m","a","d","e"," ","a","v","a","i","l","a","b","l","e"," ","b","y"," ","t","h","e"," ","a","p","p","l","i","c","a","t","i","o","n"," ","a","u","t","h","o","r",".","\n"," "," "," "," "," "," "," "," ","\n"," "," "," "," ","\n","\n"," "," "," "," ","\n"," "," "," "," "," "," "," "," ","A","n"," ","e","r","r","o","r"," ","h","a","s"," ","o","c","c","u","r","r","e","d","\n"," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","Y","o","u"," ","m","a","y"," ","w","a","n","t"," ","t","o"," ","t","r","y"," ","o","n","e"," ","o","f"," ","t","h","e"," ","f","o","l","l","o","w","i","n","g",":","\n"," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","C","l","o","s","e"," ","t","h","i","s"," ","d","i","a","l","o","g"," ","a","n","d"," ","c","o","n","t","i","n","u","e"," ","t","o"," ","u","s","e"," ","t","h","i","s"," ","p","a","g","e",".","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","R","e","l","o","a","d"," ","t","h","i","s"," ","p","a","g","e","."," ","N","o","t","e"," ","t","h","a","t"," ","y","o","u"," ","w","i","l","l"," ","l","o","s","e"," ","a","n","y"," ","u","n","s","a","v","e","d"," ","c","h","a","n","g","e","s",".","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","I","f"," ","t","h","e"," ","a","b","o","v","e"," ","d","o","e","s"," ","n","o","t"," ","w","o","r","k",","," ","t","r","y"," ","r","e","l","o","a","d","i","n","g"," ","t","h","e"," ","p","a","g","e"," ","y","o","u","r","s","e","l","f","."," ","N","o","t","e"," ","t","h","a","t"," ","y","o","u"," ","w","i","l","l"," ","l","o","s","e"," ","a","n","y"," ","u","n","s","a","v","e","d"," ","c","h","a","n","g","e","s",":","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","W","i","t","h"," ","F","i","r","e","f","o","x",":"," ","h","o","l","d"," ","d","o","w","n"," ","t","h","e"," ","s","h","i","f","t"," ","k","e","y"," ","a","n","d"," ","c","l","i","c","k"," ","t","h","e"," ","R","e","l","o","a","d"," ","b","u","t","t","o","n"," ","i","n"," ","y","o","u","r"," ","b","r","o","w","s","e","r"," ","t","o","o","l","b","a","r",".","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","W","i","t","h"," ","S","a","f","a","r","i"," ","a","n","d"," ","C","h","r","o","m","e",":"," ","c","l","i","c","k"," ","t","h","e"," ","R","e","l","o","a","d"," ","b","u","t","t","o","n"," ","i","n"," ","y","o","u","r"," ","b","r","o","w","s","e","r"," ","t","o","o","l","b","a","r",".","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","W","i","t","h"," ","I","n","t","e","r","n","e","t"," ","E","x","p","l","o","r","e","r",":"," ","h","o","l","d"," ","d","o","w","n"," ","t","h","e"," ","c","o","n","t","r","o","l"," ","k","e","y"," ","a","n","d"," ","c","l","i","c","k"," ","t","h","e"," ","R","e","l","o","a","d"," ","b","u","t","t","o","n"," ","i","n"," ","y","o","u","r"," ","b","r","o","w","s","e","r"," ","t","o","o","l","b","a","r",".","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","R","e","t","u","r","n"," ","h","o","m","e",".","\n"," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","S","h","o","w"," ","d","e","t","a","i","l","s","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","H","i","d","e"," ","d","e","t","a","i","l","s","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," ","\n"," "," "," "," ","\n"," "," "," "," ","\n"," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," ","R","e","l","o","a","d","i","n","g"," ","f","o","r","m","\n"," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","T","h","i","s"," ","f","o","r","m"," ","h","a","s"," ","t","o"," ","b","e"," ","r","e","l","o","a","d","e","d","."," ","T","h","i","s"," ","m","o","s","t"," ","l","i","k","e","l","y"," ","h","a","p","p","e","n","e","d"," ","b","e","c","a","u","s","e"," ","y","o","u","r"," ","s","e","s","s","i","o","n"," ","h","a","s"," ","e","x","p","i","r","e","d",","," ","w","h","i","c","h"," ","m","i","g","h","t","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","t","a","k","e"," ","t","o"," ","t","h","e"," ","l","o","g","i","n"," ","p","a","g","e","."," ","(","I","f"," ","y","o","u"," ","t","h","i","n","k"," ","t","h","a","t"," ","y","o","u"," ","s","h","o","u","l","d","n","'","t"," ","s","e","e"," ","t","h","i","s"," ","m","e","s","s","a","g","e"," ","a","n","d"," ","t","h","a","t"," ","t","h","e"," ","p","r","o","b","l","e","m"," ","p","e","r","s","i","s","t","s",",","\n"," "," "," "," "," "," "," "," "," "," "," "," "," "," "," "," ","p","l","e","a","s","e"," ","c","o","n","t","a","c","t"," ","s","u","p","p","o","r","t",".",")","\n"," "," "," "," "," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," ","O","K","\n"," "," "," "," "," "," "," "," ","\n"," "," "," "," ","\n","\n"," "," "," "," ","H","e","l","p","\n"," "," "," "," ","\n"," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," ","\n"," "," "," "," "," "," "," "," "," "," "," "," ","C","l","o","s","e","\n"," "," "," "," "," "," "," "," ","\n"," "," "," "," ","\n"," "," "," ","P","l","e","a","s","e"," ","e","n","t","e","r"," ","y","o","u","r"," ","f","i","r","s","t"," ","n","a","m","e",":"," "," "," "," "," "," "],"intBufferPosition":104,"intBuffer":[12,5,9,115,9,13,53,13,39,13,88,9,5,1,5,9,21,9,13,71,13,17,17,31,17,16,46,17,21,151,21,25,57,5,82,25,126,25,67,7,82,21,17,17,7,4,1,13,13,17,21,25,25,12,21,17,13,13,17,21,25,25,12,21,17,17,13,9,5,5,9,13,14,9,9,13,293,9,9,13,2,9,5,1,5,4,5,9,9,13,5,9,5,1,1,1,1,29,1,1,1,1,1,1],"lineBufferPosition":464,"lineBuffer":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],"systemIdBufferPosition":232,"systemIdBuffer":["","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","",""],"attributeCountBufferPosition":63,"attributeCountBuffer":[0,0,1,1,1,0,0,0,1,0,0,0,0,0,1,3,2,1,0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,1,1,0,1,2,0,1,0,1,2,0,1,4,1,0,1,0,1,1,1,1,1,1,1,1,0,3,1,0,2],"attributeCount":43,"stringBuilder":["xh","http://www.w3.org/1999/xhtml","xf","http://www.w3.org/2002/xforms","http://www.w3.org/1999/xhtml","html","xh:html","http://www.w3.org/1999/xhtml","head","xh:head","http://www.w3.org/1999/xhtml","title","xh:title","","id","id","CDATA","xf-1","http://www.w3.org/1999/xhtml","title","xh:title","http://www.w3.org/2002/xforms","model","xf:model","","id","id","CDATA","xf-2","http://www.w3.org/2002/xforms","instance","xf:instance","","id","id","CDATA","xf-3","","first-name","first-name","","first-name","first-name","http://www.w3.org/2002/xforms","instance","xf:instance","http://www.w3.org/2002/xforms","model","xf:model","http://www.w3.org/1999/xhtml","head","xh:head","http://www.w3.org/1999/xhtml","body","xh:body","","http://www.w3.org/1999/xhtml","http://www.w3.org/1999/xhtml","noscript","noscript","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","xforms-noscript-panel","http://www.w3.org/1999/xhtml","p","p","http://www.w3.org/1999/xhtml","p","p","http://www.w3.org/1999/xhtml","ul","ul","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","ul","ul","http://www.w3.org/1999/xhtml","div","div","http://www.w3.org/1999/xhtml","noscript","noscript","","","http://www.w3.org/1999/xhtml","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","xforms-error-dialogs","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","xforms-error-panel xforms-initially-hidden","","role","role","CDATA","dialog","","aria-labelledby","aria-labelledby","CDATA","error-dialog-title","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","hd","","id","id","CDATA","error-dialog-title","http://www.w3.org/1999/xhtml","div","div","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","bd","http://www.w3.org/1999/xhtml","p","p","http://www.w3.org/1999/xhtml","p","p","http://www.w3.org/1999/xhtml","ul","ul","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","a","a","","class","class","CDATA","xforms-error-panel-close","http://www.w3.org/1999/xhtml","a","a","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","a","a","","class","class","CDATA","xforms-error-panel-reload","http://www.w3.org/1999/xhtml","a","a","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","p","p","http://www.w3.org/1999/xhtml","p","p","http://www.w3.org/1999/xhtml","ul","ul","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","code","code","http://www.w3.org/1999/xhtml","code","code","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","code","code","http://www.w3.org/1999/xhtml","code","code","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","ul","ul","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","a","a","","href","href","CDATA","/","http://www.w3.org/1999/xhtml","a","a","http://www.w3.org/1999/xhtml","li","li","http://www.w3.org/1999/xhtml","ul","ul","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","xforms-error-panel-details-hidden","http://www.w3.org/1999/xhtml","p","p","http://www.w3.org/1999/xhtml","a","a","","class","class","CDATA","xforms-error-panel-show-details","http://www.w3.org/1999/xhtml","img","img","","src","src","CDATA","/ops/images/xforms/section-closed.png","","alt","alt","CDATA","Show Details","http://www.w3.org/1999/xhtml","img","img","http://www.w3.org/1999/xhtml","span","span","http://www.w3.org/1999/xhtml","span","span","http://www.w3.org/1999/xhtml","a","a","http://www.w3.org/1999/xhtml","p","p","http://www.w3.org/1999/xhtml","div","div","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","xforms-error-panel-details-shown xforms-disabled","http://www.w3.org/1999/xhtml","p","p","http://www.w3.org/1999/xhtml","a","a","","class","class","CDATA","xforms-error-panel-hide-details","http://www.w3.org/1999/xhtml","img","img","","src","src","CDATA","/ops/images/xforms/section-opened.png","","alt","alt","CDATA","Hide Details","http://www.w3.org/1999/xhtml","img","img","http://www.w3.org/1999/xhtml","span","span","http://www.w3.org/1999/xhtml","span","span","http://www.w3.org/1999/xhtml","a","a","http://www.w3.org/1999/xhtml","p","p","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","xforms-error-panel-details","http://www.w3.org/1999/xhtml","div","div","http://www.w3.org/1999/xhtml","div","div","http://www.w3.org/1999/xhtml","div","div","http://www.w3.org/1999/xhtml","div","div","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","xforms-login-detected-dialog modal hide fade","","tabindex","tabindex","CDATA","-1","","role","role","CDATA","dialog","","aria-hidden","aria-hidden","CDATA","true","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","modal-header","http://www.w3.org/1999/xhtml","h4","h4","http://www.w3.org/1999/xhtml","h4","h4","http://www.w3.org/1999/xhtml","div","div","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","modal-body","http://www.w3.org/1999/xhtml","p","p","http://www.w3.org/1999/xhtml","p","p","http://www.w3.org/1999/xhtml","div","div","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","modal-footer","http://www.w3.org/1999/xhtml","button","button","","class","class","CDATA","btn btn-primary","http://www.w3.org/1999/xhtml","button","button","http://www.w3.org/1999/xhtml","div","div","http://www.w3.org/1999/xhtml","div","div","http://www.w3.org/1999/xhtml","div","div","","","http://www.w3.org/1999/xhtml","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","xforms-help-panel xforms-initially-hidden","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","hd","http://www.w3.org/1999/xhtml","div","div","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","bd","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","xforms-help-panel-message","http://www.w3.org/1999/xhtml","div","div","http://www.w3.org/1999/xhtml","div","div","","class","class","CDATA","xforms-help-panel-close","http://www.w3.org/1999/xhtml","button","button","","class","class","CDATA","btn btn-small","http://www.w3.org/1999/xhtml","button","button","http://www.w3.org/1999/xhtml","div","div","http://www.w3.org/1999/xhtml","div","div","http://www.w3.org/1999/xhtml","div","div","","http://www.w3.org/1999/xhtml","p","xh:p","http://www.w3.org/2002/xforms","input","xf:input","","ref","ref","CDATA","/first-name","","incremental","incremental","CDATA","true","","id","id","CDATA","xf-4","http://www.w3.org/2002/xforms","label","xf:label","","id","id","CDATA","xf-5","http://www.w3.org/2002/xforms","label","xf:label","http://www.w3.org/2002/xforms","input","xf:input","http://www.w3.org/1999/xhtml","p","xh:p","http://www.w3.org/1999/xhtml","p","xh:p","http://www.w3.org/2002/xforms","output","xf:output","","value","value","CDATA","if (normalize-space(/first-name) = '') then '' else concat('Hello, ', /first-name, '!')","","id","id","CDATA","xf-6","http://www.w3.org/2002/xforms","output","xf:output","http://www.w3.org/1999/xhtml","p","xh:p","http://www.w3.org/1999/xhtml","body","xh:body","http://www.w3.org/1999/xhtml","html","xh:html"],"hasDocumentLocator":true,"marks":[]}}}""".stripMargin
}