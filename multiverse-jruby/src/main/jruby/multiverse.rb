require "java"
require "MultiverseJRuby.jar"
require "multiverse-alpha-unborn-0.6-SNAPSHOT.jar"

import "org.test.jruby.multiverse.MultiverseLibrary"
import "org.multiverse.api.programmatic.ProgrammaticReference"
import "org.multiverse.api.GlobalStmInstance"
import org.multiverse.api.StmUtils;
 
stm_lib = MultiverseLibrary.new
stm_lib.load(JRuby.runtime, true)


def Ref
  return GlobalStmInstance.getGlobalStmInstance().getProgrammaticReferenceFactoryBuilder().build().atomicCreateReference()
end

def Retry
  StmUtils.retry()
end

