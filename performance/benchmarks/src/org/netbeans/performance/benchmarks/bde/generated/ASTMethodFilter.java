/* Generated By:JJTree: Do not edit this line. ASTMethodFilter.java */

package org.netbeans.performance.benchmarks.bde.generated;

public class ASTMethodFilter extends SimpleNode {
  public ASTMethodFilter(int id) {
    super(id);
  }

  public ASTMethodFilter(BDEParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(BDEParserVisitor visitor, Object data) throws Exception {
    return visitor.visit(this, data);
  }
}
