/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.info;

import geometry.AddLanes;
import geometry.Gate;

import java.util.ArrayList;

public class AddLanesInfo {

    /** Number of additional lane */
    public int lanes;

    /** Side of the road: "in" or "out" */
    public String side;

    /** ???? */
    public boolean isopen;

    /** Position within the link of the additional lanes: "ulgs" or "dlgs",
     * corresponding roughly to lane drops and turn pockets, respectively. */
    public String position;

    /** Length in meters of the additional lanes */
    public float length;

    /** List of HOV gates */
    public ArrayList<GateInfo> gates;

    public AddLanesInfo(AddLanes x){
        this.lanes = x.lanes;
        this.side = x.side.toString();
        this.isopen = x.isopen;
        this.position = x.position.toString();
        this.length = x.length;
        this.gates = new ArrayList<>();
        for(Gate gate : x.gates)
            this.gates.add(new GateInfo(gate));
    }

    public int getLanes() {
        return lanes;
    }

    public String getSide() {
        return side;
    }

    public boolean isIsopen() {
        return isopen;
    }

    public String getPosition() {
        return position;
    }

    public float getLength() {
        return length;
    }

    public ArrayList<GateInfo> getGates() {
        return gates;
    }

    @Override
    public String toString() {
        return "AddLanesInfo{" +
                "lanes=" + lanes +
                ", side='" + side + '\'' +
                ", isopen=" + isopen +
                ", position='" + position + '\'' +
                ", length=" + length +
                ", gates=" + gates +
                '}';
    }
}
