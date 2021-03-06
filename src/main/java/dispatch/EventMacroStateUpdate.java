/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package dispatch;

import common.Network;
import error.OTMException;

public class EventMacroStateUpdate extends AbstractEvent  {

    public EventMacroStateUpdate(Dispatcher dispatcher, float timestamp, Object network){
        super(dispatcher,4,timestamp,network);
    }

    @Override
    public void action(boolean verbose) throws OTMException {

        super.action(verbose);

        Network network = (Network)recipient;

        // update the models.ctm state
        network.update_macro_state(timestamp);

        // register next clock tick
        float next_timestamp = timestamp+network.scenario.sim_dt;
        if(next_timestamp<=dispatcher.stop_time)
            dispatcher.register_event(new EventMacroStateUpdate(dispatcher,next_timestamp,network));
    }

}
