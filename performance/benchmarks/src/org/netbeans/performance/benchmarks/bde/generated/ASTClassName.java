/* Generated By:JJTree: Do not edit this line. ASTClassName.java */

package org.netbeans.performance.benchmarks.bde.generated;

public class ASTClassName extends SimpleNode {
  public ASTClassName(int id) {
    super(id);
  }

  public ASTClassName(BDEParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(BDEParserVisitor visitor, Object data) throws Exception {
    return visitor.visit(this, data);
  }
}
