require 'rubygems'
require 'rake/gempackagetask'


spec = Gem::Specification.new do |s|
  s.name = %q{multiverse-jruby}
  s.version = "0.1.0"

  s.specification_version = 1.0 if s.respond_to? :specification_version=

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Sai Venkatakrishnan"]
  s.date = %q{2010-05-31}
  s.description = %q{A Software Transactional Memory for JRuby based on Multiverse.}
  s.email = %q{s.sai.venkat@gmail.com}
  libs = Dir.glob(["lib/*.rb","lib/*/*.rb"])
  jars = Dir.glob(["dependencies/*.jar"])
  examples = Dir.glob("examples/*.rb")
  common_files = Dir.glob("*.rb")
  files = libs << examples << jars << common_files
  s.files = files.flatten
  s.has_rdoc = false
  s.homepage = %q{http://multiverse.codehaus.org/}
  s.require_paths = ["lib","."]
  s.summary = %q{A Software Transactional Memory for JRuby based on Multiverse.}
end

Rake::GemPackageTask.new(spec) do |pkg|
    pkg.need_tar = true
end

