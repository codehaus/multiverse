import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.Template

@Grab(group = 'org.apache.velocity', module = 'velocity', version = '1.6.4')

class IgnoreMe {}


VelocityEngine engine = new VelocityEngine();
engine.init();

def refs = createRefs();

for (def param in refs) {
  generateTranlocal(engine, param);
  generateTransactionalObject(engine, param);
}
generateObjectPool(engine, refs)
generateTransaction(engine, refs)
generateMonoTransaction(engine, refs)
generateArrayTransaction(engine, refs)
generateArrayTreeTransaction(engine, refs)

class Ref {
  String tranlocal;
  String name;
  String type;
  String initialValue;
  int classIndex;
  String typeParameter;
  boolean specialization;
}

List<Ref> createRefs() {
  def result = []
  result.add new Ref(
            name: "Ref",
            tranlocal: "RefTranlocal",
            type : "E",
            typeParameter: '<E>',
            initialValue : 'null',
            classIndex: 0,
            specialization:true)
  result.add new Ref(
          name: "IntRef",
          tranlocal: "IntRefTranlocal",
          type : "int",
          typeParameter: '',
          initialValue : '0',
          classIndex : 1,
          specialization:true)
  result.add new Ref(
          name: "LongRef",
          tranlocal: "LongRefTranlocal",
          type : "long",
          typeParameter: '',
          initialValue : '0',
          classIndex: 2,
          specialization:true)
  result.add new Ref(
          name: "DoubleRef",
          tranlocal: "DoubleRefTranlocal",
          type : "double",
          typeParameter: '',
          initialValue : '0',
          classIndex: 3,
          specialization:true)
//  result.add new Ref(
//          name: "FloatRef",
//          tranlocal: "FloatRefTranlocal",
//          type : "float",
//          typeParameter: '',
//          initialValue : '0',
//          classIndex: 4,
//          specialization:true)
//  result.add new Ref(
//          name: "BooleanRef",
//          tranlocal: "BooleanRefTranlocal",
//          type : "boolean",
//          typeParameter: '',
//          initialValue : 'false',
//          classIndex: 5,
//          specialization:true)
  result.add new Ref(
          name: "BetaTransactionalObject",
          tranlocal: "Tranlocal",       
          type : "",
          typeParameter: '',
          initialValue : '',
          classIndex: -1,
          specialization:false)
  result
}

void generateObjectPool(VelocityEngine engine, List<Ref> refs){
  Template t = engine.getTemplate("src/main/java/org/multiverse/stms/beta/ObjectPool.vm");

  VelocityContext context = new VelocityContext();
  context.put("refs", refs);

  StringWriter writer = new StringWriter();
  t.merge(context, writer);

  File file = new File('src/main/java/org/multiverse/stms/beta/ObjectPool.java')
  file.createNewFile()
  file.text = writer.toString()
}

void generateMonoTransaction(VelocityEngine engine, List<Ref> refs){
  Template t = engine.getTemplate("src/main/java/org/multiverse/stms/beta/transactions/MonoBetaTransaction.vm");

  VelocityContext context = new VelocityContext();
  context.put("refs", refs);

  StringWriter writer = new StringWriter();
  t.merge(context, writer);

  File file = new File('src/main/java/org/multiverse/stms/beta/transactions/MonoBetaTransaction.java')
  file.createNewFile()
  file.text = writer.toString()
}

void generateTransaction(VelocityEngine engine, List<Ref> refs){
  Template t = engine.getTemplate("src/main/java/org/multiverse/stms/beta/transactions/BetaTransaction.vm");

  VelocityContext context = new VelocityContext();
  context.put("refs", refs);

  StringWriter writer = new StringWriter();
  t.merge(context, writer);

  File file = new File('src/main/java/org/multiverse/stms/beta/transactions/BetaTransaction.java')
  file.createNewFile()
  file.text = writer.toString()
}


void generateArrayTransaction(VelocityEngine engine, List<Ref> refs){
  Template t = engine.getTemplate("src/main/java/org/multiverse/stms/beta/transactions/ArrayBetaTransaction.vm");

  VelocityContext context = new VelocityContext();
  context.put("refs", refs);

  StringWriter writer = new StringWriter();
  t.merge(context, writer);

  File file = new File('src/main/java/org/multiverse/stms/beta/transactions/ArrayBetaTransaction.java')
  file.createNewFile()
  file.text = writer.toString()
}

void generateArrayTreeTransaction(VelocityEngine engine, List<Ref> refs){
  Template t = engine.getTemplate("src/main/java/org/multiverse/stms/beta/transactions/ArrayTreeBetaTransaction.vm");

  VelocityContext context = new VelocityContext();
  context.put("refs", refs);

  StringWriter writer = new StringWriter();
  t.merge(context, writer);

  File file = new File('src/main/java/org/multiverse/stms/beta/transactions/ArrayTreeBetaTransaction.java')
  file.createNewFile()
  file.text = writer.toString()
}

void generateTranlocal(VelocityEngine engine, Ref ref) {
  if(!ref.isSpecialization()){
    return;
  }

  Template t = engine.getTemplate('src/main/java/org/multiverse/stms/beta/refs/RefTranlocal.vm');

  VelocityContext context = new VelocityContext();
  context.put("ref", ref);

  StringWriter writer = new StringWriter();
  t.merge(context, writer);

  File file = new File('src/main/java/org/multiverse/stms/beta/refs', "${ref.tranlocal}.java")
  file.createNewFile()
  file.text = writer.toString()
}

void generateTransactionalObject(VelocityEngine engine, Ref ref) {
  if(!ref.isSpecialization()){
    return;
  }

  Template t = engine.getTemplate("src/main/java/org/multiverse/stms/beta/refs/Ref.vm");

  VelocityContext context = new VelocityContext();
  context.put("ref", ref);

  StringWriter writer = new StringWriter();
  t.merge(context, writer);

  File file = new File('src/main/java/org/multiverse/stms/beta/refs', "${ref.name}.java")
  file.createNewFile()
  file.text = writer.toString()
}

