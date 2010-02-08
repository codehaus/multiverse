package org.benchy.graph;

/**
 * Responsible for outputting a GraphModel (a model containing all data for a graph).
 *
 * @author Peter Veentjer.
 */
public interface GraphWriter {

    void write(GraphModel graphModel);
}
