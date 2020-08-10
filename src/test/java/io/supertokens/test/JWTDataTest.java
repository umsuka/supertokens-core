/*
 *    Copyright (c) 2020, VRAI Labs and/or its affiliates. All rights reserved.
 *
 *    This software is licensed under the Apache License, Version 2.0 (the
 *    "License") as published by the Apache Software Foundation.
 *
 *    You may not use this file except in compliance with the License. You may
 *    obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 */

package io.supertokens.test;

import com.google.gson.JsonObject;
import io.supertokens.ProcessState;
import io.supertokens.exceptions.UnauthorisedException;
import io.supertokens.session.Session;
import io.supertokens.session.info.SessionInformationHolder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import static org.junit.Assert.*;

public class JWTDataTest {

    @Rule
    public TestRule watchman = Utils.getOnFailure();

    @AfterClass
    public static void afterTesting() {
        Utils.afterTesting();
    }

    @Before
    public void beforeEach() {
        Utils.reset();
    }

    //*- create session with some JWT payload -> verify to see payload is proper -> change JWT payload using session
    //* handle -> check this is reflected
    @Test
    public void testVerifyJWTPayloadChangePayloadUsingSessionHandle() throws Exception {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        //createSession with JWT payload
        String userId = "userId";
        JsonObject userDataInJWT = new JsonObject();
        userDataInJWT.addProperty("key", "value");
        JsonObject userDataInDatabase = new JsonObject();
        userDataInDatabase.addProperty("key", "value");


        SessionInformationHolder sessionInfo = Session
                .createNewSession(process.getProcess(), userId, userDataInJWT, userDataInDatabase);

        // verify to see payload is proper
        assertEquals(sessionInfo.session.userDataInJWT, userDataInJWT);


        //change JWT payload using session handle
        JsonObject newUserDataInJwt = new JsonObject();
        newUserDataInJwt.addProperty("key", "value2");
        Session.updateSession(process.getProcess(), sessionInfo.session.handle, null, newUserDataInJwt, null);

        //check that this change is reflected

        assertEquals(Session.getJWTData(process.getProcess(), sessionInfo.session.handle), newUserDataInJwt);

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    // *  - create session with some JWT payload -> verify to see payload is proper -> change JWT payload to be empty
    // using
    // *  session handle -> check this is reflected
    @Test
    public void testVerifyJWTPayloadChangeToEmptyPayloadUsingSessionHandle() throws Exception {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        //createSession with JWT payload
        String userId = "userId";
        JsonObject userDataInJWT = new JsonObject();
        userDataInJWT.addProperty("key", "value");
        JsonObject userDataInDatabase = new JsonObject();
        userDataInDatabase.addProperty("key", "value");


        SessionInformationHolder sessionInfo = Session
                .createNewSession(process.getProcess(), userId, userDataInJWT, userDataInDatabase);

        // verify to see payload is proper
        assertEquals(sessionInfo.session.userDataInJWT, userDataInJWT);

        //change JWT payload to be empty using session handle
        JsonObject emptyUserDataInJwt = new JsonObject();
        Session.updateSession(process.getProcess(), sessionInfo.session.handle, null, emptyUserDataInJwt, null);

        //check this is reflected
        assertEquals(Session.getJWTData(process.getProcess(), sessionInfo.session.handle), emptyUserDataInJwt);

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }


    // * - create session with some JWT payload -> verify to see payload is proper -> pass null to
    // changeJWTPayloadInDatabase
    // *  function -> check that JWT payload has not changed is reflected
    @Test
    public void testVerifyJWTPayloadSetPayloadToNullUsingSessionHandle() throws Exception {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        //createSession with JWT payload
        String userId = "userId";
        JsonObject userDataInJWT = new JsonObject();
        userDataInJWT.addProperty("key", "value");
        JsonObject userDataInDatabase = new JsonObject();
        userDataInDatabase.addProperty("key", "value");


        SessionInformationHolder sessionInfo = Session
                .createNewSession(process.getProcess(), userId, userDataInJWT, userDataInDatabase);

        // verify to see payload is proper
        assertEquals(sessionInfo.session.userDataInJWT, userDataInJWT);

        //change JWT payload to be null
        Session.updateSession(process.getProcess(), sessionInfo.session.handle, userDataInDatabase, null, null);

        //check that jwtData does not change
        assertEquals(Session.getJWTData(process.getProcess(), sessionInfo.session.handle), userDataInJWT);

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    // * - create session -> let it expire, remove from db -> call update function -> make sure you get unauthorised 
    // error
    @Test
    public void testExpireSessionCallUpdateAndCheckUnauthorised() throws Exception {
        String[] args = {"../"};

        Utils.setValueInConfig("access_token_validity", "1");// 1 second validity
        Utils.setValueInConfig("refresh_token_validity", "" + 1.0 / 60);// 1 second validity (value in mins)

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        //createSession with JWT payload
        String userId = "userId";
        JsonObject userDataInJWT = new JsonObject();
        userDataInJWT.addProperty("key", "value");
        JsonObject userDataInDatabase = new JsonObject();
        userDataInDatabase.addProperty("key", "value");


        SessionInformationHolder sessionInfo = Session
                .createNewSession(process.getProcess(), userId, userDataInJWT, userDataInDatabase);

        //let it expire, remove from db

        Thread.sleep(2000);
        String[] sessionHandles = {sessionInfo.session.handle};
        Session.revokeSessionUsingSessionHandles(process.getProcess(), sessionHandles);


        JsonObject newUserDataInJWT = new JsonObject();
        userDataInJWT.addProperty("key", "value2");

        //call update function
        try {
            Session.updateSession(process.getProcess(), sessionInfo.session.handle, null, newUserDataInJWT, null);
            fail();
        } catch (UnauthorisedException e) {
            assertEquals(e.getMessage(), "Session does not exist.");
        }

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    // * - create session -> let it expire, remove from db -> call get function -> make sure you get unauthorised error
    @Test
    public void testExpireSessionCallGetAndCheckUnauthorised() throws Exception {

        String[] args = {"../"};

        Utils.setValueInConfig("access_token_validity", "1");// 1 second validity
        Utils.setValueInConfig("refresh_token_validity", "" + 1.0 / 60);// 1 second validity (value in mins)

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        //createSession with JWT payload
        String userId = "userId";
        JsonObject userDataInJWT = new JsonObject();
        userDataInJWT.addProperty("key", "value");
        JsonObject userDataInDatabase = new JsonObject();
        userDataInDatabase.addProperty("key", "value");


        SessionInformationHolder sessionInfo = Session
                .createNewSession(process.getProcess(), userId, userDataInJWT, userDataInDatabase);

        //let it expire, remove from db

        Thread.sleep(2000);
        String[] sessionHandles = {sessionInfo.session.handle};
        Session.revokeSessionUsingSessionHandles(process.getProcess(), sessionHandles);


        //call update function
        try {
            Session.getJWTData(process.getProcess(), sessionInfo.session.handle);
            fail();
        } catch (UnauthorisedException e) {
            assertEquals(e.getMessage(), "Session does not exist.");
        }

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }
}
