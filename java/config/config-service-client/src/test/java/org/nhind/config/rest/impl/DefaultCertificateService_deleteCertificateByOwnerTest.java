package org.nhind.config.rest.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.nhind.config.client.ConfigServiceRunner;
import org.nhind.config.rest.CertificateService;
import org.nhind.config.testbase.BaseTestPlan;
import org.nhind.config.testbase.TestUtils;

import org.nhindirect.common.rest.exceptions.ServiceException;
import org.nhindirect.common.rest.exceptions.ServiceMethodException;

import org.nhindirect.config.model.Certificate;
import org.nhindirect.config.model.utils.CertUtils;
import org.nhindirect.config.resources.CertificateResource;

import org.nhindirect.config.store.dao.CertificateDao;

public class DefaultCertificateService_deleteCertificateByOwnerTest 
{
    protected CertificateDao certDao;
    
	static CertificateService resource;
	
	abstract class TestPlan extends BaseTestPlan 
	{
		@Override
		protected void setupMocks()
		{
			try
			{
				certDao = (CertificateDao)ConfigServiceRunner.getSpringApplicationContext().getBean("certificateDao");
				
				resource = 	(CertificateService)BaseTestPlan.getService(ConfigServiceRunner.getRestAPIBaseURL(), CERT_SERVICE);	

			}
			catch (Throwable t)
			{
				throw new RuntimeException(t);
			}
		}
		
		@Override
		protected void tearDownMocks()
		{

		}
		

		protected abstract Collection<Certificate> getCertsToAdd() throws Exception;
		
		protected abstract String getOwnerToRemove();
		
		@Override
		protected void performInner() throws Exception
		{				
			
			final Collection<Certificate> certsToAdd = getCertsToAdd();

			if (certsToAdd != null)
			{
				for (Certificate addCert : certsToAdd)
				{
					try
					{
						resource.addCertificate(addCert);
					}
					catch (ServiceException e)
					{
						throw e;
					}
				}			
			}
			
			try
			{
				resource.deleteCertificateByOwner(getOwnerToRemove());
			}
			catch (ServiceException e)
			{
				throw e;
			}
			
			
			doAssertions();
		}
			
		protected void doAssertions() throws Exception
		{
			
		}
	}	
	
	@Test
	public void testRemoveCertificatesByIds_removeExistingCerts_assertCertRemoved() throws Exception
	{
		new TestPlan()
		{
			protected Collection<Certificate> certs;
			
			@Override
			protected Collection<Certificate> getCertsToAdd() throws Exception
			{
				try
				{
					certs = new ArrayList<Certificate>();
					
					Certificate cert = new Certificate();					
					cert.setData(TestUtils.loadCert("gm2552.der").getEncoded());
					
					certs.add(cert);
		
					cert = new Certificate();					
					cert.setData(TestUtils.loadCert("umesh.der").getEncoded());
					
					certs.add(cert);
					
					return certs;
				}
				catch (Exception e)
				{
					throw new RuntimeException (e);
				}
			}
			
			@Override
			protected String getOwnerToRemove()
			{
				return "gm2552@securehealthemail.com";
			}
			
			@Override
			protected void doAssertions() throws Exception
			{
				final Collection<org.nhindirect.config.store.Certificate> certs = certDao.list((String)null);
				assertEquals(1, certs.size());
			}
		}.perform();
	}
	
	@Test
	public void testRemoveCertificatesByIds_wrappedKeys_removeExistingCerts_assertCertRemoved() throws Exception
	{
		new TestPlan()
		{
			protected Collection<Certificate> certs;
			
			@Override
			protected Collection<Certificate> getCertsToAdd() throws Exception
			{
				try
				{
					certs = new ArrayList<Certificate>();
					
					Certificate cert = new Certificate();	
					byte[] keyData = FileUtils.readFileToByteArray(new File("./src/test/resources/certs/gm2552Key.der"));
					
					cert.setData(CertUtils.certAndWrappedKeyToRawByteFormat(keyData, TestUtils.loadCert("gm2552.der")));
					
					certs.add(cert);
		
					
					return certs;
				}
				catch (Exception e)
				{
					throw new RuntimeException (e);
				}
			}
			
			@Override
			protected String getOwnerToRemove()
			{
				return "gm2552@securehealthemail.com";
			}
			
			@Override
			protected void doAssertions() throws Exception
			{
				final Collection<org.nhindirect.config.store.Certificate> certs = certDao.list((String)null);
				assertEquals(0, certs.size());
			}
		}.perform();
	}
	
	@Test
	public void testRemoveCertificatesByIds_errorInDelete_assertServierError() throws Exception
	{
		new TestPlan()
		{
			
			protected CertificateResource certService;

			@Override
			protected void setupMocks()
			{
				try
				{
					super.setupMocks();
					
					certService = (CertificateResource)ConfigServiceRunner.getSpringApplicationContext().getBean("certificateResource");

					CertificateDao mockDAO = mock(CertificateDao.class);
					doThrow(new RuntimeException()).when(mockDAO).delete((String)any());
					
					certService.setCertificateDao(mockDAO);
				}
				catch (Throwable t)
				{
					throw new RuntimeException(t);
				}
			}
			
			@Override
			protected void tearDownMocks()
			{
				super.tearDownMocks();
				
				certService.setCertificateDao(certDao);
			}			
			
			@Override
			protected Collection<Certificate> getCertsToAdd()
			{
				return null;
			}

			
			@Override
			protected String getOwnerToRemove()
			{
				return "gm2552@securehealthemail.com";
			}
			
			@Override
			protected void assertException(Exception exception) throws Exception 
			{
				assertTrue(exception instanceof ServiceMethodException);
				ServiceMethodException ex = (ServiceMethodException)exception;
				assertEquals(500, ex.getResponseCode());
			}
		}.perform();
	}	
}
