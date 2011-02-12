import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine

@Grab(group = 'org.apache.velocity', module = 'velocity', version = '1.6.4')

class AtomicClosure {
    String type
    String name
    String typeParameter
}

class AtomicBlock {
    String name
    boolean lean
}

class TransactionalObject {
    String type//the type of data it contains
    String objectType//the type of data it contains
    String initialValue//the initial value
    String typeParameter
//  String parametrizedTranlocal
    String functionClass//the class of the callable used for commuting operations
    String incFunctionMethod
    boolean isReference
    String referenceInterface
    boolean isNumber
    String predicateClass
}

VelocityEngine engine = new VelocityEngine();
engine.init();

def refs = createTransactionalObjects();
def atomicClosures = createClosures();
def atomicBlocks = [new AtomicBlock(name: 'FatGammaAtomicBlock', lean: false),
        new AtomicBlock(name: 'LeanGammaAtomicBlock', lean: true)]

generateRefFactory(engine, refs);

for (def param in refs) {
    generateRefs(engine, param)
    generatePredicate(engine, param)
    generateFunction(engine, param)
}

for (def closure in atomicClosures) {
    generateAtomicClosure(engine, closure)
}

generateAtomicBlock(engine, atomicClosures)
//generateOrElseBlock(engine, atomicClosures)
generateGammaOrElseBlock(engine, atomicClosures)
//generateStmUtils(engine, atomicClosures)

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
            type: 'E',
            objectType: '',
            typeParameter: '<E>',
            initialValue: 'null',
            referenceInterface: 'Ref',
            functionClass: 'Function',
            isReference: true,
            isNumber: false,
            predicateClass: "Predicate",
            incFunctionMethod: '')
    result.add new TransactionalObject(
            type: 'int',
            objectType: 'Integer',
            referenceInterface: 'IntRef',
            typeParameter: '',
            initialValue: '0',
            functionClass: 'IntFunction',
            isReference: true,
            isNumber: true,
            predicateClass: "IntPredicate",
            incFunctionMethod: 'newIncIntFunction')
    result.add new TransactionalObject(
            type: 'boolean',
            objectType: 'Boolean',
            referenceInterface: 'BooleanRef',
            typeParameter: '',
            initialValue: 'false',
            functionClass: 'BooleanFunction',
            isReference: true,
            isNumber: false,
            predicateClass: "BooleanPredicate",
            incFunctionMethod: '')
    result.add new TransactionalObject(
            type: 'double',
            objectType: 'Double',
            referenceInterface: 'DoubleRef',
            typeParameter: '',
            initialValue: '0',
            functionClass: 'DoubleFunction',
            isReference: true,
            isNumber: true,
            predicateClass: "DoublePredicate",
            incFunctionMethod: '')
    result.add new TransactionalObject(
            referenceInterface: 'LongRef',
            type: 'long',
            objectType: 'Long',
            typeParameter: '',
            initialValue: '0',
            functionClass: 'LongFunction',
            isReference: true,
            isNumber: true,
            predicateClass: "LongPredicate",
            incFunctionMethod: 'newIncLongFunction')
    result.add new TransactionalObject(
            type: '',
            objectType: '',
            typeParameter: '',
            initialValue: '',
            functionClass: 'Function',
            referenceInterface: '',
            isReference: false,
            isNumber: false,
            predicateClass: "",
            incFunctionMethod: '')
    result
}

void generateAtomicClosure(VelocityEngine engine, AtomicClosure closure) {
    Template t = engine.getTemplate('src/main/java/org/multiverse/api/closures/AtomicClosure.vm')

    VelocityContext context = new VelocityContext()
    context.put('closure', closure)

    StringWriter writer = new StringWriter()
    t.merge(context, writer)

    File file = new File("src/main/java/org/multiverse/api/closures/${closure.name}.java")
    file.createNewFile()
    file.text = writer.toString()
}

void generateBetaAtomicBlock(VelocityEngine engine, AtomicBlock atomicBlock, List<AtomicClosure> closures) {
    Template t = engine.getTemplate('src/main/java/org/multiverse/stms/gamma/GammaAtomicBlock.vm')

    VelocityContext context = new VelocityContext()
    context.put('atomicBlock', atomicBlock)
    context.put('closures', closures)

    StringWriter writer = new StringWriter()
    t.merge(context, writer)

    File file = new File("src/main/java/org/multiverse/stms/gamma/${atomicBlock.name}.java")
    file.createNewFile()
    file.text = writer.toString()
}

