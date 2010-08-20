package org.multiverse.integration

import static org.multiverse.integration.MultiverseGroovyLibrary.*

/**
 * Created by IntelliJ IDEA.
 * User: hari
 * Date: 3 Jul, 2010
 * Time: 8:29:09 PM
 * To change this template use File | Settings | File Templates.
 */

public class OrElseStatement {

  private Closure either
  private Closure orelse
  private Map configuration = [:]

  void either(Closure closure) {
    either = closure
  }

  void orelse(Closure closure) {
    orelse = closure
  }

  void config(Map config) {
    configuration = config
  }

  void execute() {
    orElseTransaction(configuration, either, orelse)
  }

}