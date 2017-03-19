package com.oberasoftware.max.core.behaviours.gripper;

import com.oberasoftware.max.core.behaviours.Behaviour;

/**
 * @author renarj
 */
public interface GripperBehaviour extends Behaviour {
    void open();

    void open(int percentage);

    void rest();

    void close();

    void close(int percentage);
}
