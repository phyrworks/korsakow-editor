package test.org.korsakow.ide;

import org.dsrg.soenea.domain.MapperException;
import org.dsrg.soenea.uow.UoW;
import org.junit.Assert;
import org.junit.Test;
import org.korsakow.domain.SnuFactory;
import org.korsakow.domain.interf.IResource;
import org.korsakow.ide.Application;
import org.korsakow.ide.resources.ResourceType;
import org.korsakow.ide.ui.ResourceEditor;

import test.org.korsakow.domain.AbstractDomainObjectTestCase;

/**
 * @author d
 *
 */
public class TestApplication extends AbstractDomainObjectTestCase
{
	@Test
	public void testGetOpenEditorsByResourceType() throws Exception {
		Application.initializeInstance();
		Application app = Application.getInstance();
		IResource resource = SnuFactory.createNew();
		UoW.getCurrent().registerClean(resource);
		ResourceEditor editor = app.createResourceEditor(resource);
		
		Assert.assertTrue(app.getOpenEditors(ResourceType.SNU).contains(editor));
	}
	
	@Test
	public void testGetOpenResourcesByResourceType() throws MapperException {
		Application app = Application.getInstance();
		IResource resource = SnuFactory.createNew();
		app.createResourceEditor(resource);
		
		Assert.assertTrue(app.getOpenResources(ResourceType.SNU).contains(resource));
	}
}
