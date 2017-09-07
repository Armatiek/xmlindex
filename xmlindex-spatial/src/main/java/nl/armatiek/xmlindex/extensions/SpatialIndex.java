/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.armatiek.xmlindex.extensions;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.locationtech.spatial4j.shape.jts.JtsShapeFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.gml2.GMLReader;

import net.sf.saxon.dom.NodeOverNodeInfo;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.StringValue;
import nl.armatiek.xmlindex.plugins.index.PluggableIndex;
import nl.armatiek.xmlindex.plugins.index.PluggableIndexException;
import nl.armatiek.xmlindex.plugins.index.PluggableIndexExtensionFunctionCall;
import nl.pdok.gml3.impl.gml3_1_1_2.GML3112ParserImpl;
import nl.pdok.gml3.impl.gml3_2_1.GML321ParserImpl;

public class SpatialIndex extends PluggableIndex {
  
  public final static String PROPERTYNAME_FIELDNAME = "fieldName";
  public final static String PROPERTYNAME_MAXLEVELS = "prefixTreeMaxLevels";
  
  private final static String NAMESPACE_GML_32       = "http://www.opengis.net/gml/3.2";
  private final static String NAMESPACE_GML          = "http://www.opengis.net/gml";
  private final static String ATTRNAME_SRSNAME       = "srsName";
  private final static String DEFAULT_FIELDNAME      = "shape";
  private final static int DEFAULT_MAXLEVELS         = 11;
  private final static int SRID_WGS84                = 4326;
  
  private final static HashSet<String> geometryNames32 = new HashSet<String>();
  static {
    geometryNames32.add("Point");
    geometryNames32.add("LineString");
    geometryNames32.add("Curve");
    geometryNames32.add("OrientableCurve");
    geometryNames32.add("CompositeCurve");
    geometryNames32.add("Polygon");
    geometryNames32.add("Surface");
    geometryNames32.add("OrientableSurface");
    geometryNames32.add("CompositeSurface");
    geometryNames32.add("Solid");
    geometryNames32.add("CompositeSolid");
    geometryNames32.add("MultiPoint");
    geometryNames32.add("MultiCurve");
    geometryNames32.add("MultiSurface");
    geometryNames32.add("MultiSolid");
    geometryNames32.add("MultiGeometry");
    geometryNames32.add("Points");
    geometryNames32.add("LineStrings");
    geometryNames32.add("Curves");
    geometryNames32.add("OrientableCurves");
    geometryNames32.add("CompositeCurves");
    geometryNames32.add("Polygons");
    geometryNames32.add("Surfaces");
    geometryNames32.add("OrientableSurfaces");
    geometryNames32.add("CompositeSurfaces");
    geometryNames32.add("AbstractSolids");
    geometryNames32.add("Solids");
    geometryNames32.add("CompositeSolids");
  }
  
  private final static HashSet<String> geometryNames20 = new HashSet<String>();
  static {
    geometryNames20.add("Point");
    geometryNames20.add("LineString"); 
    geometryNames20.add("LinearRing"); 
    geometryNames20.add("Polygon");
    geometryNames20.add("Box");
    geometryNames20.add("MultiGeometry");
    geometryNames20.add("MultiPoint"); 
    geometryNames20.add("MultiLineString");
    geometryNames20.add("MultiPolygon");
  }
  
  private final Map<CoordinateReferenceSystem, MathTransform> transformCache = new HashMap<CoordinateReferenceSystem, MathTransform>();
  private final Map<String, CoordinateReferenceSystem> crsCache = new HashMap<String, CoordinateReferenceSystem>();
  private final Map<Integer, GML3112ParserImpl> parser31Cache = new HashMap<Integer, GML3112ParserImpl>();
  private final Map<Integer, GML321ParserImpl> parser32Cache = new HashMap<Integer, GML321ParserImpl>();
  
  private PrecisionModel precisionModel = new PrecisionModel(1000); 
  private GeometryFactory geometryFactory = new GeometryFactory(precisionModel);
  
  private RecursivePrefixTreeStrategy spatialStrategy;
  private JtsShapeFactory shapeFactory;
  private String fieldName;
  private int prefixTreeMaxLevels;
  
  public SpatialIndex(String name) {
    super(name);
  }
  
  @Override
  public void init(Map<String, String> params) {
    super.init(params);
    fieldName = DEFAULT_FIELDNAME;
    prefixTreeMaxLevels = DEFAULT_MAXLEVELS;
    if (params != null) {
      if (params.containsKey(PROPERTYNAME_FIELDNAME))
        fieldName = params.get(PROPERTYNAME_FIELDNAME);
      if (params.containsKey(PROPERTYNAME_MAXLEVELS))
        prefixTreeMaxLevels = Integer.parseInt(params.get(PROPERTYNAME_MAXLEVELS));
    }
    
    JtsSpatialContextFactory spatialContextFactory = new JtsSpatialContextFactory();
    spatialContextFactory.geo = true;
    JtsSpatialContext spatialContext = new JtsSpatialContext(spatialContextFactory);
    SpatialPrefixTree grid = new GeohashPrefixTree(spatialContext, prefixTreeMaxLevels);
    spatialStrategy = new RecursivePrefixTreeStrategy(grid, fieldName);
    shapeFactory = (JtsShapeFactory) spatialContext.getShapeFactory();
  }
  
  @Override
  public void indexNode(Document doc, Element node) throws PluggableIndexException {
    if (!isGMLGeometry(node))
      return;
    
    if (isNestedGeometry(node)) 
      return;
    
    try {
      /* Convert GML element to JTS Geometry: */
      Geometry geometry = gmlToGeometry(node);
      
      /* Add shape fields to Lucene document: */
      for (IndexableField field:spatialStrategy.createIndexableFields(shapeFactory.makeShapeFromGeometry(geometry))) {
        doc.add(field);
      }
      
    } catch (PluggableIndexException e) {
      throw e;
    } catch (Exception e) {
      throw new PluggableIndexException("Error indexing GML", e);
    }
  }

