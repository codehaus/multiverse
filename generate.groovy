import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.Template

@Grab(group = 'org.apache.velocity', module = 'velocity', version = '1.6.4')

class IgnoreMe {}

class AtomicClosure {
  String type
  String name
  String typeParameter
}

class AtomicBlock {
  String name
  boolean lean
}

class Transaction {
  String name
  boolean lean
  String poolName
  int poolIndex
  String superClass
}

class TransactionalObject {
  String tranlocal
  String name//the name of the reference.
  String type//the type of data it contains
  String initialValue//the initial value
  int classIndex
  String typeParameter
//  String parametrizedTranlocal
  String accessModifier//the access modifier the ref gets
  String functionClass//the class of the callable used for commuting operations
  boolean isReference
}

VelocityEngine engine = new VelocityEngine();
engine.init();

def refs = createTransactionalObjects();
def atomicClosures = createClosures();
def atomicBlocks = [new AtomicBlock(name: 'FatBetaAtomicBlock', lean: false),
        new AtomicBlock(name: 'LeanBetaAtomicBlock', lean: true)]

Transaction leanMonoTransaction = new Transaction(
        name: 'LeanMonoBetaTransaction',
        lean: true,
        poolName: 'POOL_TRANSACTIONTYPE_LEAN_MONO',
        poolIndex: 0,
        superClass: 'AbstractLeanBetaTransaction')
Transaction fatMonoTransaction = new Transaction(
        name: 'FatMonoBetaTransaction',
        lean: false,
        poolName: "POOL_TRANSACTIONTYPE_FAT_MONO",
        poolIndex: 1,
        superClass: 'AbstractFatBetaTransaction')
Transaction leanArrayTransaction = new Transaction(
        name: 'LeanArrayBetaTransaction',
        lean: true,
        poolName: "POOL_TRANSACTIONTYPE_LEAN_ARRAY",
        poolIndex: 2,
        superClass: 'AbstractLeanBetaTransaction')
Transaction fatArrayTransaction = new Transaction(
        name: 'FatArrayBetaTransaction',
        lean: false,
        poolName: 'POOL_TRANSACTIONTYPE_FAT_ARRAY',
        poolIndex: 3,
        superClass: 'AbstractFatBetaTransaction')
Transaction leanArrayTreeTransaction = new Transaction(
        name: 'LeanArrayTreeBetaTransaction',
        lean: true,
        poolName: 'POOL_TRANSACTIONTYPE_LEAN_ARRAYTREE',
        poolIndex: 4,
        superClass: 'AbstractLeanBetaTransaction')
Transaction fatArrayTreeTransaction = new Transaction(
        name: 'FatArrayTreeBetaTransaction',
        lean: false,
        poolName: 'POOL_TRANSACTIONTYPE_FAT_ARRAYTREE',
        poolIndex: 5,
        superClass: 'AbstractFatBetaTransaction')

def transactions = [leanMonoTransaction, fatMonoTransaction,
        leanArrayTransaction, fatArrayTransaction,
        leanArrayTreeTransaction, fatArrayTreeTransaction]

generateMonoTransaction(engine, refs, leanMonoTransaction)
generateMonoTransaction(engine, refs, fatMonoTransaction)
generateArrayTransaction(engine, refs, leanArrayTransaction)
generateArrayTransaction(engine, refs, fatArrayTransaction)
generateArrayTreeTransaction(engine, refs, leanArrayTreeTransaction)
generateArrayTreeTransaction(engine, refs, fatArrayTreeTransaction)

for (def param in refs) {
  generateTranlocal(engine, param)
  generateTransactionalObject(engine, param)
}

 def abstractTransactionalObject = new TransactionalObject(
          name: 'AbstractBetaTransactionalObject',
          tranlocal: 'Tranlocal',
          type: "",
          typeParameter: '',
          initialValue: '',
          classIndex: -1,
          accessModifier: 'abstract',
          functionClass: 'Function',
          isReference : false
)

generateTransactionalObject(engine,abstractTransactionalObject)

generateObjectPool(engine, refs, transactions)
generateTransaction(engine, refs, transactions)

for (def closure in atomicClosures) {
  generateAtomicClosure(engine, closure)
}

