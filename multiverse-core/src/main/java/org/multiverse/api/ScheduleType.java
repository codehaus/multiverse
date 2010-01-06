package org.multiverse.api;

/**
 * An enumeration for all possible moments a scheduled task can be executed.
 *
 * @author Peter Veentjer. 
 */
public enum ScheduleType {

    preAbort, preCommit, postAbort, postCommit
}
