/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package control.sigint;

import error.OTMErrorLog;
import utils.OTMUtils;

import java.util.HashSet;
import java.util.Set;

public class Stage implements Comparable<Stage> {

    public int order;
    public float duration;          // duration in seconds of the stage, including
    public Set<Long> phase_ids;
    public float cycle_starttime;   // start time of this stage relative to

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Stage(jaxb.Stage jaxb_stage){
        this.order = jaxb_stage.getOrder();
        this.duration = jaxb_stage.getDuration();

        phase_ids = new HashSet<>();
        phase_ids.addAll(OTMUtils.csv2longlist(jaxb_stage.getPhases()));
    }

    public void validate(OTMErrorLog errorLog){

//        // positivity
//        if(yellow_time<0)
//            errorLog.addError("yellow_time<0");
//
//        if(red_clear_time<0)
//            errorLog.addError("red_clear_time<0");
//
//        if(duration-yellow_time-red_clear_time<0)
//            errorLog.addError("duration-yellow_time-red_clear_time<0");

        if(cycle_starttime<0)
            errorLog.addError("cycle_starttime<0");

//        if(phase_ids.isEmpty())
//            errorLog.addError("phases.isEmpty()");
//
//        if(phase_ids.contains(null))
//            errorLog.addError("phases.contains(null)");
    }

    @Override
    public int compareTo(Stage other) {
        return Integer.compare(this.order, other.order);
    }

}
