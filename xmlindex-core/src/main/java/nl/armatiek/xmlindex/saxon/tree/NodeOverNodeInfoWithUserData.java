package nl.armatiek.xmlindex.saxon.tree;

import org.w3c.dom.UserDataHandler;

import net.sf.saxon.dom.NodeOverNodeInfo;

public class NodeOverNodeInfoWithUserData extends NodeOverNodeInfo {
  
  private Object data;

  @Override
  public Object getUserData(String key) {
    return data;
  }

  @Override
  public Object setUserData(String key, Object data, UserDataHandler handler) {
    this.data = data;
    return data;
  }

}