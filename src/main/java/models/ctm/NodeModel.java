/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.ctm;

import common.AbstractLaneGroup;
import common.Link;
import common.Node;
import error.OTMErrorLog;
import keys.KeyCommPathOrLink;
import runner.Scenario;

import java.util.*;

public class NodeModel {

    private static int MAX_ITERATIONS = 10;
    public static double eps = 1e-3;

    private boolean doprint = false;

    public Node node;

    public Map<Long,UpLaneGroup> ulgs;  // upstrm lane groups.
    public Map<Long,RoadConnection> rcs;  // road connections.
    public Map<Long,DnLaneGroup> dlgs; /// dnstrm lane groups.

    public NodeModel(Node node) {

        this.node = node;
        rcs = new HashMap<>();

        // if the node has no road connections, it is because it is a source node, a sink node,
        // or a many-to-one node. NodeModels are not constructed for sources and sinks, so it
        // must be many-to-one.
        if(node.road_connections.isEmpty()){

            assert(!node.is_sink && !node.is_source);
            assert(node.is_many2one);

            // currently works only for one-to-one
            // TODO: GENERALIZE THIS FOR MANY-TO-ONE

            // there is only one upstream link
            assert(node.in_links.size()==1);
            Link up_link = node.in_links.values().iterator().next();

            // there is only one dnstream link
            assert(node.out_links.size()==1);
            Link dn_link = node.out_links.values().iterator().next();

            // there is only one upstream lanegroup
            assert(up_link.lanegroups.size()==1);
            models.ctm.LaneGroup up_lanegroup = (models.ctm.LaneGroup) up_link.lanegroups.values().iterator().next();

            // there is only one dnstream lanegroup
            assert(dn_link.lanegroups.size()==1);
            models.ctm.LaneGroup dn_lanegroup = (models.ctm.LaneGroup) dn_link.lanegroups.values().iterator().next();

            // add a fictitious road connection with id 0
            RoadConnection rc = new RoadConnection(0L,null);
            rcs.put(0L,rc);

            // ulgs
            ulgs = new HashMap<>();
            UpLaneGroup ulg = new UpLaneGroup(up_lanegroup);
            ulgs.put(up_lanegroup.id,ulg);
            ulg.add_road_connection(rc);
            rc.add_up_lanegroup(ulg);

            // dlgs
            dlgs = new HashMap<>();
            DnLaneGroup dlg = new DnLaneGroup(dn_lanegroup);
            dlgs.put(dn_lanegroup.id,dlg);
            rc.add_dn_lanegroup(dlg);
            dlg.add_road_connection(rc);

            return;
        }

        // case: complete road connections (could be many-to-one) ............................................

        Map<Long,UpLaneGroup> up_lgs_map = new HashMap<>();
        Map<Long,DnLaneGroup> dn_lgs_map = new HashMap<>();

        // iterate through the road connections
        for (common.RoadConnection xrc : node.road_connections) {

            // skip road connections starting or ending in non-macro links
            if( xrc.start_link.model_type!=Link.ModelType.mn && xrc.start_link.model_type!=Link.ModelType.ctm )
                continue;

            // skip if it is disconnected
            if( xrc.in_lanegroups.isEmpty() || xrc.out_lanegroups.isEmpty())
                continue;

            RoadConnection rc = new RoadConnection(xrc.getId(),xrc);
            rcs.put(xrc.getId(),rc);

            // go through its upstream lanegroups
            for (AbstractLaneGroup xup_lg : xrc.in_lanegroups) {
                UpLaneGroup ulg;
                if (!up_lgs_map.containsKey(xup_lg.id)) {
                    ulg = new UpLaneGroup((models.ctm.LaneGroup) xup_lg);
                    up_lgs_map.put(xup_lg.id, ulg);
                } else
                    ulg = up_lgs_map.get(xup_lg.id);
                ulg.add_road_connection(rc);
                rc.add_up_lanegroup(ulg);
            }

            // go through its downstream lanegroups
            for (AbstractLaneGroup xdn_lg : xrc.out_lanegroups) {
                DnLaneGroup dlg;
                if (!dn_lgs_map.containsKey(xdn_lg.id)) {
                    dlg = new DnLaneGroup((models.ctm.LaneGroup) xdn_lg);
                    dn_lgs_map.put(xdn_lg.id, dlg);
                } else
                    dlg = dn_lgs_map.get(xdn_lg.id);
                rc.add_dn_lanegroup(dlg);
                dlg.add_road_connection(rc);
            }

        }
        ulgs = up_lgs_map;
        dlgs = dn_lgs_map;
    }

