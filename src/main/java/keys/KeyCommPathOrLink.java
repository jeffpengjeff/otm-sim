/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package keys;

import commodity.Commodity;
import commodity.Path;
import common.Link;

import java.util.Objects;

public class KeyCommPathOrLink implements Comparable<KeyCommPathOrLink> {

    public final long commodity_id;
    public final long pathOrlink_id;    // id of either a link or a path
    public final boolean isPath;        // true is pathOrlink_id is path, false otherwise

    public KeyCommPathOrLink(Commodity comm, Path path, Link link) {
        this.commodity_id = comm.getId();
        this.isPath = comm.pathfull;
        this.pathOrlink_id = isPath ? path.getId() : link.getId();
    }

    public KeyCommPathOrLink(long commodity_id, long pathOrlink_id, boolean isPath) {
        this.commodity_id = commodity_id;
        this.pathOrlink_id = pathOrlink_id;
        this.isPath = isPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyCommPathOrLink that = (KeyCommPathOrLink) o;
        return commodity_id == that.commodity_id &&
                pathOrlink_id == that.pathOrlink_id &&
                isPath == that.isPath;
    }

    @Override
    public int hashCode() {
        return Objects.hash(commodity_id, pathOrlink_id, isPath);
    }

    @Override
    public int compareTo(KeyCommPathOrLink that) {

        if(this.isPath && !that.isPath)
            return 1;
        if(!this.isPath && that.isPath)
            return -1;

        if(this.commodity_id>that.commodity_id)
            return 1;
        if(this.commodity_id<that.commodity_id)
            return -1;

        if(this.pathOrlink_id>that.pathOrlink_id)
            return 1;
        if(this.pathOrlink_id<that.pathOrlink_id)
            return -1;

        return 0;
    }
}
