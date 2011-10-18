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

package org.apache.axis.components.jms;

import java.util.HashMap;
import java.util.Hashtable;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.axis.transport.jms.JMSConstants;
import org.apache.axis.transport.jms.JMSURLHelper;

/**
 * Uses JNDI to locate ConnectionFactory and Destinations
 *
 * @author Jaime Meritt (jmeritt@sonicsoftware.com)
 * @author Ray Chun (rchun@sonicsoftware.com)
 */
public class JNDIVendorAdapter extends JMSVendorAdapter
{
    public final static String CONTEXT_FACTORY                = "java.naming.factory.initial";
    public final static String PROVIDER_URL                   = "java.naming.provider.url";

    public final static String _CONNECTION_FACTORY_JNDI_NAME  = "ConnectionFactoryJNDIName";
    public final static String CONNECTION_FACTORY_JNDI_NAME   = JMSConstants.JMS_PROPERTY_PREFIX +
                                                                    _CONNECTION_FACTORY_JNDI_NAME;

    private Context context;

    public QueueConnectionFactory getQueueConnectionFactory(HashMap cfConfig)
        throws Exception
    {
        return (QueueConnectionFactory)getConnectionFactory(cfConfig);
    }

    public TopicConnectionFactory getTopicConnectionFactory(HashMap cfConfig)
        throws Exception
    {
        return (TopicConnectionFactory)getConnectionFactory(cfConfig);
    }

    private ConnectionFactory getConnectionFactory(HashMap cfProps)
        throws Exception
    {
        if(cfProps == null)
                throw new IllegalArgumentException("noCFProps");
        String jndiName = (String)cfProps.get(CONNECTION_FACTORY_JNDI_NAME);
        if(jndiName == null || jndiName.trim().length() == 0)
            throw new IllegalArgumentException("noCFName");

        Hashtable environment = new Hashtable(cfProps);

        // set the context factory if provided in the JMS URL
        String ctxFactory = (String)cfProps.get(CONTEXT_FACTORY);
        if (ctxFactory != null)
            environment.put(CONTEXT_FACTORY, ctxFactory);

        // set the provider url if provided in the JMS URL
        String providerURL = (String)cfProps.get(PROVIDER_URL);
        if (providerURL != null)
            environment.put(PROVIDER_URL, providerURL);

        context = new InitialContext(environment);

        return (ConnectionFactory)context.lookup(jndiName);
    }

    /**
     * Populates the connection factory config table with properties from
     * the JMS URL query string
     *
     * @param jmsurl The target endpoint address of the Axis call
     * @param cfConfig The set of properties necessary to create/configure the connection factory
     */
    public void addVendorConnectionFactoryProperties(JMSURLHelper jmsurl,
                                                     HashMap cfConfig)
    {
        // add the connection factory jndi name
        String cfJNDIName = jmsurl.getPropertyValue(_CONNECTION_FACTORY_JNDI_NAME);
        if (cfJNDIName != null)
            cfConfig.put(CONNECTION_FACTORY_JNDI_NAME, cfJNDIName);

        // add the initial ctx factory
        String ctxFactory = jmsurl.getPropertyValue(CONTEXT_FACTORY);
        if (ctxFactory != null)
            cfConfig.put(CONTEXT_FACTORY, ctxFactory);

        // add the provider url
        String providerURL = jmsurl.getPropertyValue(PROVIDER_URL);
        if (providerURL != null)
            cfConfig.put(PROVIDER_URL, providerURL);
    }

    /**
     * Check that the attributes of the candidate connection factory match the
     * requested connection factory properties.
     *
     * @param cf the candidate connection factory
     * @param originalJMSURL the URL which was used to create the connection factory
     * @param cfProps the set of properties that should be used to determine the match
     * @return true or false to indicate whether a match has been found
     */
    public boolean isMatchingConnectionFactory(ConnectionFactory cf,
                                               JMSURLHelper originalJMSURL,
                                               HashMap cfProps)
    {
        JMSURLHelper jmsurl = (JMSURLHelper)cfProps.get(JMSConstants.JMS_URL);

        // just check the connection factory jndi name
        String cfJndiName = jmsurl.getPropertyValue(_CONNECTION_FACTORY_JNDI_NAME);
        String originalCfJndiName = originalJMSURL.getPropertyValue(_CONNECTION_FACTORY_JNDI_NAME);

        if (cfJndiName.equalsIgnoreCase(originalCfJndiName))
            return true;

        return false;
    }

    public Queue getQueue(QueueSession session, String name)
        throws Exception
    {
        return (Queue)context.lookup(name);
    }

    public Topic getTopic(TopicSession session, String name)
        throws Exception
    {
        return (Topic)context.lookup(name);
    }
}