    public void validate(OTMErrorLog errorLog) {

    }

    public void initialize(Scenario scenario) {
        // allocate states in ulgs
        ulgs.values().forEach( ulg -> ulg.lg.states.forEach( state -> ulg.add_state(state)));
    }

    public void update_flow(float timestamp) {


        if(doprint && node.getId()==3L)
            System.out.println(timestamp + " -----------------------------------------------------------");

        // reset
        ulgs.values().forEach(x->x.reset());
        rcs.values().forEach(x->x.reset(node.network.scenario.sim_dt));
        dlgs.values().forEach(x->x.reset());

        // iteration
        int it = 0;
        while (it++ <= MAX_ITERATIONS) {
            step0();
            if (eval_stop(it))
                break;


            if(doprint && node.getId()==3L)
                System.out.println("**** ITERATION " + it + " ********");


            step1();
            step2();
            step3();
            step4();
            step5();
            step6();
        }

        // update flow accumulators
        // TODO CHECK THIS
//        update_flow_accumulators();
    }

    private boolean eval_stop(int iteration){

        if(iteration>MAX_ITERATIONS){
            System.out.println("Reached iteration limit");
            return true;
        }

        // stop if all ulgs lanegroups are either empty or blocked
        return ulgs.values().stream().allMatch(ulg->ulg.is_empty_or_blocked);
    }

    private void step0(){
        /**
         * dlgs.is_blocked
         * rcs.is_blocked
         * uplgs.is_empty_or_blocked
         */

        // block downstream lanegroups with zero supply
        dlgs.values().forEach(dlg -> dlg.update_is_blocked());

        // block road connections connecting to blocked links or with control rate = 0
        rcs.values().forEach(rc->rc.update_is_blocked());

        // upstream lanegroup is empty if sum of demands is zero
        // and blocked if any of it connectors is blocked
        ulgs.values().forEach(ulg->ulg.update_is_empty_or_blocked());

    }

    private void step1(){
        /** d_r, alpha_rj
         */

        for(RoadConnection rc : rcs.values()) {

            rc.d_r = rc.is_blocked ? 0d : rc.ulgs.stream()
                                            .mapToDouble(x -> x.rc_infos.get(rc.id).d_ir).sum();

            if(rc.d_r<NodeModel.eps)
                continue;

            // s_r: downstream supply seen by this road connection
            double s_r = rc.dnlg_infos.values().stream()
                           .mapToDouble(x->x.lambda_rj*x.dlg.s_j).sum();

            // alpha_rj: distribution amongst downstream lanegroups
            rc.dnlg_infos.values().forEach( x -> x.alpha_rj = s_r==0d ? 0d : x.lambda_rj * x.dlg.s_j / s_r);


            /////////////////////////////////
            if(doprint && node.getId()==3L){
                System.out.println("1 d_r rc from link " + rc.rc.start_link.getId() + " = " + rc.d_r);
                rc.dnlg_infos.values().forEach( x -> System.out.println("\t" + x.dlg.lg.lanes.size() + " alphar_rj " + x.alpha_rj) );
            }
            /////////////////////////////////

        }


    }

    private void step2(){
        /**  gamma_j */

        for(DnLaneGroup dlg : dlgs.values() ) {

            // for MN model
            if(Double.isInfinite(dlg.s_j))
                dlg.gamma_j = 0d;

            else if (dlg.is_blocked)
                dlg.gamma_j = 1d;

            else {

                // total_demand = sum of demands in upstream road connections, times the proportion
                // directed at this lanegroup
                Double d_j = dlg.rcs.values().stream().mapToDouble(rc -> rc.dnlg_infos.get(dlg.lg.id).alpha_rj * Math.min(rc.d_r, rc.fbar)).sum();

                dlg.gamma_j = d_j>dlg.s_j ? 1d-dlg.s_j/d_j : 0d;
            }

            /////////////////////////////////
            if(doprint && node.getId()==3L){
                System.out.println("2 gamma_j lg with lanes " + dlg.lg.lanes.size() + " = " + dlg.gamma_j);
            }
            /////////////////////////////////

        }
    }

    private void step3(){
        /** gamma_r */

        for(RoadConnection rc : rcs.values() ) {

            rc.gamma_r = rc.is_blocked ? 0d : rc.dnlg_infos.values().stream().mapToDouble(x -> x.dlg.gamma_j * x.alpha_rj).sum();

            /////////////////////////////////
            if(doprint && node.getId()==3L){
                System.out.println("3 gamma_r " + rc.gamma_r);
            }
            /////////////////////////////////
        }

    }

