package org.boundbox;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.processing.AbstractProcessor;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

//https://today.java.net/pub/a/today/2008/04/10/source-code-analysis-using-java-6-compiler-apis.html#invoking-the-compiler-from-code-the-java-compiler-api
public class BoundBoxProcessorTest {

    private BoundBoxProcessor boundBoxProcessor;

    @Before
    public void setup() {
        boundBoxProcessor = new BoundBoxProcessor();
        boundBoxProcessor.setBoundboxWriter( EasyMock.createNiceMock(IBoundboxWriter.class));
    }

    // ----------------------------------
    //  FIELDS
    // ----------------------------------
    @Test
    public void testProcess_class_with_single_field() throws URISyntaxException {
        // given
        String[] testSourceFileNames = new String[] { "TestClassWithSingleField.java" };
        CompilationTask task = processAnnotations(testSourceFileNames, boundBoxProcessor);

        // when
        // Perform the compilation task.
        task.call();

        // then
        List<FieldInfo> listFieldInfos = boundBoxProcessor.getBoundClassVisitor().getListFieldInfos();
        assertFalse(listFieldInfos.isEmpty());

        FakeFieldInfo fakeFieldInfo = new FakeFieldInfo("foo", "java.lang.String");
        assertContains(listFieldInfos, fakeFieldInfo);
    }

    @Test
    public void testProcess_class_with_inherited_field() throws URISyntaxException {
        // given
        String[] testSourceFileNames = new String[] { "TestClassWithInheritedField.java", "TestClassWithSingleField.java" };
        CompilationTask task = processAnnotations(testSourceFileNames, boundBoxProcessor);

        // when
        // Perform the compilation task.
        task.call();

        // then
        List<FieldInfo> listFieldInfos = boundBoxProcessor.getBoundClassVisitor().getListFieldInfos();
        assertFalse(listFieldInfos.isEmpty());

        FakeFieldInfo fakeFieldInfo = new FakeFieldInfo("foo", "java.lang.String");
        fakeFieldInfo.setInheritanceLevel(1);
        assertContains(listFieldInfos, fakeFieldInfo);
    }

    // ----------------------------------
    //  CONSTRUCTOR
    // ----------------------------------
    @Test
    public void testProcess_class_with_single_constructor() throws URISyntaxException {
        // given
        String[] testSourceFileNames = new String[] { "TestClassWithSingleConstructor.java" };
        CompilationTask task = processAnnotations(testSourceFileNames, boundBoxProcessor);

        // when
        // Perform the compilation task.
        task.call();

        // then
        List<MethodInfo> listConstructorInfos = boundBoxProcessor.getBoundClassVisitor().getListConstructorInfos();
        assertFalse(listConstructorInfos.isEmpty());
        assertEquals( 1, listConstructorInfos.size());
    }

    // ----------------------------------
    //  METHODS
    // ----------------------------------

    @Test
    public void testProcess_class_with_single_method() throws URISyntaxException {
        // given
        String[] testSourceFileNames = new String[] { "TestClassWithSingleMethod.java" };
        CompilationTask task = processAnnotations(testSourceFileNames, boundBoxProcessor);

        // when
        // Perform the compilation task.
        task.call();

        // then
        List<MethodInfo> listMethodInfos = boundBoxProcessor.getBoundClassVisitor().getListMethodInfos();
        assertFalse(listMethodInfos.isEmpty());
        assertEquals( 1, listMethodInfos.size());
    }

