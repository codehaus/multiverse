dependencies_dir = File.join(File.dirname(__FILE__), "..", "dependencies")
$LOAD_PATH.unshift(dependencies_dir) unless $LOAD_PATH.include? dependencies_dir


require "java"
require "multiverse-jruby-0.5.3-SNAPSHOT.jar"
require "multiverse-alpha-0.5.3-SNAPSHOT.jar"
#require "multiverse-alpha-unborn-0.6-SNAPSHOT.jar"

import "org.multiverse.integration.jruby.MultiverseLibrary"

import "org.multiverse.api.StmUtils"
import "org.multiverse.transactional.refs.BasicRef"
import "org.multiverse.transactional.refs.IntRef"
import "org.multiverse.transactional.refs.FloatRef"
import "org.multiverse.transactional.refs.LongRef"
 
stm_lib = MultiverseLibrary.new
stm_lib.load(JRuby.runtime, true)


def Ref
  return BasicRef.new
end

def IntRef
  return IntRef.new
end

def Retry
  StmUtils.retry()
end

