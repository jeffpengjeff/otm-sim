/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package geometry;

import error.OTMErrorLog;
import runner.InterfaceScenarioElement;
import runner.ScenarioElementType;

import java.util.List;

public class RoadGeometry implements InterfaceScenarioElement {

    final public long id;
    public AddLanes up_in = new AddLanes(AddLanes.Position.up,AddLanes.Side.in);
    public AddLanes up_out = new AddLanes(AddLanes.Position.up,AddLanes.Side.out);
    public AddLanes dn_in = new AddLanes(AddLanes.Position.dn,AddLanes.Side.in);
    public AddLanes dn_out = new AddLanes(AddLanes.Position.dn,AddLanes.Side.out);

    public RoadGeometry(jaxb.Roadgeom jaxb_geom) {

        // null road geom
        if(jaxb_geom==null) {
            id = Long.MIN_VALUE;
            return;
        }

        this.id = jaxb_geom.getId();

        // Warning: this does not check for repetitions
        for(jaxb.AddLanes jaxb_al : jaxb_geom.getAddLanes()){
            AddLanes addlane = new AddLanes(jaxb_al);
            if(addlane.isUp())
                if(addlane.inIn())
                    up_in = addlane;
                else
                    up_out = addlane;
            else
                if(addlane.inIn())
                    dn_in = addlane;
                else
                    dn_out = addlane;

        }
    }

    public void validate(OTMErrorLog errorLog){
        if( up_in.lanes>0 )
            errorLog.addError("Dont know how to deal with upstream addlanes");
        if( up_out.lanes>0 )
            errorLog.addError("Dont know how to deal with upstream addlanes");
        if( dn_in !=null )
            dn_in.validate(errorLog);
        if( dn_out !=null )
            dn_out.validate(errorLog);
    }

    public jaxb.Roadgeom to_jaxb(){
        jaxb.Roadgeom jgeom = new jaxb.Roadgeom();
        jgeom.setId(this.id);
        List<jaxb.AddLanes> jaddlanes = jgeom.getAddLanes();
        if(this.up_in.lanes>0)
            jaddlanes.add(this.up_in.to_jaxb());
        if(this.dn_in.lanes>0)
            jaddlanes.add(this.dn_in.to_jaxb());
        if(this.up_out.lanes>0)
            jaddlanes.add(this.up_out.to_jaxb());
        if(this.dn_out.lanes>0)
            jaddlanes.add(this.dn_out.to_jaxb());
        return jgeom;
    }

    ////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public ScenarioElementType getScenarioElementType() {
        return ScenarioElementType.roadgeom;
    }

}