generateAtomicBlock(engine, atomicClosures)

for (def atomicBlock in atomicBlocks) {
  generateBetaAtomicBlock(engine, atomicBlock, atomicClosures)
}

List<AtomicClosure> createClosures() {
  def result = []
  result.add new AtomicClosure(
          name: 'AtomicClosure',
          type: 'E',
          typeParameter: '<E>'
  )
  result.add new AtomicClosure(
          name: 'AtomicIntClosure',
          type: 'int',
          typeParameter: ''
  )
  result.add new AtomicClosure(
          name: 'AtomicLongClosure',
          type: 'long',
          typeParameter: ''
  )
  result.add new AtomicClosure(
          name: 'AtomicDoubleClosure',
          type: 'double',
          typeParameter: ''
  )
  result.add new AtomicClosure(
          name: 'AtomicBooleanClosure',
          type: 'boolean',
          typeParameter: ''
  )
  result.add new AtomicClosure(
          name: 'AtomicVoidClosure',
          type: 'void',
          typeParameter: ''
  )
  result
}

List<TransactionalObject> createTransactionalObjects() {
  def result = []
  result.add new TransactionalObject(
          name: 'Ref',
          tranlocal: 'RefTranlocal',
          type: 'E',
          typeParameter: '<E>',
//          parametrizedTranlocal: 'RefTranlocal<E>',
          initialValue: 'null',
          classIndex: 0,
          accessModifier: 'final',
          functionClass: 'Function',
          isReference: true)
  result.add new TransactionalObject(
          name: 'IntRef',
          tranlocal: 'IntRefTranlocal',
//          parametrizedTranlocal: 'IntRefTranlocal',
          type: 'int',
          typeParameter: '',
          initialValue: '0',
          classIndex: 1,
          accessModifier: 'final',
          functionClass: 'IntFunction',
          isReference: true)
  result.add new TransactionalObject(
          name: 'LongRef',
          tranlocal: 'LongRefTranlocal',
          type: 'long',
          typeParameter: '',
          initialValue: '0',
          classIndex: 2,
          accessModifier: 'final',
          functionClass: 'LongFunction',
          isReference: true)
  result.add new TransactionalObject(
          name: 'BetaTransactionalObject',
          tranlocal: 'Tranlocal',
          type: "",
          typeParameter: '',
          initialValue: '',
          classIndex: -1,
          accessModifier: 'abstract',
          functionClass: 'Function',
          isReference: false)
  result
}

void generateAtomicClosure(VelocityEngine engine, AtomicClosure closure) {
  Template t = engine.getTemplate("src/main/java/org/multiverse/api/closures/AtomicClosure.vm")

  VelocityContext context = new VelocityContext()
  context.put("closure", closure)

  StringWriter writer = new StringWriter()
  t.merge(context, writer)

  File file = new File("src/main/java/org/multiverse/api/closures/${closure.name}.java")
  file.createNewFile()
  file.text = writer.toString()
}

void generateBetaAtomicBlock(VelocityEngine engine, AtomicBlock atomicBlock, List<AtomicClosure> closures) {
  Template t = engine.getTemplate("src/main/java/org/multiverse/stms/beta/BetaAtomicBlock.vm")

  VelocityContext context = new VelocityContext()
  context.put("atomicBlock", atomicBlock)
  context.put("closures", closures)

  StringWriter writer = new StringWriter()
  t.merge(context, writer)

  File file = new File("src/main/java/org/multiverse/stms/beta/${atomicBlock.name}.java")
  file.createNewFile()
  file.text = writer.toString()
}

void generateAtomicBlock(VelocityEngine engine, List<AtomicClosure> closures) {
  Template t = engine.getTemplate("src/main/java/org/multiverse/api/AtomicBlock.vm")

  VelocityContext context = new VelocityContext()
  context.put("closures", closures)

  StringWriter writer = new StringWriter()
  t.merge(context, writer)

  File file = new File('src/main/java/org/multiverse/api/AtomicBlock.java')
  file.createNewFile()
  file.text = writer.toString()
}

