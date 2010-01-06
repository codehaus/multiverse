package org.multiverse.utils.commitlock;

/**
 * The result of acquiring a commit lock: <ol> <li><b>success</b> if the lock was acquired successfully</li>
 * <li><b>failure</b> it the lock was not obtained</li> <li><b>conflict</b> if the lock was not obtained because a
 * writeconflict already is detected. Depends on the implementation of the CommitLock is this value is really
 * returned.</li> </ol>
 *
 * @author Peter Veentjer.
 */
public enum CommitLockResult {

    success, failure, conflict
}