    private void step4(){
        /** gamma_i */

        for(UpLaneGroup ulg : ulgs.values() ) {
            ulg.gamma_i = ulg.is_empty_or_blocked ?
                    0d :
                    1d - ulg.rc_infos.values().stream().mapToDouble(rc -> rc.rc.gamma_r).max().getAsDouble();

            ////////////////////////////////
            if(doprint && node.getId()==3L){
                System.out.println("4 gamma_i " + ulg.gamma_i);
            }
            /////////////////////////////////
        }

    }

    private void step5(){
        /** delta_is, f_is */

        for(UpLaneGroup ulg : ulgs.values()){

            if(!ulg.is_empty_or_blocked){

                for( Map.Entry<KeyCommPathOrLink,UpLaneGroup.StateInfo> e : ulg.state_infos.entrySet()){

                    KeyCommPathOrLink state = e.getKey();
                    UpLaneGroup.StateInfo stateInfo = e.getValue();

                    stateInfo.delta_is = stateInfo.d_is * ulg.gamma_i;
                    stateInfo.d_is -= stateInfo.delta_is;
                    ulg.f_is.put(state,ulg.f_is.get(state)+stateInfo.delta_is);

                    // reduce d_ir
                    if(ulg.lg.state2roadconnection.containsKey(state)){
                        Long rc_id = ulg.lg.state2roadconnection.get(state);
                        ulg.rc_infos.get(rc_id).d_ir -= stateInfo.delta_is;
                    }

                    /////////////////////////////////
                    if(doprint && node.getId()==3L){
                        System.out.println("5 delta_is " + stateInfo.delta_is);
                    }
                    /////////////////////////////////

                }
            }



        }

    }

    private void step6(){
        /** delta_rs, f_rs */

        for(RoadConnection rc : rcs.values()){

            for(KeyCommPathOrLink state : rc.f_rs.keySet()){

                // delta_rs, f_rs
                double delta_rs = rc.ulgs.stream()
                        .filter(x->!x.is_empty_or_blocked)
                        .mapToDouble(x->x.state_infos.get(state).delta_is)
                        .sum();

                rc.f_rs.put( state , rc.f_rs.get(state) + delta_rs );

                // discount s_j
                for(RoadConnection.DnLgInfo rc_dnlg : rc.dnlg_infos.values())
                    rc_dnlg.dlg.s_j -= delta_rs * rc_dnlg.alpha_rj;

                /////////////////////////////////
                if(doprint && node.getId()==3L){
                    System.out.println("6 delta_rs from link " + rc.rc.start_link.getId() + " = " + delta_rs);
                }
                /////////////////////////////////
            }

        }
    }

    private void update_flow_accumulators(){

//        for(UpLaneGroup ulg : ulgs){
//
//            if(ulg.lg.commodity_flow_accumulators==null)
//                continue;
//
//            Long lg_id = ulg.lg.id;
//
//            // iterate through the flow accumulators for this lanegroup
//            for(Map.Entry<Long,FlowAccumulator> e : ulg.lg.commodity_flow_accumulators.entrySet()) {
//
//                // compute flow for this commodity
//                Long comm_id = e.getKey();
//
//                double flow_comm = in_flows.containsKey(lg_id) ?
//                        in_flows.get(lg_id).entrySet().stream()
//                                .filter(k -> k.getKey().commodity_id == comm_id)
//                                .mapToDouble(k -> k.getValue())
//                                .sum()
//                        : 0d;
//
//                // increment the accumulators
//                FlowAccumulator fac = e.getValue();
//                fac.increment(flow_comm);
//                if (ulg.lg.global_flow_accumulator != null)
//                    ulg.lg.global_flow_accumulator.increment(flow_comm);
//            }
//
//        }
//
//        // update flow accumulators
//        for(UpLaneGroup ulg : ulgs){
//            if(ulg.lg.commodity_flow_accumulators==null)
//                continue;
//            for(Map.Entry<Long,FlowAccumulator> e : ulg.lg.commodity_flow_accumulators.entrySet()){
//                Long comm_id = e.getKey();
//                double flow = ulg.get_flow_for_commodity(comm_id);
//                FlowAccumulator fac = e.getValue();
//                fac.increment(flow);
//                if(ulg.lg.global_flow_accumulator!=null)
//                    ulg.lg.global_flow_accumulator.increment(flow);
//            }
//        }

    }

}
