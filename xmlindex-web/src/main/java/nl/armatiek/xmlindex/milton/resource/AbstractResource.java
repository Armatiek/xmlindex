package nl.armatiek.xmlindex.milton.resource;

import io.milton.resource.CopyableResource;
import io.milton.resource.DigestResource;
import io.milton.resource.LockableResource;
import io.milton.resource.MoveableResource;
import io.milton.resource.Resource;
import nl.armatiek.xmlindex.Session;

public abstract class AbstractResource implements Resource, MoveableResource, CopyableResource, LockableResource, DigestResource {

  private Session session;
  
  public AbstractResource(Session session) {
    this.session = session;
  }
  
}
