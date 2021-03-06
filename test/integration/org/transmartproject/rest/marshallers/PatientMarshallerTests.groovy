/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.rest.marshallers

import grails.converters.JSON
import groovy.json.JsonSlurper
import org.codehaus.groovy.grails.web.mime.MimeType
import org.junit.Test
import org.springframework.beans.MutablePropertyValues
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.Sex
import org.transmartproject.rest.test.StubStudyLoadingService

import java.text.DateFormat
import java.text.SimpleDateFormat

import static org.codehaus.groovy.grails.test.runner.phase.IntegrationTestPhaseConfigurer.getCurrentApplicationContext
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.transmartproject.rest.test.StubStudyLoadingService.createStudy

class PatientMarshallerTests {

    private static final String ONTOLOGY_KEY = '\\\\foo bar\\foo\\test_study\\'

    private static final long ID               = 42L
    private static final String TRIAL          = 'TEST_STUDY'
    private static final String TRIAL_LC       = TRIAL.toLowerCase(Locale.ENGLISH)
    private static final String SUBJECT_ID     = 'SUBJECT_43'
    private static final Date BIRTH_DATE       = Date.parseToStringDate('Fri Feb 14 11:44:24 CET 2014')
    private static final Sex SEX               = Sex.MALE
    private static final DEATH_DATE            = null
    private static final long AGE              = 44L
    private static final String RACE           = 'Caucasian'
    private static final String MARITAL_STATUS = 'Married'
    private static final String RELIGION       = 'Judaism'

    Patient getMockPatient() {
        [
                getId:            { -> ID },
                getTrial:         { -> TRIAL },
                getInTrialId:     { -> SUBJECT_ID },
                getSex:           { -> SEX },
                getBirthDate:     { -> BIRTH_DATE },
                getDeathDate:     { -> DEATH_DATE },
                getAge:           { -> AGE },
                getRace:          { -> RACE },
                getMaritalStatus: { -> MARITAL_STATUS },
                getReligion:      { -> RELIGION },
        ] as Patient
    }

    @Test
    void basicTest() {
        def json = mockPatient as JSON

        JsonSlurper slurper = new JsonSlurper()
        println json.toString()

        assertThat slurper.parseText(json.toString()), allOf(
                hasEntry('id',            ID as Integer),
                hasEntry('trial',         TRIAL),
                hasEntry('inTrialId',     SUBJECT_ID),
                hasEntry('birthDate',     formatAsISO(BIRTH_DATE)),
                hasEntry('sex',           SEX.name()),
                hasEntry(is('deathDate'), is(nullValue())),
                hasEntry('age',           AGE as Integer),
                hasEntry('race',          RACE),
                hasEntry('maritalStatus', MARITAL_STATUS),
                hasEntry('religion',      RELIGION))
    }

    private static String formatAsISO(Date date) {
        TimeZone tz = TimeZone.getTimeZone 'UTC'
        DateFormat df = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ss\'Z\'')
        df.timeZone = tz
        df.format date
    }

    void replaceStudyLoadingService() {
        // studyLoadingServiceProxy will start proxying to this bean
        currentApplicationContext.registerBeanDefinition(
                'transmartRestApiStudyLoadingService',
                new GenericBeanDefinition(
                        beanClass:     StubStudyLoadingService,
                        propertyValues: new MutablePropertyValues(storedStudy: createStudy(TRIAL, ONTOLOGY_KEY))))
    }

    @Test
    void testHal() {
        replaceStudyLoadingService()

        def json = new JSON()
        json.contentType = MimeType.HAL_JSON.name
        json.target = mockPatient

        def stringResult = json.toString()
        println stringResult

        JsonSlurper slurper = new JsonSlurper()
        assertThat slurper.parseText(stringResult), allOf(
                hasEntry('age',           AGE as Integer),
                hasEntry('race',          RACE),
                hasEntry('maritalStatus', MARITAL_STATUS),
                // do not test the rest
                hasEntry(is('_links'),
                        hasEntry(is('self'),
                                hasEntry('href', "/studies/$TRIAL_LC/subjects/$ID".toString()))))
    }

}
