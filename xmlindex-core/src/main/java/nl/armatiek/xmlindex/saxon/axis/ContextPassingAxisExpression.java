package nl.armatiek.xmlindex.saxon.axis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Atomizer;
import net.sf.saxon.expr.AxisExpression;
import net.sf.saxon.expr.ContextItemExpression;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.FirstItemExpression;
import net.sf.saxon.expr.ItemChecker;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.SimpleStepExpression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PathMap;
import net.sf.saxon.expr.parser.RebindingMap;
import net.sf.saxon.expr.parser.RoleDiagnostic;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.pattern.AnyChildNodeTest;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.CombinedNodeTest;
import net.sf.saxon.pattern.ContentTypeTest;
import net.sf.saxon.pattern.DocumentNodeTest;
import net.sf.saxon.pattern.MultipleNodeKindTest;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.pattern.NodeTestPattern;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.pattern.SchemaNodeTest;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.AnySimpleType;
import net.sf.saxon.type.AnyType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.BuiltInListType;
import net.sf.saxon.type.ComplexType;
import net.sf.saxon.type.ErrorType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaDeclaration;
import net.sf.saxon.type.SchemaException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.type.UType;
import net.sf.saxon.type.Untyped;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.z.IntIterator;
import net.sf.saxon.z.IntSet;
import nl.armatiek.xmlindex.saxon.tree.XMLIndexNodeInfo;

public class ContextPassingAxisExpression extends Expression {

  private byte axis;
  /* @Nullable */
  private NodeTest test;
  /* @Nullable */
  private ItemType itemType = null;
  private ContextItemStaticInfo staticInfo = ContextItemStaticInfo.DEFAULT;
  private int computedCardinality = -1;
  private boolean doneTypeCheck = false;
  private boolean doneOptimize = false;

  /**
   * Constructor for an AxisExpression whose origin is the context item
   *
   * @param axis
   *          The axis to be used in this AxisExpression: relevant constants are
   *          defined in class {@link net.sf.saxon.om.AxisInfo}.
   * @param nodeTest
   *          The conditions to be satisfied by selected nodes. May be null,
   *          indicating that any node on the axis is acceptable
   * @see net.sf.saxon.om.AxisInfo
   */

  public ContextPassingAxisExpression(byte axis, /* @Nullable */ NodeTest nodeTest) {
    this.axis = axis;
    this.test = nodeTest;
  }

  /**
   * Set the axis
   *
   * @param axis
   *          the new axis
   */

  public void setAxis(byte axis) {
    this.axis = axis;
  }

  /**
   * Get a name identifying the kind of expression, in terms meaningful to a
   * user.
   *
   * @return a name identifying the kind of expression, in terms meaningful to a
   *         user. The name will always be in the form of a lexical XML QName,
   *         and should match the name used in explain() output displaying the
   *         expression.
   */

  public String getExpressionName() {
    return "axisStep";
  }

  /**
   * Simplify an expression
   *
   */

  /* @NotNull */
  public Expression simplify() throws XPathException {
    Expression e2 = super.simplify();
    if (e2 != this) {
      return e2;
    }
    if ((test == null || test == AnyNodeTest.getInstance()) && (axis == AxisInfo.PARENT || axis == AxisInfo.ANCESTOR)) {
      // get more precise type information for parent/ancestor nodes
      test = MultipleNodeKindTest.PARENT_NODE;
    }
    return this;
  }

  /**
   * Type-check the expression
   */

  /* @NotNull */
  public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
    boolean noWarnings = doneOptimize || (doneTypeCheck && this.staticInfo.getItemType().equals(contextInfo.getItemType()));
    doneTypeCheck = true;
    if (contextInfo.getItemType() == ErrorType.getInstance()) {
      XPathException err = new XPathException("Axis step " + toString() + " cannot be used here: the context item is absent");
      err.setIsTypeError(true);
      err.setErrorCode("XPDY0002");
      err.setLocation(getLocation());
      throw err;
    } else {
      staticInfo = contextInfo;
    }
    Configuration config = getConfiguration();
    TypeHierarchy th = config.getTypeHierarchy();
    int relation = th.relationship(contextInfo.getItemType(), AnyNodeTest.getInstance());

    if (relation == TypeHierarchy.DISJOINT) {
      XPathException err = new XPathException("Axis step " + toString() + " cannot be used here: the context item is not a node");
      err.setIsTypeError(true);
      err.setErrorCode("XPTY0020");
      err.setLocation(getLocation());
      throw err;
    } else if (relation == TypeHierarchy.OVERLAPS || relation == TypeHierarchy.SUBSUMES) {
      // need to insert a dynamic check of the context item type
      Expression thisExp = checkPlausibility(visitor, contextInfo, !noWarnings);
      if (Literal.isEmptySequence(thisExp)) {
        return thisExp;
      }
      ContextItemExpression exp = new ContextItemExpression();
      ExpressionTool.copyLocationInfo(this, exp);
      RoleDiagnostic role = new RoleDiagnostic(RoleDiagnostic.AXIS_STEP, "", axis);
      role.setErrorCode("XPTY0020");
      ItemChecker checker = new ItemChecker(exp, AnyNodeTest.getInstance(), role);
      ExpressionTool.copyLocationInfo(this, checker);
      SimpleStepExpression step = new SimpleStepExpression(checker, thisExp);
      ExpressionTool.copyLocationInfo(this, step);
      return step;
    }

