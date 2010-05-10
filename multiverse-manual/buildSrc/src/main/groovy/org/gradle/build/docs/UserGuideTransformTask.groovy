/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.gradle.build.docs

import groovy.xml.MarkupBuilder
import groovy.xml.dom.DOMCategory
import groovy.xml.dom.DOMUtil
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.gradle.api.tasks.*

/**
 * Transforms userguide source into docbook, replacing custom xml elements.
 */
public class UserGuideTransformTask extends DefaultTask {
    @Input
    String getVersion() { return project.version.toString() }
    @Input
    String javadocUrl
    @Input
    String websiteUrl
    @InputFile
    File sourceFile
    @OutputFile
    File destFile
    @InputDirectory
    File snippetsDir
    @Input
    Set<String> tags = new HashSet()
    @InputFiles
    FileCollection classpath;

    final SampleElementValidator validator = new SampleElementValidator();

    @TaskAction
    def transform() {
        Element root

        destFile.parentFile.mkdirs()

        System.setProperty("org.apache.xerces.xni.parser.XMLParserConfiguration",
                "org.apache.xerces.parsers.XIncludeParserConfiguration")

        // Set the thread context classloader to pick up the correct XML parser
        def uris = classpath.getFiles().collect {it.toURI().toURL()}
        def classloader = new URLClassLoader(uris as URL[], getClass().classLoader)
        def oldClassloader = Thread.currentThread().getContextClassLoader()
        Thread.currentThread().setContextClassLoader(classloader)
        try {

            root = loadAndTransform()

        } finally {
            Thread.currentThread().setContextClassLoader(oldClassloader)
        }

        destFile.parentFile.mkdirs()
        destFile.withOutputStream {OutputStream stream ->
            DOMUtil.serialize(root, stream)
        }
    }

    private Element loadAndTransform() {
        Document doc = parseSourceFile()
        Element root = doc.documentElement
        use(DOMCategory) {
            addVersionInfo(doc)
            applyConditionalChunks(doc)
            transformSamples(doc)
            transformApiLinks(doc)
            transformWebsiteLinks(doc)
            fixProgramListings(doc)
        }

        return root
    }

    def addVersionInfo(Document doc) {
        Element releaseInfo = doc.createElement('releaseinfo')
        releaseInfo.appendChild(doc.createTextNode(version.toString()))
        if (doc.documentElement.bookinfo[0]) {
            doc.documentElement.bookinfo[0].appendChild(releaseInfo)
        }
    }

    def fixProgramListings(Document doc) {
        doc.documentElement.depthFirst().findAll { it.name() == 'programlisting' || it.name() == 'screen' }.each {Element element ->
            element.setTextContent(normalise(element.getTextContent()))
        }
    }

    String normalise(String content) {
        return content.trim().replace('\t', '    ').replace('\r\n', '\n')
    }

    def transformApiLinks(Document doc) {
        File linksFile = new File(destFile.parentFile, 'links.xml')
        linksFile.withWriter {Writer writer ->
            MarkupBuilder xml = new MarkupBuilder(writer)
            xml.links {
                doc.documentElement.depthFirst().findAll { it.name() == 'apilink' }.each {Element element ->
                    String className = element.'@class'
                    String methodName = element.'@method'
                    String lang = element.'@lang' ?: 'java'

                    Element ulinkElement = doc.createElement('ulink')
                    String baseUrl = javadocUrl
                    String href = "$baseUrl/${className.replace('.', '/')}.html"
                    ulinkElement.setAttribute('url', href)

                    Element classNameElement = doc.createElement('classname')
                    ulinkElement.appendChild(classNameElement)

                    String classBaseName = className.tokenize('.').last()
                    String linkText = methodName ? "$classBaseName.$methodName()" : classBaseName
                    classNameElement.appendChild(doc.createTextNode(linkText))

                    element.parentNode.replaceChild(ulinkElement, element)

                    xml.link(className: className, lang: lang)
                }
            }
        }
    }

    def transformWebsiteLinks(Document doc) {
        doc.documentElement.depthFirst().findAll { it.name() == 'ulink' }.each {Element element ->
            String url = element.'@url'
            if (url.startsWith('website:')) {
                url = url.substring(8)
                if (websiteUrl) {
                    url = "${websiteUrl}/${url}"
                }
                element.setAttribute('url', url)
            }
        }
    }

