/* Generated By:JJTree: Do not edit this line. ASTIntegerDef.java */

package org.netbeans.performance.benchmarks.bde.generated;

public class ASTIntegerDef extends SimpleNode {
  public ASTIntegerDef(int id) {
    super(id);
  }

  public ASTIntegerDef(BDEParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(BDEParserVisitor visitor, Object data) throws Exception {
    return visitor.visit(this, data);
  }
}
