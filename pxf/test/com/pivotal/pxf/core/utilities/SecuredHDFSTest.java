package com.pivotal.pxf.core.utilities;

import com.pivotal.pxf.api.OutputFormat;
import com.pivotal.pxf.core.utilities.SecuredHDFS;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({UserGroupInformation.class})
public class SecuredHDFSTest {
    Map<String, String> parameters;
    ProtocolData mockProtocolData;
    ServletContext mockContext;

    @Test
    public void invalidIdentifierThrows() {
        when(UserGroupInformation.isSecurityEnabled()).thenReturn(true);
        when(mockProtocolData.getTokenIdentifier()).thenReturn("This is odd");

        try {
            SecuredHDFS.verifyToken(mockProtocolData, mockContext);
            fail("invalid X-GP-TOKEN-IDNT should throw");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Internal server error. String This is odd isn't a valid hex string");
        }
    }

    @Test
    public void invalidPasswordThrows() {
        when(UserGroupInformation.isSecurityEnabled()).thenReturn(true);
        when(mockProtocolData.getTokenIdentifier()).thenReturn("DEAD");
        when(mockProtocolData.getTokenPassword()).thenReturn("This is odd");
        
        try {
            SecuredHDFS.verifyToken(mockProtocolData, mockContext);
            fail("invalid X-GP-TOKEN-PASS should throw");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Internal server error. String This is odd isn't a valid hex string");
        }
    }

    public void invalidKindThrows() {
        when(UserGroupInformation.isSecurityEnabled()).thenReturn(true);
        when(mockProtocolData.getTokenIdentifier()).thenReturn("DEAD");
        when(mockProtocolData.getTokenPassword()).thenReturn("DEAD");
        when(mockProtocolData.getTokenKind()).thenReturn("This is odd");

        try {
            SecuredHDFS.verifyToken(mockProtocolData, mockContext);
            fail("invalid X-GP-TOKEN-KIND should throw");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Internal server error. String This is odd isn't a valid hex string");
        }
    }

    @Test
    public void invalidServiceThrows() {
        when(UserGroupInformation.isSecurityEnabled()).thenReturn(true);
        when(mockProtocolData.getTokenIdentifier()).thenReturn("DEAD");
        when(mockProtocolData.getTokenPassword()).thenReturn("DEAD");
        when(mockProtocolData.getTokenKind()).thenReturn("DEAD");
        when(mockProtocolData.getTokenService()).thenReturn("This is odd");

        try {
            SecuredHDFS.verifyToken(mockProtocolData, mockContext);
            fail("invalid X-GP-TOKEN-SRVC should throw");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Internal server error. String This is odd isn't a valid hex string");
        }
    }
    /*
     * setUp function called before each test
	 */
    @Before
    public void setUp() {
        parameters = new HashMap<String, String>();

        parameters.put("X-GP-ALIGNMENT", "all");
        parameters.put("X-GP-SEGMENT-ID", "-44");
        parameters.put("X-GP-SEGMENT-COUNT", "2");
        parameters.put("X-GP-HAS-FILTER", "0");
        parameters.put("X-GP-FORMAT", "TEXT");
        parameters.put("X-GP-URL-HOST", "my://bags");
        parameters.put("X-GP-URL-PORT", "-8020");
        parameters.put("X-GP-ATTRS", "-1");
        parameters.put("X-GP-ACCESSOR", "are");
        parameters.put("X-GP-RESOLVER", "packed");
        parameters.put("X-GP-DATA-DIR", "i'm/ready/to/go");
        parameters.put("X-GP-FRAGMENT-METADATA", "U29tZXRoaW5nIGluIHRoZSB3YXk=");
        parameters.put("X-GP-I'M-STANDING-HERE", "outside-your-door");

        mockProtocolData = mock(ProtocolData.class);        
        mockContext = mock(ServletContext.class);

        PowerMockito.mockStatic(UserGroupInformation.class);
    }
}
