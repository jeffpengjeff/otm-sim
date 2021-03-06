/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package dispatch;

import error.OTMException;
import models.pq.LaneGroup;
import models.pq.Vehicle;

public class EventTransitToWaiting extends AbstractEvent {

    public EventTransitToWaiting(Dispatcher dispatcher,float timestamp, Object vehicle) {
        super(dispatcher,0,timestamp,vehicle);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        Vehicle vehicle = (Vehicle)recipient;
        LaneGroup lanegroup = (LaneGroup) vehicle.get_lanegroup();
        if(verbose)
            System.out.println(String.format("\tvehicle %d, link %d, lanegroup %d",vehicle.getId(),lanegroup.link.getId(),lanegroup.id));

        vehicle.move_to_queue(timestamp,lanegroup.waiting_queue);
    }

}