    return checkPlausibility(visitor, contextInfo, !noWarnings);
  }

  private Expression checkPlausibility(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo, boolean warnings) throws XPathException {
    StaticContext env = visitor.getStaticContext();
    Configuration config = env.getConfiguration();
    ItemType contextType = contextInfo.getItemType();

    if (!(contextType instanceof NodeTest)) {
      contextType = AnyNodeTest.getInstance();
    }

    // New code in terms of UTypes

    // Test whether the requested nodetest is consistent with the requested axis
    if (test != null && !AxisInfo.getTargetUType(UType.ANY_NODE, axis).overlaps(test.getUType())) {
      if (warnings) {
        visitor.issueWarning("The " + AxisInfo.axisName[axis] + " axis will never select " + test.getUType().toStringWithIndefiniteArticle(), getLocation());
      }
      return Literal.makeEmptySequence();
    }

    if (test instanceof NameTest && axis == AxisInfo.NAMESPACE && !((NameTest) test).getNamespaceURI().isEmpty()) {
      if (warnings) {
        visitor.issueWarning("The names of namespace nodes are never prefixed, so this axis step will never select anything", getLocation());
      }
      return Literal.makeEmptySequence();
    }

    // Test whether the axis ever selects anything, when starting at this
    // context node
    UType originUType = contextType.getUType();
    UType targetUType = AxisInfo.getTargetUType(originUType, axis);
    UType testUType = test == null ? UType.ANY_NODE : test.getUType();
    if (targetUType.equals(UType.VOID)) {
      if (warnings) {
        visitor.issueWarning("The " + AxisInfo.axisName[axis] + " axis starting at " + originUType.toStringWithIndefiniteArticle() + " node will never select anything", getLocation());
      }
      return Literal.makeEmptySequence();
    }

    // Test whether the axis ever selects a node of the right kind, when
    // starting at this context node
    if (!targetUType.overlaps(testUType)) {
      if (warnings) {
        visitor.issueWarning("The " + AxisInfo.axisName[axis] + " axis starting at " + originUType.toStringWithIndefiniteArticle() + " will never select " + test.getUType().toStringWithIndefiniteArticle(), getLocation());
      }
      return Literal.makeEmptySequence();
    }

    // For an X-or-self axis, if X never selects anything, then substitute the
    // self axis.
    byte nonSelf = AxisInfo.excludeSelfAxis[axis];
    UType kind = test == null ? UType.ANY_NODE : test.getUType();
    if (axis != nonSelf) {
      UType nonSelfTarget = AxisInfo.getTargetUType(originUType, nonSelf);
      if (!nonSelfTarget.overlaps(testUType)) {
        axis = AxisInfo.SELF;
        targetUType = AxisInfo.getTargetUType(originUType, axis);
      }
    }

    ItemType target = targetUType.toItemType();
    if (test == null || test instanceof AnyNodeTest) {
      itemType = target;
    } else if (target instanceof AnyNodeTest || targetUType.subsumes(test.getUType())) {
      itemType = test;
    } else {
      itemType = new CombinedNodeTest((NodeTest) target, Token.INTERSECT, test);
    }

    // Old code
    int origin = contextType.getPrimitiveType();

    if (test != null) {

      // If the content type of the context item is known, see whether the node
      // test can select anything

      if (contextType instanceof DocumentNodeTest && kind.equals(UType.ELEMENT)) {
        NodeTest elementTest = ((DocumentNodeTest) contextType).getElementTest();
        IntSet outermostElementNames = elementTest.getRequiredNodeNames();
        if (outermostElementNames != null) {
          IntSet selectedElementNames = test.getRequiredNodeNames();
          if (selectedElementNames != null) {
            if (axis == AxisInfo.CHILD) {
              // check that the name appearing in the step is one of the names
              // allowed by the nodetest

              if (selectedElementNames.intersect(outermostElementNames).isEmpty()) {
                if (warnings) {
                  visitor.issueWarning("Starting at a document node, the step is selecting an element whose name " + "is not among the names of child elements permitted for this document node type", getLocation());
                }

                return Literal.makeEmptySequence();
              }

              if (env.getPackageData().isSchemaAware() && elementTest instanceof SchemaNodeTest && outermostElementNames.size() == 1) {
                IntIterator oeni = outermostElementNames.iterator();
                int outermostElementName = oeni.hasNext() ? oeni.next() : -1;
                SchemaDeclaration decl = config.getElementDeclaration(outermostElementName);
                if (decl == null) {
                  if (warnings) {
                    visitor.issueWarning("Element " + config.getNamePool().getDisplayName(outermostElementName) + " is not declared in the schema", getLocation());
                  }
                  itemType = elementTest;
                } else {
                  SchemaType contentType = decl.getType();
                  itemType = new CombinedNodeTest(elementTest, Token.INTERSECT, new ContentTypeTest(Type.ELEMENT, contentType, config, true));
                }
              } else {
                itemType = elementTest;
              }
              return this;

            } else if (axis == AxisInfo.DESCENDANT) {
              // check that the name appearing in the step is one of the names
              // allowed by the nodetest
              boolean canMatchOutermost = !selectedElementNames.intersect(outermostElementNames).isEmpty();
              if (!canMatchOutermost) {
                // The expression /descendant::x starting at the document node
                // doesn't match the outermost
                // element, so replace it by child::*/descendant::x, and check
                // that
                Expression path = ExpressionTool.makePathExpression(new AxisExpression(AxisInfo.CHILD, elementTest), new AxisExpression(AxisInfo.DESCENDANT, test), false);
                ExpressionTool.copyLocationInfo(this, path);
                return path.typeCheck(visitor, contextInfo);
              }
            }
          }
        }
      }

      SchemaType contentType = ((NodeTest) contextType).getContentType();
      if (contentType == AnyType.getInstance()) {
        // fast exit in non-schema-aware case
        return this;
      }

      if (!env.getPackageData().isSchemaAware()) {
        SchemaType ct = test.getContentType();
        if (!(ct == AnyType.getInstance() || ct == Untyped.getInstance() || ct == AnySimpleType.getInstance() || ct == BuiltInAtomicType.ANY_ATOMIC || ct == BuiltInAtomicType.UNTYPED_ATOMIC || ct == BuiltInAtomicType.STRING)) {
          // TODO: this test could be more precise, e.g. string is not possible
          // for elements and attribute nodes
          if (warnings) {
            visitor.issueWarning("The " + AxisInfo.axisName[axis] + " axis will never select any typed nodes, " + "because the expression is being compiled in an environment that is not schema-aware", getLocation());
          }
          return Literal.makeEmptySequence();
        }
      }

      int targetfp = test.getFingerprint();
      StructuredQName targetName = test.getMatchingNodeName();

      if (contentType.isSimpleType()) {
        if (warnings) {
          if ((axis == AxisInfo.CHILD || axis == AxisInfo.DESCENDANT || axis == AxisInfo.DESCENDANT_OR_SELF) && UType.PARENT_NODE_KINDS.union(UType.ATTRIBUTE).subsumes(kind)) {
            visitor.issueWarning("The " + AxisInfo.axisName[axis] + " axis will never select any " + kind + " nodes when starting at " + (origin == Type.ATTRIBUTE ? "an attribute node" : getStartingNodeDescription(contentType)), getLocation());
          } else if (axis == AxisInfo.CHILD && kind.equals(UType.TEXT) && (getParentExpression() instanceof Atomizer)) {
            visitor.issueWarning("Selecting the text nodes of an element with simple content may give the " + "wrong answer in the presence of comments or processing instructions. It is usually " + "better to omit the '/text()' step", getLocation());
          } else if (axis == AxisInfo.ATTRIBUTE) {
            @SuppressWarnings("rawtypes")
            Iterator extensions = config.getExtensionsOfType(contentType);
            boolean found = false;
            if (targetfp == -1) {
              while (extensions.hasNext()) {
                ComplexType extension = (ComplexType) extensions.next();
                if (extension.allowsAttributes()) {
                  found = true;
                  break;
                }
              }
            } else {
              while (extensions.hasNext()) {
                ComplexType extension = (ComplexType) extensions.next();
                try {
                  if (extension.getAttributeUseType(targetName) != null) {
                    found = true;
                    break;
                  }
                } catch (SchemaException e) {
                  // ignore the error
                }
              }
            }
            if (!found) {
              visitor.issueWarning("The " + AxisInfo.axisName[axis] + " axis will never select " + (targetName == null ? "any attribute nodes" : "an attribute node named " + getDiagnosticName(targetName, env)) + " when starting at " + getStartingNodeDescription(contentType), getLocation());
              // Despite the warning, leave the expression unchanged. This is
              // because
              // we don't necessarily know about all extended types at compile
              // time:
              // in particular, we don't seal the XML Schema namespace to block
              // extensions
              // of built-in types
            }
          }
        }
      } else if (((ComplexType) contentType).isSimpleContent() && (axis == AxisInfo.CHILD || axis == AxisInfo.DESCENDANT || axis == AxisInfo.DESCENDANT_OR_SELF) && UType.PARENT_NODE_KINDS.subsumes(kind)) {
        // We don't need to consider extended types here, because a type with
        // complex content
        // can never be defined as an extension of a type with simple content
        if (warnings) {
          visitor.issueWarning("The " + AxisInfo.axisName[axis] + " axis will never select any " + kind.toString() + " nodes when starting at " + getStartingNodeDescription(contentType) + ", as this type requires simple content", getLocation());
        }
        return Literal.makeEmptySequence();
      } else if (((ComplexType) contentType).isEmptyContent() && (axis == AxisInfo.CHILD || axis == AxisInfo.DESCENDANT || axis == AxisInfo.DESCENDANT_OR_SELF)) {
        for (@SuppressWarnings("rawtypes")
        Iterator iter = config.getExtensionsOfType(contentType); iter.hasNext();) {
          ComplexType extension = (ComplexType) iter.next();
          if (!extension.isEmptyContent()) {
            return this;
          }
        }
        if (warnings) {
          visitor.issueWarning("The " + AxisInfo.axisName[axis] + " axis will never select any" + " nodes when starting at " + getStartingNodeDescription(contentType) + ", as this type requires empty content", getLocation());
        }
        return Literal.makeEmptySequence();
      } else if (axis == AxisInfo.ATTRIBUTE) {
        if (targetfp == -1) {
          if (warnings) {
            if (!((ComplexType) contentType).allowsAttributes()) {
              visitor.issueWarning("The complex type " + contentType.getDescription() + " allows no attributes other than the standard attributes in the xsi namespace", getLocation());
            }
          }
        } else {
          try {
            SchemaType schemaType;
            if (targetfp == StandardNames.XSI_TYPE) {
              schemaType = BuiltInAtomicType.QNAME;
            } else if (targetfp == StandardNames.XSI_SCHEMA_LOCATION) {
              schemaType = BuiltInListType.ANY_URIS;
            } else if (targetfp == StandardNames.XSI_NO_NAMESPACE_SCHEMA_LOCATION) {
              schemaType = BuiltInAtomicType.ANY_URI;
            } else if (targetfp == StandardNames.XSI_NIL) {
              schemaType = BuiltInAtomicType.BOOLEAN;
            } else {
              schemaType = ((ComplexType) contentType).getAttributeUseType(targetName);
            }
            if (schemaType == null) {
              if (warnings) {
                visitor.issueWarning("The complex type " + contentType.getDescription() + " does not allow an attribute named " + getDiagnosticName(targetName, env), getLocation());
                return Literal.makeEmptySequence();
              }
            } else {
              itemType = new CombinedNodeTest(test, Token.INTERSECT, new ContentTypeTest(Type.ATTRIBUTE, schemaType, config, false));
            }
          } catch (SchemaException e) {
            // ignore the exception
          }
        }
      } else if (axis == AxisInfo.CHILD && kind.equals(UType.ELEMENT)) {
        try {
          StructuredQName childElement = targetName;
          if (targetName == null) {
            // select="child::*"
            if (((ComplexType) contentType).containsElementWildcard()) {
              return this;
            }
            HashSet<StructuredQName> children = new HashSet<StructuredQName>();
            ((ComplexType) contentType).gatherAllPermittedChildren(children, false);
            if (children.isEmpty()) {
              if (warnings) {
                visitor.issueWarning("The complex type " + contentType.getDescription() + " does not allow children", getLocation());
              }
              return Literal.makeEmptySequence();
            }
            // if (children.contains(-1)) {
            // return this;
            // }
            if (children.size() == 1) {
              Iterator<StructuredQName> iter = children.iterator();
              if (iter.hasNext()) {
                childElement = iter.next();
              }
            } else {
              return this;
            }
          }
          SchemaType schemaType = ((ComplexType) contentType).getElementParticleType(childElement, true);
          if (schemaType == null) {
            if (warnings) {
              String message = "The complex type " + contentType.getDescription() + " does not allow a child element named " + getDiagnosticName(childElement, env);
              HashSet<StructuredQName> permitted = new HashSet<StructuredQName>();
              ((ComplexType) contentType).gatherAllPermittedChildren(permitted, false);
              for (StructuredQName sq : permitted) {
                if (sq.getLocalPart().equals(childElement.getLocalPart()) && !sq.equals(childElement)) {
                  message += ". Perhaps the namespace is " + (childElement.hasURI("") ? "missing" : "wrong") + ", and " + sq.getEQName() + " was intended?";
                  break;
                }
              }
              visitor.issueWarning(message, getLocation());
            }
            return Literal.makeEmptySequence();
          } else {
            itemType = new CombinedNodeTest(test, Token.INTERSECT, new ContentTypeTest(Type.ELEMENT, schemaType, getConfiguration(), true));
            computedCardinality = ((ComplexType) contentType).getElementParticleCardinality(childElement, true);
            ExpressionTool.resetStaticProperties(this);
            if (computedCardinality == StaticProperty.ALLOWS_ZERO) {
              // this shouldn't happen, because we've already checked for this a
              // different way.
              // but it's worth being safe (there was a bug involving an
              // incorrect inference here)
              visitor.issueWarning("The complex type " + contentType.getDescription() + " appears not to allow a child element named " + getDiagnosticName(childElement, env), getLocation());
              return Literal.makeEmptySequence();
            }
            if (!Cardinality.allowsMany(computedCardinality)) {
              // if there can be at most one child of this name, create a
              // FirstItemExpression
              // to stop the search after the first one is found
              return FirstItemExpression.makeFirstItemExpression(this);
            }
          }
        } catch (SchemaException e) {
          // ignore the exception
        }
      } else if (axis == AxisInfo.DESCENDANT && kind.equals(UType.ELEMENT) && targetfp != -1) {
        // when searching for a specific element on the descendant axis, try to
        // produce a more
        // specific path that avoids searching branches of the tree where the
        // element cannot occur
        try {
          HashSet<StructuredQName> descendants = new HashSet<StructuredQName>();
          ((ComplexType) contentType).gatherAllPermittedDescendants(descendants);
          if (descendants.contains(StandardNames.getStructuredQName(StandardNames.XS_INVALID_NAME))) {
            return this;
          }
          if (descendants.contains(targetName)) {
            HashSet<StructuredQName> children = new HashSet<StructuredQName>();
            ((ComplexType) contentType).gatherAllPermittedChildren(children, false);
            HashSet<StructuredQName> usefulChildren = new HashSet<StructuredQName>();
            boolean considerSelf = false;
            boolean considerDescendants = false;
            for (StructuredQName c : children) {
              if (c.equals(targetName)) {
                usefulChildren.add(c);
                considerSelf = true;
              }
              SchemaType st = ((ComplexType) contentType).getElementParticleType(c, true);
              if (st == null) {
                throw new AssertionError("Can't find type for child element " + c);
              }
              if (st instanceof ComplexType) {
                HashSet<StructuredQName> subDescendants = new HashSet<StructuredQName>();
                ((ComplexType) st).gatherAllPermittedDescendants(subDescendants);
                if (subDescendants.contains(targetName)) {
                  usefulChildren.add(c);
                  considerDescendants = true;
                }
              }
            }
            itemType = test;
            if (considerDescendants) {
              SchemaType st = ((ComplexType) contentType).getDescendantElementType(targetName);
              if (st != AnyType.getInstance()) {
                itemType = new CombinedNodeTest(test, Token.INTERSECT, new ContentTypeTest(Type.ELEMENT, st, config, true));
              }
              // return this;
            }
            if (usefulChildren.size() < children.size()) {
              NodeTest childTest = makeUnionNodeTest(usefulChildren, getConfiguration().getNamePool());
              AxisExpression first = new AxisExpression(AxisInfo.CHILD, childTest);
              ExpressionTool.copyLocationInfo(this, first);
              byte nextAxis;
              if (considerSelf) {
                nextAxis = considerDescendants ? AxisInfo.DESCENDANT_OR_SELF : AxisInfo.SELF;
              } else {
                nextAxis = AxisInfo.DESCENDANT;
              }
              AxisExpression next = new AxisExpression(nextAxis, (NodeTest) itemType);
              ExpressionTool.copyLocationInfo(this, next);
              Expression path = ExpressionTool.makePathExpression(first, next, false);
              ExpressionTool.copyLocationInfo(this, path);
              return path.typeCheck(visitor, contextInfo);
            }
          } else {
            if (warnings) {
              visitor.issueWarning("The complex type " + contentType.getDescription() + " does not allow a descendant element named " + getDiagnosticName(targetName, env), getLocation());
            }
          }
        } catch (SchemaException e) {
          throw new AssertionError(e);
        }

      }
    }

    return this;
  }

  /*
   * Get a string representation of a name to use in diagnostics
   */

  private static String getDiagnosticName(StructuredQName name, StaticContext env) {
    String uri = name.getURI();
    if (uri.equals("")) {
      return name.getLocalPart();
    } else {
      NamespaceResolver resolver = env.getNamespaceResolver();
      for (Iterator<String> it = resolver.iteratePrefixes(); it.hasNext();) {
        String prefix = it.next();
        if (uri.equals(resolver.getURIForPrefix(prefix, true))) {
          if (prefix.isEmpty()) {
            return "Q{" + uri + "}" + name.getLocalPart();
          } else {
            return prefix + ":" + name.getLocalPart();
          }
        }
      }
    }
    return "Q{" + uri + "}" + name.getLocalPart();
  }

  private static String getStartingNodeDescription(SchemaType type) {
    String s = type.getDescription();
    if (s.startsWith("of element")) {
      return "a valid element named" + s.substring("of element".length());
    } else if (s.startsWith("of attribute")) {
      return "a valid attribute named" + s.substring("of attribute".length());
    } else {
      return "a node with " + (type.isSimpleType() ? "simple" : "complex") + " type " + s;
    }
  }

  /**
   * Make a union node test for a set of supplied element fingerprints
   *
   * @param elements
   *          the set of integer element fingerprints to be tested for. Must not
   *          be empty.
   * @param pool
   *          the name pool
   * @return a NodeTest that returns true if the node is an element whose name
   *         is one of the names in this set
   */

  private NodeTest makeUnionNodeTest(HashSet<StructuredQName> elements, NamePool pool) {
    NodeTest test = null;
    for (StructuredQName fp : elements) {
      NodeTest nextTest = new NameTest(Type.ELEMENT, fp.getURI(), fp.getLocalPart(), pool);
      if (test == null) {
        test = nextTest;
      } else {
        test = new CombinedNodeTest(test, Token.UNION, nextTest);
      }
    }
    return test;
  }

  /**
   * Get the static type of the context item for this AxisExpression. May be
   * null if not known.
   *
   * @return the statically-inferred type, or null if not known
   */

  public ItemType getContextItemType() {
    return staticInfo.getItemType();
  }

  /**
   * Perform optimisation of an expression and its subexpressions.
   * <p/>
   * <p>
   * This method is called after all references to functions and variables have
   * been resolved to the declaration of the function or variable, and after all
   * type checking has been done.
   * </p>
   *
   * @param visitor
   *          an expression visitor
   * @param contextInfo
   *          the static type of "." at the point where this expression is
   *          invoked. The parameter is set to null if it is known statically
   *          that the context item will be undefined. If the type of the
   *          context item is not known statically, the argument is set to
   *          {@link net.sf.saxon.type.Type#ITEM_TYPE}
   * @return the original expression, rewritten if appropriate to optimize
   *         execution
   */

  public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) {
    doneOptimize = true; // This ensures no more warnings about empty axes,
                         // because (a) we've probably output the
                         // warning already, and (b) we're now looking at a
                         // different expression from what the user
                         // wrote. In particular, prevent spurious warnings
                         // after function inlining.
    staticInfo = contextInfo;
    return this;
  }

  /**
   * Return the estimated cost of evaluating an expression. This is a very crude
   * measure based on the syntactic form of the expression (we have no knowledge
   * of data values). We take the cost of evaluating a simple scalar comparison
   * or arithmetic expression as 1 (one), and we assume that a sequence has
   * length 5. The resulting estimates may be used, for example, to reorder the
   * predicates in a filter expression so cheaper predicates are evaluated
   * first.
   */
  @Override
  public int getCost() {
    switch (axis) {
    case AxisInfo.SELF:
    case AxisInfo.PARENT:
    case AxisInfo.ATTRIBUTE:
      return 1;
    case AxisInfo.CHILD:
    case AxisInfo.FOLLOWING_SIBLING:
    case AxisInfo.PRECEDING_SIBLING:
    case AxisInfo.ANCESTOR:
    case AxisInfo.ANCESTOR_OR_SELF:
      return 5;
    default:
      return 20;
    }
  }

  /**
   * Is this expression the same as another expression?
   */

  public boolean equals(Object other) {
    if (!(other instanceof AxisExpression)) {
      return false;
    }
    if (axis != ((ContextPassingAxisExpression) other).axis) {
      return false;
    }
    if (test == null) {
      return ((ContextPassingAxisExpression) other).test == null;
    }
    return test.toString().equals(((ContextPassingAxisExpression) other).test.toString());
  }

  /**
   * get HashCode for comparing two expressions
   */

  public int hashCode() {
    // generate an arbitrary hash code that depends on the axis and the node
    // test
    int h = 9375162 + axis << 20;
    if (test != null) {
      h ^= test.getPrimitiveType() << 16;
      h ^= test.getFingerprint();
    }
    return h;
  }

  /**
   * Determine which aspects of the context the expression depends on. The
   * result is a bitwise-or'ed value composed from constants such as
   * XPathContext.VARIABLES and XPathContext.CURRENT_NODE
   */

  // public int getIntrinsicDependencies() {
  // return StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
  // }

  /**
   * Copy an expression. This makes a deep copy.
   *
   * @return the copy of the original expression
   * @param rebindings
   */

  /* @NotNull */
  public Expression copy(RebindingMap rebindings) {
    ContextPassingAxisExpression a2 = new ContextPassingAxisExpression(axis, test);
    a2.itemType = itemType;
    a2.staticInfo = staticInfo;
    a2.computedCardinality = computedCardinality;
    a2.doneTypeCheck = doneTypeCheck;
    a2.doneOptimize = doneOptimize;
    ExpressionTool.copyLocationInfo(this, a2);
    return a2;
  }

  /**
   * Get the static properties of this expression (other than its type). The
   * result is bit-signficant. These properties are used for optimizations. In
   * general, if property bit is set, it is true, but if it is unset, the value
   * is unknown.
   */

  public int computeSpecialProperties() {
    return StaticProperty.CONTEXT_DOCUMENT_NODESET | StaticProperty.SINGLE_DOCUMENT_NODESET | StaticProperty.NON_CREATIVE | (AxisInfo.isForwards[axis] ? StaticProperty.ORDERED_NODESET : StaticProperty.REVERSE_DOCUMENT_ORDER) | (AxisInfo.isPeerAxis[axis] || isPeerNodeTest(test) ? StaticProperty.PEER_NODESET : 0) | (AxisInfo.isSubtreeAxis[axis] ? StaticProperty.SUBTREE_NODESET : 0) | (axis == AxisInfo.ATTRIBUTE || axis == AxisInfo.NAMESPACE ? StaticProperty.ATTRIBUTE_NS_NODESET : 0);
  }

  /**
   * Determine whether a node test is a peer node test. A peer node test is one
   * that, if it matches a node, cannot match any of its descendants. For
   * example, text() is a peer node-test.
   *
   * @param test
   *          the node test
   * @return true if nodes selected by this node-test will never contain each
   *         other as descendants
   */

  private static boolean isPeerNodeTest(NodeTest test) {
    if (test == null) {
      return false;
    }
    UType uType = test.getUType();
    if (uType.overlaps(UType.ELEMENT)) {
      // can match elements; for the moment, assume these can contain each other
      return false;
    } else if (uType.overlaps(UType.DOCUMENT)) {
      // can match documents; return false if we can also match non-documents
      return uType.equals(UType.DOCUMENT);
    } else {
      return true;
    }
  }

  /**
   * Determine the data type of the items returned by this expression
   *
   * @return Type.NODE or a subtype, based on the NodeTest in the axis step,
   *         plus information about the content type if this is known from
   *         schema analysis
   */

  /* @NotNull */
  public final ItemType getItemType() {
    if (itemType != null) {
      return itemType;
    }
    int p = AxisInfo.principalNodeType[axis];
    switch (p) {
    case Type.ATTRIBUTE:
    case Type.NAMESPACE:
      return NodeKindTest.makeNodeKindTest(p);
    default:
      if (test == null) {
        return AnyNodeTest.getInstance();
      } else {
        return test;
      }
    }
  }

  /**
   * Determine the intrinsic dependencies of an expression, that is, those which
   * are not derived from the dependencies of its subexpressions. For example,
   * position() has an intrinsic dependency on the context position, while
   * (position()+1) does not. The default implementation of the method returns
   * 0, indicating "no dependencies".
   *
   * @return a set of bit-significant flags identifying the "intrinsic"
   *         dependencies. The flags are documented in class
   *         net.sf.saxon.value.StaticProperty
   */
  @Override
  public int getIntrinsicDependencies() {
    return StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
  }

  /**
   * Determine the cardinality of the result of this expression
   */

  public final int computeCardinality() {
    if (computedCardinality != -1) {
      // This takes care of the case where cardinality was computed during type
      // checking of the child axis
      return computedCardinality;
    }
    NodeTest originNodeType;
    NodeTest nodeTest = test;
    ItemType contextItemType = staticInfo.getItemType();
    if (contextItemType instanceof NodeTest) {
      originNodeType = (NodeTest) contextItemType;
    } else if (contextItemType instanceof AnyItemType) {
      originNodeType = AnyNodeTest.getInstance();
    } else {
      // context item not a node - we'll report a type error somewhere along the
      // line
      return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }
    if (axis == AxisInfo.ATTRIBUTE && nodeTest instanceof NameTest) {
      SchemaType contentType = originNodeType.getContentType();
      if (contentType instanceof ComplexType) {
        try {
          return ((ComplexType) contentType).getAttributeUseCardinality(nodeTest.getMatchingNodeName());
        } catch (SchemaException err) {
          // shouldn't happen; play safe
          return StaticProperty.ALLOWS_ZERO_OR_ONE;
        }
      } else if (contentType instanceof SimpleType) {
        return StaticProperty.EMPTY;
      }
      return StaticProperty.ALLOWS_ZERO_OR_ONE;
    } else if (axis == AxisInfo.DESCENDANT && nodeTest instanceof NameTest && nodeTest.getPrimitiveType() == Type.ELEMENT) {
      SchemaType contentType = originNodeType.getContentType();
      if (contentType instanceof ComplexType) {
        try {
          return ((ComplexType) contentType).getDescendantElementCardinality(nodeTest.getMatchingNodeName());
        } catch (SchemaException err) {
          // shouldn't happen; play safe
          return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
      } else {
        return StaticProperty.EMPTY;
      }

    } else if (axis == AxisInfo.SELF) {
      return StaticProperty.ALLOWS_ZERO_OR_ONE;
    } else {
      return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }
    // the parent axis isn't handled by this class
  }

  /**
   * Determine whether the expression can be evaluated without reference to the
   * part of the context document outside the subtree rooted at the context
   * node.
   *
   * @return true if the expression has no dependencies on the context node, or
   *         if the only dependencies on the context node are downward
   *         selections using the self, child, descendant, attribute, and
   *         namespace axes.
   */

  public boolean isSubtreeExpression() {
    return AxisInfo.isSubtreeAxis[axis];
  }

  /**
   * Get the axis
   *
   * @return the axis number, for example {@link net.sf.saxon.om.AxisInfo#CHILD}
   */

  public byte getAxis() {
    return axis;
  }

  /**
   * Get the NodeTest. Returns null if the AxisExpression can return any node.
   *
   * @return the node test, or null if all nodes are returned
   */

  public NodeTest getNodeTest() {
    return test;
  }

  /**
   * Add a representation of this expression to a PathMap. The PathMap captures
   * a map of the nodes visited by an expression in a source tree.
   *
   * @param pathMap
   *          the PathMap to which the expression should be added
   * @param pathMapNodeSet
   *          the PathMapNodeSet to which the paths embodied in this expression
   *          should be added
   * @return the pathMapNode representing the focus established by this
   *         expression, in the case where this expression is the first operand
   *         of a path expression or filter expression
   */

  public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
    if (pathMapNodeSet == null) {
      ContextItemExpression cie = new ContextItemExpression();
      // cie.setContainer(getContainer());
      pathMapNodeSet = new PathMap.PathMapNodeSet(pathMap.makeNewRoot(cie));
    }
    return pathMapNodeSet.createArc(axis, test == null ? AnyNodeTest.getInstance() : test);
  }

  /**
   * Ask whether there is a possibility that the context item will be undefined
   *
   * @return true if this is a possibility
   */

  public boolean isContextPossiblyUndefined() {
    return staticInfo.isPossiblyAbsent();
  }

  /**
   * Convert this expression to an equivalent XSLT pattern
   *
   * @param config
   *          the Saxon configuration
   * @param is30
   *          true if this is XSLT 3.0
   * @return the equivalent pattern
   * @throws net.sf.saxon.trans.XPathException
   *           if conversion is not possible
   */
  @Override
  public Pattern toPattern(Configuration config, boolean is30) throws XPathException {

    NodeTest test = getNodeTest();
    Pattern pat;

    if (test == null) {
      test = AnyNodeTest.getInstance();
    }
    if (test instanceof AnyNodeTest && (axis == AxisInfo.CHILD || axis == AxisInfo.DESCENDANT || axis == AxisInfo.SELF)) {
      test = AnyChildNodeTest.getInstance();
    }
    int kind = test.getPrimitiveType();
    if (axis == AxisInfo.SELF) {
      pat = new NodeTestPattern(test);
    } else if (axis == AxisInfo.ATTRIBUTE) {
      if (kind == Type.NODE) {
        // attribute::node() matches any attribute, and only an attribute
        pat = new NodeTestPattern(NodeKindTest.ATTRIBUTE);
      } else if (!AxisInfo.containsNodeKind(axis, kind)) {
        // for example, attribute::comment()
        pat = new NodeTestPattern(ErrorType.getInstance());
      } else {
        pat = new NodeTestPattern(test);
      }
    } else if (axis == AxisInfo.CHILD || axis == AxisInfo.DESCENDANT || axis == AxisInfo.DESCENDANT_OR_SELF) {
      if (kind != Type.NODE && !AxisInfo.containsNodeKind(axis, kind)) {
        pat = new NodeTestPattern(ErrorType.getInstance());
      } else {
        pat = new NodeTestPattern(test);
      }
    } else if (axis == AxisInfo.NAMESPACE) {
      if (kind == Type.NODE) {
        // namespace::node() matches any attribute, and only an attribute
        pat = new NodeTestPattern(NodeKindTest.NAMESPACE);
      } else if (!AxisInfo.containsNodeKind(axis, kind)) {
        // for example, namespace::comment()
        pat = new NodeTestPattern(ErrorType.getInstance());
      } else {
        pat = new NodeTestPattern(test);
      }
    } else {
      throw new XPathException("Only downwards axes are allowed in a pattern", "XTSE0340");
    }
    ExpressionTool.copyLocationInfo(this, pat);
    return pat;
  }

  @Override
  public int getImplementationMethod() {
    return ITERATE_METHOD;
  }

  /**
   * Evaluate the path-expression in a given context to return a NodeSet
   *
   * @param context
   *          the evaluation context
   */

  /* @NotNull */
  public AxisIterator iterate(XPathContext context) throws XPathException {
    Item item = context.getContextItem();
    if (item == null) {
      // Might as well do the test anyway, whether or not contextMaybeUndefined
      // is set
      XPathException err = new XPathException("The context item for axis step " + toString() + " is absent");
      err.setErrorCode("XPDY0002");
      err.setXPathContext(context);
      err.setLocation(getLocation());
      err.setIsTypeError(true);
      throw err;
    }
    try {
      if (test == null) {
        return ((XMLIndexNodeInfo) item).iterateAxis(axis, context);
      } else {
        return ((XMLIndexNodeInfo) item).iterateAxis(axis, test, context);
      }
    } catch (ClassCastException cce) {
      XPathException err = new XPathException("The context item for axis step " + toString() + " is not a node");
      err.setErrorCode("XPTY0020");
      err.setXPathContext(context);
      err.setLocation(getLocation());
      err.setIsTypeError(true);
      throw err;
    } catch (UnsupportedOperationException err) {
      if (err.getCause() instanceof XPathException) {
        XPathException ec = (XPathException) err.getCause();
        ec.maybeSetLocation(getLocation());
        ec.maybeSetContext(context);
        throw ec;
      } else {
        // the namespace axis is not supported for all tree implementations
        dynamicError(err.getMessage(), "XPST0010", context);
        return null;
      }
    }
  }

  /**
   * Iterate the axis from a given starting node, without regard to context
   *
   * @param origin
   *          the starting node
   * @return the iterator over the axis
   */

  public AxisIterator iterate(NodeInfo origin) {
    if (test == null) {
      return origin.iterateAxis(axis);
    } else {
      return origin.iterateAxis(axis, test);
    }
  }

  /**
   * Diagnostic print of expression structure. The abstract expression tree is
   * written to the supplied output destination.
   */

  public void export(ExpressionPresenter destination) throws XPathException {
    destination.startElement("axis", this);
    destination.emitAttribute("name", AxisInfo.axisName[axis]);
    destination.emitAttribute("nodeTest", test == null ? "node()" : test.toString());
    if ("JS".equals(destination.getOption("target"))) {
      NodeTest known = AnyNodeTest.getInstance();
      if (axis == AxisInfo.ATTRIBUTE) {
        known = NodeKindTest.makeNodeKindTest(Type.ATTRIBUTE);
      } else if (axis == AxisInfo.NAMESPACE) {
        known = NodeKindTest.makeNodeKindTest(Type.NAMESPACE);
      }
      try {
        destination.emitAttribute("jsTest", test == null ? "return true;" : test.generateJavaScriptItemTypeTest(known));
      } catch (XPathException e) {
        e.maybeSetLocation(getLocation());
        throw e;
      }
    }
    destination.endElement();
  }

  /**
   * Represent the expression as a string. The resulting string will be a valid
   * XPath 3.0 expression with no dependencies on namespace bindings other than
   * the binding of the prefix "xs" to the XML Schema namespace.
   *
   * @return the expression as a string in XPath 3.0 syntax
   */

  public String toString() {
    FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.C16);
    fsb.append(AxisInfo.axisName[axis]);
    fsb.append("::");
    fsb.append(test == null ? "node()" : test.toString());
    return fsb.toString();
  }

  @Override
  public String toShortString() {
    FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.C16);
    if (axis == AxisInfo.CHILD) {
      // no action
    } else if (axis == AxisInfo.ATTRIBUTE) {
      fsb.append("@");
    } else {
      fsb.append(AxisInfo.axisName[axis]);
      fsb.append("::");
    }
    if (test == null) {
      fsb.append("node()");
    } else if (test instanceof NameTest) {
      if (((NameTest) test).getNodeKind() != AxisInfo.principalNodeType[axis]) {
        fsb.append(test.toString());
      } else {
        fsb.append(test.getMatchingNodeName().getDisplayName());
      }
    } else {
      fsb.append(test.toString());
    }
    return fsb.toString();
  }

  /**
   * Find any necessary preconditions for the satisfaction of this expression as
   * a set of boolean expressions to be evaluated on the context node
   *
   * @return A set of conditions, or null if none have been computed
   */
  public Set<Expression> getPreconditions() {
    HashSet<Expression> pre = new HashSet<Expression>(1);
    /*
     * Expression args[] = new Expression[1]; args[0] = this.copy();
     * pre.add(SystemFunctionCall.makeSystemFunction( "exists", args));
     */
    Expression a = this.copy(new RebindingMap());
    a.setRetainedStaticContext(getRetainedStaticContext());
    pre.add(a);
    return pre;
  }

}
