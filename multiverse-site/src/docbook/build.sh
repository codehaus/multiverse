xsltproc \
 --stringparam chunk.section.depth 0 \
 --stringparam section.autolabel 1 \
 --stringparam section.label.includes.component.label 1 \
 --stringparam html.stylesheet style.css \
 -o ../../build/site/manual/manual.html /usr/share/xml/docbook/stylesheet/nwalsh/xhtml/chunk.xsl manual.xml

cp style.css ../../build/site/manual