/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

package test.soap;

import junit.framework.TestCase;
import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.SimpleTargetedChain;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.axis.providers.java.RPCProvider;
import org.apache.axis.server.AxisServer;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.transport.local.LocalResponder;
import org.apache.axis.transport.local.LocalTransport;

import java.util.Random;

/**
 * A fairly comprehensive test of MustUnderstand/Actor combinations.
 *
 * @author Glen Daniels (gdaniels@apache.org)
 */
public class TestHeaderAttrs extends TestCase {
    static final String PROP_DOUBLEIT = "double_result";
    
    static final String GOOD_HEADER_NS = "http://testMU/";
    static final String GOOD_HEADER_NAME = "doubleIt";
    
    static final String BAD_HEADER_NS = "http://incorrect-ns/";
    static final String BAD_HEADER_NAME = "startThermonuclearWar";
    
    static final String ACTOR = "http://some.actor/";
    
    static SOAPHeaderElement goodHeader = 
                                       new SOAPHeaderElement(GOOD_HEADER_NS, 
                                                             GOOD_HEADER_NAME);
    static SOAPHeaderElement badHeader = 
                                       new SOAPHeaderElement(BAD_HEADER_NS, 
                                                             BAD_HEADER_NAME);

    private SimpleProvider provider = new SimpleProvider();
    private AxisServer engine = new AxisServer(provider);
    private LocalTransport localTransport = new LocalTransport(engine);

    static final String localURL = "local:///testService";

    // Which SOAP version are we using?  Default to SOAP 1.1
    protected SOAPConstants soapVersion = SOAPConstants.SOAP11_CONSTANTS;

    public TestHeaderAttrs(String name) {
        super(name);
    }
    
    /**
     * Prep work.  Make a server, tie a LocalTransport to it, and deploy
     * our little test service therein.
     */ 
    public void setUp() throws Exception {
        engine.init();
        localTransport.setUrl(localURL);
        
        SOAPService service = new SOAPService(new TestHandler(),
                                              new RPCProvider(),
                                              null);
        
        service.setOption("className", TestService.class.getName());
        service.setOption("allowedMethods", "*");
        
        provider.deployService("testService", service);

        SimpleTargetedChain serverTransport =
                new SimpleTargetedChain(null, null, new LocalResponder());
        provider.deployTransport("local", serverTransport);
    }
    
    /**
     * Test an unrecognized header with MustUnderstand="true"
     */ 
    public void testMUBadHeader() throws Exception
    {
        // 1. MU header to unrecognized actor -> should work fine
        badHeader.setActor(ACTOR);
        badHeader.setMustUnderstand(true);
        
        assertTrue("Bad result from test", runTest(badHeader, false));
        
        // 2. MU header to NEXT -> should fail
        badHeader.setActor(soapVersion.getNextRoleURI());
        badHeader.setMustUnderstand(true);
        
        // Test (should produce MU failure)
        try {
            runTest(badHeader, false);
        } catch (Exception e) {
            assertTrue("Non AxisFault Exception : " + e, 
                       e instanceof AxisFault);
            AxisFault fault = (AxisFault)e;
            assertEquals("Bad fault code!", soapVersion.getMustunderstandFaultQName(),
                         fault.getFaultCode());
            return;
        }
        
        fail("Should have gotten mustUnderstand fault!");
    }
    
    /**
     * Test an unrecognized header with MustUnderstand="false"
     */ 
    public void testNonMUBadHeader() throws Exception
    {
        badHeader.setActor(soapVersion.getNextRoleURI());
        badHeader.setMustUnderstand(false);

        assertTrue("Non-MU bad header to next actor returned bad result!",
                   runTest(badHeader, false));

        badHeader.setActor(ACTOR);
        
        assertTrue("Non-MU bad header to unrecognized actor returned bad result!", 
                   runTest(badHeader, false));
    }
    
    /**
     * Test a recognized header (make sure it has the desired result)
     */ 
    public void testGoodHeader() throws Exception
    {
        goodHeader.setActor(soapVersion.getNextRoleURI());
        assertTrue("Good header with next actor returned bad result!",
                   runTest(goodHeader, true));
    }
    
    /**
     * Test a recognized header with a particular actor attribute
     */ 
    public void testGoodHeaderWithActors() throws Exception
    {
        // 1. Good header to unrecognized actor -> should be ignored, and
        //    we should get a non-doubled result
        goodHeader.setActor(ACTOR);
        assertTrue("Good header with unrecognized actor returned bad result!",
                   runTest(goodHeader, false));
        
        // Now tell the engine to recognize the ACTOR value
        engine.addActorURI(ACTOR);
        
        // 2. Good header should now be processed and return doubled result
        assertTrue("Good header with recognized actor returned bad result!",
                   runTest(goodHeader, true));
        
        engine.removeActorURI(ACTOR);
    }
    
    /**
     * Call the service with a random string.  Returns true if the result
     * is the length of the string (doubled if the doubled arg is true).
     */ 
    public boolean runTest(SOAPHeaderElement header,
                           boolean doubled) throws Exception
    {
        Call call = new Call(new Service());
        call.setSOAPVersion(soapVersion);
        call.setTransport(localTransport);
        
        call.addHeader(header);
        
        String str = "a";
        int maxChars = new Random().nextInt(50);
        for (int i = 0; i < maxChars; i++) {
            str += "a";
        }

        Integer i = (Integer)call.invoke("countChars", new Object [] { str });
        
        int desiredResult = str.length();
        if (doubled) desiredResult = desiredResult * 2;
        
        return (i.intValue() == desiredResult);
    }
}
