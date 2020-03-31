package org.noise_planet.noisemodelling.wps

import groovy.sql.Sql
import org.h2gis.functions.io.shp.SHPRead
import org.h2gis.functions.io.shp.SHPWrite
import org.noise_planet.noisemodelling.wps.Receivers.Building_Grid

class TestReceivers extends JdbcTestCase {



    public void testBuildingGrid() {
        def sql = new Sql(connection)

        SHPRead.readShape(connection, TestReceivers.getResource("buildings.shp").getPath())
        sql.execute("CREATE SPATIAL INDEX ON BUILDINGS(THE_GEOM)")
        sql.execute("CREATE INDEX bheight ON BUILDINGS(height)")

        SHPRead.readShape(connection, TestReceivers.getResource("roads.shp").getPath())

        new Building_Grid().exec(connection,  ["tableBuilding" : "BUILDINGS",
                                               "delta" : 5,
                                                "height" : 6,
                                               "sourcesTableName" : "ROADS",
                                               "fenceTableName" : "BUILDINGS"])


        def receivers_in_buildings = sql.firstRow("SELECT COUNT(*) from receivers r, buildings b where r.the_geom && b.the_geom and st_intersects(r.the_geom, b.the_geom) and ST_Z(r.the_geom) < b.height ")[0] as Integer
        assertEquals(0, receivers_in_buildings)

        // SHPWrite.exportTable(connection, "target/receivers.shp", "RECEIVERS")
    }

    public void testBuildingGridWithPop() {
        def sql = new Sql(connection)

        SHPRead.readShape(connection, TestReceivers.getResource("buildings.shp").getPath(), "BUILDINGS_NOPOP")
        sql.execute("DROP TABLE IF EXISTS BUILDINGS")
        sql.execute("CREATE TABLE BUILDINGS(pk serial, the_geom geometry, height double, pop double) AS SELECT pk, the_geom, height, ST_AREA(THE_GEOM) / 15 as pop from buildings_nopop")

        SHPRead.readShape(connection, TestReceivers.getResource("roads.shp").getPath())

        new Building_Grid().exec(connection,  ["tableBuilding" : "BUILDINGS",
                                               "delta" : 5,
                                               "height" : 6,
                                               "sourcesTableName" : "ROADS",
                                               "fenceTableName" : "BUILDINGS"])
        //SHPWrite.exportTable(connection, "target/receivers.shp", "RECEIVERS")

        def receivers_pop = sql.firstRow("SELECT sum(pop) from receivers")[0] as Double

        def buildings_pop = sql.firstRow("SELECT sum(pop) from buildings")[0] as Double

        assertEquals(0, buildings_pop - receivers_pop, 0.1);
    }
}
