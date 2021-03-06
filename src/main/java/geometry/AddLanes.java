/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package geometry;

import error.OTMErrorLog;

import java.util.TreeSet;

public class AddLanes {

    public enum Side {in, out}
    public enum Position {up,dn}

    public int lanes;
    public Side side;
    public boolean isopen;
    public Position position;
    public float length;        // meters
    public TreeSet<Gate> gates = new TreeSet<>();    // sorted set

    public AddLanes(AddLanes.Position pos,AddLanes.Side side){
        this.lanes = 0;
        this.side = side;
        this.position = pos;
        this.length = 0f;
    }

    public AddLanes(jaxb.AddLanes jaxb_al) {

        if(jaxb_al==null){
            this.lanes = 0;
            this.length = 0;
            return;
        }

        this.lanes = jaxb_al.getLanes();
        this.side = Side.valueOf(jaxb_al.getSide().toLowerCase());
        this.isopen = jaxb_al.isIsopen();

        boolean has_start = jaxb_al.getStartPos()!=null && !jaxb_al.getStartPos().isNaN();
        boolean has_end = jaxb_al.getEndPos()!=null && !jaxb_al.getEndPos().isNaN();
        if( has_start & !has_end ){
            this.position = Position.dn;
            this.length = Math.abs(jaxb_al.getStartPos());
        }
        else if( has_end & !has_start ){
            this.position = Position.up;
            this.length = Math.abs(jaxb_al.getEndPos());
        }
        else{
            // this is an error condition
        }

        if(jaxb_al.getGates()!=null)
            for(jaxb.Gate jaxb_gate : jaxb_al.getGates().getGate())
                gates.add(new Gate(jaxb_gate));
    }

    public boolean isUp(){
        return this.position.equals(Position.up);
    }

    public boolean inIn(){
        return this.side.equals(Side.in);
    }

    public void validate(OTMErrorLog errorLog){
        if(side==null)
            errorLog.addError("No side specified");
//        if(lanes<=0)
//            scenario.error_log.addError("lanes<=0");

        // gates mustn't overlap
        // assume they were correctly inserted in order
        if(!gates.isEmpty()){

            // gates are within the addlane
            if( gates.first().start_pos<0 )
                errorLog.addError("gates.first().start_pos<0");

            if( gates.last().end_pos>this.length)
                errorLog.addError("gates.last().end_pos>this.length");

            // validate gates
            for(Gate gate : this.gates)
                gate.validate(errorLog);

            float prev_end = -1f;
            for(Gate gate : this.gates){
                if (gate.start_pos < prev_end)
                    errorLog.addError("gate.start_pos < prev_end");
                prev_end = gate.end_pos;
            }
        }
    }

    public jaxb.AddLanes to_jaxb(){
        jaxb.AddLanes j1 = new jaxb.AddLanes();
        if(position.equals(Position.dn))
            j1.setStartPos(this.length);
        else
            j1.setEndPos(this.length);
        j1.setLanes(lanes);
        j1.setIsopen(isopen);
        j1.setSide(side.toString());
        if(gates!=null && !gates.isEmpty()) {
            jaxb.Gates jgates = new jaxb.Gates();
            j1.setGates(jgates);
            for (Gate gate : gates) {
                jaxb.Gate jgate = new jaxb.Gate();
                jgates.getGate().add(jgate);
                jgate.setStartPos(gate.start_pos);
                jgate.setEndPos(gate.end_pos);
            }
        }
        return j1;
    }
}
