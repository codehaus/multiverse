package org.multiverse.api;

/**
 * With the PropagationLevel you have control on how nesting of transaction happens.
 *
 * @author Peter Veentjer.
 */
public enum PropagationLevel {

    RequiresNew, Mandatory, Requires, Supports, Never
}
