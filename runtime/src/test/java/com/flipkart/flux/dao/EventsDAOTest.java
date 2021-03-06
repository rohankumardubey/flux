/*
 * Copyright 2012-2016, the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.flux.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.flux.InjectFromRole;
import com.flipkart.flux.api.EventData;
import com.flipkart.flux.client.FluxClientInterceptorModule;
import com.flipkart.flux.dao.iface.EventsDAO;
import com.flipkart.flux.dao.iface.StateMachinesDAO;
import com.flipkart.flux.domain.Event;
import com.flipkart.flux.domain.StateMachine;
import com.flipkart.flux.guice.module.OrchestrationTaskModule;
import com.flipkart.flux.guice.module.OrchestratorContainerModule;
import com.flipkart.flux.guice.module.ShardModule;
import com.flipkart.flux.integration.StringEvent;
import com.flipkart.flux.module.RuntimeTestModule;
import com.flipkart.flux.rule.DbClearWithTestSMRule;
import com.flipkart.flux.runner.GuiceJunit4Runner;
import com.flipkart.flux.runner.Modules;
import com.flipkart.flux.util.TestUtils;

/**
 * <code>EventsDAOTest</code> class tests the functionality of {@link EventsDAO} using JUnit tests.
 *
 * @author shyam.akirala
 * @author kartik.bommepally
 */
@RunWith(GuiceJunit4Runner.class)
@Modules(orchestrationModules = {ShardModule.class, RuntimeTestModule.class, OrchestratorContainerModule.class,
        OrchestrationTaskModule.class, FluxClientInterceptorModule.class})
public class EventsDAOTest {

    @InjectFromRole
    EventsDAO eventsDAO;

    @InjectFromRole
    @Rule
    public DbClearWithTestSMRule dbClearWithTestSMRule;

    @InjectFromRole
    StateMachinesDAO stateMachinesDAO;

    ObjectMapper objectMapper;

    @Before
    public void setup() {
        objectMapper = new ObjectMapper();
    }

    @Test
    public void createEventTest() throws JsonProcessingException {
        StateMachine stateMachine = dbClearWithTestSMRule.getStateMachine();
        StringEvent data = new StringEvent("event_dat");
        Event event = new Event("test_event_name", "Internal", Event.EventStatus.pending, stateMachine.getId(), objectMapper.writeValueAsString(data), "state1");
        eventsDAO.create(event.getStateMachineInstanceId(), event);

        Event event1 = eventsDAO.findBySMIdAndName(event.getStateMachineInstanceId(), event.getName());
        assertThat(event1).isEqualTo(event);
    }

    @Test
    public void testRetrieveByEventNamesAndSmId() throws Exception {
        final StateMachine standardTestMachine = TestUtils.getStandardTestMachine();
        stateMachinesDAO.create(standardTestMachine.getId(), standardTestMachine);
        final Event event1 = new Event("event1", "someType", Event.EventStatus.pending, standardTestMachine.getId(), null, null);
        final EventData eventData1 = new EventData(event1.getName(), event1.getType(), event1.getEventData(), event1.getEventSource());
        eventsDAO.create(event1.getStateMachineInstanceId(), event1);
        final Event event3 = new Event("event3", "someType", Event.EventStatus.pending, standardTestMachine.getId(), null, null);
        final EventData eventData3 = new EventData(event3.getName(), event3.getType(), event3.getEventData(), event3.getEventSource());
        eventsDAO.create(event3.getStateMachineInstanceId(), event3);

        assertThat(eventsDAO.findByEventNamesAndSMId(standardTestMachine.getId(), new LinkedList<String>() {{
            add("event1");
            add("event3");
        }})).containsExactly(eventData1, eventData3);
    }

    @Test
    public void testRetrieveByEventNamesAndSmId_forEmptyEventNameSet() throws Exception {
        /* Doesn't matter, but still setting it up */
        final StateMachine standardTestMachine = TestUtils.getStandardTestMachine();
        stateMachinesDAO.create(standardTestMachine.getId(), standardTestMachine);
        final Event event1 = new Event("event1", "someType", Event.EventStatus.pending, standardTestMachine.getId(), null, null);
        eventsDAO.create(event1.getStateMachineInstanceId(), event1);
        final Event event3 = new Event("event3", "someType", Event.EventStatus.pending, standardTestMachine.getId(), null, null);
        eventsDAO.create(event3.getStateMachineInstanceId(), event3);

        /* Actual test */
        assertThat(eventsDAO.findByEventNamesAndSMId(standardTestMachine.getId(), Collections.<String>emptyList())).isEmpty();
    }
}