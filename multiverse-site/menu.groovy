class Menu {

  String name
  MenuItem[] items
}

class MenuItem {

  String url, title, pageid
}

class Page {

  String pageid, file
}

//=======================================================

def lastupdate = String.format("%te %<tB %<tY", new GregorianCalendar())

def basedir = project.properties['basedir']

def templatecontent = new File(basedir + 'site/pagetemplate.html').text

def menus = [
        new Menu(name: 'Menu', items: [
                new MenuItem(title: 'Overview', pageid: 'overview'),
                new MenuItem(title: 'Download', pageid: 'download'),
                new MenuItem(title: 'Features', pageid: 'features'),
                new MenuItem(title: 'Mission Statement', pageid: 'missionstatement'),
                new MenuItem(title: 'NoSQL', pageid: 'nosql'),
                new MenuItem(title: 'Sponsors', pageid: 'sponsors'),
                new MenuItem(title: 'Team', pageid: 'team'),
                new MenuItem(title: 'Development', pageid: 'development'),
                new MenuItem(title: 'Support', pageid: 'support'),
                new MenuItem(title: 'Blog', url: 'http://pveentjer.wordpress.com'),
                new MenuItem(title: 'License', pageid: 'license')
        ]),

        new Menu(name: 'Documentation', items: [
                new MenuItem(title: 'Overview', pageid: 'documentationoverview'),
                new MenuItem(title: 'Reference Manual', pageid: 'manual'),
                new MenuItem(title: 'Javadoc', url: 'http://multiverse.codehaus.org/maven-site/apidocs/')
        ])
]

//this is redundant information, all pages can be derived from the menu.
def pages = [
        new Page(pageid: 'overview'),
        new Page(pageid: 'nosql'),
        new Page(pageid: 'flyingstart'),
        new Page(pageid: 'architecture'),
        new Page(pageid: '60second'),
        new Page(pageid: 'manual'),
        new Page(pageid: 'manual-blocking'),
        new Page(pageid: 'manual-isolation'),
        new Page(pageid: 'manual-api'),
        new Page(pageid: 'manual-mapping'),
        new Page(pageid: 'manual-lifecycleevents'),
        new Page(pageid: 'manual-referencesandprimitives'),
        new Page(pageid: 'manual-jmm'),
        new Page(pageid: 'manual-2phasecommit'),
        new Page(pageid: 'manual-templates'),
        new Page(pageid: 'manual-javaagent'),
        new Page(pageid: 'manual-compiler'),
        new Page(pageid: 'contact'),
        new Page(pageid: 'missionstatement'),
        new Page(pageid: 'download'),
        new Page(pageid: 'guide-misc'),
        new Page(pageid: 'license'),
        new Page(pageid: 'features'),
        new Page(pageid: '0.3.release'),
        new Page(pageid: '0.4.release'),
        new Page(pageid: '0.5.release'),
        new Page(pageid: '0.6.release'),
        new Page(pageid: 'faq'),
        new Page(pageid: 'team'),
        new Page(pageid: 'development'),
        new Page(pageid: 'sponsors'),
        new Page(pageid: 'tutorial'),
        new Page(pageid: 'index'),
        new Page(pageid: 'setup-javaagent'),
        new Page(pageid: 'support'),
        new Page(pageid: 'developconfiguration'),
        new Page(pageid: 'otherjvmlanguages'),
        new Page(pageid: 'documentationoverview')
]

def outputdirectory = "$basedir/target/site"

//=============== template engine ==================

println('starting')
def outputdirectoryfile = new File(outputdirectory)
if (!outputdirectoryfile.exists()) {
  if (!outputdirectoryfile.mkdirs()) {
    throw new Exception("file could not be created $outputdirectory")
  }
}

for (page in pages) {
  def filename = "${page.pageid}.html"
  def engine = new groovy.text.GStringTemplateEngine()
  def template = engine.createTemplate(templatecontent)
  def pagecontent = new File("$basedir/site/$filename").text
  def binding = [menus: menus,
          pagecontent: pagecontent,
          page: page,
          lastupdate: lastupdate]
  def result = template.make(binding).toString()
  def output = new File("$outputdirectory/$filename")
  println(output)
  output.createNewFile()
  output.write(result)
}

def output = new File("$outputdirectory/style.css")
output.createNewFile()
output.write(new File("${basedir}/site/style.css").text)

def settingsxml = new File("$outputdirectory/settings.xml")
settingsxml.createNewFile()
settingsxml.write(new File("${basedir}/site/settings.xml").text)

def pomxml = new File("$outputdirectory/pom.xml")
pomxml.createNewFile()
pomxml.write(new File("${basedir}/site/pom.xml").text)


println('finished')
