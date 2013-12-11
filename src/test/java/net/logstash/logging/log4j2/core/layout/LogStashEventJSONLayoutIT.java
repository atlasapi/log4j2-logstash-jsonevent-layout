package net.logstash.logging.log4j2.core.layout;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections.IteratorUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.hamcrest.Matcher;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static net.logstash.logging.event.LogStashEvent.LOGSTASH_EVENT_JSON_KEYS;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class LogStashEventJSONLayoutIT {
    private static Logger logger = LogManager.getLogger(LogStashEventJSONLayoutIT.class);

    @Test(enabled = false, dataProvider = "dp")
    public void f(Integer n, String s) {
    }

    @DataProvider
    public Object[][] dp() {
        return new Object[][]{
                new Object[]{1, "a"},
                new Object[]{2, "b"},
        };
    }

    @BeforeTest
    public void beforeTest() {
    }

    @AfterTest
    public void afterTest() {
    }


    LogStashEventJSONLayout layout = LogStashEventJSONLayout.createLayout("UTF-8", "true", null, null, null, null, null);

    //TODO make that token replacement for layout.foo() component
    String simpleRoundTripJSON = "    {\n" +
            "      \"@fields\" : {\n" +
            "        \"source_address\" : \"" + layout.getLocalhostAddress() + "\",\n" +
            "        \"logger_name\" : \"net.logstash.logging.log4j2.core.layout.LogStashEventJSONLayoutIT\",\n" +
            "        \"thread_name\" : \"Test worker\",\n" +
            "        \"level\" : \"DEBUG\",\n" +
            "        \"level_value\" : 5,\n" +
            "        \"ndc\" : [ ],\n" +
            "        \"message_type\" : \"SimpleMessage\",\n" +
            "        \"message_formatted\" : \"Test Message\"\n" +
            "      },\n" +
            "      \"@source_host\" : \"" + layout.getHostName() + "\",\n" +
            "      \"@timestamp\" : \"2013-06-30T09:22:12.322-07:00\",\n" +
            "      \"@message\" : \"Test Message\"\n" +
            "    }";

    ObjectMapper mapper = new ObjectMapper();


    @Test
    public void BasicSimpleTest() throws JsonParseException, JsonMappingException, IOException {
        Message simpleMessage = new SimpleMessage("Test Message");
        LogEvent event = new Log4jLogEvent(logger.getName(),
                null,
                this.getClass().getCanonicalName(),
                Level.DEBUG,
                simpleMessage,
                null);
        LogStashEventJSONLayout layout = LogStashEventJSONLayout.createLayout("UTF-8", "true", null, null, null, null, null);
        String layoutJSON = layout.toSerializable(event);

        ObjectNode expectedLayout = mapper.readValue(simpleRoundTripJSON, ObjectNode.class);
        ObjectNode resultLayout = mapper.readValue(layoutJSON, ObjectNode.class);

        //Remove timestamp (it's always goign to be different)
        expectedLayout.remove("@timestamp");
        resultLayout.remove("@timestamp");

        assertThat(resultLayout, equalTo(expectedLayout));
    }
    //TODO att order

    //TODO test & demo local properties

    //TODO demo tags

    //TODO test & demo extra properties

    //TODO test throwables
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void TestThrowable() throws IOException {
        Message simpleMessage = new SimpleMessage("Test Message");

        Throwable nestedThrowable = null;
        Throwable divideByZero = null;

        try {
            @SuppressWarnings("unused")
            int divide_by_zero = 12 / 0;
        } catch (RuntimeException r) {
            divideByZero = r;
            try {
                throw new IOException("Test IO Exception", r);
            } catch (IOException e) {
                nestedThrowable = new RuntimeException("Runtime Exception", e);
            }

        }

        LogEvent event = new Log4jLogEvent(logger.getName(),
                null,
                this.getClass().getCanonicalName(),
                Level.DEBUG,
                simpleMessage,
                nestedThrowable);
        LogStashEventJSONLayout layout = LogStashEventJSONLayout.createLayout("UTF-8", "true", null, null, null, null, null);
        String layoutJSON = layout.toSerializable(event);

        ObjectNode resultLayout = mapper.readValue(layoutJSON, ObjectNode.class);

        System.out.println("--- resultLayout ---");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultLayout));
        System.out.println("--- resultLayout ---");

        assertThat(resultLayout.has(LOGSTASH_EVENT_JSON_KEYS.FIELDS), is(true));

        assertThat(resultLayout.get(LOGSTASH_EVENT_JSON_KEYS.FIELDS).has("thrown"), is(true));

        JsonNode throwableNode = resultLayout.get(LOGSTASH_EVENT_JSON_KEYS.FIELDS).get("thrown");

        System.out.println("--- throwableNODE ---");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(throwableNode));
        System.out.println("--- throwableNODE ---");


        assertThat(IteratorUtils.toList(throwableNode.fieldNames()),
                (Matcher) hasItems("exception_class", "exception_message",
                        "stacktrace", "root_cause_throwable"));


        assertThat(throwableNode.get("exception_class").textValue(), equalTo("RuntimeException"));


    }
    //TODO test variety of objects

    //TODO test all message types

    //TODO test markers

    //TODO demo expansion scenarios like audit log marker

    //TODO demo parameter object like audit

    //TODO test context


    /**
     * This test requires logstash (installed manually) and makes assumptions
     * about both configuration and operating system...
     * <p/>
     * ... So, you probably ought not run it by default :)
     */
    @Test(groups = "integration")
    public void LogToLogStashTest() {
        System.out.println("&&&&&&&&&&&&&&&&&&&");
        System.out.println("&&&&&&&&&&&&&&&&&&&");
        System.out.println("&&&&&&&&&&&&&&&&&&&");
        System.out.println("&&&&&&&&&&&&&&&&&&&");
        logger.info("TEST IS WIRED");

    }

}
