/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.pq;

import common.AbstractLaneGroup;
import common.AbstractVehicle;
import keys.KeyCommPathOrLink;
import packet.AbstractPacketLaneGroup;
import packet.PacketLink;
import packet.PartialVehicleMemory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PacketLaneGroup extends AbstractPacketLaneGroup {

    public Set<AbstractVehicle> vehicles=new HashSet<>();
    public PartialVehicleMemory pvm = new PartialVehicleMemory();

    // TODO: THis should also carry the partial vehicle memory, which should be accounted for in add_vehicles
    // TODO: The partial vehicle memory should live in the pq LaneGroup

    public PacketLaneGroup(){}

    public PacketLaneGroup(Set<AbstractLaneGroup> target_lanegroups){
        super(target_lanegroups);
    }

    @Override
    public boolean isEmpty(){
        return vehicles==null || vehicles.isEmpty();
    }

    @Override
    public void add_link_packet(PacketLink vp) {
        if(vp.vehicles!=null)
            vp.vehicles.forEach(v->add_micro(v.get_key(),v));
        if(vp.state2vehicles!=null)
            vp.state2vehicles.forEach( (k,v)->add_macro(k,v));
    }

    @Override
    public void add_macro(KeyCommPathOrLink key, Double value) {

        double add_value = pvm.get_value(key) + value;
        int veh = (int) add_value;

        // create vehicles
        for(int i=0;i<veh;i++)
            vehicles.add(new Vehicle(key,null));

        // update pvm
        pvm.set_value(key,add_value - veh);
    }

    @Override
    public void add_micro(KeyCommPathOrLink key, AbstractVehicle vehicle) {
        vehicles.add(vehicle);
    }

    @Override
    public AbstractPacketLaneGroup times(double x) {
        // this should nevere be called!
        return null;
    }

}
