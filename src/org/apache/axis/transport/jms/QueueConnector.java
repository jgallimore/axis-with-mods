/*
 * Copyright 2001, 2002,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.axis.transport.jms;

import org.apache.axis.components.jms.JMSVendorAdapter;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

/**
 * QueueConnector is a concrete JMSConnector subclass that specifically handles
 *   connections to queues (ptp domain).
 *
 * @author Jaime Meritt  (jmeritt@sonicsoftware.com)
 * @author Richard Chung (rchung@sonicsoftware.com)
 * @author Dave Chappell (chappell@sonicsoftware.com)
 */
public class QueueConnector extends JMSConnector
{

    public QueueConnector(ConnectionFactory factory,
                          int numRetries,
                          int numSessions,
                          long connectRetryInterval,
                          long interactRetryInterval,
                          long timeoutTime,
                          boolean allowReceive,
                          String clientID,
                          String username,
                          String password,
                          JMSVendorAdapter adapter,
                          JMSURLHelper jmsurl)
        throws JMSException
    {
        super(factory, numRetries, numSessions, connectRetryInterval,
              interactRetryInterval, timeoutTime, allowReceive, clientID,
              username, password, adapter, jmsurl);
    }

    public JMSEndpoint createEndpoint(String destination)
    {
        return new QueueEndpoint(destination);
    }

    /**
     * Create an endpoint for a queue destination.
     *
     * @param destination
     * @return
     * @throws JMSException
     */
    public JMSEndpoint createEndpoint(Destination destination)
        throws JMSException
    {
        if(!(destination instanceof Queue))
            throw new IllegalArgumentException("The input must be a queue for this connector");
        return new QueueDestinationEndpoint((Queue)destination);
    }

    protected Connection internalConnect(ConnectionFactory connectionFactory,
                                         String username,
                                         String password)
        throws JMSException
    {
        QueueConnectionFactory qcf = (QueueConnectionFactory)connectionFactory;
        if(username == null)
            return qcf.createQueueConnection();

        return qcf.createQueueConnection(username, password);
    }


    protected SyncConnection createSyncConnection(ConnectionFactory factory,
                                                  Connection connection,
                                                  int numSessions,
                                                  String threadName,
                                                  String clientID,
                                                  String username,
                                                  String password)

        throws JMSException
    {
        return new QueueSyncConnection((QueueConnectionFactory)factory,
                                       (QueueConnection)connection, numSessions,
                                       threadName, clientID, username, password);
    }

    private QueueSession createQueueSession(QueueConnection connection, int ackMode)
        throws JMSException
    {
        return connection.createQueueSession(false, ackMode);
    }

    private Queue createQueue(QueueSession session, String subject)
        throws Exception
    {
        return m_adapter.getQueue(session, subject);
    }

    private QueueReceiver createReceiver(QueueSession session,
                                         Queue queue,
                                         String messageSelector)
        throws JMSException
    {
        return session.createReceiver(queue, messageSelector);
    }

    private final class QueueSyncConnection extends SyncConnection
    {
        QueueSyncConnection(QueueConnectionFactory connectionFactory,
                            QueueConnection connection,
                            int numSessions,
                            String threadName,
                            String clientID,
                            String username,
                            String password)
            throws JMSException
        {
            super(connectionFactory, connection, numSessions, threadName,
                  clientID, username, password);
        }

        protected SendSession createSendSession(javax.jms.Connection connection)
            throws JMSException
        {
            QueueSession session = createQueueSession((QueueConnection)connection,
                                        JMSConstants.DEFAULT_ACKNOWLEDGE_MODE);
            QueueSender sender = session.createSender(null);
            return new QueueSendSession(session, sender);
        }

        private final class QueueSendSession extends SendSession
        {
            QueueSendSession(QueueSession session,
                             QueueSender  sender)
                throws JMSException
            {
                super(session, sender);
            }

            protected MessageConsumer createConsumer(Destination destination)
                throws JMSException
            {
                return createReceiver((QueueSession)m_session, (Queue)destination, null);
            }


            protected Destination createTemporaryDestination()
                throws JMSException
            {
                return ((QueueSession)m_session).createTemporaryQueue();
            }

            protected void deleteTemporaryDestination(Destination destination)
                throws JMSException
            {
                ((TemporaryQueue)destination).delete();
            }

            protected void send(Destination destination, Message message,
                                int deliveryMode, int priority, long timeToLive)
                throws JMSException
            {
                ((QueueSender)m_producer).send((Queue)destination, message,
                                                deliveryMode, priority, timeToLive);
            }

        }
    }

    private class QueueEndpoint
        extends JMSEndpoint
    {
        String m_queueName;

        QueueEndpoint(String queueName)
        {
            super(QueueConnector.this);
            m_queueName = queueName;
        }

        Destination getDestination(Session session)
            throws Exception
        {
            return createQueue((QueueSession)session, m_queueName);
        }

        public String toString()
        {
            StringBuffer buffer = new StringBuffer("QueueEndpoint:");
            buffer.append(m_queueName);
            return buffer.toString();
        }

        public boolean equals(Object object)
        {
            if(!super.equals(object))
                return false;

            if(!(object instanceof QueueEndpoint))
                return false;

            return m_queueName.equals(((QueueEndpoint)object).m_queueName);
        }
    }


    private final class QueueDestinationEndpoint
        extends QueueEndpoint
    {
        Queue m_queue;

        QueueDestinationEndpoint(Queue queue)
            throws JMSException
        {
            super(queue.getQueueName());
            m_queue = queue;
        }

        Destination getDestination(Session session)
        {
            return m_queue;
        }

    }

    protected AsyncConnection createAsyncConnection(ConnectionFactory factory,
                                                    Connection connection,
                                                    String threadName,
                                                    String clientID,
                                                    String username,
                                                    String password)
        throws JMSException
    {
        return new QueueAsyncConnection((QueueConnectionFactory)factory,
                                        (QueueConnection)connection, threadName,
                                        clientID, username, password);
    }

    private final class QueueAsyncConnection extends AsyncConnection
    {

        QueueAsyncConnection(QueueConnectionFactory connectionFactory,
                             QueueConnection connection,
                             String threadName,
                             String clientID,
                             String username,
                             String password)
            throws JMSException
        {
            super(connectionFactory, connection, threadName, clientID, username, password);
        }

        protected ListenerSession createListenerSession(javax.jms.Connection connection,
                                                        Subscription subscription)
            throws Exception
        {
            QueueSession session = createQueueSession((QueueConnection)connection,
                                                      subscription.m_ackMode);
            QueueReceiver receiver = createReceiver(session,
                        (Queue)subscription.m_endpoint.getDestination(session),
                        subscription.m_messageSelector);
            return new ListenerSession(session, receiver, subscription);
        }

    }

}