/**
 * NoiseModelling is a free and open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 * as part of:
 * the Eval-PDU project (ANR-08-VILL-0005) 2008-2011, funded by the Agence Nationale de la Recherche (French)
 * the CENSE project (ANR-16-CE22-0012) 2017-2021, funded by the Agence Nationale de la Recherche (French)
 * the Nature4cities (N4C) project, funded by European Union’s Horizon 2020 research and innovation programme under grant agreement No 730468
 *
 * Noisemap is distributed under GPL 3 license.
 *
 * Contact: contact@noise-planet.org
 *
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488) and Ifsttar
 * Copyright (C) 2013-2019 Ifsttar and CNRS
 * Copyright (C) 2020 Université Gustave Eiffel and CNRS
 *
 * @Author Nicolas Fortin, Univ Gustave Eiffel
 * @Author Pierre Aumond, Univ Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Database_Manager

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.time.TimeCategory
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation

import java.sql.Connection
import java.sql.Statement

title = 'Delete all database tables'
description = 'Delete all non-system tables of the database.'

inputs = [
        areYouSure: [name: 'Are you sure ?', title: 'Are you sure?', description: 'Are you sure you want to delete all the tables in the database?', type: Boolean.class]
]

outputs = [result: [name: 'Result output string', title: 'Result output string', description: 'This type of result does not allow the blocks to be linked together.',  type: String.class]]

static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def exec(Connection connection, input) {

    // output string, the information given back to the user
    String resultString = null

    // print to command window
    System.out.println('Start : Clean Database')
    def start = new Date()

    // Get name of the table
    Boolean areYouSure  = input['areYouSure'] as Boolean

    // list of the system tables
    List<String> ignorelst = ["SPATIAL_REF_SYS", "GEOMETRY_COLUMNS"]

    if (areYouSure) {
        // Build the result string with every tables
        StringBuilder sb = new StringBuilder()

        // Get every table names
        List<String> tables = JDBCUtilities.getTableNames(connection.getMetaData(), null, "PUBLIC", "%", null)
        // Loop over the tables
        tables.each { t ->
            TableLocation tab = TableLocation.parse(t)
            if (!ignorelst.contains(tab.getTable())) {
                // Add the name of the table in the string builder
                if (sb.size() > 0) {
                    sb.append(" || ")
                }
                sb.append(tab.getTable())

                // Create a connection statement to interact with the database in SQL
                Statement stmt = connection.createStatement()
                // Drop the table
                stmt.execute("drop table if exists " + tab)
            }
        }
        resultString = "The table(s) " + sb.toString() + " was/were dropped."
    } else {
        resultString = "If you're not sure, we won't do anything !"
    }

    // print to command window
    System.out.println('Result : ' + resultString)
    System.out.println('End : Clean Database')
    System.out.println('Duration : ' + TimeCategory.minus( new Date(), start ))

    // print to WPS Builder
    return resultString

}

def run(input) {

    // Get name of the database
    // by default an embedded h2gis database is created
    // Advanced user can replace this database for a postGis or h2Gis server database.
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result: exec(connection, input)]
    }
}