/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package dispatch;

import common.AbstractSource;
import error.OTMException;
import models.pq.Source;

public class EventCreateVehicle extends AbstractEvent {

    public EventCreateVehicle(Dispatcher dispatcher, float timestamp, AbstractSource source) {
        super(dispatcher,0, timestamp,source);
    }

    @Override
    public void action(boolean verbose) throws OTMException {
        super.action(verbose);
        Source source = (Source)recipient;
        source.insert_vehicle(timestamp);
        source.schedule_next_vehicle(dispatcher,timestamp);
    }

}
