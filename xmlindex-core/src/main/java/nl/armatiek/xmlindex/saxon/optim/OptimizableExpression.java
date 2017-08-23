package nl.armatiek.xmlindex.saxon.optim;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.VennExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import nl.armatiek.xmlindex.saxon.tree.XMLIndexNodeInfo;

public class OptimizableExpression extends VennExpression {

  public OptimizableExpression(Expression origExpr, Expression optExpr) {
    super(origExpr, Token.UNION, optExpr);
  }

  @Override
  public SequenceIterator iterate(XPathContext context) throws XPathException {
    Item item = context.getContextItem();
    if (item instanceof XMLIndexNodeInfo)
      return getRhsExpression().iterate(context);
    return getLhsExpression().iterate(context);
  }
  
  @Override
  public String getExpressionName() {
    return "optimizable-expression";
  }
  
  @Override
  protected String tag() {
    return getExpressionName();
  }
  
  @Override
  public void export(ExpressionPresenter out) throws XPathException {
    out.startElement(tag(), this);
    explainExtraAttributes(out);
    getLhsExpression().export(out);
    getRhsExpression().export(out);
    out.endElement();
  }
  
}