    @Test
    public void testProcess_class_with_may_methods() throws URISyntaxException {
        // given
        String[] testSourceFileNames = new String[] { "TestClassWithManyMethods.java" };
        CompilationTask task = processAnnotations(testSourceFileNames, boundBoxProcessor);

        // when
        // Perform the compilation task.
        task.call();

        // then
        List<MethodInfo> listMethodInfos = boundBoxProcessor.getBoundClassVisitor().getListMethodInfos();
        assertFalse(listMethodInfos.isEmpty());

        FakeMethodInfo fakeMethodInfo = new FakeMethodInfo("simple", "void", new ArrayList<FieldInfo>(), null);
        assertContains(listMethodInfos, fakeMethodInfo);
        
        FieldInfo paramInt = new FakeFieldInfo("a", int.class.getName());
        FakeMethodInfo fakeMethodInfo2 = new FakeMethodInfo("withPrimitiveArgument", "void", Arrays.asList(paramInt), null);
        assertContains(listMethodInfos, fakeMethodInfo2);

        FieldInfo paramObject = new FakeFieldInfo("a", Object.class.getName());
        FakeMethodInfo fakeMethodInfo3 = new FakeMethodInfo("withObjectArgument", "void", Arrays.asList(paramObject), null);
        assertContains(listMethodInfos, fakeMethodInfo3);

        FieldInfo paramObject2 = new FakeFieldInfo("b", Object.class.getName());
        FakeMethodInfo fakeMethodInfo4 = new FakeMethodInfo("withManyArguments", "void", Arrays.asList(paramInt,paramObject2), null);
        assertContains(listMethodInfos, fakeMethodInfo4);
        
        FakeMethodInfo fakeMethodInfo5 = new FakeMethodInfo("withPrimitiveIntReturnType", int.class.getName(), new ArrayList<FieldInfo>(), null);
        assertContains(listMethodInfos, fakeMethodInfo5);
        
        FakeMethodInfo fakeMethodInfo6 = new FakeMethodInfo("withPrimitiveDoubleReturnType", double.class.getName(), new ArrayList<FieldInfo>(), null);
        assertContains(listMethodInfos, fakeMethodInfo6);

        FakeMethodInfo fakeMethodInfo7 = new FakeMethodInfo("withPrimitiveBooleanReturnType", boolean.class.getName(), new ArrayList<FieldInfo>(), null);
        assertContains(listMethodInfos, fakeMethodInfo7);
        
        FakeMethodInfo fakeMethodInfo8= new FakeMethodInfo("withSingleThrownType", "void", new ArrayList<FieldInfo>(), Arrays.asList(IOException.class.getName()));
        assertContains(listMethodInfos, fakeMethodInfo8);

        FakeMethodInfo fakeMethodInfo9 = new FakeMethodInfo("withManyThrownType", "void", new ArrayList<FieldInfo>(), Arrays.asList(IOException.class.getName(), RuntimeException.class.getName()));
        assertContains(listMethodInfos, fakeMethodInfo9);
    }

    // ----------------------------------
    //  PRIVATE METHODS
    // ----------------------------------

    private CompilationTask processAnnotations(String[] testSourceFileNames, BoundBoxProcessor boundBoxProcessor)
            throws URISyntaxException {
        // Get an instance of java compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        // Get a new instance of the standard file manager implementation
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        // Get the list of java file objects, in this case we have only
        // one file, TestClass.java
        // http://stackoverflow.com/a/676102/693752
        List<File> listSourceFiles = new ArrayList<File>();
        for (String sourceFileName : testSourceFileNames) {
            listSourceFiles.add(new File(ClassLoader.getSystemResource(sourceFileName).toURI()));
        }
        Iterable<? extends JavaFileObject> compilationUnits1 = fileManager.getJavaFileObjectsFromFiles(listSourceFiles);

        // Create the compilation task
        CompilationTask task = compiler.getTask(null, fileManager, null, null, null, compilationUnits1);

        // Create a list to hold annotation processors
        LinkedList<AbstractProcessor> processors = new LinkedList<AbstractProcessor>();

        // Add an annotation processor to the list
        processors.add(boundBoxProcessor);

        // Set the annotation processor to the compiler task
        task.setProcessors(processors);
        return task;
    }

    private void assertContains(List<FieldInfo> listFieldInfos, FakeFieldInfo fakeFieldInfo) {
        FieldInfo fieldInfo2 = retrieveFieldInfo(listFieldInfos, fakeFieldInfo);
        assertNotNull(fieldInfo2);
        assertEquals(fakeFieldInfo.getFieldTypeName(), fieldInfo2.getFieldType().toString());
        assertEquals(fakeFieldInfo.getInheritanceLevel(), fieldInfo2.getInheritanceLevel());
    }

    private void assertContains(List<MethodInfo> listMethodInfos, FakeMethodInfo fakeMethodInfo) {
        MethodInfo methodInfo2 = retrieveMethodInfo(listMethodInfos, fakeMethodInfo);
        assertNotNull(methodInfo2);
        assertEquals(fakeMethodInfo.getReturnTypeName(), methodInfo2.getReturnType().toString());
        assertEquals(fakeMethodInfo.getInheritanceLevel(), methodInfo2.getInheritanceLevel());
        for( int indexThrownType =0; indexThrownType < methodInfo2.getThrownTypes().size(); indexThrownType ++ ) {
            TypeMirror thrownType = methodInfo2.getThrownTypes().get(indexThrownType);
            assertEquals(fakeMethodInfo.getListThrownTypeNames().get(indexThrownType), thrownType.toString());
        }
    }

    private FieldInfo retrieveFieldInfo(List<FieldInfo> listFieldInfos, FakeFieldInfo fakeFieldInfo) {
        for (FieldInfo fieldInfo : listFieldInfos) {
            if (fieldInfo.equals(fakeFieldInfo)) {
                return fieldInfo;
            }
        }
        return null;
    }

    private MethodInfo retrieveMethodInfo(List<MethodInfo> listMethodInfos, FakeMethodInfo fakeMethodInfo) {
        for (MethodInfo methodInfo : listMethodInfos) {
            if (methodInfo.equals(fakeMethodInfo)) {
                return methodInfo;
            }
        }
        return null;
    }


}