  @Override
  public PluggableIndexExtensionFunctionCall getFunctionCall() {
    PluggableIndexExtensionFunctionCall call = new SpatialIndexExtensionFunctionCall();
    call.setDefinition(new SpatialIndexExtensionFunctionDefinition());
    return call;
  }

  @Override
  public Query getQuery(Sequence[] functionParams) {
    try {
      String op = ((StringValue) functionParams[1]).getStringValue();
      
      NodeInfo nodeInfo;
      
      if (functionParams[2] instanceof GroundedValue)
        nodeInfo = (NodeInfo) ((GroundedValue) functionParams[2]).head();
      else 
        nodeInfo = (NodeInfo) functionParams[2];
      
      Node gmlNode = NodeOverNodeInfo.wrap(unwrapNodeInfo(nodeInfo));
      
      /* Convert GML element to JTS Geometry: */
      Geometry geometry = gmlToGeometry((Element) gmlNode);
      
      SpatialOperation operation = SpatialOperation.get(op);
      
      SpatialArgs args = new SpatialArgs(operation, shapeFactory.makeShapeFromGeometry(geometry)); 
      return spatialStrategy.makeQuery(args);
      
    } catch (Exception e) {
      throw new PluggableIndexException("Error querying GML", e);
    }
  }
  
  private Geometry gmlToGeometry(Element gmlNode) throws Exception {
    String srsName = getSrsName(gmlNode);
   
    if (StringUtils.isBlank(srsName))
      throw new PluggableIndexException("GML geometry element has no attribute \"" + ATTRNAME_SRSNAME + "\"");
    
    /* Transform coordinate reference system to WGS84 if necessary: */
    CoordinateReferenceSystem crs = tryCrsCache(srsName);
    
    int srid = Integer.parseInt(CRS.toSRS(crs, true));
    
    /* Serialize GML DOM element to string: */
    String gml = nodeToString(gmlNode);
    
    /* Parse GML string to JTS geometry: */
    Geometry geometry;
    if (gmlNode.getNamespaceURI().equals(NAMESPACE_GML_32))
      geometry = tryParser32Cache(srid).toJTSGeometry(gml);
    else
      try {
        geometry = tryParser31Cache(srid).toJTSGeometry(gml);
      } catch (Exception e) {
        GMLReader gmlReader = new GMLReader(); 
        geometry = gmlReader.read(gml, geometryFactory);
      }
    
    if (srid != SRID_WGS84) {
      MathTransform transform = tryTransformCache(crs);
      Geometry targetGeometry = JTS.transform(geometry, transform);
      geometry = targetGeometry;
    }
    
    return geometry;
  }
  
  private CoordinateReferenceSystem tryCrsCache(String code) throws NoSuchAuthorityCodeException, FactoryException {
    CoordinateReferenceSystem crs = crsCache.get(code);
    if (crs == null) {
      crs = CRS.decode(code);
      crsCache.put(code, crs);
    }
    return crs;
  }
  
  private MathTransform tryTransformCache(CoordinateReferenceSystem crs) throws FactoryException {
    MathTransform transform = transformCache.get(crs);
    if (transform == null) {
      transform = CRS.findMathTransform(crs, DefaultGeographicCRS.WGS84);
      transformCache.put(crs, transform);
    }
    return transform;
  }
  
  private GML3112ParserImpl tryParser31Cache(int srid) throws NoSuchAuthorityCodeException, FactoryException {
    GML3112ParserImpl parser = parser31Cache.get(srid);
    if (parser == null) {
      parser = new GML3112ParserImpl(0.001, srid);
      parser31Cache.put(srid, parser);
    }
    return parser;
  }
  
  private GML321ParserImpl tryParser32Cache(int srid) throws NoSuchAuthorityCodeException, FactoryException {
    GML321ParserImpl parser = parser32Cache.get(srid);
    if (parser == null) {
      parser = new GML321ParserImpl(0.001, srid);
      parser32Cache.put(srid, parser);
    }
    return parser;
  }
  
  private String getSrsName(Element elem) {
    Node e = elem; 
    String srsName = "";
    while (e != null && e.getNodeType() != Node.DOCUMENT_NODE && (srsName = ((Element) e).getAttribute(ATTRNAME_SRSNAME)).equals(""))
      e = e.getParentNode();
    return srsName;
  }
  
  private boolean isGMLGeometry(Element node) {
    String namespace = node.getNamespaceURI();
    if (namespace == null || !namespace.startsWith(NAMESPACE_GML))
      return false;
    if (!geometryNames32.contains(node.getLocalName()))
      return false;
    return true;
  }
  
  private boolean isNestedGeometry(Element elem) {
    Node parent = elem.getParentNode();
    while (parent != null && parent.getNodeType() != Node.DOCUMENT_NODE) {
      if (isGMLGeometry((Element) parent))
        return true;
      parent = parent.getParentNode();
    }
    return false;
  }
  
  private String nodeToString(Node node) throws Exception {
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    DOMSource source = new DOMSource(node);
    StringWriter sw = new StringWriter();
    StreamResult result = new StreamResult(sw);
    transformer.transform(source, result);
    return sw.toString();
  }
  
  private NodeInfo unwrapNodeInfo(NodeInfo nodeInfo) {
    if (nodeInfo != null && nodeInfo.getNodeKind() == Type.DOCUMENT) {
      nodeInfo = nodeInfo.iterateAxis(AxisInfo.CHILD, NodeKindTest.ELEMENT).next();
    }
    return nodeInfo;
  }

}