void generateAtomicBlock(VelocityEngine engine, List<AtomicClosure> closures) {
    Template t = engine.getTemplate('src/main/java/org/multiverse/api/AtomicBlock.vm')

    VelocityContext context = new VelocityContext()
    context.put('closures', closures)

    StringWriter writer = new StringWriter()
    t.merge(context, writer)

    File file = new File('src/main/java/org/multiverse/api/AtomicBlock.java')
    file.createNewFile()
    file.text = writer.toString()
}

void generateOrElseBlock(VelocityEngine engine, List<AtomicClosure> closures) {
    Template t = engine.getTemplate('src/main/java/org/multiverse/api/OrElseBlock.vm')

    VelocityContext context = new VelocityContext()
    context.put('closures', closures)

    StringWriter writer = new StringWriter()
    t.merge(context, writer)

    File file = new File('src/main/java/org/multiverse/api/OrElseBlock.java')
    file.createNewFile()
    file.text = writer.toString()
}

void generateGammaOrElseBlock(VelocityEngine engine, List<AtomicClosure> closures) {
    Template t = engine.getTemplate('src/main/java/org/multiverse/stms/gamma/GammaOrElseBlock.vm')

    VelocityContext context = new VelocityContext()
    context.put('closures', closures)

    StringWriter writer = new StringWriter()
    t.merge(context, writer)

    File file = new File('src/main/java/org/multiverse/stms/gamma/GammaOrElseBlock.java')
    file.createNewFile()
    file.text = writer.toString()
}

void generateStmUtils(VelocityEngine engine, List<AtomicClosure> closures) {
    Template t = engine.getTemplate('src/main/java/org/multiverse/api/StmUtils.vm')

    VelocityContext context = new VelocityContext()
    context.put('closures', closures)

    StringWriter writer = new StringWriter()
    t.merge(context, writer)

    File file = new File('src/main/java/org/multiverse/api/StmUtils.java')
    file.createNewFile()
    file.text = writer.toString()
}

void generatePredicate(VelocityEngine engine, TransactionalObject transactionalObject) {
    if (!transactionalObject.isReference) {
        return
    }

    Template t = engine.getTemplate('src/main/java/org/multiverse/api/predicates/Predicate.vm')

    VelocityContext context = new VelocityContext()
    context.put('transactionalObject', transactionalObject)

    StringWriter writer = new StringWriter()
    t.merge(context, writer)

    File file = new File('src/main/java/org/multiverse/api/predicates/', "${transactionalObject.predicateClass}.java")
    file.createNewFile()
    file.text = writer.toString()
}

void generateFunction(VelocityEngine engine, TransactionalObject transactionalObject) {
    if (!transactionalObject.isReference) {
        return
    }

    Template t = engine.getTemplate('src/main/java/org/multiverse/api/functions/Function.vm')

    VelocityContext context = new VelocityContext()
    context.put('transactionalObject', transactionalObject)

    StringWriter writer = new StringWriter()
    t.merge(context, writer)

    File file = new File('src/main/java/org/multiverse/api/functions/', "${transactionalObject.functionClass}.java")
    file.createNewFile()
    file.text = writer.toString()
}

void generateRefs(VelocityEngine engine, TransactionalObject transactionalObject) {
    if (!transactionalObject.isReference) {
        return;
    }

    Template t = engine.getTemplate('src/main/java/org/multiverse/api/references/Ref.vm');

    VelocityContext context = new VelocityContext()
    context.put('transactionalObject', transactionalObject)

    StringWriter writer = new StringWriter()
    t.merge(context, writer)

    File file = new File('src/main/java/org/multiverse/api/references/', "${transactionalObject.referenceInterface}.java")
    file.createNewFile()
    file.text = writer.toString()
}

void generateRefFactory(VelocityEngine engine, List<TransactionalObject> refs) {
    Template t = engine.getTemplate('src/main/java/org/multiverse/api/references/RefFactory.vm');

    VelocityContext context = new VelocityContext()
    context.put('refs', refs)

    StringWriter writer = new StringWriter()
    t.merge(context, writer)

    File file = new File('src/main/java/org/multiverse/api/references/RefFactory.java')
    file.createNewFile()
    file.text = writer.toString()
}

