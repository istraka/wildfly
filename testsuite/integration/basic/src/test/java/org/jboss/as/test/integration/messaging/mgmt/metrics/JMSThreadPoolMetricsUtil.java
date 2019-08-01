/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.messaging.mgmt.metrics;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Assert;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivan Straka
 */
public class JMSThreadPoolMetricsUtil {

    public static final String SCHEDULED_ACTIVE_COUNT = "global-client-scheduled-thread-pool-active-count";
    public static final String SCHEDULED_COMPLETED_COUNT = "global-client-scheduled-thread-pool-completed-task-count";
    public static final String SCHEDULED_CURRENT_COUNT = "global-client-scheduled-thread-pool-current-thread-count";
    public static final String SCHEDULED_KEEPALIVE_TIME = "global-client-scheduled-thread-pool-keepalive-time";
    public static final String SCHEDULED_LARGEST_COUNT = "global-client-scheduled-thread-pool-largest-thread-count";
    public static final String SCHEDULED_TASK_COUNT = "global-client-scheduled-thread-pool-task-count";

    public static final String ACTIVE_COUNT = "global-client-thread-pool-active-count";
    public static final String COMPLETED_COUNT = "global-client-thread-pool-completed-task-count";
    public static final String CURRENT_COUNT = "global-client-thread-pool-current-thread-count";
    public static final String KEEPALIVE_TIME = "global-client-thread-pool-keepalive-time";
    public static final String LARGEST_COUNT = "global-client-thread-pool-largest-thread-count";
    public static final String TASK_COUNT = "global-client-thread-pool-task-count";
    public static final int MESSAGES_COUNT = 10;

    private static final ModelNode readJMSResources = new ModelNode();

    static {
        ModelNode serverAddress = new ModelNode();
        serverAddress.add("subsystem", "messaging-activemq");

        readJMSResources.get("address").set(serverAddress);
        readJMSResources.get("operation").set("read-resource");
        readJMSResources.get("include-runtime").set(true);
    }

    public static Map<String, ModelNode> getResources(ManagementClient client) throws IOException {
        Map<String, ModelNode> resources = new HashMap<>();
        ModelNode execute = client.getControllerClient().execute(readJMSResources);
        List<Property> propertyList = execute.get("result").asPropertyList();

        for (Property property : propertyList) {
            resources.put(property.getName(), property.getValue());
        }
        return resources;
    }

    public static void send(final Session session, final Destination dest, final String text, final boolean useRCF) throws JMSException {
        TextMessage message = session.createTextMessage(text);
        message.setJMSReplyTo(dest);
        message.setBooleanProperty("useRCF", useRCF);

        final MessageProducer messageProducer = session.createProducer(dest);
        messageProducer.send(message);
        messageProducer.close();
    }

    public static boolean useRCF(TextMessage message) throws JMSException {
        return message.getBooleanProperty("useRCF");
    }

    public static void reply(final Session session, final Destination dest, final TextMessage message) throws JMSException {
        final Message replyMsg = session.createTextMessage(message.getText());
        replyMsg.setJMSCorrelationID(message.getJMSMessageID());

        final MessageProducer messageProducer = session.createProducer(dest);
        messageProducer.send(message);
        messageProducer.close();
    }

    public static TextMessage receiveReply(final Session session, final Destination dest, final long waitInMillis) throws JMSException {
        MessageConsumer consumer = session.createConsumer(dest);
        try {
            return (TextMessage) consumer.receive(waitInMillis);
        } finally {
            consumer.close();
        }
    }

    public static void assertGreater(String message, long greater, long lesser) {
        Assert.assertTrue(String.format("%s: expected <%d> is greater than <%d>", message, greater, lesser), greater > lesser);
    }

    public static void assertGreaterOrEqual(String message, long greater, long lesser) {
        Assert.assertTrue(String.format("%s: expected <%d> is greater or equal to <%d>", message, greater, lesser), greater >= lesser);
    }
}