void generateObjectPool(VelocityEngine engine, List<TransactionalObject> transactionalObjects, List<Transaction> transactions) {
  Template t = engine.getTemplate("src/main/java/org/multiverse/stms/beta/BetaObjectPool.vm")

  VelocityContext context = new VelocityContext()
  context.put("transactionalObjects", transactionalObjects)
  context.put("transactions", transactions)

  StringWriter writer = new StringWriter()
  t.merge(context, writer)

  File file = new File('src/main/java/org/multiverse/stms/beta/BetaObjectPool.java')
  file.createNewFile()
  file.text = writer.toString()
}

void generateTransaction(VelocityEngine engine, List<TransactionalObject> transactionalObjects, List<Transaction> transactions) {
  Template t = engine.getTemplate("src/main/java/org/multiverse/stms/beta/transactions/BetaTransaction.vm")

  VelocityContext context = new VelocityContext()
  context.put("transactionalObjects", transactionalObjects)
  context.put("transactions", transactions)

  StringWriter writer = new StringWriter()
  t.merge(context, writer)

  File file = new File('src/main/java/org/multiverse/stms/beta/transactions/BetaTransaction.java')
  file.createNewFile()
  file.text = writer.toString()
}

void generateArrayTransaction(VelocityEngine engine, List<TransactionalObject> transactionalObjects, Transaction tx) {
  Template t = engine.getTemplate("src/main/java/org/multiverse/stms/beta/transactions/ArrayBetaTransaction.vm")

  VelocityContext context = new VelocityContext()
  context.put("transactionalObjects", transactionalObjects)
  context.put("transaction", tx)

  StringWriter writer = new StringWriter()
  t.merge(context, writer)

  File file = new File("src/main/java/org/multiverse/stms/beta/transactions/${tx.name}.java")
  file.createNewFile()
  file.text = writer.toString()
}

void generateArrayTreeTransaction(VelocityEngine engine, List<TransactionalObject> transactionalObjects, Transaction tx) {
  Template t = engine.getTemplate("src/main/java/org/multiverse/stms/beta/transactions/ArrayTreeBetaTransaction.vm")

  VelocityContext context = new VelocityContext()
  context.put("transactionalObjects", transactionalObjects)
  context.put("transaction", tx)

  StringWriter writer = new StringWriter()
  t.merge(context, writer)

  File file = new File("src/main/java/org/multiverse/stms/beta/transactions/${tx.name}.java")
  file.createNewFile()
  file.text = writer.toString()
}

void generateMonoTransaction(VelocityEngine engine, List<TransactionalObject> transactionalObjects, Transaction tx) {
  Template t = engine.getTemplate("src/main/java/org/multiverse/stms/beta/transactions/MonoBetaTransaction.vm")

  VelocityContext context = new VelocityContext()
  context.put("transactionalObjects", transactionalObjects)
  context.put("transaction", tx)

  StringWriter writer = new StringWriter()
  t.merge(context, writer)

  File file = new File("src/main/java/org/multiverse/stms/beta/transactions/${tx.name}.java")
  file.createNewFile()
  file.text = writer.toString()
}

void generateTranlocal(VelocityEngine engine, TransactionalObject transactionalObject) {
  if (!transactionalObject.isReference) {
    return
  }

  Template t = engine.getTemplate('src/main/java/org/multiverse/stms/beta/transactionalobjects/RefTranlocal.vm')

  VelocityContext context = new VelocityContext()
  context.put("transactionalObject", transactionalObject)

  StringWriter writer = new StringWriter()
  t.merge(context, writer)

  File file = new File('src/main/java/org/multiverse/stms/beta/transactionalobjects', "${transactionalObject.tranlocal}.java")
  file.createNewFile()
  file.text = writer.toString()
}

void generateTransactionalObject(VelocityEngine engine, TransactionalObject transactionalObject) {
  if (!transactionalObject.isReference) {
    return;
  }

  Template t = engine.getTemplate("src/main/java/org/multiverse/stms/beta/transactionalobjects/Ref.vm");

  VelocityContext context = new VelocityContext()
  context.put("transactionalObject", transactionalObject)

  StringWriter writer = new StringWriter()
  t.merge(context, writer)

  File file = new File('src/main/java/org/multiverse/stms/beta/transactionalobjects', "${transactionalObject.name}.java")
  file.createNewFile()
  file.text = writer.toString()
}