    def transformSamples(Document doc) {
        File samplesFile = new File(destFile.parentFile, 'samples.xml')
        samplesFile.withWriter {Writer writer ->
            MarkupBuilder xml = new MarkupBuilder(writer)
            xml.samples {
                doc.documentElement.depthFirst().findAll { it.name() == 'sample' }.each {Element element ->
                	validator.validate(element)
                    String sampleId = element.'@id'
                    String srcDir = element.'@dir'

                    // This class handles the responsibility of adding the location tips to the first child of first
                    // example defined in the sample.
                    SampleElementLocationHandler locationHandler = new SampleElementLocationHandler(doc, element, srcDir)
                   
                    xml.sample(id: sampleId, dir: srcDir)

                    String title = element.'@title' 

                    Element exampleElement = doc.createElement('example')
                    exampleElement.setAttribute('id', sampleId)
                    Element titleElement = doc.createElement('title')
               	   	titleElement.appendChild(doc.createTextNode(title))
               	   	exampleElement.appendChild(titleElement);
                    
                    element.children().each {Element child ->
                        if (child.name() == 'sourcefile') {
                            String file = child.'@file'
                           
                            Element sourcefileTitle = doc.createElement("para")
                    		Element commandElement = doc.createElement('filename')
                        	commandElement.appendChild(doc.createTextNode(file))
                        	sourcefileTitle.appendChild(commandElement)
                        	exampleElement.appendChild(sourcefileTitle);

                            Element programListingElement = doc.createElement('programlisting')
                            if (file.endsWith('.gradle') || file.endsWith('.groovy')) {
                                programListingElement.setAttribute('language', 'java')
                            }
                            else if (file.endsWith('.xml')) {
                                programListingElement.setAttribute('language', 'xml')
                            }
                            File srcFile
                            String snippet = child.'@snippet'
                            if (snippet) {
                                srcFile = new File(snippetsDir, "$srcDir/$file-$snippet")
                            } else {
                                srcFile = new File(snippetsDir, "$srcDir/$file")
                            }
                            programListingElement.appendChild(doc.createTextNode(normalise(srcFile.text)))
                            exampleElement.appendChild(programListingElement)
                        } else if (child.name() == 'output') {
                            String args = child.'@args'
                            String outputFile = child.'@outputFile' ? child.'@outputFile' : "${sampleId}.out"

                            xml.sample(id: sampleId, dir: srcDir, args: args, outputFile: outputFile)

                            Element outputTitle = doc.createElement("para")
                            outputTitle.appendChild(doc.createTextNode("Output of "))
                            Element commandElement = doc.createElement('userinput')
                            commandElement.appendChild(doc.createTextNode("gradle $args"))
                            outputTitle.appendChild(commandElement)
                            exampleElement.appendChild(outputTitle)

                            Element screenElement = doc.createElement('screen')
                            File srcFile = new File(sourceFile.parentFile, "../../../src/samples/userguideOutput/${outputFile}")
                            screenElement.appendChild(doc.createTextNode("> gradle $args\n" + normalise(srcFile.text)))
                            exampleElement.appendChild(screenElement)
                        } else if (child.name() == 'test') {
                            String args = child.'@args'

                            xml.sample(id: sampleId, dir: srcDir, args: args)
                        } else if (child.name() == 'layout') {
                        	Element outputTitle = doc.createElement("para")
                    		outputTitle.appendChild(doc.createTextNode("Build layout"))
                    		exampleElement.appendChild(outputTitle)

                            Element programListingElement = doc.createElement('programlisting')
                            exampleElement.appendChild(programListingElement)
                            StringBuilder content = new StringBuilder()
                            content.append("${srcDir.tokenize('/').last()}/\n")
                            List stack = []
                            child.text().eachLine {
                                def fileName = it.trim()
                                if (!fileName) {
                                    return
                                }
                                File file = new File(snippetsDir, "$srcDir/$fileName")
                                if (!file.exists()) {
                                    throw new RuntimeException("Sample file $file does not exist for sample ${sampleId}.")
                                }
                                List context = fileName.tokenize('/')

                                int common = 0;
                                for (;common < stack.size() && common < context.size() && stack[common] == context[common]; common++) { ; }
                                stack = stack.subList(0, common)

                                (stack.size() + 1).times { content.append("  ") }
                                content.append("${context.subList(stack.size(), context.size()).join('/')}${file.directory ? '/' : ''}\n")
                                if (file.directory) {
                                    stack = context
                                }
                            }
                            programListingElement.appendChild(doc.createTextNode(content.toString()))
                        }

                        locationHandler.processSampleLocation(exampleElement)
                    }
                    element.parentNode.insertBefore(exampleElement, element)
                    element.parentNode.removeChild(element)
                }
            }
        }
    }

    void applyConditionalChunks(Document doc) {
        doc.documentElement.depthFirst().findAll { it.'@condition' }.each {Element element ->
            if (!tags.contains(element.'@condition')) {
                element.parentNode.removeChild(element)
            }
        }
    }

    private Document parseSourceFile() {
        DocumentBuilderFactory factory = Class.forName('com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl', true, Thread.currentThread().contextClassLoader).newInstance()
        factory.setNamespaceAware(true)
        DocumentBuilder builder = factory.newDocumentBuilder()
        return builder.parse(sourceFile)
    }
}
